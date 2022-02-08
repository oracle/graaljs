/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.function;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.InitializeInstanceElementsNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.control.ResumableNode;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * ES6 14.5.14 Runtime Semantics: ClassDefinitionEvaluation.
 */
public final class ClassDefinitionNode extends NamedEvaluationTargetNode implements FunctionNameHolder, ResumableNode.WithObjectState {

    private final JSContext context;
    @Child private JavaScriptNode constructorFunctionNode;
    @Child private JavaScriptNode classHeritageNode;
    @Children private final ObjectLiteralMemberNode[] memberNodes;

    @Child private JSWriteFrameSlotNode writeClassBindingNode;
    @Child private PropertyGetNode getPrototypeNode;
    @Child private CreateMethodPropertyNode setConstructorNode;
    @Child private CreateObjectNode.CreateObjectWithPrototypeNode createPrototypeNode;
    @Child private DefineMethodNode defineConstructorMethodNode;
    @Child private PropertySetNode setFieldsNode;
    @Child private InitializeInstanceElementsNode staticElementsNode;
    @Child private PropertySetNode setPrivateBrandNode;
    @Child private SetFunctionNameNode setFunctionName;
    @Child private IsConstructorNode isConstructorNode;
    private final BranchProfile errorBranch = BranchProfile.create();

    private final boolean hasName;
    private final int instanceFieldCount;
    private final int staticElementCount;

    protected ClassDefinitionNode(JSContext context, JSFunctionExpressionNode constructorFunctionNode, JavaScriptNode classHeritageNode, ObjectLiteralMemberNode[] memberNodes,
                    JSWriteFrameSlotNode writeClassBindingNode, boolean hasName, int instanceFieldCount, int staticElementCount, boolean hasPrivateInstanceMethods, int blockScopeSlot) {
        this.context = context;
        this.constructorFunctionNode = constructorFunctionNode;
        this.classHeritageNode = classHeritageNode;
        this.memberNodes = memberNodes;
        this.hasName = hasName;
        this.instanceFieldCount = instanceFieldCount;
        this.staticElementCount = staticElementCount;

        this.writeClassBindingNode = writeClassBindingNode;
        this.getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);
        this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        this.createPrototypeNode = CreateObjectNode.createOrdinaryWithPrototype(context);
        this.defineConstructorMethodNode = DefineMethodNode.create(context, constructorFunctionNode, blockScopeSlot);
        this.setFieldsNode = instanceFieldCount != 0 ? PropertySetNode.createSetHidden(JSFunction.CLASS_FIELDS_ID, context) : null;
        this.setPrivateBrandNode = hasPrivateInstanceMethods ? PropertySetNode.createSetHidden(JSFunction.PRIVATE_BRAND_ID, context) : null;
        this.setFunctionName = hasName ? null : SetFunctionNameNode.create();
        this.isConstructorNode = IsConstructorNode.create();
    }

    public static ClassDefinitionNode create(JSContext context, JSFunctionExpressionNode constructorFunction, JavaScriptNode classHeritage, ObjectLiteralMemberNode[] members,
                    JSWriteFrameSlotNode writeClassBinding, boolean hasName, int instanceFieldCount, int staticFieldCount, boolean hasPrivateInstanceMethods, JSFrameSlot blockScopeSlot) {
        return new ClassDefinitionNode(context, constructorFunction, classHeritage, members, writeClassBinding, hasName, instanceFieldCount, staticFieldCount, hasPrivateInstanceMethods,
                        blockScopeSlot != null ? blockScopeSlot.getIndex() : -1);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        return executeWithName(frame, null);
    }

    @Override
    public Object resume(VirtualFrame frame, int stateSlot) {
        Object maybeState = getState(frame, stateSlot);
        ClassDefinitionResumptionRecord resumptionRecord = null;
        if (maybeState instanceof ClassDefinitionResumptionRecord) {
            resumptionRecord = (ClassDefinitionResumptionRecord) maybeState;
        }
        return executeWithName(frame, null, resumptionRecord, stateSlot);
    }

    @Override
    public DynamicObject executeWithName(VirtualFrame frame, Object className) {
        return executeWithName(frame, className, null, -1);
    }

    private DynamicObject executeWithName(VirtualFrame frame, Object className, ClassDefinitionResumptionRecord resumptionRecord, int stateSlot) {
        DynamicObject proto;
        DynamicObject constructor;
        Object[][] instanceFields;
        Object[][] staticElements;
        int instanceFieldIndex;
        int staticElementIndex;
        int startIndex;
        if (resumptionRecord == null) {
            JSRealm realm = getRealm();
            Object protoParent = realm.getObjectPrototype();
            Object constructorParent = realm.getFunctionPrototype();
            if (classHeritageNode != null) {
                Object superclass = classHeritageNode.execute(frame);
                if (superclass == Null.instance) {
                    protoParent = Null.instance;
                } else if (!isConstructorNode.executeBoolean(superclass)) {
                    // 6.f. if IsConstructor(superclass) is false, throw a TypeError.
                    errorBranch.enter();
                    throw Errors.createTypeError("not a constructor", this);
                } else if (JSRuntime.isGenerator(superclass)) {
                    // 6.g.i. if superclass.[[FunctionKind]] is "generator", throw a TypeError
                    errorBranch.enter();
                    throw Errors.createTypeError("class cannot extend a generator function", this);
                } else {
                    protoParent = getPrototypeNode.getValue(superclass);
                    if (protoParent != Null.instance && !JSRuntime.isObject(protoParent)) {
                        errorBranch.enter();
                        throw Errors.createTypeError("protoParent is neither Object nor Null", this);
                    }
                    constructorParent = superclass;
                }
            }

            /* Let proto be ObjectCreate(protoParent). */
            assert protoParent == Null.instance || JSRuntime.isObject(protoParent);
            proto = createPrototypeNode.execute(frame, ((DynamicObject) protoParent));

            /*
             * Let constructorInfo be the result of performing DefineMethod for constructor with
             * arguments proto and constructorParent as the optional functionPrototype argument.
             */
            constructor = defineConstructorMethodNode.execute(frame, proto, (DynamicObject) constructorParent);

            // Perform MakeConstructor(F, writablePrototype=false, proto).
            JSFunction.setClassPrototype(constructor, proto);

            // If className is not undefined, perform SetFunctionName(F, className).
            if (setFunctionName != null && className != null) {
                setFunctionName.execute(constructor, className);
            }

            // Perform CreateMethodProperty(proto, "constructor", F).
            setConstructorNode.executeVoid(proto, constructor);

            instanceFields = instanceFieldCount == 0 ? null : new Object[instanceFieldCount][];
            staticElements = staticElementCount == 0 ? null : new Object[staticElementCount][];
            instanceFieldIndex = 0;
            staticElementIndex = 0;
            startIndex = 0;
        } else {
            proto = resumptionRecord.proto;
            constructor = resumptionRecord.constructor;
            instanceFields = resumptionRecord.instanceFields;
            staticElements = resumptionRecord.staticElements;
            instanceFieldIndex = resumptionRecord.instanceFieldIndex;
            staticElementIndex = resumptionRecord.staticElementIndex;
            startIndex = resumptionRecord.index;
        }

        initializeMembers(frame, proto, constructor, instanceFields, staticElements, startIndex, instanceFieldIndex, staticElementIndex, stateSlot);

        if (writeClassBindingNode != null) {
            writeClassBindingNode.executeWrite(frame, constructor);
        }

        if (setFieldsNode != null) {
            setFieldsNode.setValue(constructor, instanceFields);
        }

        // If the class contains a private instance method or accessor, set F.[[PrivateBrand]].
        if (setPrivateBrandNode != null) {
            HiddenKey privateBrand = new HiddenKey("Brand");
            setPrivateBrandNode.setValue(constructor, privateBrand);
        }

        if (staticElementCount != 0) {
            InitializeInstanceElementsNode initializeStaticElements = this.staticElementsNode;
            if (initializeStaticElements == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.staticElementsNode = initializeStaticElements = insert(InitializeInstanceElementsNode.create(context));
            }
            initializeStaticElements.executeStaticElements(constructor, staticElements);
        }

        return constructor;
    }

    @ExplodeLoop
    private void initializeMembers(VirtualFrame frame, DynamicObject proto, DynamicObject constructor, Object[][] instanceFields, Object[][] staticElements, int startIndex, int instanceFieldIdx,
                    int staticElementIdx, int stateSlot) {
        /* For each ClassElement e in order from NonConstructorMethodDefinitions of ClassBody */
        int instanceFieldIndex = instanceFieldIdx;
        int staticElementIndex = staticElementIdx;
        int i = 0;
        try {
            for (; i < memberNodes.length; i++) {
                if (i >= startIndex) {
                    ObjectLiteralMemberNode memberNode = memberNodes[i];
                    DynamicObject homeObject = memberNode.isStatic() ? constructor : proto;
                    memberNode.executeVoid(frame, homeObject, context);
                    if (memberNode.isFieldOrStaticBlock()) {
                        Object key = memberNode.evaluateKey(frame);
                        Object value = memberNode.evaluateValue(frame, homeObject);
                        Object[] field = new Object[]{key, value, memberNode.isAnonymousFunctionDefinition()};
                        if (memberNode.isStatic()) {
                            staticElements[staticElementIndex++] = field;
                        } else if (instanceFields != null) {
                            instanceFields[instanceFieldIndex++] = field;
                        } else {
                            throw Errors.shouldNotReachHere();
                        }
                    }
                }
            }
        } catch (YieldException e) {
            setState(frame, stateSlot, new ClassDefinitionResumptionRecord(proto, constructor, instanceFields, staticElements, instanceFieldIndex, staticElementIndex, i));
            throw e;
        }
        assert instanceFieldIndex == instanceFieldCount && staticElementIndex == staticElementCount;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    @Override
    public String getFunctionName() {
        return hasName ? ((FunctionNameHolder) constructorFunctionNode).getFunctionName() : "";
    }

    @Override
    public void setFunctionName(String name) {
        ((FunctionNameHolder) constructorFunctionNode).setFunctionName(name);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new ClassDefinitionNode(context, (JSFunctionExpressionNode) cloneUninitialized(constructorFunctionNode, materializedTags), cloneUninitialized(classHeritageNode, materializedTags),
                        ObjectLiteralMemberNode.cloneUninitialized(memberNodes, materializedTags),
                        cloneUninitialized(writeClassBindingNode, materializedTags), hasName, instanceFieldCount, staticElementCount, setPrivateBrandNode != null,
                        defineConstructorMethodNode.getBlockScopeSlot());
    }

    static class ClassDefinitionResumptionRecord {
        final DynamicObject proto;
        final DynamicObject constructor;
        final Object[][] instanceFields;
        final Object[][] staticElements;
        final int instanceFieldIndex;
        final int staticElementIndex;
        final int index;

        ClassDefinitionResumptionRecord(
                        DynamicObject proto,
                        DynamicObject constructor,
                        Object[][] instanceFields,
                        Object[][] staticElements,
                        int instanceFieldIndex,
                        int staticElementIndex,
                        int index) {
            this.proto = proto;
            this.constructor = constructor;
            this.instanceFields = instanceFields;
            this.staticElements = staticElements;
            this.instanceFieldIndex = instanceFieldIndex;
            this.staticElementIndex = staticElementIndex;
            this.index = index;
        }
    }

}

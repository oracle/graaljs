/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.decorators.ApplyDecoratorsToClassDefinitionNode;
import com.oracle.truffle.js.decorators.ApplyDecoratorsToElementDefinition;
import com.oracle.truffle.js.decorators.DecoratorListEvaluationNode;
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
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * ES6 14.5.14 Runtime Semantics: ClassDefinitionEvaluation.
 */
public final class ClassDefinitionNode extends NamedEvaluationTargetNode implements FunctionNameHolder, ResumableNode.WithObjectState {

    @Children private JavaScriptNode[] classDecorators;
    @Children private ObjectLiteralMemberNode[] memberNodes;
    @Children private DecoratorListEvaluationNode[] memberDecorators;
    @Children private ApplyDecoratorsToElementDefinition[] applyDecoratorsToElementDefinition;

    @Child private JavaScriptNode constructorFunctionNode;
    @Child private JavaScriptNode classHeritageNode;

    @Child private ApplyDecoratorsToClassDefinitionNode decorateClassDefinition;

    @Child private JSWriteFrameSlotNode writeClassBindingNode;
    @Child private JSWriteFrameSlotNode writeInternalConstructorBrand;
    @Child private PropertyGetNode getPrototypeNode;
    @Child private CreateMethodPropertyNode setConstructorNode;
    @Child private CreateObjectNode.CreateObjectWithPrototypeNode createPrototypeNode;
    @Child private DefineMethodNode defineConstructorMethodNode;
    @Child private PropertySetNode setElementsNode;
    @Child private PropertySetNode setInitializersNode;
    @Child private InitializeInstanceElementsNode staticElementsNode;
    @Child private PropertySetNode setPrivateBrandNode;
    @Child private SetFunctionNameNode setFunctionName;
    @Child private IsConstructorNode isConstructorNode;
    @Child private JSFunctionCallNode staticExtraInitializersCallNode;

    private final JSContext context;
    private final TruffleString className;
    private final boolean hasName;
    private final int instanceElementCount;
    private final int staticElementCount;

    private final BranchProfile errorBranch = BranchProfile.create();

    protected ClassDefinitionNode(JSContext context, JSFunctionExpressionNode constructorFunctionNode, JavaScriptNode classHeritageNode, ObjectLiteralMemberNode[] memberNodes,
                    JSWriteFrameSlotNode writeClassBindingNode, JSWriteFrameSlotNode writeInternalConstructorBrand, JavaScriptNode[] classDecorators, DecoratorListEvaluationNode[] memberDecorators,
                    TruffleString className, int instanceElementsCount, int staticElementCount, boolean hasPrivateInstanceMethods, boolean hasInstanceFieldsOrAccessors, int blockScopeSlot) {

        this.context = context;
        this.constructorFunctionNode = constructorFunctionNode;
        this.classHeritageNode = classHeritageNode;
        this.memberNodes = memberNodes;
        this.className = className;
        this.hasName = className != null;
        this.instanceElementCount = instanceElementsCount;
        this.staticElementCount = staticElementCount;
        assert staticElementCount + instanceElementsCount == memberNodes.length;

        this.writeClassBindingNode = writeClassBindingNode;
        this.writeInternalConstructorBrand = writeInternalConstructorBrand;
        this.getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);
        this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        this.createPrototypeNode = CreateObjectNode.createOrdinaryWithPrototype(context);
        this.defineConstructorMethodNode = DefineMethodNode.create(context, constructorFunctionNode, blockScopeSlot);
        this.setElementsNode = hasInstanceFieldsOrAccessors ? PropertySetNode.createSetHidden(JSFunction.CLASS_ELEMENTS_ID, context) : null;
        this.setPrivateBrandNode = hasPrivateInstanceMethods ? PropertySetNode.createSetHidden(JSFunction.PRIVATE_BRAND_ID, context) : null;
        this.setFunctionName = hasName ? null : SetFunctionNameNode.create();
        this.isConstructorNode = IsConstructorNode.create();
        this.classDecorators = classDecorators;
        this.memberDecorators = memberDecorators;
        this.setInitializersNode = PropertySetNode.createSetHidden(JSFunction.CLASS_INITIALIZERS_ID, context);
        this.applyDecoratorsToElementDefinition = initApplyDecoratorsToElementDefinitionNodes(context, memberNodes, memberDecorators);
        this.staticExtraInitializersCallNode = JSFunctionCallNode.createCall();
    }

    public static ClassDefinitionNode create(JSContext context, JSFunctionExpressionNode constructorFunction, JavaScriptNode classHeritage, ObjectLiteralMemberNode[] members,
                    JSWriteFrameSlotNode writeClassBinding, JSWriteFrameSlotNode writeInternalConstructorBrand, TruffleString className, JavaScriptNode[] classDecorators,
                    DecoratorListEvaluationNode[] memberDecorators, int instanceFieldCount, int staticElementCount, boolean hasPrivateInstanceMethods, boolean hasInstanceFieldsOrAccessors,
                    JSFrameSlot blockScopeSlot) {
        return new ClassDefinitionNode(context, constructorFunction, classHeritage, members, writeClassBinding, writeInternalConstructorBrand, classDecorators, memberDecorators, className,
                        instanceFieldCount, staticElementCount, hasPrivateInstanceMethods, hasInstanceFieldsOrAccessors,
                        blockScopeSlot != null ? blockScopeSlot.getIndex() : -1);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeWithName(frame, className);
    }

    @Override
    public Object resume(VirtualFrame frame, int stateSlot) {
        Object maybeState = getState(frame, stateSlot);
        ClassDefinitionResumptionRecord resumptionRecord = null;
        if (maybeState instanceof ClassDefinitionResumptionRecord) {
            resetState(frame, stateSlot);
            resumptionRecord = (ClassDefinitionResumptionRecord) maybeState;
        }
        return executeWithName(frame, className, resumptionRecord, stateSlot);
    }

    @Override
    public Object executeWithName(VirtualFrame frame, Object name) {
        return executeWithName(frame, name, null, -1);
    }

    private Object executeWithName(VirtualFrame frame, Object name, ClassDefinitionResumptionRecord resumptionRecord, int stateSlot) {
        JSObject proto;
        JSFunctionObject constructor;
        Object[] decorators;
        ClassElementDefinitionRecord[] instanceElements;
        ClassElementDefinitionRecord[] staticElements;
        int instanceElementIndex;
        int staticElementIndex;
        int startIndex;
        JSRealm realm = getRealm();
        if (resumptionRecord == null) {
            JSDynamicObject protoParent = realm.getObjectPrototype();
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
                    var uncheckedProtoParent = getPrototypeNode.getValue(superclass);
                    if (uncheckedProtoParent != Null.instance && !JSRuntime.isObject(uncheckedProtoParent)) {
                        errorBranch.enter();
                        throw Errors.createTypeError("protoParent is neither Object nor Null", this);
                    }
                    protoParent = (JSDynamicObject) uncheckedProtoParent;
                    constructorParent = superclass;
                }
            }

            decorators = classDecoratorListEvaluation(frame);

            /* Let proto be ObjectCreate(protoParent). */
            assert protoParent == Null.instance || JSRuntime.isObject(protoParent);
            proto = createPrototypeNode.execute(protoParent);

            // Prototypes derived from Array.prototype should have been marked as such.
            assert !JSShape.isArrayPrototypeOrDerivative(protoParent) || JSShape.isArrayPrototypeOrDerivative(proto);

            /*
             * Let constructorInfo be the result of performing DefineMethod for constructor with
             * arguments proto and constructorParent as the optional functionPrototype argument.
             */
            constructor = defineConstructorMethodNode.execute(frame, proto, (JSDynamicObject) constructorParent);

            // Perform MakeConstructor(F, writablePrototype=false, proto).
            JSFunction.setClassPrototype(constructor, proto);

            // If className is not undefined, perform SetFunctionName(F, className).
            if (setFunctionName != null && name != null) {
                setFunctionName.execute(constructor, name);
            }

            // Perform CreateMethodProperty(proto, "constructor", F).
            setConstructorNode.executeVoid(proto, constructor);

            instanceElements = instanceElementCount == 0 ? null : new ClassElementDefinitionRecord[instanceElementCount];
            staticElements = staticElementCount == 0 ? null : new ClassElementDefinitionRecord[staticElementCount];
            instanceElementIndex = 0;
            staticElementIndex = 0;
            startIndex = 0;
        } else {
            proto = resumptionRecord.proto;
            constructor = resumptionRecord.constructor;

            instanceElements = resumptionRecord.instanceElements;
            staticElements = resumptionRecord.staticElements;

            instanceElementIndex = resumptionRecord.instanceElementIndex;
            staticElementIndex = resumptionRecord.staticElementIndex;
            startIndex = resumptionRecord.startIndex;

            decorators = resumptionRecord.decorators;
        }

        return defineClassElements(frame, proto, constructor, decorators,
                        instanceElements,
                        staticElements,
                        startIndex,
                        instanceElementIndex,
                        staticElementIndex,
                        stateSlot,
                        realm);
    }

    private Object defineClassElements(VirtualFrame frame, JSObject proto, JSFunctionObject constructor, Object[] decorators, ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] staticElements, int startIndex, int instanceElementIndex,
                    int staticElementIndex, int stateSlot, JSRealm realm) {
        initializeMembers(frame, proto, constructor,
                        instanceElements,
                        staticElements,
                        startIndex,
                        instanceElementIndex,
                        staticElementIndex,
                        stateSlot,
                        realm);

        SimpleArrayList<Object> staticExtraInitializers = SimpleArrayList.createEmpty();
        SimpleArrayList<Object> instanceExtraInitializers = SimpleArrayList.createEmpty();
        applyDecoratorsAndDefineMethods(frame,
                        instanceElements,
                        instanceExtraInitializers,
                        staticExtraInitializers,
                        staticElements,
                        constructor,
                        proto);

        if (setElementsNode != null) {
            setElementsNode.setValue(constructor, instanceElements);
        }
        setInitializersNode.setValue(constructor, instanceExtraInitializers.toArray());

        // If the class contains a private instance method or accessor, set F.[[PrivateBrand]].
        if (setPrivateBrandNode != null) {
            HiddenKey privateBrand = new HiddenKey("Brand");
            setPrivateBrandNode.setValue(constructor, privateBrand);
        }

        // internal constructor binding used for private brand checks.
        // Should set before static blocks execution.
        if (writeInternalConstructorBrand != null) {
            writeInternalConstructorBrand.executeWrite(frame, constructor);
        }

        SimpleArrayList<Object> classExtraInitializers = SimpleArrayList.createEmpty();
        Object newConstructor = applyDecoratorsToClassDefinition(frame, getClassName(), constructor, decorators, classExtraInitializers);

        if (writeClassBindingNode != null) {
            writeClassBindingNode.executeWrite(frame, newConstructor);
        }

        executeStaticExtraInitializers(newConstructor, staticExtraInitializers.toArray());

        if (staticElementCount != 0) {
            InitializeInstanceElementsNode initializeStaticElements = this.staticElementsNode;
            if (initializeStaticElements == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.staticElementsNode = initializeStaticElements = insert(InitializeInstanceElementsNode.create(context));
            }
            initializeStaticElements.executeStaticElements(newConstructor, staticElements);
        }

        executeStaticExtraInitializers(newConstructor, classExtraInitializers.toArray());
        return newConstructor;
    }

    private void applyDecoratorsAndDefineMethods(VirtualFrame frame,
                    ClassElementDefinitionRecord[] instanceElements,
                    SimpleArrayList<Object> instanceExtraInitializers,
                    SimpleArrayList<Object> staticExtraInitializers,
                    ClassElementDefinitionRecord[] staticElements,
                    JSObject constructor,
                    JSObject proto) {
        applyDecoratorsAndDefineMethods(frame, constructor, staticElements, staticExtraInitializers, true);
        applyDecoratorsAndDefineMethods(frame, proto, instanceElements, instanceExtraInitializers, false);
        applyDecoratorsToElements(frame, constructor, staticElements, staticExtraInitializers, true);
        applyDecoratorsToElements(frame, proto, instanceElements, instanceExtraInitializers, false);
    }

    private void executeStaticExtraInitializers(Object target, Object[] initializers) {
        for (Object initializer : initializers) {
            staticExtraInitializersCallNode.executeCall(JSArguments.createZeroArg(target, initializer));
        }
    }

    @ExplodeLoop
    private void applyDecoratorsAndDefineMethods(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord[] elements, SimpleArrayList<Object> extraInitializers, boolean isStatic) {
        if (elements == null) {
            return;
        }
        CompilerAsserts.partialEvaluationConstant(memberNodes.length);
        int elementIndex = 0;
        for (int i = 0; i < memberNodes.length; i++) {
            ObjectLiteralMemberNode member = memberNodes[i];
            if (member.isStatic() == isStatic) {
                if (!member.isFieldOrStaticBlock()) {
                    ClassElementDefinitionRecord m = elements[elementIndex];
                    if (applyDecoratorsToElementDefinition != null && applyDecoratorsToElementDefinition[i] != null) {
                        applyDecoratorsToElementDefinition[i].executeDecorator(frame, homeObject, m, extraInitializers);
                    }
                    member.defineClassElement(frame, homeObject, m);
                }
                elementIndex++;
            }
        }
        assert elementIndex == elements.length;
    }

    @ExplodeLoop
    private void applyDecoratorsToElements(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord[] elements, SimpleArrayList<Object> extraInitializers, boolean isStatic) {
        if (elements == null) {
            return;
        }
        CompilerAsserts.partialEvaluationConstant(memberNodes.length);
        int elementIndex = 0;
        for (int i = 0; i < memberNodes.length; i++) {
            ObjectLiteralMemberNode member = memberNodes[i];
            if (member.isStatic() == isStatic) {
                if (member.isFieldOrStaticBlock()) {
                    ClassElementDefinitionRecord f = elements[elementIndex];
                    if (applyDecoratorsToElementDefinition != null && applyDecoratorsToElementDefinition[i] != null) {
                        applyDecoratorsToElementDefinition[i].executeDecorator(frame, homeObject, f, extraInitializers);
                    }
                }
                elementIndex++;
            }
        }
        assert elementIndex == elements.length;
    }

    private static ApplyDecoratorsToElementDefinition[] initApplyDecoratorsToElementDefinitionNodes(JSContext context,
                    ObjectLiteralMemberNode[] memberNodes, DecoratorListEvaluationNode[] memberDecorators) {
        CompilerAsserts.neverPartOfCompilation();
        if (memberDecorators == null || memberDecorators.length == 0) {
            return null;
        }
        assert memberNodes.length == memberDecorators.length;
        int size = memberNodes.length;
        ApplyDecoratorsToElementDefinition[] nodes = new ApplyDecoratorsToElementDefinition[size];
        for (int i = 0; i < size; i++) {
            if (memberDecorators[i] != null) {
                ObjectLiteralMemberNode memberNode = memberNodes[i];
                nodes[i] = ApplyDecoratorsToElementDefinition.create(context, memberNode);
            }
        }
        return nodes;
    }

    @ExplodeLoop
    private Object[] classDecoratorListEvaluation(VirtualFrame frame) {
        CompilerAsserts.partialEvaluationConstant(classDecorators.length);
        Object[] decorators = new Object[classDecorators.length];
        for (int i = 0; i < classDecorators.length; i++) {
            Object maybeDecorator = classDecorators[i].execute(frame);
            decorators[decorators.length - i - 1] = maybeDecorator;
        }
        return decorators;
    }

    private Object applyDecoratorsToClassDefinition(VirtualFrame frame, Object name, JSObject constructor, Object[] decorators, SimpleArrayList<Object> classExtraInitializers) {
        if (this.classDecorators.length == 0) {
            return constructor;
        }
        if (decorateClassDefinition == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            decorateClassDefinition = insert(ApplyDecoratorsToClassDefinitionNode.create(context));
        }
        return decorateClassDefinition.executeDecorators(frame, name, constructor, decorators, classExtraInitializers);
    }

    @ExplodeLoop
    private void initializeMembers(VirtualFrame frame, JSObject proto, JSFunctionObject constructor, ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] staticElements, int startIndex, int instanceElementsIdx,
                    int staticElementIdx, int stateSlot, JSRealm realm) {
        /* For each ClassElement e in order from NonConstructorMethodDefinitions of ClassBody */
        int instanceElementIndex = instanceElementsIdx;
        int staticElementIndex = staticElementIdx;
        Object[] decorators = null;
        int i = 0;
        try {
            for (; i < memberNodes.length; i++) {
                if (i >= startIndex) {
                    ObjectLiteralMemberNode memberNode = memberNodes[i];
                    boolean isStatic = memberNode.isStatic();
                    JSObject homeObject = isStatic ? constructor : proto;
                    decorators = memberDecorators != null && memberDecorators[i] != null ? memberDecorators[i].execute(frame) : null;
                    ClassElementDefinitionRecord classElementDef = memberNode.evaluateClassElementDefinition(frame, homeObject, realm, decorators);
                    if (isStatic) {
                        staticElements[staticElementIndex++] = classElementDef;
                    } else {
                        instanceElements[instanceElementIndex++] = classElementDef;
                    }
                }
            }
        } catch (YieldException e) {
            setState(frame, stateSlot, new ClassDefinitionResumptionRecord(
                            proto,
                            constructor,
                            instanceElements,
                            staticElements,
                            instanceElementIndex,
                            staticElementIndex,
                            decorators,
                            i));
            throw e;
        }
        assert instanceElementIndex == instanceElementCount && staticElementIndex == staticElementCount;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == JSDynamicObject.class;
    }

    @Override
    public TruffleString getFunctionName() {
        return hasName ? ((FunctionNameHolder) constructorFunctionNode).getFunctionName() : Strings.EMPTY_STRING;
    }

    public TruffleString getClassName() {
        return hasName ? className : Strings.EMPTY_STRING;
    }

    @Override
    public void setFunctionName(TruffleString name) {
        ((FunctionNameHolder) constructorFunctionNode).setFunctionName(name);
    }

    public CreateObjectNode.CreateObjectWithPrototypeNode getCreatePrototypeNode() {
        return createPrototypeNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new ClassDefinitionNode(context,
                        (JSFunctionExpressionNode) cloneUninitialized(constructorFunctionNode, materializedTags),
                        cloneUninitialized(classHeritageNode, materializedTags),
                        ObjectLiteralMemberNode.cloneUninitialized(memberNodes, materializedTags),
                        cloneUninitialized(writeClassBindingNode, materializedTags),
                        cloneUninitialized(writeInternalConstructorBrand, materializedTags),
                        cloneUninitialized(classDecorators, materializedTags),
                        cloneUninitialized(memberDecorators, materializedTags),
                        className,
                        instanceElementCount,
                        staticElementCount,
                        setPrivateBrandNode != null,
                        setElementsNode != null,
                        defineConstructorMethodNode.getBlockScopeSlot());
    }

    private record ClassDefinitionResumptionRecord(
                    JSObject proto,
                    JSFunctionObject constructor,
                    ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] staticElements,
                    int instanceElementIndex,
                    int staticElementIndex,
                    Object[] decorators,
                    int startIndex) {
    }
}

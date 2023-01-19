/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
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
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * ES6 14.5.14 Runtime Semantics: ClassDefinitionEvaluation.
 */
public final class ClassDefinitionNode extends NamedEvaluationTargetNode implements FunctionNameHolder, ResumableNode.WithObjectState {

    private static final JSFunctionObject[] EMPTY = new JSFunctionObject[0];

    @Children private JavaScriptNode[] classDecorators;
    @Children private ObjectLiteralMemberNode[] memberNodes;
    @Children private DecoratorListEvaluationNode[] memberDecorators;
    @Children private ApplyDecoratorsToElementDefinition[] defineStaticMethodDecorators;
    @Children private ApplyDecoratorsToElementDefinition[] defineInstanceMethodDecorators;
    @Children private ApplyDecoratorsToElementDefinition[] defineStaticElementDecorators;
    @Children private ApplyDecoratorsToElementDefinition[] defineInstanceElementDecorators;

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
    private final int instanceMethodsCount;
    private final int staticMethodsCount;

    private final BranchProfile errorBranch = BranchProfile.create();

    protected ClassDefinitionNode(JSContext context, JSFunctionExpressionNode constructorFunctionNode, JavaScriptNode classHeritageNode, ObjectLiteralMemberNode[] memberNodes,
                    JSWriteFrameSlotNode writeClassBindingNode, JSWriteFrameSlotNode writeInternalConstructorBrand, JavaScriptNode[] classDecorators, DecoratorListEvaluationNode[] memberDecorators,
                    TruffleString className, int instanceElementsCount, int staticElementCount, boolean hasPrivateInstanceMethods, int blockScopeSlot) {

        this.context = context;
        this.constructorFunctionNode = constructorFunctionNode;
        this.classHeritageNode = classHeritageNode;
        this.memberNodes = memberNodes;
        this.className = className;
        this.hasName = className != null;
        this.instanceElementCount = instanceElementsCount;
        this.staticElementCount = staticElementCount;
        this.instanceMethodsCount = countMethods(memberNodes, false);
        this.staticMethodsCount = countMethods(memberNodes, true);

        this.writeClassBindingNode = writeClassBindingNode;
        this.writeInternalConstructorBrand = writeInternalConstructorBrand;
        this.getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);
        this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        this.createPrototypeNode = CreateObjectNode.createOrdinaryWithPrototype(context);
        this.defineConstructorMethodNode = DefineMethodNode.create(context, constructorFunctionNode, blockScopeSlot);
        this.setElementsNode = instanceElementsCount != 0 ? PropertySetNode.createSetHidden(JSFunction.CLASS_FIELDS_ID, context) : null;
        this.setPrivateBrandNode = hasPrivateInstanceMethods ? PropertySetNode.createSetHidden(JSFunction.PRIVATE_BRAND_ID, context) : null;
        this.setFunctionName = hasName ? null : SetFunctionNameNode.create();
        this.isConstructorNode = IsConstructorNode.create();
        this.classDecorators = classDecorators;
        this.memberDecorators = memberDecorators;
        boolean hasMemberDecorators = memberDecorators != null;
        this.setInitializersNode = PropertySetNode.createSetHidden(JSFunction.CLASS_INITIALIZERS_ID, context);
        this.defineStaticMethodDecorators = hasMemberDecorators ? initDecoratorsElementDefinitionNodes(context, this.staticMethodsCount, true) : null;
        this.defineInstanceMethodDecorators = hasMemberDecorators ? initDecoratorsElementDefinitionNodes(context, this.instanceMethodsCount, false) : null;
        this.defineStaticElementDecorators = hasMemberDecorators ? initDecoratorsElementDefinitionNodes(context, this.staticElementCount, true) : null;
        this.defineInstanceElementDecorators = hasMemberDecorators ? initDecoratorsElementDefinitionNodes(context, this.instanceElementCount, false) : null;
        this.staticExtraInitializersCallNode = JSFunctionCallNode.createCall();
    }

    private static int countMethods(ObjectLiteralMemberNode[] memberNodes, boolean countStatic) {
        int total = 0;
        for (ObjectLiteralMemberNode member : memberNodes) {
            if (countStatic == member.isStatic()) {
                if (member.isMethod()) {
                    total++;
                } else if (member.isAccessor()) {
                    total++;
                }
            }
        }
        return total;
    }

    public static ClassDefinitionNode create(JSContext context, JSFunctionExpressionNode constructorFunction, JavaScriptNode classHeritage, ObjectLiteralMemberNode[] members,
                    JSWriteFrameSlotNode writeClassBinding, JSWriteFrameSlotNode writeInternalConstructorBrand, TruffleString className, JavaScriptNode[] classDecorators,
                    DecoratorListEvaluationNode[] memberDecorators, int instanceFieldCount, int staticFieldCount, boolean hasPrivateInstanceMethods, JSFrameSlot blockScopeSlot) {
        return new ClassDefinitionNode(context, constructorFunction, classHeritage, members, writeClassBinding, writeInternalConstructorBrand, classDecorators, memberDecorators, className,
                        instanceFieldCount, staticFieldCount, hasPrivateInstanceMethods,
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
        JSDynamicObject proto;
        JSObject constructor;
        Object[] decorators;
        ClassElementDefinitionRecord[] instanceElements;
        ClassElementDefinitionRecord[] instanceMethods;
        ClassElementDefinitionRecord[] staticElements;
        ClassElementDefinitionRecord[] staticMethods;
        int instanceElementIndex;
        int staticElementIndex;
        int instanceMethodIndex;
        int staticMethodIndex;
        int startIndex;
        JSRealm realm = getRealm();
        if (resumptionRecord == null) {
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

            decorators = classDecoratorListEvaluation(frame);

            /* Let proto be ObjectCreate(protoParent). */
            assert protoParent == Null.instance || JSRuntime.isObject(protoParent);
            proto = createPrototypeNode.execute((JSDynamicObject) protoParent);

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
            instanceMethods = instanceMethodsCount == 0 ? null : new ClassElementDefinitionRecord[instanceMethodsCount];
            staticMethods = staticMethodsCount == 0 ? null : new ClassElementDefinitionRecord[staticMethodsCount];
            instanceElementIndex = 0;
            staticElementIndex = 0;
            instanceMethodIndex = 0;
            staticMethodIndex = 0;
            startIndex = 0;
        } else {
            proto = resumptionRecord.proto;
            constructor = resumptionRecord.constructor;

            instanceElements = resumptionRecord.instanceElements;
            staticElements = resumptionRecord.staticElements;
            instanceMethods = resumptionRecord.instanceMethods;
            staticMethods = resumptionRecord.staticMethods;

            instanceElementIndex = resumptionRecord.instanceElementIndex;
            staticElementIndex = resumptionRecord.staticElementIndex;
            instanceMethodIndex = resumptionRecord.instanceMethodIndex;
            staticMethodIndex = resumptionRecord.staticMethodIndex;
            startIndex = resumptionRecord.startIndex;

            decorators = resumptionRecord.decorators;
        }

        return defineClassElements(frame, proto, constructor, decorators,
                        instanceElements,
                        instanceMethods,
                        staticElements,
                        staticMethods,
                        startIndex,
                        instanceElementIndex,
                        instanceMethodIndex,
                        staticElementIndex,
                        staticMethodIndex,
                        stateSlot,
                        realm);
    }

    private Object defineClassElements(VirtualFrame frame, JSDynamicObject proto, JSObject constructor, Object[] decorators, ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] instanceMethods, ClassElementDefinitionRecord[] staticElements, ClassElementDefinitionRecord[] staticMethods,
                    int startIndex, int instanceElementIndex, int instanceMethodIndex, int staticElementIndex, int staticMethodIndex,
                    int stateSlot, JSRealm realm) {
        initializeMembers(frame, proto, constructor,
                        instanceElements,
                        instanceMethods,
                        staticElements,
                        staticMethods,
                        startIndex,
                        instanceElementIndex,
                        instanceMethodIndex,
                        staticElementIndex,
                        staticMethodIndex,
                        stateSlot,
                        realm);

        if (writeClassBindingNode != null) {
            writeClassBindingNode.executeWrite(frame, constructor);
        }

        List<Object> staticExtraInitializers = new ArrayList<>();
        List<Object> instanceExtraInitializers = new ArrayList<>();
        applyDecoratorsAndDefineMethods(frame,
                        instanceElements,
                        instanceMethods,
                        instanceExtraInitializers,
                        staticExtraInitializers,
                        staticElements,
                        staticMethods,
                        constructor,
                        proto);

        if (setElementsNode != null) {
            setElementsNode.setValue(constructor, instanceElements);
        }
        setInitializersNode.setValue(constructor, instanceExtraInitializers.toArray(EMPTY));

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

        executeStaticExtraInitializers(constructor, staticExtraInitializers.toArray(EMPTY));

        if (staticElementCount != 0) {
            InitializeInstanceElementsNode initializeStaticElements = this.staticElementsNode;
            if (initializeStaticElements == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.staticElementsNode = initializeStaticElements = insert(InitializeInstanceElementsNode.create(context));
            }
            initializeStaticElements.executeStaticElements(constructor, staticElements);
        }

        List<Object> classExtraInitializers = new ArrayList<>();
        return applyDecoratorsToClassDefinition(frame, getClassName(), constructor, decorators, classExtraInitializers);
    }

    private void applyDecoratorsAndDefineMethods(VirtualFrame frame,
                    ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] instanceMethods,
                    List<Object> instanceExtraInitializers,
                    List<Object> staticExtraInitializers,
                    ClassElementDefinitionRecord[] staticElements,
                    ClassElementDefinitionRecord[] staticMethods,
                    JSDynamicObject constructor,
                    JSDynamicObject proto) {
        applyDecoratorsAndDefineStaticMethods(frame, staticMethods, staticExtraInitializers, constructor);
        applyDecoratorsAndDefineInstanceMethods(frame, instanceMethods, instanceExtraInitializers, proto);
        applyDecoratorsToStaticElements(frame, staticElements, staticExtraInitializers, constructor);
        applyDecoratorsToInstanceElements(frame, instanceElements, instanceExtraInitializers, proto);
    }

    private void executeStaticExtraInitializers(Object target, Object[] initializers) {
        for (Object initializer : initializers) {
            staticExtraInitializersCallNode.executeCall(JSArguments.createZeroArg(target, initializer));
        }
    }

    @ExplodeLoop
    private void applyDecoratorsAndDefineStaticMethods(VirtualFrame frame, ClassElementDefinitionRecord[] staticMethods, List<Object> extraInitializers, JSDynamicObject homeObject) {
        if (staticMethods == null) {
            return;
        }
        CompilerAsserts.partialEvaluationConstant(memberNodes.length);
        int staticMethodIndex = 0;
        for (ObjectLiteralMemberNode member : memberNodes) {
            if (member.isStatic() && (member.isMethod() || member.isAccessor())) {
                ClassElementDefinitionRecord m = staticMethods[staticMethodIndex];
                if (defineStaticMethodDecorators != null) {
                    defineStaticMethodDecorators[staticMethodIndex].executeDecorator(frame, homeObject, m, extraInitializers);
                }
                member.defineClassElement(frame, homeObject, m);
                staticMethodIndex++;
            }
        }
    }

    @ExplodeLoop
    private void applyDecoratorsAndDefineInstanceMethods(VirtualFrame frame, ClassElementDefinitionRecord[] instanceMethods, List<Object> extraInitializers, JSDynamicObject homeObject) {
        if (instanceMethods == null) {
            return;
        }
        CompilerAsserts.partialEvaluationConstant(memberNodes.length);
        int instanceMethodIndex = 0;
        for (ObjectLiteralMemberNode member : memberNodes) {
            if (!member.isStatic() && (member.isMethod() || member.isAccessor())) {
                ClassElementDefinitionRecord m = instanceMethods[instanceMethodIndex];
                if (defineInstanceMethodDecorators != null) {
                    defineInstanceMethodDecorators[instanceMethodIndex].executeDecorator(frame, homeObject, m, extraInitializers);
                }
                member.defineClassElement(frame, homeObject, m);
                instanceMethodIndex++;
            }
        }
    }

    @ExplodeLoop
    private void applyDecoratorsToStaticElements(VirtualFrame frame, ClassElementDefinitionRecord[] staticElements, List<Object> extraInitializers, JSDynamicObject homeObject) {
        if (defineStaticElementDecorators == null || staticElements == null) {
            return;
        }
        CompilerAsserts.partialEvaluationConstant(memberNodes.length);
        int staticElementIndex = 0;
        for (ObjectLiteralMemberNode member : memberNodes) {
            if (member.isStatic() && !(member.isMethod() || member.isAccessor())) {
                ClassElementDefinitionRecord f = staticElements[staticElementIndex];
                if (defineStaticElementDecorators != null) {
                    defineStaticElementDecorators[staticElementIndex].executeDecorator(frame, homeObject, f, extraInitializers);
                }
                staticElementIndex++;
            }
        }
    }

    @ExplodeLoop
    private void applyDecoratorsToInstanceElements(VirtualFrame frame, ClassElementDefinitionRecord[] instanceFields, List<Object> extraInitializers, JSDynamicObject homeObject) {
        if (defineInstanceElementDecorators == null || instanceFields == null) {
            return;
        }
        CompilerAsserts.partialEvaluationConstant(memberNodes.length);
        int instanceElementIndex = 0;
        for (ObjectLiteralMemberNode member : memberNodes) {
            if (member.isStatic() && !(member.isMethod() || member.isAccessor())) {
                ClassElementDefinitionRecord f = instanceFields[instanceElementIndex];
                if (defineInstanceElementDecorators != null) {
                    defineInstanceElementDecorators[instanceElementIndex].executeDecorator(frame, homeObject, f, extraInitializers);
                }
                instanceElementIndex++;
            }
        }
    }

    private static ApplyDecoratorsToElementDefinition[] initDecoratorsElementDefinitionNodes(JSContext context, int size, boolean isStatic) {
        CompilerAsserts.neverPartOfCompilation();
        if (size == 0) {
            return null;
        }
        ApplyDecoratorsToElementDefinition[] nodes = new ApplyDecoratorsToElementDefinition[size];
        for (int i = 0; i < size; i++) {
            nodes[i] = ApplyDecoratorsToElementDefinition.create(context, isStatic);
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

    private Object applyDecoratorsToClassDefinition(VirtualFrame frame, Object name, JSObject constructor, Object[] decorators, List<Object> classExtraInitializers) {
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
    private void initializeMembers(VirtualFrame frame, JSDynamicObject proto, JSObject constructor, ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] instanceMethods, ClassElementDefinitionRecord[] staticElements, ClassElementDefinitionRecord[] staticMethods,
                    int startIndex, int instanceElementsIdx, int instanceMethodsIdx, int staticElementIdx, int staticMethodIdx,
                    int stateSlot, JSRealm realm) {
        /* For each ClassElement e in order from NonConstructorMethodDefinitions of ClassBody */
        int instanceElementIndex = instanceElementsIdx;
        int instanceMethodIndex = instanceMethodsIdx;
        int staticElementIndex = staticElementIdx;
        int staticMethodIndex = staticMethodIdx;
        Object[] decorators = null;
        int i = 0;
        try {
            for (; i < memberNodes.length; i++) {
                if (i >= startIndex) {
                    ObjectLiteralMemberNode memberNode = memberNodes[i];
                    boolean isStatic = memberNode.isStatic();
                    JSDynamicObject homeObject = isStatic ? constructor : proto;
                    decorators = memberDecorators != null && memberDecorators[i] != null ? memberDecorators[i].execute(frame) : null;
                    Object key = memberNode.evaluateKey(frame);
                    ClassElementDefinitionRecord classElementDef = memberNode.evaluateClassElementDefinition(frame, homeObject, key, realm, decorators);
                    if (memberNode.isFieldOrStaticBlock() || memberNode.isAutoAccessor()) {
                        if (isStatic) {
                            staticElements[staticElementIndex++] = classElementDef;
                        } else {
                            instanceElements[instanceElementIndex++] = classElementDef;
                        }
                    } else {
                        if (isStatic) {
                            staticMethods[staticMethodIndex++] = classElementDef;
                        } else {
                            instanceMethods[instanceMethodIndex++] = classElementDef;
                        }
                    }
                }
            }
        } catch (YieldException e) {
            setState(frame, stateSlot, new ClassDefinitionResumptionRecord(
                            proto,
                            constructor,
                            instanceElements,
                            staticElements,
                            instanceMethods,
                            staticMethods,
                            instanceElementIndex,
                            staticElementIndex,
                            instanceMethodIndex,
                            staticMethodIndex,
                            decorators,
                            i));
            throw e;
        }
        assert instanceElementIndex == instanceElementCount && instanceMethodIndex == instanceMethodsCount &&
                        staticElementIndex == staticElementCount && staticMethodIndex == staticMethodsCount;
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
                        defineConstructorMethodNode.getBlockScopeSlot());
    }

    static class ClassDefinitionResumptionRecord {
        final JSDynamicObject proto;
        final JSObject constructor;
        final ClassElementDefinitionRecord[] instanceElements;
        final ClassElementDefinitionRecord[] staticElements;
        final ClassElementDefinitionRecord[] instanceMethods;
        final ClassElementDefinitionRecord[] staticMethods;
        final int instanceElementIndex;
        final int instanceMethodIndex;
        final int staticElementIndex;
        final int staticMethodIndex;
        final int startIndex;
        final Object[] decorators;

        ClassDefinitionResumptionRecord(
                        JSDynamicObject proto,
                        JSObject constructor,
                        ClassElementDefinitionRecord[] instanceFields,
                        ClassElementDefinitionRecord[] staticElements,
                        ClassElementDefinitionRecord[] instanceMethods,
                        ClassElementDefinitionRecord[] staticMethods,
                        int instanceElementIndex,
                        int staticElementIndex,
                        int instanceMethodIndex,
                        int staticMethodIndex,
                        Object[] decorators,
                        int startIndex) {
            this.proto = proto;
            this.constructor = constructor;
            this.instanceElements = instanceFields;
            this.staticElements = staticElements;
            this.instanceMethods = instanceMethods;
            this.staticMethods = staticMethods;
            this.instanceElementIndex = instanceElementIndex;
            this.staticElementIndex = staticElementIndex;
            this.instanceMethodIndex = instanceMethodIndex;
            this.staticMethodIndex = staticMethodIndex;
            this.startIndex = startIndex;
            this.decorators = decorators;
        }
    }
}

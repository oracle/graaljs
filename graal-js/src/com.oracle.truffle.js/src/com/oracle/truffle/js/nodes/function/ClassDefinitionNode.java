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
import com.oracle.truffle.js.decorators.DefineMethodPropertyNode;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.InitializeInstanceElementsNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.AccessorMemberNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.AutoAccessorDataMemberNode;
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

import static com.oracle.truffle.js.nodes.access.ObjectLiteralNode.isAccessor;
import static com.oracle.truffle.js.nodes.access.ObjectLiteralNode.isAutoAccessor;
import static com.oracle.truffle.js.nodes.access.ObjectLiteralNode.isMethod;

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
    @Child private DefineMethodPropertyNode defineMethodProperty;

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
        this.setInitializersNode = PropertySetNode.createSetHidden(JSFunction.CLASS_INITIALIZERS_ID, context);
        this.defineStaticMethodDecorators = initDecoratorsElementDefinitionNodes(context, this.staticMethodsCount, true);
        this.defineInstanceMethodDecorators = initDecoratorsElementDefinitionNodes(context, this.instanceMethodsCount, false);
        this.defineStaticElementDecorators = initDecoratorsElementDefinitionNodes(context, this.staticElementCount, true);
        this.defineInstanceElementDecorators = initDecoratorsElementDefinitionNodes(context, this.instanceElementCount, false);
        this.staticExtraInitializersCallNode = JSFunctionCallNode.createCall();
    }

    private static int countMethods(ObjectLiteralMemberNode[] memberNodes, boolean countStatic) {
        int total = 0;
        for (ObjectLiteralMemberNode member : memberNodes) {
            if (countStatic == member.isStatic()) {
                if (isMethod(member)) {
                    total++;
                } else if (isAccessor(member)) {
                    AccessorMemberNode accessor = (AccessorMemberNode) member;
                    assert accessor.hasGetter() || accessor.hasSetter();
                    if (accessor.hasGetter()) {
                        total++;
                    }
                    if (accessor.hasSetter()) {
                        total++;
                    }
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
        applyDecorators(frame,
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
        return applyDecoratorsClassDefinition(frame, getClassName(), constructor, decorators, classExtraInitializers);
    }

    private void applyDecorators(VirtualFrame frame,
                    ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] instanceMethods,
                    List<Object> instanceExtraInitializers,
                    List<Object> staticExtraInitializers,
                    ClassElementDefinitionRecord[] staticElements,
                    ClassElementDefinitionRecord[] staticMethods,
                    JSDynamicObject constructor,
                    JSDynamicObject proto) {
        applyDecoratorsStaticMethods(frame, staticMethods, staticExtraInitializers, constructor);
        applyDecoratorsInstanceMethods(frame, instanceMethods, instanceExtraInitializers, proto);
        applyDecoratorsStaticElements(frame, staticElements, staticExtraInitializers, constructor);
        applyDecoratorsInstanceElements(frame, instanceElements, instanceExtraInitializers, proto);
    }

    private void executeStaticExtraInitializers(Object target, Object[] initializers) {
        for (Object initializer : initializers) {
            staticExtraInitializersCallNode.executeCall(JSArguments.createZeroArg(target, initializer));
        }
    }

    @ExplodeLoop
    private void applyDecoratorsStaticMethods(VirtualFrame frame, ClassElementDefinitionRecord[] staticMethods, List<Object> instanceExtraInitializers, JSDynamicObject proto) {
        if (staticMethods == null) {
            return;
        }
        int i = 0;
        for (ClassElementDefinitionRecord m : staticMethods) {
            assert (m.isMethod() || m.isSetter() || m.isGetter());
            defineStaticMethodDecorators[i++].executeDecorator(frame, proto, m, instanceExtraInitializers);
            getDefineMethodProperty().executeDefine(proto, m, false);
        }
    }

    @ExplodeLoop
    private void applyDecoratorsInstanceMethods(VirtualFrame frame, ClassElementDefinitionRecord[] instanceMethods, List<Object> extraInitializers, JSDynamicObject homeObject) {
        if (instanceMethods == null) {
            return;
        }
        int i = 0;
        for (ClassElementDefinitionRecord m : instanceMethods) {
            assert instanceMethods.length == instanceMethodsCount;
            defineInstanceMethodDecorators[i++].executeDecorator(frame, homeObject, m, extraInitializers);
            getDefineMethodProperty().executeDefine(homeObject, m, false);
        }
    }

    @ExplodeLoop
    private void applyDecoratorsStaticElements(VirtualFrame frame, ClassElementDefinitionRecord[] staticElements, List<Object> instanceExtraInitializers, JSDynamicObject proto) {
        if (staticElements == null) {
            return;
        }
        int i = 0;
        for (ClassElementDefinitionRecord f : staticElements) {
            if (!(f.isMethod() || f.isSetter() || f.isGetter())) {
                defineStaticElementDecorators[i++].executeDecorator(frame, proto, f, instanceExtraInitializers);
            }
        }
    }

    @ExplodeLoop
    private void applyDecoratorsInstanceElements(VirtualFrame frame, ClassElementDefinitionRecord[] instanceFields, List<Object> instanceExtraInitializers, JSDynamicObject proto) {
        if (instanceFields == null) {
            return;
        }
        int i = 0;
        for (ClassElementDefinitionRecord f : instanceFields) {
            defineInstanceElementDecorators[i++].executeDecorator(frame, proto, f, instanceExtraInitializers);
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

    private DefineMethodPropertyNode getDefineMethodProperty() {
        if (defineMethodProperty == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            defineMethodProperty = insert(DefineMethodPropertyNode.create());
        }
        return defineMethodProperty;
    }

    private Object[] classDecoratorListEvaluation(VirtualFrame frame) {
        Object[] decorators = new Object[classDecorators.length];
        for (int i = 0; i < decorators.length; i++) {
            Object maybeDecorator = classDecorators[i].execute(frame);
            decorators[decorators.length - i - 1] = maybeDecorator;
        }
        return decorators;
    }

    private Object applyDecoratorsClassDefinition(VirtualFrame frame, Object name, JSObject constructor, Object[] decorators, List<Object> classExtraInitializers) {
        if (this.classDecorators.length == 0) {
            return constructor;
        }
        if (decorateClassDefinition == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            decorateClassDefinition = insert(ApplyDecoratorsToClassDefinitionNode.create(context));
        }
        return decorateClassDefinition.executeDecorators(frame, name, constructor, decorators, classExtraInitializers);
    }

    private static void storeElement(ClassElementDefinitionRecordIndexes indexes, ClassElementDefinitionRecord[] staticStorage, ClassElementDefinitionRecord[] instanceStorage,
                    ClassElementDefinitionRecord element, boolean isStatic) {
        if (isStatic) {
            staticStorage[indexes.staticElementIndex++] = element;
        } else {
            instanceStorage[indexes.instanceElementIndex++] = element;
        }
    }

    private static void storeMethod(ClassElementDefinitionRecordIndexes indexes, ClassElementDefinitionRecord[] staticStorage, ClassElementDefinitionRecord[] instanceStorage,
                    ClassElementDefinitionRecord element, boolean isStatic) {
        if (isStatic) {
            staticStorage[indexes.staticMethodIndex++] = element;
        } else {
            instanceStorage[indexes.instanceMethodIndex++] = element;
        }
    }

    @ExplodeLoop
    private void initializeMembers(VirtualFrame frame, JSDynamicObject proto, JSObject constructor, ClassElementDefinitionRecord[] instanceElements,
                    ClassElementDefinitionRecord[] instanceMethods, ClassElementDefinitionRecord[] staticElements, ClassElementDefinitionRecord[] staticMethods,
                    int startIndex, int instanceElementsIdx, int instanceMethodsIdx, int staticElementIdx, int staticMethodIdx,
                    int stateSlot, JSRealm realm) {
        /* For each ClassElement e in order from NonConstructorMethodDefinitions of ClassBody */
        ClassElementDefinitionRecordIndexes indexes = new ClassElementDefinitionRecordIndexes(instanceElementsIdx, instanceMethodsIdx, staticElementIdx, staticMethodIdx);
        Object[] decorators = null;
        int i = 0;
        try {
            for (; i < memberNodes.length; i++) {
                if (i >= startIndex) {
                    ObjectLiteralMemberNode memberNode = memberNodes[i];
                    boolean isStatic = memberNode.isStatic();
                    JSDynamicObject homeObject = isStatic ? constructor : proto;
                    decorators = memberDecorators[i] != null ? memberDecorators[i].execute(frame) : null;
                    if (memberNode.isFieldOrStaticBlock()) {
                        ClassElementDefinitionRecord field = initField(frame, realm, decorators, memberNode, homeObject);
                        storeElement(indexes, staticElements, instanceElements, field, isStatic);
                    } else {
                        Object key = memberNode.evaluateKey(frame);
                        if (isAutoAccessor(memberNode)) {
                            ClassElementDefinitionRecord autoAccessor = initAutoAccessor(frame, realm, decorators, memberNode, homeObject, key);
                            storeElement(indexes, staticElements, instanceElements, autoAccessor, isStatic);
                        } else {
                            if (isMethod(memberNode)) {
                                Object value = memberNode.evaluateValue(frame, homeObject, key, realm);
                                memberNode.evaluateWithKeyAndValue(frame, homeObject, key, value, realm);

                                ClassElementDefinitionRecord method;
                                if (memberNode instanceof ObjectLiteralNode.PrivateMethodMemberNode) {
                                    ObjectLiteralNode.PrivateMethodMemberNode privateMember = (ObjectLiteralNode.PrivateMethodMemberNode) memberNode;
                                    int slot = privateMember.getWritePrivateNode().getSlotIndex();
                                    int brandSlot = privateMember.getPrivateBrandSlotIndex();
                                    int blockSlot = defineConstructorMethodNode.getBlockScopeSlot();
                                    method = ClassElementDefinitionRecord.createPrivateMethod(context, key, slot, brandSlot, blockSlot, value, memberNode.isAnonymousFunctionDefinition(), decorators);
                                } else {
                                    method = ClassElementDefinitionRecord.createPublicMethod(context, key, value, memberNode.isAnonymousFunctionDefinition(), decorators);
                                }
                                storeMethod(indexes, staticMethods, instanceMethods, method, isStatic);
                            } else if (isAccessor(memberNode)) {
                                // no need to eval 'value' for accessors: values are getter/setter.
                                memberNode.evaluateWithKeyAndValue(frame, homeObject, key, null, realm);
                                AccessorMemberNode accessorMember = (AccessorMemberNode) memberNode;
                                assert accessorMember.hasGetter() || accessorMember.hasSetter();
                                if (accessorMember.hasGetter()) {
                                    Object getter = accessorMember.evaluateGetter(frame, homeObject, key, realm);
                                    ClassElementDefinitionRecord element;
                                    if (memberNode instanceof ObjectLiteralNode.PrivateAccessorMemberNode) {
                                        ObjectLiteralNode.PrivateAccessorMemberNode privateMember = (ObjectLiteralNode.PrivateAccessorMemberNode) memberNode;
                                        int slot = privateMember.getWritePrivateNode().getSlotIndex();
                                        int brandSlot = privateMember.getPrivateBrandSlotIndex();
                                        int blockSlot = defineConstructorMethodNode.getBlockScopeSlot();
                                        element = ClassElementDefinitionRecord.createPrivateGetter(context, key, slot, brandSlot, blockSlot, getter, memberNode.isAnonymousFunctionDefinition(),
                                                        decorators);
                                    } else {
                                        element = ClassElementDefinitionRecord.createPublicGetter(context, key, getter, memberNode.isAnonymousFunctionDefinition(), decorators);
                                    }
                                    storeMethod(indexes, staticMethods, instanceMethods, element, isStatic);
                                }
                                if (accessorMember.hasSetter()) {
                                    Object setter = accessorMember.evaluateSetter(frame, homeObject, key, realm);
                                    ClassElementDefinitionRecord element;
                                    if (memberNode instanceof ObjectLiteralNode.PrivateAccessorMemberNode) {
                                        ObjectLiteralNode.PrivateAccessorMemberNode privateMember = (ObjectLiteralNode.PrivateAccessorMemberNode) memberNode;
                                        int slot = privateMember.getWritePrivateNode().getSlotIndex();
                                        int brandSlot = privateMember.getPrivateBrandSlotIndex();
                                        int blockSlot = defineConstructorMethodNode.getBlockScopeSlot();
                                        element = ClassElementDefinitionRecord.createPrivateSetter(context, key, slot, brandSlot, blockSlot, setter, memberNode.isAnonymousFunctionDefinition(),
                                                        decorators);
                                    } else {
                                        element = ClassElementDefinitionRecord.createPublicSetter(context, key, setter, memberNode.isAnonymousFunctionDefinition(), decorators);
                                    }
                                    storeMethod(indexes, staticMethods, instanceMethods, element, isStatic);
                                }
                            }
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
                            indexes.instanceElementIndex,
                            indexes.staticElementIndex,
                            indexes.instanceMethodIndex,
                            indexes.staticMethodIndex,
                            decorators,
                            i));
            throw e;
        }
        assert indexes.instanceElementIndex == instanceElementCount && indexes.staticElementIndex == staticElementCount;
    }

    private static ClassElementDefinitionRecord initField(VirtualFrame frame, JSRealm realm, Object[] decorators, ObjectLiteralMemberNode memberNode, JSDynamicObject homeObject) {
        memberNode.executeVoid(frame, homeObject, realm);
        Object key = memberNode.evaluateKey(frame);
        Object value = memberNode.evaluateValue(frame, homeObject, key, realm);
        return ClassElementDefinitionRecord.createField(realm.getContext(), key, value, memberNode.isPrivate(), memberNode.isAnonymousFunctionDefinition(), decorators);
    }

    private static ClassElementDefinitionRecord initAutoAccessor(VirtualFrame frame, JSRealm realm, Object[] decorators, ObjectLiteralMemberNode memberNode, JSDynamicObject homeObject,
                    Object key) {
        AutoAccessorDataMemberNode autoAccessorDataMemberNode = (AutoAccessorDataMemberNode) memberNode;
        HiddenKey backingStorageKey = autoAccessorDataMemberNode.createBackingStorageKey(key);
        JSFunctionObject setter = autoAccessorDataMemberNode.createAutoAccessorSetter(backingStorageKey);
        JSFunctionObject getter = autoAccessorDataMemberNode.createAutoAccessorGetter(backingStorageKey);
        autoAccessorDataMemberNode.executeWithGetterSetter(homeObject, key, getter, setter);
        Object value = memberNode.evaluateValue(frame, homeObject, key, realm);
        ClassElementDefinitionRecord field = ClassElementDefinitionRecord.createAutoAccessor(realm.getContext(), key, backingStorageKey, value, memberNode.isPrivate(),
                        memberNode.isAnonymousFunctionDefinition(), decorators);
        field.setSetter(setter);
        field.setGetter(getter);
        return field;
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

    static class ClassElementDefinitionRecordIndexes {
        int instanceElementIndex;
        int instanceMethodIndex;
        int staticElementIndex;
        int staticMethodIndex;

        ClassElementDefinitionRecordIndexes(int instanceElementsIdx, int instanceMethodsIdx, int staticElementIdx, int staticMethodIdx) {
            this.instanceElementIndex = instanceElementsIdx;
            this.instanceMethodIndex = instanceMethodsIdx;
            this.staticElementIndex = staticElementIdx;
            this.staticMethodIndex = staticMethodIdx;
        }
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

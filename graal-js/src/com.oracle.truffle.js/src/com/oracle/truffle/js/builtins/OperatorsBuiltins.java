/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.HasPropertyCacheNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.OperatorSet;

public final class OperatorsBuiltins extends JSBuiltinsContainer.Lambda {

    public static final TruffleString OPERATORS = Strings.constant("Operators");

    private static final TruffleString LEFT_ID = Strings.constant("left");
    private static final TruffleString RIGHT_ID = Strings.constant("right");
    private static final TruffleString OPEN_ID = Strings.constant("open");

    public static final JSBuiltinsContainer BUILTINS = new OperatorsBuiltins();

    protected static final HiddenKey OPERATOR_DEFINITIONS_ID = new HiddenKey("OperatorDefinitions");

    protected OperatorsBuiltins() {
        super(null);
        defineFunction(OPERATORS, 1, JSAttributes.getDefault(),
                        (context, builtin) -> OperatorsBuiltinsFactory.OperatorsNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context)));
    }

    /**
     * This is a stub function that always returns {@code true}. If we ever implement the
     * {@code with operators from} part of the operator overloading proposal, this is where we would
     * perform the check.
     */
    @SuppressWarnings("unused")
    public static boolean overloadedOperatorsAllowed(JSDynamicObject arg) {
        return true;
    }

    public static void checkOverloadedOperatorsAllowed(JSDynamicObject arg, Node originatingNode) {
        if (!overloadedOperatorsAllowed(arg)) {
            throw Errors.createTypeError("use of overloaded operators is not enabled by a `with operators from` clause", originatingNode);
        }
    }

    /**
     * This class implements the Operators builtin. This is a function that takes as input a series
     * of objects that map operator names to their overloaded implementations. The result is a class
     * object that should be subclassed by a class that wishes to have the supplied operator
     * semantics.
     */
    public abstract static class OperatorsNode extends JSBuiltinNode {
        @Child private CreateObjectNode createPrototypeNode;
        @Child private ConstructOperatorSetNode constructOperatorSetNode;
        @Child private CreateMethodPropertyNode setConstructorNode;
        @Child private PropertySetNode setOperatorDefinitionsNode;

        public OperatorsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createPrototypeNode = CreateObjectNode.create(context);
            this.constructOperatorSetNode = ConstructOperatorSetNode.create(context);
            this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
            this.setOperatorDefinitionsNode = PropertySetNode.createSetHidden(OPERATOR_DEFINITIONS_ID, context);
        }

        @Specialization
        protected JSFunctionObject doOperators(VirtualFrame frame, Object table, Object... extraTables) {
            JSDynamicObject prototype = createPrototypeNode.execute(frame);
            OperatorSet operatorSet = constructOperatorSetNode.execute(table, extraTables);
            JSFunctionData constructorFunctionData = getContext().getOrCreateBuiltinFunctionData(
                            JSContext.BuiltinFunctionKey.OperatorsConstructor, OperatorsNode::createConstructorImpl);
            JSFunctionObject constructor = JSFunction.create(getRealm(), constructorFunctionData);
            JSFunction.setClassPrototype(constructor, prototype);
            setConstructorNode.executeVoid(prototype, constructor);
            setOperatorDefinitionsNode.setValue(constructor, operatorSet);
            return constructor;
        }

        private static JSFunctionData createConstructorImpl(JSContext context) {
            CallTarget callTarget = new JavaScriptRootNode(context.getLanguage()) {
                @Child private PropertyGetNode getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, context);
                @Child private PropertyGetNode getOperatorDefinitionsNode = PropertyGetNode.createGetHidden(OPERATOR_DEFINITIONS_ID, context);
                @Child private CreateOverloadedOperatorsObjectNode createOverloadedOperatorsObjectNode = CreateOverloadedOperatorsObjectNode.create(context);

                @Override
                public Object execute(VirtualFrame frame) {
                    Object newTarget = JSArguments.getNewTarget(frame.getArguments());
                    Object constructor = JSArguments.getFunctionObject(frame.getArguments());
                    OperatorSet operatorSet = (OperatorSet) getOperatorDefinitionsNode.getValue(constructor);
                    Object prototype = getPrototypeNode.getValue(newTarget);
                    return createOverloadedOperatorsObjectNode.execute(prototype, operatorSet);
                }
            }.getCallTarget();
            return JSFunctionData.create(context, callTarget, 0, Strings.EMPTY_STRING);
        }
    }

    public abstract static class CreateOverloadedOperatorsObjectNode extends JavaScriptBaseNode {

        protected final JSContext context;

        protected CreateOverloadedOperatorsObjectNode(JSContext context) {
            this.context = context;
        }

        public static CreateOverloadedOperatorsObjectNode create(JSContext context) {
            return OperatorsBuiltinsFactory.CreateOverloadedOperatorsObjectNodeGen.create(context);
        }

        @Idempotent
        protected final JSContext getContext() {
            return context;
        }

        protected abstract JSOverloadedOperatorsObject execute(Object prototype, OperatorSet operatorSet);

        protected Shape getProtoChildShape(Object prototype) {
            CompilerAsserts.neverPartOfCompilation();
            if (JSGuards.isJSObject(prototype)) {
                JSObject jsproto = (JSObject) prototype;
                return JSObjectUtil.getProtoChildShape(jsproto, JSOrdinary.OVERLOADED_OPERATORS_INSTANCE, getContext());
            }
            return null;
        }

        @NeverDefault
        protected Shape getShapeWithoutProto() {
            CompilerAsserts.neverPartOfCompilation();
            return JSObjectUtil.getProtoChildShape(null, JSOrdinary.OVERLOADED_OPERATORS_INSTANCE, getContext());
        }

        @NeverDefault
        protected Shape getShapeWithDefaultProto(JSRealm realm) {
            CompilerAsserts.neverPartOfCompilation();
            return JSObjectUtil.getProtoChildShape(realm.getObjectPrototype(), JSOrdinary.OVERLOADED_OPERATORS_INSTANCE, getContext());
        }

        @Specialization(guards = {"!getContext().isMultiContext()", "prototype == cachedPrototype", "isJSObject(cachedPrototype)"}, limit = "getContext().getPropertyCacheLimit()")
        protected JSOverloadedOperatorsObject doCachedProto(@SuppressWarnings("unused") Object prototype, OperatorSet operatorSet,
                        @Cached("prototype") @SuppressWarnings("unused") Object cachedPrototype,
                        @Cached("getProtoChildShape(prototype)") Shape cachedShape) {
            return JSOverloadedOperatorsObject.create(getContext(), cachedShape, (JSObject) cachedPrototype, operatorSet);
        }

        @Specialization
        protected JSOverloadedOperatorsObject createWithProto(JSObject prototype, OperatorSet operatorSet,
                        @CachedLibrary(limit = "3") DynamicObjectLibrary setProtoNode,
                        @Cached("getShapeWithoutProto()") Shape cachedShape) {
            JSOverloadedOperatorsObject object = JSOverloadedOperatorsObject.create(getContext(), cachedShape, prototype, operatorSet);
            setProtoNode.put(object, JSObject.HIDDEN_PROTO, prototype);
            return object;
        }

        @Specialization(guards = {"!isJSObject(prototype)"})
        public JSOverloadedOperatorsObject createDefaultProto(@SuppressWarnings("unused") Object prototype, OperatorSet operatorSet,
                        @Cached("getShapeWithDefaultProto(getRealm())") Shape cachedShape) {
            return JSOverloadedOperatorsObject.create(getContext(), cachedShape, getRealm().getObjectPrototype(), operatorSet);
        }
    }

    public abstract static class ConstructOperatorSetNode extends JavaScriptBaseNode {

        @Child private IsObjectNode tableIsObjectNode;
        @Child private JSHasPropertyNode tableHasKeyNode;
        @Child private ReadElementNode tableGetNode;
        @Child private IsCallableNode isCallableNode;
        @Child private PropertyGetNode getOperatorDefinitionsNode;
        @Child private JSGetLengthNode getOpenSetLengthNode;
        @Child private ReadElementNode readOpenSetElementNode;
        @Child private JSToStringNode openOperatorToStringNode;
        @Child private IsJSObjectNode typeIsJSObjectNode;
        @Child private IsConstructorNode typeIsConstructorNode;
        @Child private GetPrototypeNode getSuperclassNode;
        @Child private HasPropertyCacheNode hasOperatorDefinitionsNode;
        @Child private PropertyGetNode getClassNameNode;
        @Child private JSToStringNode classNameToStringNode;

        protected final JSContext context;

        protected ConstructOperatorSetNode(JSContext context) {
            this.context = context;
        }

        public static ConstructOperatorSetNode create(JSContext context) {
            return OperatorsBuiltinsFactory.ConstructOperatorSetNodeGen.create(context);
        }

        public abstract OperatorSet execute(Object table, Object[] extraTables);

        @Specialization
        protected OperatorSet construct(Object table, Object[] extraTables) {
            JSRealm realm = getRealm();
            JSRealm mainRealm = JSRealm.getMain(this);
            int operatorCounter = mainRealm.getOperatorCounter();

            if (!tableIsObject(table)) {
                throw Errors.createTypeErrorNotAnObject(table, this);
            }

            EconomicMap<TruffleString, Object> selfOperatorDefinitions = Boundaries.economicMapCreate();
            for (TruffleString operator : OperatorSet.ALL_OPERATORS) {
                if (tableHasKey(table, operator)) {
                    Object value = tableGet(table, operator);
                    if (!isCallable(value)) {
                        throw Errors.createTypeError(Boundaries.stringFormat("the implementation of the operator [[Class]] %s [[Class]] is not a function", operator), this);
                    }
                    Boundaries.economicMapPut(selfOperatorDefinitions, operator, value);
                }
            }

            EconomicSet<TruffleString> openOperators;
            if (tableHasKey(table, OPEN_ID)) {
                openOperators = Boundaries.economicSetCreate();
                Object openSet = tableGet(table, OPEN_ID);
                long openSetLength = getOpenSetLength(openSet);
                for (int i = 0; i < openSetLength; i++) {
                    Object element = readOpenSetElement(openSet, i);
                    if (!(element instanceof TruffleString str) || !Boundaries.economicSetContains(OperatorSet.ALL_OPERATORS, str)) {
                        throw Errors.createTypeError(Boundaries.stringFormat("unrecognized operator %s", openOperatorToString(element)), this);
                    }
                    Boundaries.economicSetAdd(openOperators, (TruffleString) element);
                }
            } else {
                openOperators = OperatorSet.ALL_OPERATORS;
            }

            EconomicMap<TruffleString, Object[]> leftOperatorDefinitions = Boundaries.economicMapCreate();
            EconomicMap<TruffleString, Object[]> rightOperatorDefinitions = Boundaries.economicMapCreate();
            for (Object extraTable : extraTables) {
                if (!tableIsObject(extraTable)) {
                    throw Errors.createTypeErrorNotAnObject(extraTable, this);
                }
                if (tableHasKey(extraTable, LEFT_ID)) {
                    if (tableHasKey(extraTable, RIGHT_ID)) {
                        throw Errors.createTypeError("overload table must not be both left and right", this);
                    }
                    Object leftType = tableGet(extraTable, LEFT_ID);
                    if (!isJSConstructor(leftType)) {
                        throw Errors.createTypeError("the left: value must be an ECMAScript constructor", this);
                    }
                    OperatorSet leftSet = getOperatorSetOfClass(realm, (JSDynamicObject) leftType);
                    if (leftSet == null) {
                        throw Errors.createTypeError(Boundaries.stringFormat("the left: value %s must be a class with operators overloaded", getClassName(leftType)), this);
                    }
                    for (TruffleString operator : OperatorSet.BINARY_OPERATORS) {
                        if (tableHasKey(extraTable, operator)) {
                            Object operatorImplementation = tableGet(extraTable, operator);
                            if (!isCallable(operatorImplementation)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the implementation of the operator %s %s [[Class]] is not a function", getClassName(leftType), operator), this);
                            }
                            if (!leftSet.isOperatorOpen(operator)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the operator %s may not be overloaded on the provided type %s", operator, getClassName(leftType)), this);
                            }
                            if (!Boundaries.economicMapContainsKey(rightOperatorDefinitions, operator)) {
                                Boundaries.economicMapPut(rightOperatorDefinitions, operator, new JSDynamicObject[operatorCounter]);
                            }
                            Boundaries.economicMapGet(rightOperatorDefinitions, operator)[leftSet.getOperatorCounter()] = operatorImplementation;
                        }
                    }
                } else {
                    if (!tableHasKey(extraTable, RIGHT_ID)) {
                        throw Errors.createTypeError("Either left: or right: must be provided", this);
                    }
                    Object rightType = tableGet(extraTable, RIGHT_ID);
                    if (!isJSConstructor(rightType)) {
                        throw Errors.createTypeError("the right: value must be an ECMAScript constructor", this);
                    }
                    OperatorSet rightSet = getOperatorSetOfClass(realm, (JSDynamicObject) rightType);
                    if (rightSet == null) {
                        throw Errors.createTypeError(Boundaries.stringFormat("the right: value %s must be a class with operators overloaded", getClassName(rightType)), this);
                    }
                    for (TruffleString operator : OperatorSet.BINARY_OPERATORS) {
                        if (tableHasKey(extraTable, operator)) {
                            Object operatorImplementation = tableGet(extraTable, operator);
                            if (!isCallable(operatorImplementation)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the implementation of the operator [[Class]] %s %s is not a function", operator, getClassName(rightType)), this);
                            }
                            if (!rightSet.isOperatorOpen(operator)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the operator %s may not be overloaded on the provided type %s", operator, getClassName(rightType)), this);
                            }
                            if (!Boundaries.economicMapContainsKey(leftOperatorDefinitions, operator)) {
                                Boundaries.economicMapPut(leftOperatorDefinitions, operator, new JSDynamicObject[operatorCounter]);
                            }
                            Boundaries.economicMapGet(leftOperatorDefinitions, operator)[rightSet.getOperatorCounter()] = operatorImplementation;
                        }
                    }
                }
            }

            // Only bump the operator counter if we actually return an OperatorSet.
            mainRealm.incOperatorCounter();
            return new OperatorSet(operatorCounter, selfOperatorDefinitions, leftOperatorDefinitions, rightOperatorDefinitions, openOperators);
        }

        protected OperatorSet getOperatorSetOfClass(JSRealm realm, JSDynamicObject constructor) {
            if (constructor == realm.getNumberConstructor()) {
                return OperatorSet.NUMBER_OPERATOR_SET;
            } else if (constructor == realm.getBigIntConstructor()) {
                return OperatorSet.BIGINT_OPERATOR_SET;
            } else if (constructor == realm.getStringConstructor()) {
                return OperatorSet.STRING_OPERATOR_SET;
            } else {
                return findOperatorDefinitions(constructor);
            }
        }

        protected JSContext getContext() {
            return context;
        }

        protected boolean tableIsObject(Object table) {
            if (tableIsObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                tableIsObjectNode = insert(IsObjectNode.create());
            }
            return tableIsObjectNode.executeBoolean(table);
        }

        protected boolean tableHasKey(Object table, TruffleString key) {
            if (tableHasKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                tableHasKeyNode = insert(JSHasPropertyNode.create());
            }
            return tableHasKeyNode.executeBoolean(table, key);
        }

        protected Object tableGet(Object table, TruffleString key) {
            if (tableGetNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                tableGetNode = insert(ReadElementNode.create(getContext()));
            }
            return tableGetNode.executeWithTargetAndIndex(table, key);
        }

        protected boolean isCallable(Object operatorImplementation) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(operatorImplementation);
        }

        protected long getOpenSetLength(Object openSet) {
            if (getOpenSetLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOpenSetLengthNode = insert(JSGetLengthNode.create(getContext()));
            }
            return getOpenSetLengthNode.executeLong(openSet);
        }

        protected Object readOpenSetElement(Object openSet, long index) {
            if (readOpenSetElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readOpenSetElementNode = insert(ReadElementNode.create(getContext()));
            }
            return readOpenSetElementNode.executeWithTargetAndIndex(openSet, index);
        }

        protected TruffleString openOperatorToString(Object openOperator) {
            if (openOperatorToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                openOperatorToStringNode = insert(JSToStringNode.create());
            }
            return openOperatorToStringNode.executeString(openOperator);
        }

        protected boolean isJSConstructor(Object type) {
            if (typeIsJSObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                typeIsJSObjectNode = IsJSObjectNode.create();
            }
            if (typeIsConstructorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                typeIsConstructorNode = IsConstructorNode.create();
            }
            return typeIsJSObjectNode.executeBoolean(type) && typeIsConstructorNode.executeBoolean(type);
        }

        protected boolean hasOperatorDefinitions(JSDynamicObject constructor) {
            if (hasOperatorDefinitionsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasOperatorDefinitionsNode = insert(HasPropertyCacheNode.create(OPERATOR_DEFINITIONS_ID, getContext(), true));
            }
            return hasOperatorDefinitionsNode.hasProperty(constructor);
        }

        protected JSDynamicObject getSuperclass(JSDynamicObject constructor) {
            if (getSuperclassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSuperclassNode = insert(GetPrototypeNode.create());
            }
            return getSuperclassNode.execute(constructor);
        }

        protected OperatorSet getOperatorDefinitions(JSDynamicObject constructor) {
            if (getOperatorDefinitionsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOperatorDefinitionsNode = insert(PropertyGetNode.createGetHidden(OPERATOR_DEFINITIONS_ID, getContext()));
            }
            return (OperatorSet) getOperatorDefinitionsNode.getValue(constructor);
        }

        protected OperatorSet findOperatorDefinitions(JSDynamicObject arg) {
            JSDynamicObject constructor = arg;
            while (constructor != Null.instance && !hasOperatorDefinitions(constructor)) {
                constructor = getSuperclass(constructor);
            }
            if (constructor == Null.instance) {
                return null;
            } else {
                return getOperatorDefinitions(constructor);
            }
        }

        protected TruffleString getClassName(Object constructor) {
            if (getClassNameNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getClassNameNode = insert(PropertyGetNode.create(JSFunction.NAME, getContext()));
            }
            if (classNameToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                classNameToStringNode = insert(JSToStringNode.create());
            }
            return classNameToStringNode.executeString(getClassNameNode.getValue(constructor));
        }
    }
}

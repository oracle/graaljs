/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

import java.util.Arrays;
import java.util.List;

public final class OperatorsBuiltins extends JSBuiltinsContainer.Lambda {

    public static final JSBuiltinsContainer BUILTINS = new OperatorsBuiltins();

    public static final HiddenKey OPERATOR_SET_ID = new HiddenKey("OperatorSet");
    protected static final HiddenKey OPERATOR_DEFINITIONS_ID = new HiddenKey("OperatorDefinitions");

    protected OperatorsBuiltins() {
        super(null);
        defineFunction("Operators", 1, JSAttributes.getDefault(),
                        (context, builtin) -> OperatorsBuiltinsFactory.OperatorsNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context)));
    }

    public abstract static class OperatorsNode extends JSBuiltinNode {
        @Child private CreateObjectNode createPrototypeNode;
        @Child private CreateMethodPropertyNode setConstructorNode;

        public OperatorsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createPrototypeNode = CreateObjectNode.create(context);
            this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        }

        @Specialization(guards = "isJSObject(table)")
        protected DynamicObject doOperators(VirtualFrame frame, DynamicObject table, Object... extraTables) {
            DynamicObject prototype = createPrototypeNode.execute(frame);
            OperatorSet operatorSet = new OperatorSet(getContext().getRealm(), getContext().incOperatorCounter(), table, extraTables);
            DynamicObject constructor = createConstructor(operatorSet);
            JSFunction.setClassPrototype(constructor, prototype);
            setConstructorNode.executeVoid(prototype, constructor);
            JSObject.set(constructor, OPERATOR_DEFINITIONS_ID, operatorSet);
            return constructor;
        }

        @TruffleBoundary
        private DynamicObject createConstructor(OperatorSet operatorSet) {
            JSFunctionData constructorFunctionData = JSFunctionData.create(getContext(), Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(getContext().getLanguage(), null, null) {
                @Child private SpecializedNewObjectNode newObjectNode = SpecializedNewObjectNode.create(getContext(), false, true, false, false);
                @Child private UpdateOverloadedShapeNode updateOverloadedShapeNode = UpdateOverloadedShapeNode.create();

                @Override
                public Object execute(VirtualFrame innerFrame) {
                    Object constructor = JSArguments.getNewTarget(innerFrame.getArguments());
                    DynamicObject object = newObjectNode.execute(innerFrame, (DynamicObject) constructor);
                    updateOverloadedShapeNode.execute(object, operatorSet);
                    return object;
                }
            }), 0, "");
            return JSFunction.create(getContext().getRealm(), constructorFunctionData);
        }

        @Specialization(guards = "!isJSObject(arg)")
        protected DynamicObject doTypeError(Object arg, @SuppressWarnings("unused") Object... extraArgs) {
            throw Errors.createTypeErrorNotAnObject(arg, this);
        }
    }

    public abstract static class UpdateOverloadedShapeNode extends Node {

        public static UpdateOverloadedShapeNode create() {
            return OperatorsBuiltinsFactory.UpdateOverloadedShapeNodeGen.create();
        }

        public abstract DynamicObject execute(DynamicObject object, OperatorSet operatorSet);

        @Specialization(guards = "oldShape.check(object)", limit = "3")
        protected DynamicObject doCached(DynamicObject object, @SuppressWarnings("unused") OperatorSet operatorSet,
                                @CachedLibrary("object") DynamicObjectLibrary dynamicObjectLibrary,
                                @Cached("object.getShape()") @SuppressWarnings("unused") Shape oldShape,
                                @Cached("doGeneric(object, operatorSet, dynamicObjectLibrary).getShape()") Shape newShape) {
            dynamicObjectLibrary.resetShape(object, newShape);
            return object;
        }

        @Specialization(replaces = {"doCached"})
        protected DynamicObject doGeneric(DynamicObject object, OperatorSet operatorSet,
                                 @CachedLibrary(limit = "0") DynamicObjectLibrary dynamicObjectLibrary) {
            dynamicObjectLibrary.putConstant(object, OPERATOR_SET_ID, operatorSet, 0);
            return object;
        }
    }

    @TruffleBoundary
    public static boolean hasOverloadedOperators(DynamicObject object) {
        return hasOverloadedOperators(object.getShape());
    }

    @TruffleBoundary
    public static boolean hasOverloadedOperators(Shape shape) {
        return shape.hasProperty(OPERATOR_SET_ID);
    }

    public static OperatorSet getOperatorSet(Object object) {
        if (JSRuntime.isNumber(object)) {
            return OperatorSet.NUMBER_OPERATOR_SET;
        } else if (JSRuntime.isBigInt(object)) {
            return OperatorSet.BIGINT_OPERATOR_SET;
        } else if (JSRuntime.isString(object)) {
            return OperatorSet.STRING_OPERATOR_SET;
        } else {
            assert JSRuntime.isObject(object) && hasOverloadedOperators((DynamicObject) object);
            return getOperatorSet((DynamicObject) object);
        }
    }

    @TruffleBoundary
    public static OperatorSet getOperatorSet(DynamicObject object) {
        return (OperatorSet) DynamicObjectLibrary.getUncached().getOrDefault(object, OperatorsBuiltins.OPERATOR_SET_ID, null);
    }

    public static boolean overloadedOperatorsAllowed(DynamicObject arg) {
        return true;
    }

    public static void checkOverloadedOperatorsAllowed(DynamicObject arg) {
        if (!overloadedOperatorsAllowed(arg)) {
            throw Errors.createTypeError("use of overloaded operators is not enabled by a `with operators from` clause");
        }
    }

    public static class OperatorSet {

        public static final EconomicSet<String> BINARY_OPERATORS;
        public static final EconomicSet<String> UNARY_OPERATORS;
        public static final EconomicSet<String> ALL_OPERATORS;

        private static final EconomicSet<String> STRING_OPEN_OPERATORS;

        static {
            List<String> binaryOperators = Arrays.asList("-", "*", "/", "%", "**", "&", "^", "|", "<<", ">>", ">>>", "==", "+", "<");
            BINARY_OPERATORS = EconomicSet.create(binaryOperators.size());
            BINARY_OPERATORS.addAll(binaryOperators);

            List<String> unaryOperators = Arrays.asList("pos", "neg", "++", "--", "~");
            UNARY_OPERATORS = EconomicSet.create(unaryOperators.size());
            UNARY_OPERATORS.addAll(unaryOperators);

            ALL_OPERATORS = EconomicSet.create(BINARY_OPERATORS.size() + UNARY_OPERATORS.size());
            ALL_OPERATORS.addAll(BINARY_OPERATORS);
            ALL_OPERATORS.addAll(UNARY_OPERATORS);

            STRING_OPEN_OPERATORS = EconomicSet.create(3);
            STRING_OPEN_OPERATORS.addAll(Arrays.asList("+", "==", "<"));
        }

        public static final OperatorSet NUMBER_OPERATOR_SET = new OperatorSet(0, BINARY_OPERATORS);
        public static final OperatorSet BIGINT_OPERATOR_SET = new OperatorSet(1, BINARY_OPERATORS);
        public static final OperatorSet STRING_OPERATOR_SET = new OperatorSet(2, STRING_OPEN_OPERATORS);

        public final int operatorCounter;
        public final EconomicMap<String, Object> selfOperatorDefinition;
        public final EconomicMap<String, Object[]> leftOperatorDefinitions;
        public final EconomicMap<String, Object[]> rightOperatorDefinitions;
        public final EconomicSet<String> openOperators;

        public OperatorSet(int operatorCounter, EconomicSet<String> openOperators) {
            this.operatorCounter = operatorCounter;
            this.selfOperatorDefinition = null;
            this.leftOperatorDefinitions = null;
            this.rightOperatorDefinitions = null;
            this.openOperators = openOperators;
        }

        @TruffleBoundary
        public OperatorSet(JSRealm realm, int operatorCounter, DynamicObject table, Object[] extraTables) {
            this.operatorCounter = operatorCounter;

            List<String> keys = JSObject.enumerableOwnNames(table);
            this.selfOperatorDefinition = EconomicMap.create(keys.size());
            for (String key : keys) {
                if (ALL_OPERATORS.contains(key)) {
                    Object value = JSObject.get(table, key);
                    if (!JSRuntime.isCallable(value)) {
                        throw Errors.createTypeError("Operators must be functions");
                    }
                    this.selfOperatorDefinition.put(key, value);
                }
            }

            Object openSet = JSObject.get(table, "open");
            if (openSet != Undefined.instance) {
                this.openOperators = EconomicSet.create();
                for (Object element : JSArray.toArray((DynamicObject) openSet)) {
                    if (!(element instanceof String) || !ALL_OPERATORS.contains((String) element)) {
                        throw Errors.createTypeError("unrecognized operator " + JSRuntime.toString(element));
                    }
                    openOperators.add((String) element);
                }
            } else {
                this.openOperators = ALL_OPERATORS;
            }

            this.leftOperatorDefinitions = EconomicMap.create();
            this.rightOperatorDefinitions = EconomicMap.create();
            for (Object extraTable : extraTables) {
                if (!(extraTable instanceof DynamicObject)) {
                    throw Errors.createTypeErrorNotAnObject(extraTable);
                }
                DynamicObject extraTableDynObj = (DynamicObject) extraTable;
                if (JSObject.hasProperty(extraTableDynObj, "left")) {
                    if (JSObject.hasProperty(extraTableDynObj, "right")) {
                        throw Errors.createTypeError("overload table must not be both left and right");
                    }
                    Object leftType = JSObject.get(extraTableDynObj, "left");
                    OperatorSet leftSet = getOperatorSetOfClass(realm, (DynamicObject) leftType, "the left: value must be a class with operators overloaded");
                    for (String operator : JSObject.enumerableOwnNames(extraTableDynObj)) {
                        if (ALL_OPERATORS.contains(operator)) {
                            Object operatorImplementation = JSObject.get(extraTableDynObj, operator);
                            if (!JSRuntime.isCallable(operatorImplementation)) {
                                throw Errors.createTypeError("Operators must be functions");
                            }
                            if (!leftSet.openOperators.contains(operator)) {
                                throw Errors.createTypeError("the operator " + operator + " may not be overloaded on the provided type");
                            }
                            addRightOperator(operator, leftSet, operatorImplementation);
                        }
                    }
                } else {
                    if (!JSObject.hasProperty(extraTableDynObj, "right")) {
                        throw Errors.createTypeError("Either left: or right: must be provided");
                    }
                    Object rightType = JSObject.get(extraTableDynObj, "right");
                    OperatorSet rightSet = getOperatorSetOfClass(realm, (DynamicObject) rightType, "the right: value must be a class with operators overloaded");
                    for (String operator : JSObject.enumerableOwnNames(extraTableDynObj)) {
                        if (ALL_OPERATORS.contains(operator)) {
                            Object operatorImplementation = JSObject.get(extraTableDynObj, operator);
                            if (!JSRuntime.isCallable(operatorImplementation)) {
                                throw Errors.createTypeError("Operators must be functions");
                            }
                            if (!rightSet.openOperators.contains(operator)) {
                                throw Errors.createTypeError("the operator " + operator + " may not be overloaded on the provided type");
                            }
                            addLeftOperation(operator, rightSet, operatorImplementation);
                        }
                    }
                }
            }
        }

        private void addLeftOperation(String operator, OperatorSet rightSet, Object operatorImplementation) {
            if (!leftOperatorDefinitions.containsKey(operator)) {
                leftOperatorDefinitions.put(operator, new DynamicObject[this.operatorCounter]);
            }
            leftOperatorDefinitions.get(operator)[rightSet.operatorCounter] = operatorImplementation;
        }

        private void addRightOperator(String operator, OperatorSet leftSet, Object operatorImplementation) {
            if (!rightOperatorDefinitions.containsKey(operator)) {
                rightOperatorDefinitions.put(operator, new DynamicObject[this.operatorCounter]);
            }
            rightOperatorDefinitions.get(operator)[leftSet.operatorCounter] = operatorImplementation;
        }

        private static OperatorSet getOperatorSetOfClass(JSRealm realm, DynamicObject constructor, String errorMessage) {
            if (constructor == realm.getNumberConstructor()) {
                return NUMBER_OPERATOR_SET;
            } else if (constructor == realm.getBigIntConstructor()) {
                return BIGINT_OPERATOR_SET;
            } else if (constructor == realm.getStringConstructor()) {
                return STRING_OPERATOR_SET;
            } else {
                Object operatorSetObj = JSObject.get(constructor, OPERATOR_DEFINITIONS_ID);
                if (!JSObject.hasProperty(constructor, OPERATOR_DEFINITIONS_ID)) {
                    throw Errors.createTypeError(errorMessage);
                }
                return (OperatorSet) operatorSetObj;
            }
        }
    }
}

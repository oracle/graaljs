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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.HasPropertyCacheNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.OperatorSet;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

public final class OperatorsBuiltins extends JSBuiltinsContainer.Lambda {

    public static final JSBuiltinsContainer BUILTINS = new OperatorsBuiltins();

    protected static final HiddenKey OPERATOR_DEFINITIONS_ID = new HiddenKey("OperatorDefinitions");

    private static final String LEFT_ID = "left";
    private static final String RIGHT_ID = "right";
    private static final String OPEN_ID = "open";

    protected OperatorsBuiltins() {
        super(null);
        defineFunction("Operators", 1, JSAttributes.getDefault(),
                        (context, builtin) -> OperatorsBuiltinsFactory.OperatorsNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context)));
    }

    /**
     * This is a stub function that always returns {@code true}. If we ever implement the
     * {@code with operators from} part of the operator overloading proposal, this is where we would
     * perform the check.
     */
    @SuppressWarnings("unused")
    public static boolean overloadedOperatorsAllowed(DynamicObject arg) {
        return true;
    }

    public static void checkOverloadedOperatorsAllowed(DynamicObject arg, Node originatingNode) {
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
        protected DynamicObject doOperators(VirtualFrame frame, Object table, Object... extraTables) {
            DynamicObject prototype = createPrototypeNode.execute(frame);
            OperatorSet operatorSet = constructOperatorSetNode.execute(table, extraTables);
            DynamicObject constructor = createConstructor(operatorSet);
            JSFunction.setClassPrototype(constructor, prototype);
            setConstructorNode.executeVoid(prototype, constructor);
            setOperatorDefinitionsNode.setValue(constructor, operatorSet);
            return constructor;
        }

        @TruffleBoundary
        private DynamicObject createConstructor(OperatorSet operatorSet) {
            JSFunctionData constructorFunctionData = JSFunctionData.create(getContext(), Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(getContext().getLanguage(), null, null) {
                @Child private SpecializedNewObjectNode newObjectNode = SpecializedNewObjectNode.create(getContext(), false, true, false, false);
                @Child private UpdateOverloadedShapeNode updateOverloadedShapeNode = UpdateOverloadedShapeNode.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object constructor = JSArguments.getNewTarget(frame.getArguments());
                    DynamicObject object = newObjectNode.execute(frame, (DynamicObject) constructor);
                    updateOverloadedShapeNode.execute(object, operatorSet);
                    return object;
                }
            }), 0, "");
            return JSFunction.create(getContext().getRealm(), constructorFunctionData);
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
            dynamicObjectLibrary.putConstant(object, OperatorSet.OPERATOR_SET_ID, operatorSet, 0);
            return object;
        }
    }

    public abstract static class ConstructOperatorSetNode extends JavaScriptBaseNode {

        @Child private JSHasPropertyNode tableHasKeyNode;
        @Child private ReadElementNode tableGetNode;
        @Child private HasPropertyCacheNode hasOperatorDefinitionsNode;
        @Child private GetPrototypeNode getSuperclassNode;
        @Child private PropertyGetNode getOperatorDefinitionsNode;
        @Child private JSGetLengthNode getOpenSetLengthNode;
        @Child private ReadElementNode readOpenSetElementNode;
        @Child private PropertyGetNode getClassNameNode;

        protected final JSContext context;

        protected ConstructOperatorSetNode(JSContext context) {
            this.context = context;
        }

        public static ConstructOperatorSetNode create(JSContext context) {
            return OperatorsBuiltinsFactory.ConstructOperatorSetNodeGen.create(context);
        }

        public abstract OperatorSet execute(Object table, Object[] extraTables);

        @Specialization
        protected OperatorSet construct(DynamicObject table, Object[] extraTables, @CachedContext(JavaScriptLanguage.class) JSRealm realm) {

            int operatorCounter = getContext().incOperatorCounter();

            EconomicMap<String, Object> selfOperatorDefinitions = Boundaries.economicMapCreate();
            for (String operator : OperatorSet.ALL_OPERATORS) {
                if (tableHasKey(table, operator)) {
                    Object value = tableGet(table, operator);
                    if (!JSRuntime.isCallable(value)) {
                        throw Errors.createTypeError(Boundaries.stringFormat("the implementation of the operator [[Class]] %s [[Class]] is not a function", operator), this);
                    }
                    Boundaries.economicMapPut(selfOperatorDefinitions, operator, value);
                }
            }

            EconomicSet<String> openOperators;
            if (tableHasKey(table, OPEN_ID)) {
                openOperators = Boundaries.economicSetCreate();
                Object openSet = tableGet(table, OPEN_ID);
                long openSetLength = getOpenSetLength(openSet);
                for (int i = 0; i < openSetLength; i++) {
                    Object element = readOpenSetElement(openSet, i);
                    if (!(JSRuntime.isString(element)) || !OperatorSet.ALL_OPERATORS.contains(JSRuntime.toStringIsString(element))) {
                        throw Errors.createTypeError("unrecognized operator " + JSRuntime.toString(element), this);
                    }
                    Boundaries.economicSetAdd(openOperators, JSRuntime.toStringIsString(element));
                }
            } else {
                openOperators = OperatorSet.ALL_OPERATORS;
            }

            EconomicMap<String, Object[]> leftOperatorDefinitions = Boundaries.economicMapCreate();
            EconomicMap<String, Object[]> rightOperatorDefinitions = Boundaries.economicMapCreate();
            for (Object extraTable : extraTables) {
                if (!JSRuntime.isObject(extraTable)) {
                    throw Errors.createTypeErrorNotAnObject(extraTable, this);
                }
                if (tableHasKey(extraTable, LEFT_ID)) {
                    if (tableHasKey(extraTable, RIGHT_ID)) {
                        throw Errors.createTypeError("overload table must not be both left and right", this);
                    }
                    Object leftType = tableGet(extraTable, LEFT_ID);
                    if (!(JSRuntime.isObject(leftType) && JSRuntime.isConstructor(leftType))) {
                        throw Errors.createTypeError("the left: value must be an ECMAScript constructor", this);
                    }
                    OperatorSet leftSet = getOperatorSetOfClass(realm, (DynamicObject) leftType);
                    if (leftSet == null) {
                        throw Errors.createTypeError(Boundaries.stringFormat("the left: value %s must be a class with operators overloaded", getClassName(leftType)), this);
                    }
                    for (String operator : OperatorSet.BINARY_OPERATORS) {
                        if (tableHasKey(extraTable, operator)) {
                            Object operatorImplementation = tableGet(extraTable, operator);
                            if (!JSRuntime.isCallable(operatorImplementation)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the implementation of the operator %s %s [[Class]] is not a function", getClassName(leftType), operator), this);
                            }
                            if (!leftSet.isOperatorOpen(operator)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the operator %s may not be overloaded on the provided type %s", operator, getClassName(leftType), this));
                            }
                            if (!Boundaries.economicMapContainsKey(rightOperatorDefinitions, operator)) {
                                Boundaries.economicMapPut(rightOperatorDefinitions, operator, new DynamicObject[operatorCounter]);
                            }
                            Boundaries.economicMapGet(rightOperatorDefinitions, operator)[leftSet.getOperatorCounter()] = operatorImplementation;
                        }
                    }
                } else {
                    if (!tableHasKey(extraTable, RIGHT_ID)) {
                        throw Errors.createTypeError("Either left: or right: must be provided", this);
                    }
                    Object rightType = tableGet(extraTable, RIGHT_ID);
                    if (!(JSRuntime.isObject(rightType) && JSRuntime.isConstructor(rightType))) {
                        throw Errors.createTypeError("the right: value must be an ECMAScript constructor", this);
                    }
                    OperatorSet rightSet = getOperatorSetOfClass(realm, (DynamicObject) rightType);
                    if (rightSet == null) {
                        throw Errors.createTypeError(Boundaries.stringFormat("the right: value %s must be a class with operators overloaded", getClassName(rightType)), this);
                    }
                    for (String operator : OperatorSet.BINARY_OPERATORS) {
                        if (tableHasKey(extraTable, operator)) {
                            Object operatorImplementation = tableGet(extraTable, operator);
                            if (!JSRuntime.isCallable(operatorImplementation)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the implementation of the operator [[Class]] %s %s is not a function", operator, getClassName(rightType)), this);
                            }
                            if (!rightSet.isOperatorOpen(operator)) {
                                throw Errors.createTypeError(Boundaries.stringFormat("the operator %s may not be overloaded on the provided type %s", operator, getClassName(rightType), this));
                            }
                            if (!leftOperatorDefinitions.containsKey(operator)) {
                                leftOperatorDefinitions.put(operator, new DynamicObject[operatorCounter]);
                            }
                            leftOperatorDefinitions.get(operator)[rightSet.getOperatorCounter()] = operatorImplementation;
                        }
                    }
                }
            }

            return new OperatorSet(operatorCounter, selfOperatorDefinitions, leftOperatorDefinitions, rightOperatorDefinitions, openOperators);
        }

        protected OperatorSet getOperatorSetOfClass(JSRealm realm, DynamicObject constructor) {
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

        protected boolean tableHasKey(Object table, String key) {
            if (tableHasKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                tableHasKeyNode = insert(JSHasPropertyNode.create());
            }
            return tableHasKeyNode.executeBoolean(table, key);
        }

        protected Object tableGet(Object table, String key) {
            if (tableGetNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                tableGetNode = insert(ReadElementNode.create(getContext()));
            }
            return tableGetNode.executeWithTargetAndIndex(table, key);
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

        protected boolean hasOperatorDefinitions(DynamicObject constructor) {
            if (hasOperatorDefinitionsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasOperatorDefinitionsNode = insert(HasPropertyCacheNode.create(OPERATOR_DEFINITIONS_ID, getContext(), true));
            }
            return hasOperatorDefinitionsNode.hasProperty(constructor);
        }

        protected DynamicObject getSuperclass(DynamicObject constructor) {
            if (getSuperclassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSuperclassNode = insert(GetPrototypeNode.create());
            }
            return getSuperclassNode.executeJSObject(constructor);
        }

        protected OperatorSet getOperatorDefinitions(DynamicObject constructor) {
            if (getOperatorDefinitionsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOperatorDefinitionsNode = insert(PropertyGetNode.createGetHidden(OPERATOR_DEFINITIONS_ID, getContext()));
            }
            return (OperatorSet) getOperatorDefinitionsNode.getValue(constructor);
        }

        protected OperatorSet findOperatorDefinitions(DynamicObject arg) {
            DynamicObject constructor = arg;
            while (constructor != Null.instance && !hasOperatorDefinitions(constructor)) {
                constructor = getSuperclass(constructor);
            }
            if (constructor == Null.instance) {
                return null;
            } else {
                return getOperatorDefinitions(constructor);
            }
        }

        protected String getClassName(Object constructor) {
            if (getClassNameNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getClassNameNode = insert(PropertyGetNode.create(JSFunction.NAME, getContext()));
            }
            return getClassNameNode.getValue(constructor).toString();
        }
    }
}

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
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.HasPropertyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
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

    @SuppressWarnings("unused")
    public static boolean overloadedOperatorsAllowed(DynamicObject arg) {
        return true;
    }

    public static void checkOverloadedOperatorsAllowed(DynamicObject arg, Node originatingNode) {
        if (!overloadedOperatorsAllowed(arg)) {
            throw Errors.createTypeError("use of overloaded operators is not enabled by a `with operators from` clause", originatingNode);
        }
    }

    public abstract static class OperatorsNode extends JSBuiltinNode {
        @Child private CreateObjectNode createPrototypeNode;
        @Child private ConstructOperatorSetNode constructOperatorSetNode;
        @Child private CreateMethodPropertyNode setConstructorNode;

        public OperatorsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createPrototypeNode = CreateObjectNode.create(context);
            this.constructOperatorSetNode = ConstructOperatorSetNode.create(context);
            this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        }

        @Specialization
        protected DynamicObject doOperators(VirtualFrame frame, Object table, Object... extraTables) {
            DynamicObject prototype = createPrototypeNode.execute(frame);
            OperatorSet operatorSet = constructOperatorSetNode.execute(table, extraTables);
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

        @Child private HasPropertyCacheNode hasOperatorDefinitionsNode;
        @Child private PropertyGetNode getOperatorDefinitionsNode;

        protected final JSContext context;

        protected ConstructOperatorSetNode(JSContext context) {
            this.context = context;
        }

        public static ConstructOperatorSetNode create(JSContext context) {
            return OperatorsBuiltinsFactory.ConstructOperatorSetNodeGen.create(context);
        }

        public abstract OperatorSet execute(Object table, Object[] extraTables);

        @Specialization(limit = "3")
        protected OperatorSet doCached(Object table, Object[] extraTables,
                        @CachedContext(JavaScriptLanguage.class) JSRealm realm,
                        @CachedLibrary("table") InteropLibrary libTable,
                        @CachedLibrary(limit = "1") InteropLibrary libExtraTables,
                        @CachedLibrary(limit = "2") InteropLibrary libKeys,
                        @CachedLibrary(limit = "2") InteropLibrary libKey,
                        @CachedLibrary(limit = "1") InteropLibrary libOpenSet,
                        @CachedLibrary(limit = "1") InteropLibrary libOpenSetIterator,
                        @CachedLibrary(limit = "1") InteropLibrary libOpenSetElement) {
            try {
                int operatorCounter = getContext().incOperatorCounter();
                EconomicMap<String, Object> selfOperatorDefinitions;
                EconomicMap<String, Object[]> leftOperatorDefinitions;
                EconomicMap<String, Object[]> rightOperatorDefinitions;
                EconomicSet<String> openOperators;

                if (!libTable.hasMembers(table)) {
                    throw Errors.createTypeErrorNotAnObject(table, this);
                }
                Object keys = libTable.getMembers(table);
                selfOperatorDefinitions = Boundaries.economicMapCreate((int) libKeys.getArraySize(keys));
                for (int i = 0; i < libKeys.getArraySize(keys); i++) {
                    String key = libKey.asString(libKeys.readArrayElement(keys, i));
                    if (OperatorSet.ALL_OPERATORS.contains(key)) {
                        Object value = libTable.readMember(table, key);
                        if (!JSRuntime.isCallable(value)) {
                            throw Errors.createTypeErrorNotAFunction(value, this);
                        }
                        Boundaries.economicMapPut(selfOperatorDefinitions, key, value);
                    }
                }

                if (libTable.isMemberReadable(table, OPEN_ID)) {
                    openOperators = Boundaries.economicSetCreate();
                    Object openSet = libTable.readMember(table, OPEN_ID);
                    if (!libOpenSet.hasIterator(openSet)) {
                        throw Errors.createTypeErrorNotIterable(openSet, this);
                    }
                    Object openSetIterator = libOpenSet.getIterator(openSet);
                    while (libOpenSetIterator.hasIteratorNextElement(openSetIterator)) {
                        Object element = libOpenSetIterator.getIteratorNextElement(openSetIterator);
                        if (!libOpenSetElement.isString(element) || !OperatorSet.ALL_OPERATORS.contains(libOpenSetElement.asString(element))) {
                            throw Errors.createTypeError("unrecognized operator " + JSRuntime.toString(element), this);
                        }
                        Boundaries.economicSetAdd(openOperators, libOpenSetElement.asString(element));
                    }
                } else {
                    openOperators = OperatorSet.ALL_OPERATORS;
                }

                leftOperatorDefinitions = Boundaries.economicMapCreate();
                rightOperatorDefinitions = Boundaries.economicMapCreate();
                for (Object extraTable : extraTables) {
                    if (!libExtraTables.hasMembers(extraTable)) {
                        throw Errors.createTypeErrorNotAnObject(extraTable, this);
                    }
                    if (libExtraTables.isMemberReadable(extraTable, LEFT_ID)) {
                        if (libExtraTables.isMemberReadable(extraTable, RIGHT_ID)) {
                            throw Errors.createTypeError("overload table must not be both left and right", this);
                        }
                        Object leftType = libExtraTables.readMember(extraTable, LEFT_ID);
                        if (!JSRuntime.isObject(leftType)) {
                            throw Errors.createTypeError("the left: value must be an ECMAScript constructor", this);
                        }
                        OperatorSet leftSet = getOperatorSetOfClass(realm, (DynamicObject) leftType, "the left: value must be a class with operators overloaded");
                        Object extraTableKeys = libExtraTables.getMembers(extraTable);
                        for (int i = 0; i < libKeys.getArraySize(extraTableKeys); i++) {
                            String operator = libKey.asString(libKeys.readArrayElement(extraTableKeys, i));
                            if (OperatorSet.ALL_OPERATORS.contains(operator)) {
                                Object operatorImplementation = libExtraTables.readMember(extraTable, operator);
                                if (!JSRuntime.isCallable(operatorImplementation)) {
                                    throw Errors.createTypeErrorNotAFunction(operatorImplementation, this);
                                }
                                if (!leftSet.isOperatorOpen(operator)) {
                                    throw Errors.createTypeError("the operator " + operator + " may not be overloaded on the provided type", this);
                                }
                                if (!Boundaries.economicMapContainsKey(rightOperatorDefinitions, operator)) {
                                    Boundaries.economicMapPut(rightOperatorDefinitions, operator, new DynamicObject[operatorCounter]);
                                }
                                Boundaries.economicMapGet(rightOperatorDefinitions, operator)[leftSet.getOperatorCounter()] = operatorImplementation;
                            }
                        }
                    } else {
                        if (!libExtraTables.isMemberReadable(extraTable, RIGHT_ID)) {
                            throw Errors.createTypeError("Either left: or right: must be provided", this);
                        }
                        Object rightType = libExtraTables.readMember(extraTable, RIGHT_ID);
                        if (!JSRuntime.isObject(rightType)) {
                            throw Errors.createTypeError("the right: value must be an ECMAScript constructor", this);
                        }
                        OperatorSet rightSet = getOperatorSetOfClass(realm, (DynamicObject) rightType, "the right: value must be a class with operators overloaded");
                        Object extraTableKeys = libExtraTables.getMembers(extraTable);
                        for (int i = 0; i < libKeys.getArraySize(extraTableKeys); i++) {
                            String operator = libKey.asString(libKeys.readArrayElement(extraTableKeys, i));
                            if (OperatorSet.ALL_OPERATORS.contains(operator)) {
                                Object operatorImplementation = libExtraTables.readMember(extraTable, operator);
                                if (!JSRuntime.isCallable(operatorImplementation)) {
                                    throw Errors.createTypeErrorNotAFunction(operatorImplementation, this);
                                }
                                if (!rightSet.isOperatorOpen(operator)) {
                                    throw Errors.createTypeError("the operator " + operator + " may not be overloaded on the provided type", this);
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
            } catch (UnsupportedMessageException | UnknownIdentifierException | InvalidArrayIndexException | StopIterationException e) {
                throw Errors.createTypeError("invalid arguments to Operator function", e, this);
            }
        }

        protected OperatorSet getOperatorSetOfClass(JSRealm realm, DynamicObject constructor, String errorMessage) {
            if (constructor == realm.getNumberConstructor()) {
                return OperatorSet.NUMBER_OPERATOR_SET;
            } else if (constructor == realm.getBigIntConstructor()) {
                return OperatorSet.BIGINT_OPERATOR_SET;
            } else if (constructor == realm.getStringConstructor()) {
                return OperatorSet.STRING_OPERATOR_SET;
            } else if (hasOperatorDefinitions(constructor)) {
                return getOperatorDefinitions(constructor);
            } else {
                throw Errors.createTypeError(errorMessage, this);
            }
        }

        protected JSContext getContext() {
            return context;
        }

        protected boolean hasOperatorDefinitions(DynamicObject constructor) {
            if (hasOperatorDefinitionsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasOperatorDefinitionsNode = insert(HasPropertyCacheNode.create(OPERATOR_DEFINITIONS_ID, getContext()));
            }
            return hasOperatorDefinitionsNode.hasProperty(constructor);
        }

        protected OperatorSet getOperatorDefinitions(DynamicObject constructor) {
            if (getOperatorDefinitionsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOperatorDefinitionsNode = insert(PropertyGetNode.createGetHidden(OPERATOR_DEFINITIONS_ID, getContext()));
            }
            return (OperatorSet) getOperatorDefinitionsNode.getValue(constructor);
        }
    }
}

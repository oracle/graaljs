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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

import java.util.List;

public final class OperatorsBuiltins extends JSBuiltinsContainer.Lambda {

    public static final JSBuiltinsContainer BUILTINS = new OperatorsBuiltins();

    protected static final HiddenKey OPERATOR_SET_ID = new HiddenKey("OperatorSet");

    protected OperatorsBuiltins() {
        super(null);
        defineFunction("Operators", 1, JSAttributes.getDefault(), (context, builtin) -> OperatorsBuiltinsFactory.OperatorsNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context)));
    }

    public abstract static class OperatorsNode extends JSBuiltinNode {
        @Child private CreateObjectNode createPrototypeNode;
        @Child private CreateMethodPropertyNode setConstructorNode;

        public OperatorsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createPrototypeNode = CreateObjectNode.create(context);
            this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        }

        @Specialization
        protected DynamicObject operators(VirtualFrame frame, DynamicObject table) {
            DynamicObject prototype = createPrototypeNode.execute(frame);
            OperatorSet operatorSet = new OperatorSet(getContext().incOperatorCounter(), table);
            JSFunctionData constructorFunctionData = JSFunctionData.create(getContext(), Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(getContext().getLanguage(), null, null) {
                private DynamicObjectLibrary dynamicObjectLibrary = DynamicObjectLibrary.getFactory().createDispatched(1);

                @Override
                public Object execute(VirtualFrame innerFrame) {
                    DynamicObject object = JSOrdinary.create(getContext());
                    dynamicObjectLibrary.putConstant(object, OPERATOR_SET_ID, operatorSet, 0);
                    return object;
                }
            }), 0, "");
            DynamicObject constructor = JSFunction.create(getContext().getRealm(), constructorFunctionData);
            JSFunction.setClassPrototype(constructor, prototype);
            setConstructorNode.executeVoid(prototype, constructor);
            return constructor;
        }
    }

    public static boolean hasOverloadedOperators(DynamicObject object) {
        return object.getShape().hasProperty(OPERATOR_SET_ID);
    }

    public static boolean hasOverloadedOperators(Shape shape) {
        return shape.hasProperty(OPERATOR_SET_ID);
    }

    public static OperatorSet getOperatorSet(DynamicObject object) {
        return (OperatorSet) JSObject.get(object, OperatorsBuiltins.OPERATOR_SET_ID);
    }

    public static class OperatorSet {

        public static final OperatorSet NUMBER_OPERATOR_SET = new OperatorSet(0);
        public static final OperatorSet BIGINT_OPERATOR_SET = new OperatorSet(1);

        public static final EconomicSet<String> BINARY_OPERATORS;
        public static final EconomicSet<String> UNARY_OPERATORS;
        public static final EconomicSet<String> ALL_OPERATORS;

        static {
            String[] binaryOperators = new String[] {"-", "*", "/", "%", "**", "&", "^", "|", "<<", ">>", ">>>", "==", "+", "<" };
            BINARY_OPERATORS = EconomicSet.create(binaryOperators.length);
            for (String binaryOperator : binaryOperators) {
                BINARY_OPERATORS.add(binaryOperator);
            }
            String[] unaryOperators = new String[] { "pos", "neg", "++", "--", "~" };
            UNARY_OPERATORS = EconomicSet.create(unaryOperators.length);
            for (String unaryOperator : unaryOperators) {
                UNARY_OPERATORS.add(unaryOperator);
            }
            ALL_OPERATORS = EconomicSet.create(BINARY_OPERATORS.size() + UNARY_OPERATORS.size());
            ALL_OPERATORS.addAll(BINARY_OPERATORS);
            ALL_OPERATORS.addAll(UNARY_OPERATORS);
        }

        public final int operatorCounter;
        public final EconomicMap<String, Object> selfOperatorDefinition;
        public EconomicMap<String, DynamicObject[]> leftOperatorDefinitions;
        public EconomicMap<String, DynamicObject[]> rightOperatorDefinitions;
        public EconomicSet<String> openOperators;

        public OperatorSet(int operatorCounter) {
            this.operatorCounter = operatorCounter;
            this.selfOperatorDefinition = null;
            this.leftOperatorDefinitions = null;
            this.rightOperatorDefinitions = null;
        }

        public OperatorSet(int operatorCounter, DynamicObject table) {
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
        }
    }
}

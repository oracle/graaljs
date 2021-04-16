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
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.OperatorsBuiltins.OperatorSet;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.binary.JSOverloadedBinaryNodeGen.DispatchBinaryOperatorNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToNumericOperandNode;
import com.oracle.truffle.js.nodes.cast.JSToOperandNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.Hint;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.objects.Undefined;

import static com.oracle.truffle.js.nodes.JSGuards.isString;

public abstract class JSOverloadedBinaryNode extends JavaScriptBaseNode {

    private final String overloadedOperatorName;
    private final boolean numeric;
    private final Hint hint;
    private final boolean leftToRight;

    protected JSOverloadedBinaryNode(String overloadedOperatorName, boolean numeric, Hint hint, boolean leftToRight) {
        this.overloadedOperatorName = overloadedOperatorName;
        this.numeric = numeric;
        this.hint = hint;
        this.leftToRight = leftToRight;
    }

    public static JSOverloadedBinaryNode create(String overloadedOperatorName, Hint hint) {
        return JSOverloadedBinaryNodeGen.create(overloadedOperatorName, false, hint, true);
    }

    public static JSOverloadedBinaryNode createHintNone(String overloadedOperatorName) {
        return JSOverloadedBinaryNodeGen.create(overloadedOperatorName, false, Hint.None, true);
    }

    public static JSOverloadedBinaryNode createHintNumber(String overloadedOperatorName) {
        return JSOverloadedBinaryNodeGen.create(overloadedOperatorName, false, Hint.Number, true);
    }

    public static JSOverloadedBinaryNode createHintNumberLeftToRight(String overloadedOperatorName) {
        return JSOverloadedBinaryNodeGen.create(overloadedOperatorName, false, Hint.Number, true);
    }

    public static JSOverloadedBinaryNode createHintNumberRightToLeft(String overloadedOperatorName) {
        return JSOverloadedBinaryNodeGen.create(overloadedOperatorName, false, Hint.Number, false);
    }

    public static JSOverloadedBinaryNode createHintString(String overloadedOperatorName) {
        return JSOverloadedBinaryNodeGen.create(overloadedOperatorName, false, Hint.String, true);
    }

    public static JSOverloadedBinaryNode createNumeric(String overloadedOperatorName) {
        return JSOverloadedBinaryNodeGen.create(overloadedOperatorName, true, null, true);
    }

    public abstract Object execute(Object left, Object right);

    @Specialization(guards = {"!isNumeric()", "!isAddition()"})
    protected Object doToOperandGeneric(Object left,
                    Object right,
                    @Cached("create(getHint(), !isEquality())") JSToOperandNode toOperandLeftNode,
                    @Cached("create(getHint(), !isEquality())") JSToOperandNode toOperandRightNode,
                    @Cached("create(getOverloadedOperatorName())") DispatchBinaryOperatorNode dispatchBinaryOperatorNode) {
        Object leftOperand;
        Object rightOperand;

        if (leftToRight) {
            leftOperand = toOperandLeftNode.execute(left);
            rightOperand = toOperandRightNode.execute(right);
        } else {
            rightOperand = toOperandRightNode.execute(right);
            leftOperand = toOperandLeftNode.execute(left);
        }

        return dispatchBinaryOperatorNode.execute(leftOperand, rightOperand);
    }

    @Specialization(guards = {"!isNumeric()", "isAddition()"})
    protected Object doToOperandAddition(Object left,
                    Object right,
                    @Cached("create(getHint())") JSToOperandNode toOperandLeftNode,
                    @Cached("create(getHint())") JSToOperandNode toOperandRightNode,
                    @Cached("create(getOverloadedOperatorName())") DispatchBinaryOperatorNode dispatchBinaryOperatorNode,
                    @Cached("create()") JSToStringNode toStringLeftNode,
                    @Cached("create()") JSToStringNode toStringRightNode,
                    @Cached("createBinaryProfile()") ConditionProfile leftStringProfile,
                    @Cached("createBinaryProfile()") ConditionProfile rightStringProfile,
                    @Cached("createUnoptimized()") JSAddNode addNode) {
        Object leftOperand;
        Object rightOperand;

        if (leftToRight) {
            leftOperand = toOperandLeftNode.execute(left);
            rightOperand = toOperandRightNode.execute(right);
        } else {
            rightOperand = toOperandRightNode.execute(right);
            leftOperand = toOperandLeftNode.execute(left);
        }

        if (leftStringProfile.profile(isString(leftOperand))) {
            return addNode.execute(leftOperand, toStringRightNode.executeString(rightOperand));
        } else if (rightStringProfile.profile(isString(rightOperand))) {
            return addNode.execute(toStringLeftNode.executeString(leftOperand), rightOperand);
        } else {
            return dispatchBinaryOperatorNode.execute(leftOperand, rightOperand);
        }

    }

    @Specialization(guards = {"isNumeric()"})
    protected Object doToNumericOperand(Object left,
                    Object right,
                    @Cached("create()") JSToNumericOperandNode toNumericOperandLeftNode,
                    @Cached("create()") JSToNumericOperandNode toNumericOperandRightNode,
                    @Cached("create(getOverloadedOperatorName())") DispatchBinaryOperatorNode dispatchBinaryOperatorNode) {
        Object leftOperand;
        Object rightOperand;

        if (leftToRight) {
            leftOperand = toNumericOperandLeftNode.execute(left);
            rightOperand = toNumericOperandRightNode.execute(right);
        } else {
            rightOperand = toNumericOperandRightNode.execute(right);
            leftOperand = toNumericOperandLeftNode.execute(left);
        }

        return dispatchBinaryOperatorNode.execute(leftOperand, rightOperand);
    }

    protected String getOverloadedOperatorName() {
        return overloadedOperatorName;
    }

    protected boolean isNumeric() {
        return numeric;
    }

    protected Hint getHint() {
        return hint;
    }

    protected boolean isAddition() {
        return overloadedOperatorName.equals("+");
    }

    protected boolean isEquality() {
        return overloadedOperatorName.equals("==");
    }

    @ImportStatic(OperatorSet.class)
    public abstract static class DispatchBinaryOperatorNode extends JavaScriptBaseNode {

        private final String overloadedOperatorName;

        protected DispatchBinaryOperatorNode(String overloadedOperatorName) {
            this.overloadedOperatorName = overloadedOperatorName;
        }

        public static DispatchBinaryOperatorNode create(String overloadedOperatorName) {
            return DispatchBinaryOperatorNodeGen.create(overloadedOperatorName);
        }

        protected abstract Object execute(Object left, Object right);

        @Specialization(guards = {"hasOverloadedOperators(leftShape)", "hasOverloadedOperators(rightShape)", "leftShape.check(left)", "rightShape.check(right)"})
        protected Object doOverloadedOverloaded(DynamicObject left,
                        DynamicObject right,
                        @Cached("left.getShape()") @SuppressWarnings("unused") Shape leftShape,
                        @Cached("right.getShape()") @SuppressWarnings("unused") Shape rightShape,
                        @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        @Specialization(guards = {"hasOverloadedOperators(leftShape)", "leftShape.check(left)", "isNumber(right)"})
        protected Object doOverloadedNumber(DynamicObject left,
                        Object right,
                        @Cached("left.getShape()") @SuppressWarnings("unused") Shape leftShape,
                        @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        @Specialization(guards = {"hasOverloadedOperators(leftShape)", "leftShape.check(left)"})
        protected Object doOverloadedBigInt(DynamicObject left,
                        BigInt right,
                        @Cached("left.getShape()") @SuppressWarnings("unused") Shape leftShape,
                        @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        @Specialization(guards = {"hasOverloadedOperators(leftShape)", "leftShape.check(left)", "isString(right)", "!isAddition()"})
        protected Object doOverloadedString(DynamicObject left,
                        Object right,
                        @Cached("left.getShape()") @SuppressWarnings("unused") Shape leftShape,
                        @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        @Specialization(guards = {"hasOverloadedOperators(rightShape)", "rightShape.check(right)", "isNumber(left)"})
        protected Object doNumberOverloaded(Object left,
                        DynamicObject right,
                        @Cached("right.getShape()") @SuppressWarnings("unused") Shape rightShape,
                        @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        @Specialization(guards = {"hasOverloadedOperators(rightShape)", "rightShape.check(right)"})
        protected Object doBigIntOverloaded(BigInt left,
                        DynamicObject right,
                        @Cached("right.getShape()") @SuppressWarnings("unused") Shape rightShape,
                        @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        @Specialization(guards = {"hasOverloadedOperators(rightShape)", "rightShape.check(right)", "isString(left)", "!isAddition()"})
        protected Object doStringOverloaded(Object left,
                        DynamicObject right,
                        @Cached("right.getShape()") @SuppressWarnings("unused") Shape rightShape,
                        @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        @Specialization(replaces = {"doOverloadedOverloaded", "doOverloadedNumber", "doOverloadedBigInt", "doOverloadedString", "doNumberOverloaded", "doBigIntOverloaded", "doStringOverloaded"})
        protected Object doGeneric(Object left,
                        Object right,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            Object operatorImplementation = OperatorSet.getOperatorImplementation(left, right, getOverloadedOperatorName());
            return performOverloaded(callNode, operatorImplementation, left, right);
        }

        private Object performOverloaded(JSFunctionCallNode callNode, Object operatorImplementation, Object left, Object right) {
            if (operatorImplementation == null) {
                if (isEquality()) {
                    return false;
                } else {
                    throw Errors.createTypeError("No overload found for " + getOverloadedOperatorName(), this);
                }
            }
            // What should be the value of 'this' when invoking overloaded operators?
            // Currently, we set it to 'undefined'.
            return callNode.executeCall(JSArguments.create(Undefined.instance, operatorImplementation, left, right));
        }

        protected String getOverloadedOperatorName() {
            return overloadedOperatorName;
        }

        protected boolean isAddition() {
            return overloadedOperatorName.equals("+");
        }

        protected boolean isEquality() {
            return overloadedOperatorName.equals("==");
        }
    }
}

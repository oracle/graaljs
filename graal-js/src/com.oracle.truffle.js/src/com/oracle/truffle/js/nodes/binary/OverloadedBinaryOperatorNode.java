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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.cast.JSToNumericOperandNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.objects.Undefined;

import static com.oracle.truffle.js.builtins.OperatorsBuiltins.checkOverloadedOperatorsAllowed;

@ImportStatic(JSGuards.class)
public abstract class OverloadedBinaryOperatorNode extends Node {

    private final String overloadedOperatorName;

    protected OverloadedBinaryOperatorNode(String overloadedOperatorName) {
        this.overloadedOperatorName = overloadedOperatorName;
    }

    public static OverloadedBinaryOperatorNode create(String overloadedOperatorName) {
        return OverloadedBinaryOperatorNodeGen.create(overloadedOperatorName);
    }

    public abstract Object execute(Object left, Object right);

    @Specialization(guards = {"hasOverloadedOperators(leftShape)", "hasOverloadedOperators(rightShape)", "leftShape.check(left)", "rightShape.check(right)"})
    protected Object doOverloadedOverloaded(DynamicObject left,
                                            DynamicObject right,
                                            @Cached("left.getShape()") @SuppressWarnings("unused") Shape leftShape,
                                            @Cached("right.getShape()") @SuppressWarnings("unused") Shape rightShape,
                                            @Cached("getOperatorImplementation(left, right, getOverloadedOperatorName())") Object operatorImplementation,
                                            @Cached("createCall()") JSFunctionCallNode callNode) {
        checkOverloadedOperatorsAllowed(left);
        checkOverloadedOperatorsAllowed(right);
        return performOverloaded(callNode, operatorImplementation, left, right);
    }

    @Specialization(guards = {"hasOverloadedOperators(leftShape)", "leftShape.check(left)", "isNumber(right)"})
    protected Object doOverloadedNumber(DynamicObject left,
                                        Object right,
                                        @Cached("left.getShape()") @SuppressWarnings("unused") Shape leftShape,
                                        @Cached("getOperatorImplementation(left, getNumberOperatorSet(), getOverloadedOperatorName())") Object operatorImplementation,
                                        @Cached("createCall()") JSFunctionCallNode callNode) {
        checkOverloadedOperatorsAllowed(left);
        return performOverloaded(callNode, operatorImplementation, left, right);
    }

    @Specialization(guards = {"hasOverloadedOperators(leftShape)", "leftShape.check(left)"})
    protected Object doOverloadedBigInt(DynamicObject left,
                                        BigInt right,
                                        @Cached("left.getShape()") @SuppressWarnings("unused") Shape leftShape,
                                        @Cached("getOperatorImplementation(left, getBigIntOperatorSet(), getOverloadedOperatorName())") Object operatorImplementation,
                                        @Cached("createCall()") JSFunctionCallNode callNode) {
        checkOverloadedOperatorsAllowed(left);
        return performOverloaded(callNode, operatorImplementation, left, right);
    }

    @Specialization(guards = {"hasOverloadedOperators(rightShape)", "rightShape.check(right)", "isNumber(left)"})
    protected Object doNumberOverloaded(Object left,
                                        DynamicObject right,
                                        @Cached("right.getShape()") @SuppressWarnings("unused") Shape rightShape,
                                        @Cached("getOperatorImplementation(getNumberOperatorSet(), right, getOverloadedOperatorName())") Object operatorImplementation,
                                        @Cached("createCall()") JSFunctionCallNode callNode) {
        checkOverloadedOperatorsAllowed(right);
        return performOverloaded(callNode, operatorImplementation, left, right);
    }

    @Specialization(guards = {"hasOverloadedOperators(rightShape)", "rightShape.check(right)"})
    protected Object doBigIntOverloaded(BigInt left,
                                        DynamicObject right,
                                        @Cached("right.getShape()") @SuppressWarnings("unused") Shape rightShape,
                                        @Cached("getOperatorImplementation(getBigIntOperatorSet(), right, getOverloadedOperatorName())") Object operatorImplementation,
                                        @Cached("createCall()") JSFunctionCallNode callNode) {
        checkOverloadedOperatorsAllowed(right);
        return performOverloaded(callNode, operatorImplementation, left, right);
    }

    @Specialization(replaces = {"doOverloadedOverloaded", "doOverloadedNumber", "doOverloadedBigInt", "doNumberOverloaded", "doBigIntOverloaded"})
    protected Object doGeneric(Object left,
                               Object right,
                               @Cached("create(getOverloadedOperatorName())") OverloadedBinaryOperatorNode overloadedOperatorNode,
                               @Cached("create()") JSToNumericOperandNode toNumericOperandLeftNode,
                               @Cached("create()") JSToNumericOperandNode toNumericOperandRightNode) {
        Object leftOperand = toNumericOperandLeftNode.execute(left);
        Object rightOperand = toNumericOperandRightNode.execute(right);

        return overloadedOperatorNode.execute(leftOperand, rightOperand);
    }

    private Object performOverloaded(JSFunctionCallNode callNode, Object operatorImplementation, Object left, Object right) {
        if (operatorImplementation == null) {
            throw Errors.createTypeError("No overload found for " + getOverloadedOperatorName());
        }
        return callNode.executeCall(JSArguments.create(Undefined.instance, operatorImplementation, left, right));
    }

    protected String getOverloadedOperatorName() {
        return overloadedOperatorName;
    }
}
/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;

@NodeInfo(shortName = "*")
public abstract class JSMultiplyNode extends JSBinaryNode {

    protected JSMultiplyNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        if (left instanceof JSConstantIntegerNode && right instanceof JSConstantIntegerNode) {
            int leftValue = ((JSConstantIntegerNode) left).executeInt(null);
            int rightValue = ((JSConstantIntegerNode) right).executeInt(null);
            long result = (long) leftValue * (long) rightValue;
            if (result == 0 && (leftValue < 0 || rightValue < 0)) {
                return JSConstantNode.createDouble(-0.0);
            } else if (JSRuntime.longIsRepresentableAsInt(result)) {
                return JSConstantNode.createInt((int) result);
            } else {
                return JSConstantNode.createDouble(result);
            }
        }
        return JSMultiplyNodeGen.create(left, right);
    }

    public static JSMultiplyNode create() {
        return (JSMultiplyNode) create(null, null);
    }

    public abstract Object execute(Object a, Object b);

    @Specialization(guards = "b > 0", rewriteOn = ArithmeticException.class)
    protected int doIntBLargerZero(int a, int b) {
        return Math.multiplyExact(a, b);
    }

    @Specialization(guards = "a > 0", rewriteOn = ArithmeticException.class)
    protected int doIntALargerZero(int a, int b) {
        return Math.multiplyExact(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected int doInt(int a, int b, //
                    @Cached("create()") BranchProfile resultZeroBranch) {
        int result = Math.multiplyExact(a, b);
        if (result == 0) {
            resultZeroBranch.enter();
            if (a < 0 || b < 0) {
                throw new ArithmeticException("could be -0");
            }
        }
        return result;
    }

    @Specialization
    protected double doDouble(double a, double b) {
        return a * b;
    }

    @Specialization
    @TruffleBoundary
    protected BigInt doBigInts(BigInt a, BigInt b) {
        try {
            return a.multiply(b);
        } catch (ArithmeticException ae) {
            throw Errors.createRangeErrorBigIntMaxSizeExceeded();
        }
    }

    @Specialization(replaces = "doDouble")
    protected Object doGeneric(Object a, Object b,
                    @Cached("create()") JSMultiplyNode nestedMultiplyNode,
                    @Cached("create()") JSToNumericNode toNumeric1Node,
                    @Cached("create()") JSToNumericNode toNumeric2Node) {
        Object operandA = toNumeric1Node.execute(a);
        Object operandB = toNumeric2Node.execute(b);
        JSRuntime.ensureBothSameNumericType(operandA, operandB);
        return nestedMultiplyNode.execute(operandA, operandB);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSMultiplyNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}

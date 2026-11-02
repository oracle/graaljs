/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;

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

    static boolean hasPositiveConstantOperand(int a, int b) {
        // A positive int operand rules out -0. This shortcut eliminates the -0 check for
        // PE constants without adding extra a > 0 or b > 0 checks for variable operands.
        return CompilerDirectives.inCompiledCode() &&
                        ((CompilerDirectives.isPartialEvaluationConstant(a) && a > 0) || (CompilerDirectives.isPartialEvaluationConstant(b) && b > 0));
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected static int doInt(int a, int b) {
        int result = Math.multiplyExact(a, b);
        if (hasPositiveConstantOperand(a, b)) {
            return result;
        }
        // With overflow checked above, a zero product implies at least one operand is zero,
        // so (a ^ b) < 0 and (a | b) < 0 are equivalent here. Prefer XOR: for x * x, x ^ x
        // folds to zero, eliminating this check without having to prove x * x == 0 implies x == 0.
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, result == 0) && (a ^ b) < 0) {
            throw arithmeticException();
        }
        return result;
    }

    @Specialization
    protected static double doDouble(double a, double b) {
        return a * b;
    }

    @Specialization
    @TruffleBoundary
    protected static BigInt doBigInts(BigInt a, BigInt b) {
        try {
            return a.multiply(b);
        } catch (ArithmeticException ae) {
            throw Errors.createRangeErrorBigIntMaxSizeExceeded();
        }
    }

    @InliningCutoff
    @Specialization(guards = {"hasOverloadedOperators(a) || hasOverloadedOperators(b)"})
    protected static Object doOverloaded(Object a, Object b,
                    @Cached("createNumeric(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(a, b);
    }

    protected static TruffleString getOverloadedOperatorName() {
        return Strings.SYMBOL_STAR;
    }

    @Specialization(guards = {"!hasOverloadedOperators(a)", "!hasOverloadedOperators(b)"}, replaces = "doDouble")
    protected static Object doGeneric(Object a, Object b,
                    @Bind Node node,
                    @Cached JSMultiplyNode nestedMultiplyNode,
                    @Cached JSToNumericNode toNumeric1Node,
                    @Cached JSToNumericNode toNumeric2Node,
                    @Cached @Exclusive InlinedBranchProfile mixedNumericTypes) {
        Object operandA = toNumeric1Node.execute(a);
        Object operandB = toNumeric2Node.execute(b);
        ensureBothSameNumericType(operandA, operandB, node, mixedNumericTypes);
        return nestedMultiplyNode.execute(operandA, operandB);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSMultiplyNodeGen.create(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags));
    }
}

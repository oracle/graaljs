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

import static com.oracle.truffle.js.nodes.JSGuards.isString;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantDoubleNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;

@GenerateCached(false)
@NodeInfo(shortName = "+")
public abstract class JSAddNode extends JSBinaryNode {

    protected JSAddNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right, boolean truncate) {
        if (JSConfig.UseSuperOperations) {
            if (left instanceof JSConstantIntegerNode && right instanceof JSConstantIntegerNode) {
                int leftValue = ((JSConstantIntegerNode) left).executeInt(null);
                int rightValue = ((JSConstantIntegerNode) right).executeInt(null);
                long value = (long) leftValue + (long) rightValue;
                return JSRuntime.longIsRepresentableAsInt(value) ? JSConstantNode.createInt((int) value) : JSConstantNode.createDouble(value);
            } else if (right instanceof JSConstantIntegerNode || right instanceof JSConstantDoubleNode) {
                Object rightValue = right.execute(null);
                return JSAddConstantRightNumberNodeGen.create(left, (Number) rightValue, truncate);
            } else if (left instanceof JSConstantStringNode && right instanceof JSConstantStringNode) {
                return JSConstantNode.createString(((TruffleString) left.execute(null)).concatUncached((TruffleString) right.execute(null), TruffleString.Encoding.UTF_16, false));
            } else if (left instanceof JSConstantIntegerNode || left instanceof JSConstantDoubleNode) {
                Object leftValue = left.execute(null);
                return JSAddConstantLeftNumberNodeGen.create((Number) leftValue, right, truncate);
            }
        }
        return createUnoptimized(left, right, truncate);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        return create(left, right, false);
    }

    public static JSAddNode createUnoptimized(JavaScriptNode left, JavaScriptNode right, boolean truncate) {
        if (truncate) {
            return JSAddTruncNodeGen.create(left, right);
        } else {
            return JSAddNormalNodeGen.create(left, right);
        }
    }

    public static JSAddNode createUnoptimized() {
        return createUnoptimized(null, null, false);
    }

    public abstract Object execute(Object a, Object b);

    static Object doIntOverflowStaticLong(long result) {
        if (JSRuntime.longIsRepresentableAsInt(result)) {
            return (int) result;
        } else if (JSRuntime.isSafeInteger(result)) {
            return SafeInteger.valueOf(result);
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new ArithmeticException();
        }
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    public static SafeInteger doIntSafeInteger(int a, SafeInteger b) {
        return SafeInteger.valueOf(a).addExact(b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    public static SafeInteger doSafeIntegerInt(SafeInteger a, int b) {
        return a.addExact(SafeInteger.valueOf(b));
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    public static SafeInteger doSafeInteger(SafeInteger a, SafeInteger b) {
        return a.addExact(b);
    }

    @Specialization
    public static double doDouble(double a, double b) {
        return a + b;
    }

    @Specialization
    public static BigInt doBigInt(BigInt left, BigInt right) {
        return left.add(right);
    }

    @Specialization
    public static TruffleString doString(TruffleString a, TruffleString b,
                    @Cached @Shared JSConcatStringsNode concatStringsNode) {
        return concatStringsNode.executeTString(a, b);
    }

    @Specialization
    public static TruffleString doStringInt(TruffleString a, int b,
                    @Cached @Shared JSConcatStringsNode concatStringsNode,
                    @Cached @Shared TruffleString.FromLongNode stringFromLongNode) {
        return concatStringsNode.executeTString(a, Strings.fromLong(stringFromLongNode, b));
    }

    @Specialization
    public static TruffleString doIntString(int a, TruffleString b,
                    @Cached @Shared JSConcatStringsNode concatStringsNode,
                    @Cached @Shared TruffleString.FromLongNode stringFromLongNode) {
        return concatStringsNode.executeTString(Strings.fromLong(stringFromLongNode, a), b);
    }

    @Specialization(guards = "isNumber(b)")
    public static Object doStringNumber(TruffleString a, Object b,
                    @Cached @Shared JSConcatStringsNode concatStringsNode,
                    @Cached @Shared JSDoubleToStringNode doubleToStringNode) {
        return concatStringsNode.executeTString(a, doubleToStringNode.executeString(b));
    }

    @Specialization(guards = "isNumber(a)")
    public static Object doNumberString(Object a, TruffleString b,
                    @Cached @Shared JSConcatStringsNode concatStringsNode,
                    @Cached @Shared JSDoubleToStringNode doubleToStringNode) {
        return concatStringsNode.executeTString(doubleToStringNode.executeString(a), b);
    }

    /*
     * Operators that can be overloaded use the following pattern. We introduce a specialization for
     * the overloaded case (used when at least one of the operands features overloaded operators).
     * This specialization is placed towards the end of the list of specializations, just before we
     * hit the most generic case, which is usually the only case that admits ordinary JS objects as
     * arguments. This new specialization contains within itself the added complexity of supporting
     * overloaded operators. The only change to the rest of the node is adding guards to the generic
     * cases which restrict them to cases that don't feature overloaded operators.
     */
    @InliningCutoff
    @Specialization(guards = "hasOverloadedOperators(a) || hasOverloadedOperators(b)")
    public static Object doOverloaded(Object a, Object b,
                    @Cached("createHintDefault(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(a, b);
    }

    public static TruffleString getOverloadedOperatorName() {
        return Strings.SYMBOL_PLUS;
    }

    @InliningCutoff
    @Specialization(guards = {"!hasOverloadedOperators(a)", "!hasOverloadedOperators(b)"}, replaces = {"doSafeInteger",
                    "doIntSafeInteger", "doSafeIntegerInt", "doDouble", "doBigInt", "doString", "doStringInt", "doIntString", "doStringNumber", "doNumberString"})
    public static Object doPrimitiveConversion(Object a, Object b,
                    @Bind Node node,
                    @Cached JSToPrimitiveNode toPrimitiveA,
                    @Cached JSToPrimitiveNode toPrimitiveB,
                    @Cached JSToNumericNode toNumericA,
                    @Cached JSToNumericNode toNumericB,
                    @Cached JSToStringNode toStringA,
                    @Cached JSToStringNode toStringB,
                    @Cached InlinedConditionProfile profileA,
                    @Cached InlinedConditionProfile profileB,
                    @Cached("createRecursive()") JSAddNode add,
                    @Cached InlinedBranchProfile mixedNumericTypes) {

        Object primitiveA = toPrimitiveA.executeHintDefault(a);
        Object primitiveB = toPrimitiveB.executeHintDefault(b);

        Object castA;
        Object castB;
        if (profileA.profile(node, isString(primitiveA))) {
            castA = primitiveA;
            castB = toStringB.executeString(primitiveB);
        } else if (profileB.profile(node, isString(primitiveB))) {
            castA = toStringA.executeString(primitiveA);
            castB = primitiveB;
        } else {
            castA = toNumericA.execute(primitiveA);
            castB = toNumericB.execute(primitiveB);
            ensureBothSameNumericType(castA, castB, node, mixedNumericTypes);
        }
        return add.execute(castA, castB);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        boolean truncate = this instanceof JSAddTruncNode;
        return createUnoptimized(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags), truncate);
    }
}

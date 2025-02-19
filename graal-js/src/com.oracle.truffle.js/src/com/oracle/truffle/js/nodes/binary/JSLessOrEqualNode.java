/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringOrNumberNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.Undefined;

@NodeInfo(shortName = "<=")
public abstract class JSLessOrEqualNode extends JSCompareNode {

    protected JSLessOrEqualNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSLessOrEqualNode create(JavaScriptNode left, JavaScriptNode right) {
        return JSLessOrEqualNodeGen.create(left, right);
    }

    public static JSLessOrEqualNode create() {
        return JSLessOrEqualNodeGen.create(null, null);
    }

    public abstract boolean executeBoolean(Object a, Object b);

    @Specialization
    protected static boolean doInt(int a, int b) {
        return a <= b;
    }

    @Specialization
    protected static boolean doSafeInteger(int a, SafeInteger b) {
        return a <= b.longValue();
    }

    @Specialization
    protected static boolean doSafeInteger(SafeInteger a, int b) {
        return a.longValue() <= b;
    }

    @Specialization
    protected static boolean doSafeInteger(SafeInteger a, SafeInteger b) {
        return a.longValue() <= b.longValue();
    }

    @Specialization
    protected static boolean doLong(long a, long b) {
        return a <= b;
    }

    @Specialization
    protected static boolean doDouble(double a, double b) {
        return a <= b;
    }

    @Specialization
    protected static boolean doString(TruffleString a, TruffleString b,
                    @Cached TruffleString.CompareCharsUTF16Node compareNode) {
        return Strings.compareTo(compareNode, a, b) <= 0;
    }

    @Specialization
    protected static boolean doStringDouble(TruffleString a, double b,
                    @Shared @Cached JSStringToNumberNode stringToDouble) {
        return doDouble(stringToDouble.execute(a), b);
    }

    @Specialization
    protected static boolean doDoubleString(double a, TruffleString b,
                    @Shared @Cached JSStringToNumberNode stringToDouble) {
        return doDouble(a, stringToDouble.execute(b));
    }

    @Specialization
    protected static boolean doStringBigInt(TruffleString a, BigInt b) {
        BigInt aBigInt = JSRuntime.stringToBigInt(a);
        return (aBigInt == null) ? false : doBigInt(aBigInt, b);
    }

    @Specialization
    protected static boolean doBigIntString(BigInt a, TruffleString b) {
        BigInt bBigInt = JSRuntime.stringToBigInt(b);
        return (bBigInt == null) ? false : doBigInt(a, bBigInt);
    }

    @Specialization
    protected static boolean doBigInt(BigInt a, BigInt b) {
        return a.compareTo(b) <= 0;
    }

    @Specialization
    protected static boolean doBigIntAndInt(BigInt a, int b) {
        return a.compareValueTo(b) <= 0;
    }

    @Specialization
    protected static boolean doBigIntAndNumber(BigInt a, double b) {
        if (Double.isNaN(b)) {
            return false;
        }
        return a.compareValueTo(b) <= 0;
    }

    @Specialization
    protected static boolean doIntAndBigInt(int a, BigInt b) {
        return b.compareValueTo(a) >= 0;
    }

    @Specialization
    protected static boolean doNumberAndBigInt(double a, BigInt b) {
        if (Double.isNaN(a)) {
            return false;
        }
        return b.compareValueTo(a) >= 0;
    }

    @InliningCutoff
    @Specialization(guards = {"hasOverloadedOperators(a) || hasOverloadedOperators(b)"})
    protected static boolean doOverloaded(Object a, Object b,
                    @Bind Node node,
                    @Cached("createHintNumberRightToLeft(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode,
                    @Cached(inline = true) JSToBooleanNode toBooleanNode) {
        Object result = overloadedOperatorNode.execute(b, a);
        if (result == Undefined.instance) {
            return false;
        } else {
            return !toBooleanNode.executeBoolean(node, result);
        }
    }

    protected TruffleString getOverloadedOperatorName() {
        return Strings.ANGLE_BRACKET_OPEN;
    }

    @InliningCutoff
    @Specialization(guards = {"!hasOverloadedOperators(a)", "!hasOverloadedOperators(b)"}, replaces = {
                    "doString", "doStringDouble", "doDoubleString", "doStringBigInt", "doBigIntString",
                    "doBigInt", "doBigIntAndInt", "doIntAndBigInt", "doBigIntAndNumber", "doNumberAndBigInt"})
    protected static boolean doGeneric(Object a, Object b,
                    @Cached JSToStringOrNumberNode toStringOrNumber1,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitive1,
                    @Cached JSToStringOrNumberNode toStringOrNumber2,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitive2,
                    @Cached JSLessOrEqualNode lessOrEqualNode) {
        Object aPrimitive = toPrimitive1.execute(a);
        Object bPrimitive = toPrimitive2.execute(b);
        aPrimitive = toStringOrNumber1.execute(aPrimitive);
        bPrimitive = toStringOrNumber2.execute(bPrimitive);
        return lessOrEqualNode.executeBoolean(aPrimitive, bPrimitive);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSLessOrEqualNodeGen.create(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags));
    }
}

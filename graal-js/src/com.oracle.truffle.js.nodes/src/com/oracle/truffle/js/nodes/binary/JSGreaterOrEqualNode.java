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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringOrNumberNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;

@NodeInfo(shortName = ">=")
public abstract class JSGreaterOrEqualNode extends JSCompareNode {

    protected JSGreaterOrEqualNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSGreaterOrEqualNode create(JavaScriptNode left, JavaScriptNode right) {
        return JSGreaterOrEqualNodeGen.create(left, right);
    }

    public static JSGreaterOrEqualNode create() {
        return JSGreaterOrEqualNodeGen.create(null, null);
    }

    public abstract boolean executeBoolean(Object a, Object b);

    @Specialization
    protected boolean doInt(int a, int b) {
        return a >= b;
    }

    @Specialization
    protected boolean doLargeInteger(int a, LargeInteger b) {
        return a >= b.longValue();
    }

    @Specialization
    protected boolean doLargeInteger(LargeInteger a, int b) {
        return a.longValue() >= b;
    }

    @Specialization
    protected boolean doLargeInteger(LargeInteger a, LargeInteger b) {
        return a.longValue() >= b.longValue();
    }

    @Specialization
    protected boolean doDouble(double a, double b) {
        return a >= b;
    }

    @Specialization
    protected boolean doString(String a, String b) {
        return Boundaries.stringCompareTo(a, b) >= 0;
    }

    @Specialization
    protected boolean doStringDouble(String a, double b) {
        return doDouble(stringToDouble(a), b);
    }

    @Specialization
    protected boolean doDoubleString(double a, String b) {
        return doDouble(a, stringToDouble(b));
    }

    @Specialization
    protected boolean doBigInt(BigInt a, BigInt b) {
        return a.compareTo(b) >= 0;
    }

    @Specialization
    protected boolean doBigIntAndInt(BigInt a, int b) {
        return a.compareTo(BigInt.valueOf(b)) >= 0;
    }

    @Specialization
    protected boolean doBigIntAndNumber(BigInt a, double b) {
        if (Double.isNaN(b)) {
            return false;
        }
        return a.compareValueTo(b) >= 0;
    }

    @Specialization
    protected boolean doIntAndBigInt(int a, BigInt b) {
        return b.compareTo(BigInt.valueOf(a)) <= 0;
    }

    @Specialization
    protected boolean doNumberAndBigInt(double a, BigInt b) {
        if (Double.isNaN(a)) {
            return false;
        }
        return b.compareValueTo(a) <= 0;
    }

    @Specialization(guards = {"isJavaNumber(a)", "isJavaNumber(b)"})
    protected boolean doJavaNumber(Object a, Object b) {
        return doDouble(JSRuntime.doubleValue((Number) a), JSRuntime.doubleValue((Number) b));
    }

    @Specialization(replaces = {"doInt", "doDouble", "doString", "doStringDouble", "doDoubleString", "doBigInt", "doBigIntAndNumber", "doNumberAndBigInt", "doJavaNumber"})
    protected boolean doGeneric(Object a, Object b,
                    @Cached("create()") JSToStringOrNumberNode toStringOrNumber1,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitive1,
                    @Cached("create()") JSToStringOrNumberNode toStringOrNumber2,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitive2,
                    @Cached("create()") JSGreaterOrEqualNode greaterOrEqualNode) {
        return greaterOrEqualNode.executeBoolean(toStringOrNumber1.execute(toPrimitive1.execute(a)), toStringOrNumber2.execute(toPrimitive2.execute(b)));
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSGreaterOrEqualNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}

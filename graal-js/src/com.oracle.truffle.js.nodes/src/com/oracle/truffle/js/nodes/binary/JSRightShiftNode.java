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
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

/**
 * 11.7.2 The Signed Right Shift Operator ( >> ).
 */
@NodeInfo(shortName = ">>")
public abstract class JSRightShiftNode extends JSBinaryIntegerShiftNode {

    protected JSRightShiftNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        Truncatable.truncate(left);
        Truncatable.truncate(right);
        if (JSTruffleOptions.UseSuperOperations && (right instanceof JSConstantIntegerNode)) {
            return JSRightShiftConstantNode.create(left, right);
        }
        return JSRightShiftNodeGen.create(left, right);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    public abstract Object execute(Object a, Object b);

    @Specialization
    protected int doInteger(int a, int b) {
        return a >> b;
    }

    @Specialization
    protected BigInt doBigInt(BigInt a, BigInt b,
                    @Cached("create()") JSLeftShiftNode leftShift) {
        return leftShift.doBigInt(a, b.negate());
    }

    @Specialization(guards = "!largerThan2e32(b)")
    protected int doIntDouble(int a, double b) {
        return a >> (int) ((long) b);
    }

    @Specialization
    protected Object doDouble(double a, double b,
                    @Cached("create()") JSRightShiftNode rightShift,
                    @Cached("create()") JSToInt32Node leftInt32,
                    @Cached("create()") JSToUInt32Node rightUInt32) {

        return rightShift.execute(leftInt32.executeInt(a), rightUInt32.execute(b));
    }

    @Specialization(replaces = {"doInteger", "doIntDouble", "doDouble", "doBigInt"})
    protected Object doGeneric(Object a, Object b,
                    @Cached("create()") JSRightShiftNode rightShift,
                    @Cached("create()") JSToNumericNode leftToNumeric,
                    @Cached("create()") JSToNumericNode rightToNumeric) {
        Object operandA = leftToNumeric.execute(a);
        Object operandB = rightToNumeric.execute(b);
        JSRuntime.ensureBothSameNumericType(operandA, operandB);
        return rightShift.execute(operandA, operandB);
    }

    public static JSRightShiftNode create() {
        return JSRightShiftNodeGen.create(null, null);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSRightShiftNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}

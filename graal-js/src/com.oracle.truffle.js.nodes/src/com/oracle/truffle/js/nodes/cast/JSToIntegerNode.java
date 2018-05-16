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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode.JSStringToNumberWithTrimNode;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * Basically ECMAScript ToInteger, but incorrect for values outside the int32 range. Used by
 * built-in functions that do not care about values outside this range.
 */
public abstract class JSToIntegerNode extends JavaScriptBaseNode {

    @Child private JSToNumberNode toNumberNode;

    public static JSToIntegerNode create() {
        return JSToIntegerNodeGen.create();
    }

    public abstract int executeInt(Object operand);

    @Specialization
    protected static int doInteger(int value) {
        return value;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    protected static boolean inInt32Range(double value) {
        return value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE;
    }

    @Specialization(guards = "inInt32Range(value)")
    protected static int doDoubleInt32Range(double value) {
        return (int) ((long) value & 0xFFFFFFFFL);
    }

    @Specialization(guards = "!inInt32Range(value)")
    protected static int doDouble(double value) {
        if (Double.isNaN(value)) {
            return 0;
        } else if (value > 0) {
            return Integer.MAX_VALUE;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Specialization(guards = "isUndefined(value)")
    protected static int doUndefined(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization
    protected final int doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization
    protected final int doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a BigInt value", this);
    }

    @Specialization
    protected int doString(String value,
                    @Cached("create()") JSToIntegerNode nestedToIntegerNode,
                    @Cached("create()") JSStringToNumberWithTrimNode stringToNumberNode) {
        return nestedToIntegerNode.executeInt(stringToNumberNode.executeString(value));
    }

    @Specialization(guards = "isJSObject(value)")
    protected int doJSObject(DynamicObject value) {
        return JSRuntime.toInt32(getToNumberNode().executeNumber(value));
    }

    @Specialization(guards = "isForeignObject(object)")
    protected int doCrossLanguage(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode unboxOrGetNode) {
        Object unboxedForeign = unboxOrGetNode.executeWithTarget(object);
        return JSRuntime.toInt32(getToNumberNode().executeNumber(unboxedForeign));
    }

    private JSToNumberNode getToNumberNode() {
        if (toNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toNumberNode = insert(JSToNumberNode.create());
        }
        return toNumberNode;
    }
}

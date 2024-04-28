/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Basically ECMAScript ToInteger, but correct only for values in the safe integer range. Used by
 * built-in functions that do not care about values outside this range, such as array length or
 * array index conversion. Returns long; values that do not fit into long will be clamped to
 * Long.MAX_VALUE or Long.MIN_VALUE.
 *
 * @see JSToIndexNode
 * @see JSToIntegerAsIntNode
 */
@ImportStatic(Double.class)
public abstract class JSToIntegerAsLongNode extends JavaScriptBaseNode {

    @NeverDefault
    public static JSToIntegerAsLongNode create() {
        return JSToIntegerAsLongNodeGen.create();
    }

    public abstract long executeLong(Object operand);

    @Specialization
    protected static long doInteger(int value) {
        return value;
    }

    @Specialization
    protected static long doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected static long doLong(long value) {
        return value;
    }

    @Specialization
    protected static long doSafeInteger(SafeInteger value) {
        return value.longValue();
    }

    @Specialization(guards = "!isInfinite(value)")
    protected static long doDouble(double value) {
        return (long) value;
    }

    @Specialization(guards = "isInfinite(value)")
    protected static long doDoubleInfinite(double value) {
        return value > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static long doUndefined(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static long doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization
    protected final long doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization
    protected final long doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a BigInt value", this);
    }

    @Specialization
    protected long doString(TruffleString value,
                    @Cached JSToIntegerAsLongNode nestedToIntegerNode,
                    @Cached JSStringToNumberNode stringToNumberNode) {
        return nestedToIntegerNode.executeLong(stringToNumberNode.execute(value));
    }

    @Specialization
    protected long doJSObject(JSObject value,
                    @Cached @Shared JSToNumberNode toNumberNode) {
        return JSRuntime.toInteger(toNumberNode.executeNumber(value));
    }

    @Specialization(guards = {"isJSObject(value) || isForeignObject(value)"}, replaces = "doJSObject")
    protected long doJSOrForeignObject(Object value,
                    @Cached @Shared JSToNumberNode toNumberNode) {
        return JSRuntime.toInteger(toNumberNode.executeNumber(value));
    }

}

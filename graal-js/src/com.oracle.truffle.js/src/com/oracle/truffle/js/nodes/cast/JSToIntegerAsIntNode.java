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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Basically ECMAScript ToInteger, but correct only for values in the int32 range. Used by built-in
 * functions that do not care about values outside this range, such as string index conversion.
 * Larger and smaller values will be clamped to Integer.MAX_VALUE and Integer.MIN_VALUE,
 * respectively.
 *
 * @see JSToIntegerAsLongNode
 */
public abstract class JSToIntegerAsIntNode extends JavaScriptBaseNode {

    @NeverDefault
    public static JSToIntegerAsIntNode create() {
        return JSToIntegerAsIntNodeGen.create();
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

    @Specialization(guards = "isLongRepresentableAsInt32(value)")
    protected static int doLongInt32Range(long value) {
        return (int) value;
    }

    @Specialization(guards = "!isLongRepresentableAsInt32(value)")
    protected static int doLongOther(long value) {
        if (value < 0) {
            return Integer.MIN_VALUE;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Specialization(guards = "isLongRepresentableAsInt32(value.longValue())")
    protected static int doSafeIntegerInt32Range(SafeInteger value) {
        return value.intValue();
    }

    @Specialization(guards = "!isLongRepresentableAsInt32(value.longValue())")
    protected static int doSafeIntegerOther(SafeInteger value) {
        return doLongOther(value.longValue());
    }

    @Specialization
    protected static int doDouble(double value) {
        return (int) value;
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
    protected int doString(TruffleString value,
                    @Cached JSToIntegerAsIntNode nestedToIntegerNode,
                    @Cached JSStringToNumberNode stringToNumberNode) {
        return nestedToIntegerNode.executeInt(stringToNumberNode.execute(value));
    }

    @Specialization
    protected int doJSObject(JSObject value,
                    @Shared @Cached JSToNumberNode toNumberNode) {
        return JSRuntime.toInt32(toNumberNode.executeNumber(value));
    }

    @Specialization(guards = {"isJSObject(value) || isForeignObject(value)"}, replaces = "doJSObject")
    protected int doJSOrForeignObject(Object value,
                    @Shared @Cached JSToNumberNode toNumberNode) {
        return JSRuntime.toInt32(toNumberNode.executeNumber(value));
    }
}

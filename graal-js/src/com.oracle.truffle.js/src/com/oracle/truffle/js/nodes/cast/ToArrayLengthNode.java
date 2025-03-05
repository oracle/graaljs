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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;

/**
 * Checks if the provided value is a valid length argument for the Array(len) constructor and
 * converts it to a uint32 value. Returns a negative result if the value is not a valid length.
 */
@ImportStatic({JSRuntime.class})
public abstract class ToArrayLengthNode extends JavaScriptBaseNode {

    private static final long RANGE_ERROR = JSRuntime.INVALID_ARRAY_INDEX;
    private static final long TYPE_NOT_NUMBER = JSRuntime.INVALID_SAFE_INTEGER;

    ToArrayLengthNode() {
    }

    public abstract long executeLong(Object value);

    public boolean isTypeNumber(long result) {
        return result != TYPE_NOT_NUMBER;
    }

    @Specialization
    protected static long doInt(int value) {
        return value;
    }

    @Specialization(guards = {"isValidArrayLength(value.longValue())"})
    protected static long doSafeInteger(SafeInteger value) {
        return JSRuntime.toUInt32(value.longValue());
    }

    @Specialization(guards = {"!isValidArrayLength(value.longValue())"})
    protected static long rangeError(@SuppressWarnings("unused") SafeInteger value) {
        return RANGE_ERROR;
    }

    @Specialization(guards = {"isValidArrayLength(value)"})
    protected static long doLong(long value) {
        return JSRuntime.toUInt32(value);
    }

    @Specialization(guards = {"!isValidArrayLength(value)"})
    protected static long rangeError(@SuppressWarnings("unused") long value) {
        return RANGE_ERROR;
    }

    @Specialization(guards = {"isValidArrayLength(value)"})
    protected static long doDouble(double value) {
        return JSRuntime.toUInt32((long) value);
    }

    @Specialization(guards = {"!isValidArrayLength(value)"})
    protected static long rangeError(@SuppressWarnings("unused") double value) {
        return RANGE_ERROR;
    }

    @Specialization(guards = {"!isNumber(value)", "!isNumberLong(value)"})
    protected static long typeNotNumber(@SuppressWarnings("unused") Object value) {
        return TYPE_NOT_NUMBER;
    }

}

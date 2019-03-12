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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

/**
 * This type represents an integer value, useful for all ranges up to JSRuntime.MAX_SAFE_INTEGER.
 */
@ValueType
public final class LargeInteger extends Number implements Comparable<LargeInteger>, TruffleObject {
    private final long value;

    private LargeInteger(long value) {
        this.value = value;
    }

    public static LargeInteger valueOf(int value) {
        return new LargeInteger(value);
    }

    public static LargeInteger valueOf(long value) {
        if (!JSRuntime.isSafeInteger(value)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IllegalArgumentException("not in safe integer range");
        }
        return new LargeInteger(value);
    }

    public static LargeInteger parseUnsignedInt(String value) {
        return valueOf(Integer.parseUnsignedInt(value));
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return longValue();
    }

    @Override
    public double doubleValue() {
        return longValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LargeInteger) {
            return value == ((LargeInteger) obj).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public int compareTo(LargeInteger other) {
        return Long.compareUnsigned(value, other.value);
    }

    private static final long serialVersionUID = 2017825230215806491L;

    public boolean isNegative() {
        return value < 0;
    }

    public LargeInteger incrementExact() {
        if (value == JSRuntime.MAX_SAFE_INTEGER_LONG) {
            throw new ArithmeticException();
        }
        return LargeInteger.valueOf(value + 1);
    }

    public LargeInteger decrementExact() {
        if (value == JSRuntime.MIN_SAFE_INTEGER_LONG) {
            throw new ArithmeticException();
        }
        return LargeInteger.valueOf(value - 1);
    }

    public LargeInteger addExact(LargeInteger other) {
        long result = this.value + other.value;
        if (result < JSRuntime.MIN_SAFE_INTEGER_LONG || result > JSRuntime.MAX_SAFE_INTEGER_LONG) {
            throw new ArithmeticException();
        }
        return LargeInteger.valueOf(result);
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof LargeInteger;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return LargeIntegerMessageResolutionForeign.ACCESS;
    }
}

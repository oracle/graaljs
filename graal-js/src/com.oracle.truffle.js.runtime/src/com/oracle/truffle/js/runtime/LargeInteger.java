/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.ValueType;

/**
 * This type represents an integer value, useful for all ranges up to JSRuntime.MAX_SAFE_INTEGER.
 */
@ValueType
public final class LargeInteger extends Number implements Comparable<LargeInteger> {
    private final long value;

    private LargeInteger(long value) {
        this.value = value;
    }

    public static LargeInteger valueOf(int value) {
        return new LargeInteger(value);
    }

    public static LargeInteger valueOf(long value) {
        if (!JSRuntime.isSafeInteger(value)) {
            CompilerDirectives.transferToInterpreter();
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
}

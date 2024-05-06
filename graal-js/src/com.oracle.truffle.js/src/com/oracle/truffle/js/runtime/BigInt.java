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
package com.oracle.truffle.js.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.interop.JSMetaType;

@ExportLibrary(InteropLibrary.class)
@ValueType
public final class BigInt implements Comparable<BigInt>, TruffleObject {

    static final long serialVersionUID = 6019523258212492110L;

    private final BigInteger value;
    private final boolean foreign;

    public static final BigInt ZERO = new BigInt(BigInteger.ZERO);
    public static final BigInt ONE = new BigInt(BigInteger.ONE);
    public static final BigInt NEGATIVE_ONE = new BigInt(BigInteger.valueOf(-1));
    public static final BigInt TWO = new BigInt(BigInteger.valueOf(2));

    public static final BigInt MAX_INT = new BigInt(BigInteger.valueOf(Integer.MAX_VALUE));
    public static final BigInt MIN_INT = new BigInt(BigInteger.valueOf(Integer.MIN_VALUE));

    private static final BigInteger TWO64 = BigInteger.ONE.shiftLeft(64);

    public BigInt(BigInteger v) {
        this(v, false);
    }

    private BigInt(BigInteger v, boolean foreign) {
        this.foreign = foreign;
        this.value = v;
    }

    @TruffleBoundary
    public static BigInt fromBigInteger(BigInteger value) {
        if (value.equals(BigInteger.ZERO)) {
            return ZERO;
        } else if (value.equals(BigInteger.ONE)) {
            return ONE;
        } else {
            return new BigInt(value);
        }
    }

    @TruffleBoundary
    public static BigInt fromForeignBigInteger(BigInteger value) {
        return new BigInt(value, true);
    }

    @TruffleBoundary
    public static BigInt valueOf(String s) {
        return new BigInt(parseBigInteger(s));
    }

    @TruffleBoundary
    public static BigInt valueOf(long i) {
        return new BigInt(BigInteger.valueOf(i));
    }

    @TruffleBoundary
    public static BigInt valueOfUnsigned(long i) {
        if (i >= 0) {
            return new BigInt(BigInteger.valueOf(i));
        } else {
            return new BigInt(BigInteger.valueOf(i).mod(TWO64));
        }
    }

    @TruffleBoundary
    private static BigInteger parseBigInteger(final String valueString) {

        String trimmedString = valueString.trim();

        if (trimmedString.isEmpty()) {
            return BigInteger.ZERO;
        }

        if (trimmedString.charAt(0) == '0') {
            if (trimmedString.length() > 2) {
                switch (trimmedString.charAt(1)) {
                    case 'x':
                    case 'X':
                        return new BigInteger(trimmedString.substring(2), 16);
                    case 'o':
                    case 'O':
                        return new BigInteger(trimmedString.substring(2), 8);
                    case 'b':
                    case 'B':
                        return new BigInteger(trimmedString.substring(2), 2);
                    default:
                        return new BigInteger(trimmedString, 10);
                }
            } else if (trimmedString.length() == 1) {
                return BigInteger.ZERO;
            }
        }
        return new BigInteger(trimmedString, 10);
    }

    @TruffleBoundary
    public int intValue() {
        return value.intValue();
    }

    @TruffleBoundary
    public double doubleValue() {
        return value.doubleValue();
    }

    @TruffleBoundary
    public static double doubleValueOf(BigInteger value) {
        return value.doubleValue();
    }

    public BigInteger bigIntegerValue() {
        return value;
    }

    @TruffleBoundary
    public BigInt toBigInt64() {
        return valueOf(value.longValue());
    }

    @TruffleBoundary
    public BigInt toBigUint64() {
        return new BigInt(value.mod(TWO64));
    }

    @TruffleBoundary
    public BigInt pow(int e) {
        return new BigInt(value.pow(e));
    }

    @TruffleBoundary
    public BigInt mod(BigInt m) {
        return new BigInt(value.mod(m.value));
    }

    @Override
    @TruffleBoundary
    public int compareTo(BigInt b) {
        return value.compareTo(b.value);
    }

    @TruffleBoundary
    public int compareValueTo(long b) {
        return value.compareTo(BigInteger.valueOf(b));
    }

    @TruffleBoundary
    public int compareValueTo(double b) {

        assert !Double.isNaN(b) : "unexpected NAN in BigInt value comparison";

        if (b == Double.POSITIVE_INFINITY) {
            return -1;
        } else if (b == Double.NEGATIVE_INFINITY) {
            return 1;
        } else {
            BigDecimal thisValue = new BigDecimal(value);
            BigDecimal theOtherValue = new BigDecimal(b);
            return thisValue.compareTo(theOtherValue);
        }
    }

    @TruffleBoundary
    public BigInt subtract(BigInt b) {
        return new BigInt(value.subtract(b.value));
    }

    @TruffleBoundary
    public BigInt add(BigInt b) {
        return new BigInt(value.add(b.value));
    }

    @TruffleBoundary
    public String toString(int radix) {
        return value.toString(radix);
    }

    @TruffleBoundary
    public TruffleString toTString() {
        return toTString(10);
    }

    @TruffleBoundary
    public TruffleString toTString(int radix) {
        return Strings.fromJavaString(value.toString(radix));
    }

    @TruffleBoundary
    public boolean testBit(int n) {
        return value.testBit(n);
    }

    @TruffleBoundary(allowInlining = true)
    public int signum() {
        return value.signum();
    }

    @TruffleBoundary
    public BigInt negate() {
        return new BigInt(value.negate());
    }

    @TruffleBoundary
    public BigInt not() {
        return new BigInt(value.not());
    }

    @Override
    @TruffleBoundary
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    @TruffleBoundary
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BigInt other = (BigInt) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @TruffleBoundary
    public BigInt and(BigInt b) {
        return new BigInt(value.and(b.value));
    }

    @TruffleBoundary
    public BigInt or(BigInt b) {
        return new BigInt(value.or(b.value));
    }

    @TruffleBoundary
    public BigInt xor(BigInt b) {
        return new BigInt(value.xor(b.value));
    }

    @TruffleBoundary
    public BigInt multiply(BigInt b) {
        return new BigInt(value.multiply(b.value));
    }

    @TruffleBoundary
    public BigInt divide(BigInt b) {
        return new BigInt(value.divide(b.value));
    }

    @TruffleBoundary
    public BigInt remainder(BigInt b) {
        return new BigInt(value.remainder(b.value));
    }

    @TruffleBoundary
    public BigInt shiftLeft(int b) {
        return new BigInt(value.shiftLeft(b));
    }

    @TruffleBoundary
    public BigInt shiftRight(int b) {
        return new BigInt(value.shiftRight(b));
    }

    @TruffleBoundary
    public BigInt abs() {
        return new BigInt(value.abs());
    }

    @TruffleBoundary
    public long longValueExact() {
        return value.longValueExact();
    }

    @TruffleBoundary
    public long longValue() {
        return value.longValue();
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return value.toString(10);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isNumber() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean fitsInBigInteger() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    boolean fitsInByte() {
        return value.bitLength() < Byte.SIZE;
    }

    @ExportMessage
    @TruffleBoundary
    boolean fitsInShort() {
        return value.bitLength() < Short.SIZE;
    }

    @ExportMessage
    @TruffleBoundary
    boolean fitsInInt() {
        return value.bitLength() < Integer.SIZE;
    }

    @ExportMessage
    @TruffleBoundary
    public boolean fitsInLong() {
        return value.bitLength() < Long.SIZE;
    }

    @ExportMessage
    @TruffleBoundary
    public boolean fitsInDouble() {
        if (value.bitLength() <= 53) { // 53 = size of double mantissa + 1
            return true;
        } else {
            double doubleValue = value.doubleValue();
            if (!Double.isFinite(doubleValue)) {
                return false;
            }
            return new BigDecimal(doubleValue).toBigIntegerExact().equals(value);
        }
    }

    @ExportMessage
    @TruffleBoundary
    boolean fitsInFloat() {
        if (value.bitLength() <= 24) { // 24 = size of float mantissa + 1
            return true;
        } else {
            float floatValue = value.floatValue();
            if (!Float.isFinite(floatValue)) {
                return false;
            }
            return new BigDecimal(floatValue).toBigIntegerExact().equals(value);
        }
    }

    @ExportMessage
    BigInteger asBigInteger() {
        return value;
    }

    @ExportMessage
    @TruffleBoundary
    byte asByte() throws UnsupportedMessageException {
        try {
            return value.byteValueExact();
        } catch (ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    short asShort() throws UnsupportedMessageException {
        try {
            return value.shortValueExact();
        } catch (ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    int asInt() throws UnsupportedMessageException {
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    long asLong() throws UnsupportedMessageException {
        try {
            return longValueExact();
        } catch (ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    float asFloat() throws UnsupportedMessageException {
        if (fitsInFloat()) {
            return value.floatValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    double asDouble() throws UnsupportedMessageException {
        if (fitsInDouble()) {
            return value.doubleValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return JavaScriptLanguage.class;
    }

    @TruffleBoundary
    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return toString() + 'n';
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Object getMetaObject() {
        return JSMetaType.JS_BIGINT;
    }

    public boolean isForeign() {
        return foreign;
    }

    public BigInt clearForeign() {
        return setForeign(false);
    }

    private BigInt setForeign(boolean foreign) {
        if (this.foreign == foreign) {
            return this;
        }
        return new BigInt(value, foreign);
    }

    @TruffleBoundary(allowInlining = true)
    public int bitLength() {
        return value.bitLength();
    }
}

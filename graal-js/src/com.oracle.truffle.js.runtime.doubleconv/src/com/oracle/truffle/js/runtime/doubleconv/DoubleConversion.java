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

// This file is available under and governed by the Universal Permissive License
// (UPL) 1.0 only. However, the following notice accompanied the original version
// of this file:
//
// Copyright 2011 the V8 project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of Google Inc. nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.oracle.truffle.js.runtime.doubleconv;

/**
 * This class provides the public API for the double conversion package.
 *
 * Using double-conversion version 3.3.0.
 */
public final class DoubleConversion {

    private DoubleConversion() {
        // should not be constructed
    }

    private static final int kMaxFixedDigitsBeforePoint = 60;
    private static final int kMaxFixedDigitsAfterPoint = 100;
    private static final int kMaxExponentialDigits = 120;
    private static final int kBase10MaximalLength = 17;

    /**
     * Converts a double number to its shortest string representation.
     *
     * @param value number to convert
     * @return formatted number
     */
    public static String toShortest(final double value) {
        assert Double.isFinite(value) : value;

        final DtoaBuffer buffer = new DtoaBuffer(FastDtoa.kFastDtoaMaximalLength);
        dtoaShortest(value, buffer);

        return buffer.format(DtoaMode.SHORTEST, 0);
    }

    private static void dtoaShortest(final double value, final DtoaBuffer buffer) {
        final double absValue = Math.abs(value);

        if (value < 0) {
            buffer.isNegative = true;
        }

        if (value == 0) {
            buffer.append('0');
            buffer.decimalPoint = 1;
        } else if (!fastDtoaShortest(absValue, buffer)) {
            buffer.reset();
            bignumDtoa(absValue, DtoaMode.SHORTEST, 0, buffer);
        }
    }

    /**
     * Converts a double number to a string representation with a fixed number of digits after the
     * decimal point.
     *
     * @param value number to convert.
     * @param requestedDigits number of digits after decimal point
     * @return formatted number
     */
    public static String toFixed(final double value, final int requestedDigits) {
        assert Double.isFinite(value) : value;

        final DtoaBuffer buffer = new DtoaBuffer(kMaxFixedDigitsBeforePoint + kMaxFixedDigitsAfterPoint);
        final double absValue = Math.abs(value);

        if (value < 0) {
            buffer.isNegative = true;
        }

        if (value == 0) {
            buffer.append('0');
            buffer.decimalPoint = 1;
        } else if (!fixedDtoa(absValue, requestedDigits, buffer)) {
            buffer.reset();
            bignumDtoa(absValue, DtoaMode.FIXED, requestedDigits, buffer);
        }

        return buffer.format(DtoaMode.FIXED, requestedDigits);
    }

    /**
     * Converts a double number to a string representation with a fixed number of digits.
     *
     * @param value number to convert
     * @param precision number of digits to create
     * @return formatted number
     */
    public static String toPrecision(final double value, final int precision) {
        assert Double.isFinite(value) : value;

        final DtoaBuffer buffer = new DtoaBuffer(precision);
        dtoaPrecision(value, precision, buffer);

        return buffer.format(DtoaMode.PRECISION, 0);
    }

    private static void dtoaPrecision(final double value, final int precision, final DtoaBuffer buffer) {
        final double absValue = Math.abs(value);

        if (value < 0) {
            buffer.isNegative = true;
        }

        if (value == 0) {
            for (int i = 0; i < precision; i++) {
                buffer.append('0');
            }
            buffer.decimalPoint = 1;

        } else if (!fastDtoaCounted(absValue, precision, buffer)) {
            buffer.reset();
            bignumDtoa(absValue, DtoaMode.PRECISION, precision, buffer);
        }
    }

    /**
     * Converts a double number to a string representation using the {@code BignumDtoa} algorithm
     * and the specified conversion mode and number of digits.
     *
     * @param v number to convert
     * @param mode conversion mode
     * @param digits number of digits
     * @param buffer buffer to use
     */
    public static void bignumDtoa(final double v, final DtoaMode mode, final int digits, final DtoaBuffer buffer) {
        assert v > 0 && !Double.isNaN(v) && !Double.isInfinite(v) : v;

        BignumDtoa.bignumDtoa(v, mode, digits, buffer);
    }

    /**
     * Converts a double number to its shortest string representation using the {@code FastDtoa}
     * algorithm.
     *
     * @param v number to convert
     * @param buffer buffer to use
     * @return true if conversion succeeded
     */
    public static boolean fastDtoaShortest(final double v, final DtoaBuffer buffer) {
        assert v > 0 && !Double.isNaN(v) && !Double.isInfinite(v) : v;

        return FastDtoa.grisu3(v, buffer);
    }

    /**
     * Converts a double number to a string representation with the given number of digits using the
     * {@code FastDtoa} algorithm.
     *
     * @param v number to convert
     * @param precision number of digits to generate
     * @param buffer buffer to use
     * @return true if conversion succeeded
     */
    public static boolean fastDtoaCounted(final double v, final int precision, final DtoaBuffer buffer) {
        assert v > 0 && !Double.isNaN(v) && !Double.isInfinite(v) : v;

        return FastDtoa.grisu3Counted(v, precision, buffer);
    }

    /**
     * Converts a double number to a string representation with a fixed number of digits after the
     * decimal point using the {@code FixedDtoa} algorithm.
     *
     * @param v number to convert.
     * @param digits number of digits after the decimal point
     * @param buffer buffer to use
     * @return true if conversion succeeded
     */
    public static boolean fixedDtoa(final double v, final int digits, final DtoaBuffer buffer) {
        assert v > 0 && !Double.isNaN(v) && !Double.isInfinite(v) : v;

        return FixedDtoa.fastFixedDtoa(v, digits, buffer);
    }

    /**
     * Computes a representation in exponential format with requestedDigits after the decimal point.
     * The last emitted digit is rounded. If requestedDigits equals -1, then the shortest
     * exponential representation is computed.
     *
     * @param value number to convert
     * @param requestedDigits number of digits after the decimal point
     * @return true if conversion succeeded
     */
    public static String toExponential(double value, int requestedDigits) {
        return toExponential(value, requestedDigits, true);
    }

    /**
     * Computes a representation in exponential format with requestedDigits after the decimal point.
     * The last emitted digit is rounded. If requestedDigits equals -1, then the shortest
     * exponential representation is computed.
     *
     * @param value number to convert
     * @param requestedDigits number of digits after the decimal point
     * @param uniqueZero "-0.0" is converted to "0.0".
     * @return true if conversion succeeded
     */
    public static String toExponential(double value, int requestedDigits, boolean uniqueZero) {
        assert Double.isFinite(value) : value;
        assert !(requestedDigits < -1 || requestedDigits > kMaxExponentialDigits) : requestedDigits;

        boolean sign = value < 0.0;
        double absValue = Math.abs(value);

        // Add space for digit before the decimal point.
        int kDecimalRepCapacity = kMaxExponentialDigits + 1;
        assert kDecimalRepCapacity > kBase10MaximalLength;
        final DtoaBuffer buffer = new DtoaBuffer(kDecimalRepCapacity);

        if (requestedDigits == -1) {
            dtoaShortest(absValue, buffer);
        } else {
            dtoaPrecision(absValue, requestedDigits + 1, buffer);
            assert buffer.getLength() <= requestedDigits + 1;

            for (int i = buffer.getLength(); i < requestedDigits + 1; ++i) {
                buffer.append('0');
            }
            assert buffer.getLength() == requestedDigits + 1;
        }

        boolean minus = sign && (value != 0.0 || !uniqueZero);
        return buffer.toExponentialFormat(minus);
    }
}

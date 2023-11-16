/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

// Copyright 2010 the V8 project authors. All rights reserved.
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

import java.nio.charset.StandardCharsets;

/**
 * A buffer for generating string representations of doubles.
 */
public final class DtoaBuffer {

    private static final char EXPONENT_CHARACTER = 'e';

    // The character buffer
    final byte[] chars;

    // The number of characters in the buffer
    int length = 0;

    // The position of the decimal point
    int decimalPoint = 0;

    // Is this a negative number?
    boolean isNegative = false;

    /**
     * Maximal length of numbers converted by FastDtoa
     */
    public static final int kFastDtoaMaximalLength = FastDtoa.kFastDtoaMaximalLength;

    /**
     * Create a buffer with the given capacity.
     *
     * @param capacity the capacity of the buffer.
     */
    public DtoaBuffer(final int capacity) {
        chars = new byte[capacity];
    }

    /**
     * Append a character to the buffer, increasing its length.
     *
     * @param c character
     */
    void append(final int c) {
        assert (c & 0xff) == c : c;
        chars[length++] = (byte) c;
    }

    /**
     * Clear the buffer contents and set its length to {@code 0}.
     */
    public void reset() {
        length = 0;
        decimalPoint = 0;
    }

    /**
     * Get the raw digits of this buffer as string.
     *
     * @return the raw buffer contents
     */
    public String getRawDigits() {
        return new String(chars, 0, length);
    }

    /**
     * Get the position of the decimal point.
     *
     * @return the decimal point position
     */
    public int getDecimalPoint() {
        return decimalPoint;
    }

    /**
     * Returns the number of characters in the buffer.
     *
     * @return buffer length
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the formatted buffer content as string, using the specified conversion mode and
     * padding.
     *
     * @param mode conversion mode
     * @param digitsAfterPoint number of digits after point
     * @return formatted string
     */
    public String format(final DtoaMode mode, final int digitsAfterPoint) {
        switch (mode) {
            case SHORTEST:
                if (decimalPoint < -5 || decimalPoint > 21) {
                    return toExponentialFormat(isNegative);
                } else {
                    return toFixedFormat(digitsAfterPoint, isNegative);
                }
            case FIXED:
                return toFixedFormat(digitsAfterPoint, isNegative);
            case PRECISION:
            default:
                if (decimalPoint < -5 || decimalPoint > length) {
                    return toExponentialFormat(isNegative);
                } else {
                    return toFixedFormat(digitsAfterPoint, isNegative);
                }
        }
    }

    private String toFixedFormat(final int digitsAfterPoint, boolean minus) {
        final int formatLength = calculateFixedFormatLength(digitsAfterPoint, minus);
        final StringBuilder buffer = new StringBuilder(formatLength);
        if (minus) {
            buffer.append('-');
        }
        if (decimalPoint <= 0) {
            // < 1,
            buffer.append('0');
            if (length > 0) {
                buffer.append('.');
                final int padding = -decimalPoint;
                for (int i = 0; i < padding; i++) {
                    buffer.append('0');
                }
                appendBytes(buffer, chars, 0, length);
            } else {
                decimalPoint = 1;
            }
        } else if (decimalPoint >= length) {
            // large integer, add trailing zeroes
            appendBytes(buffer, chars, 0, length);
            for (int i = length; i < decimalPoint; i++) {
                buffer.append('0');
            }
        } else {
            assert decimalPoint < length;
            // >= 1, split decimals and insert decimalPoint
            appendBytes(buffer, chars, 0, decimalPoint);
            buffer.append('.');
            appendBytes(buffer, chars, decimalPoint, length - decimalPoint);
        }

        // Create trailing zeros if requested
        if (digitsAfterPoint > 0) {
            if (decimalPoint >= length) {
                buffer.append('.');
            }
            for (int i = Math.max(0, length - decimalPoint); i < digitsAfterPoint; i++) {
                buffer.append('0');
            }
        }
        assert buffer.length() == formatLength : "expected length: " + formatLength + " actual length: " + buffer.length();
        return buffer.toString();
    }

    private int calculateFixedFormatLength(final int digitsAfterPoint, boolean minus) {
        int formatLength = minus ? 1 : 0; // '-'
        if (decimalPoint <= 0) {
            // < 1,
            formatLength += 1; // '0'
            if (length > 0) {
                formatLength += 1; // '1'
                final int padding = -decimalPoint;
                formatLength += padding; // zero padding
                formatLength += length; // digits
            } else {
                decimalPoint = 1;
            }
        } else if (decimalPoint >= length) {
            // large integer, add trailing zeroes
            formatLength += length; // digits
            formatLength += decimalPoint - length; // trailing zeroes
        } else {
            assert decimalPoint < length;
            // >= 1, split decimals and insert decimalPoint
            formatLength += decimalPoint; // digits
            formatLength += 1; // '.'
            formatLength += length - decimalPoint; // digits
        }

        // Create trailing zeros if requested
        if (digitsAfterPoint > 0) {
            if (decimalPoint >= length) {
                formatLength += 1; // '.'
            }
            formatLength += Math.max(0, digitsAfterPoint - Math.max(0, length - decimalPoint));
        }
        return formatLength;
    }

    String toExponentialFormat(boolean minus) {
        final int formatLength = calculateExponentialFormatLength(minus);
        final StringBuilder buffer = new StringBuilder(formatLength);
        if (minus) {
            buffer.append('-');
        }
        assert length != 0;
        buffer.append((char) chars[0]);
        if (length > 1) {
            // insert decimal decimalPoint if more than one digit was produced
            buffer.append('.');
            appendBytes(buffer, chars, 1, length - 1);
        }
        buffer.append(EXPONENT_CHARACTER);
        final int exponent = decimalPoint - 1;
        assert Math.abs(exponent) < 10000;
        if (exponent >= 0) {
            buffer.append('+');
        }
        buffer.append(exponent);
        assert buffer.length() == formatLength : "expected length: " + formatLength + " actual length: " + buffer.length();
        return buffer.toString();
    }

    private int calculateExponentialFormatLength(boolean minus) {
        assert length != 0;
        int formatLength = minus ? 1 : 0; // '-'
        formatLength += length;
        if (length > 1) {
            formatLength += 1; // '.'
        }
        formatLength += 2; // 'e+' or 'e-'
        int exponent = decimalPoint - 1;
        formatLength += numberOfDigits(Math.abs(exponent));
        return formatLength;
    }

    private static int numberOfDigits(int x) {
        assert x >= 0 : x;
        for (int i = 1, p = 10; i < 10; i++, p *= 10) {
            if (x < p) {
                return i;
            }
        }
        return 10;
    }

    private static void appendBytes(StringBuilder buffer, byte[] chars, int start, int len) {
        for (int i = start; i < start + len; i++) {
            buffer.append((char) chars[i]);
        }
    }

    @Override
    public String toString() {
        return "[chars:" + new String(chars, 0, length, StandardCharsets.ISO_8859_1) + ", decimalPoint:" + decimalPoint + "]";
    }
}

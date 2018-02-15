/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.js.parser;

// @formatter:off
/**
 * Representation for ECMAScript types - this maps directly to the ECMA script standard
 */
public final class JSType {
    private JSType() {
    }

    /** Max value for an uint32 in JavaScript */
    private static final long MAX_UINT = 0xFFFF_FFFFL;

    private static final double INT32_LIMIT = 4294967296.0;

    /**
     * Returns true if double number can be represented as an int. Note that it returns true for negative
     * zero. If you need to exclude negative zero, use {@link #isStrictlyRepresentableAsInt(double)}.
     *
     * @param number a double to inspect
     *
     * @return true for int representable doubles
     */
    public static boolean isRepresentableAsInt(final double number) {
        return (int) number == number;
    }

    /**
     * Returns true if double number can be represented as an int. Note that it returns false for negative
     * zero. If you don't need to distinguish negative zero, use {@link #isRepresentableAsInt(double)}.
     *
     * @param number a double to inspect
     *
     * @return true for int representable doubles
     */
    public static boolean isStrictlyRepresentableAsInt(final double number) {
        return isRepresentableAsInt(number) && isNotNegativeZero(number);
    }

    /**
     * Returns true if double number can be represented as a long. Note that it returns true for negative
     * zero. If you need to exclude negative zero, use {@link #isStrictlyRepresentableAsLong(double)}.
     *
     * @param number a double to inspect
     * @return true for long representable doubles
     */
    public static boolean isRepresentableAsLong(final double number) {
        return (long) number == number;
    }

    /**
     * Returns true if double number can be represented as a long. Note that it returns false for negative
     * zero. If you don't need to distinguish negative zero, use {@link #isRepresentableAsLong(double)}.
     *
     * @param number a double to inspect
     *
     * @return true for long representable doubles
     */
    public static boolean isStrictlyRepresentableAsLong(final double number) {
        return isRepresentableAsLong(number) && isNotNegativeZero(number);
    }

    /**
     * Returns true if the number is not the negative zero ({@code -0.0d}).
     * @param number the number to test
     * @return true if it is not the negative zero, false otherwise.
     */
    private static boolean isNotNegativeZero(final double number) {
        return Double.doubleToRawLongBits(number) != 0x8000000000000000L;
    }

    /**
     * JavaScript compliant conversion of Object to boolean See ECMA 9.2 ToBoolean
     *
     * @param obj an object
     *
     * @return a boolean
     */
    public static boolean toBoolean(final Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof Number) {
            final double num = ((Number) obj).doubleValue();
            return num != 0 && !Double.isNaN(num);
        }

        if (obj instanceof String) {
            return ((String) obj).length() > 0;
        }

        return true;
    }

    /**
     * JavaScript compliant conversion of Object to number See ECMA 9.3 ToNumber
     *
     * @param obj an object
     *
     * @return a number
     */
    public static double toNumber(final Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return toNumberGeneric(obj);
    }

    /**
     * Digit representation for a character
     *
     * @param ch a character
     * @param radix radix
     *
     * @return the digit for this character
     */
    public static int digit(final char ch, final int radix) {
        return digit(ch, radix, false);
    }

    /**
     * Digit representation for a character
     *
     * @param ch a character
     * @param radix radix
     * @param onlyIsoLatin1 iso latin conversion only
     *
     * @return the digit for this character
     */
    public static int digit(final char ch, final int radix, final boolean onlyIsoLatin1) {
        final char maxInRadix = (char) ('a' + (radix - 1) - 10);
        final char c = Character.toLowerCase(ch);

        if (c >= 'a' && c <= maxInRadix) {
            return Character.digit(ch, radix);
        }

        if (Character.isDigit(ch)) {
            if (!onlyIsoLatin1 || ch >= '0' && ch <= '9') {
                return Character.digit(ch, radix);
            }
        }

        return -1;
    }

    /**
     * JavaScript compliant String to number conversion
     *
     * @param str a string
     *
     * @return a number
     */
    public static double toNumber(final String str) {
        int end = str.length();
        if (end == 0) {
            return 0.0; // Empty string
        }

        int start = 0;
        char f = str.charAt(0);

        while (Lexer.isJSWhitespace(f)) {
            if (++start == end) {
                return 0.0d; // All whitespace string
            }
            f = str.charAt(start);
        }

        // Guaranteed to terminate even without start >= end check, as the previous loop found at
        // least one non-whitespace character.
        while (Lexer.isJSWhitespace(str.charAt(end - 1))) {
            end--;
        }

        final boolean negative;
        if (f == '-') {
            if (++start == end) {
                return Double.NaN; // Single-char "-" string
            }
            f = str.charAt(start);
            negative = true;
        } else {
            if (f == '+') {
                if (++start == end) {
                    return Double.NaN; // Single-char "+" string
                }
                f = str.charAt(start);
            }
            negative = false;
        }

        final double value;
        if (start + 1 < end && f == '0' && Character.toLowerCase(str.charAt(start + 1)) == 'x') {
            // decode hex string
            value = parseRadix(str.toCharArray(), start + 2, end, 16);
        } else if (f == 'I' && end - start == 8 && str.regionMatches(start, "Infinity", 0, 8)) {
            return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        } else {
            // Fast (no NumberFormatException) path to NaN for non-numeric strings.
            for (int i = start; i < end; i++) {
                f = str.charAt(i);
                if ((f < '0' || f > '9') && f != '.' && f != 'e' && f != 'E' && f != '+' && f != '-') {
                    return Double.NaN;
                }
            }
            try {
                value = Double.parseDouble(str.substring(start, end));
            } catch (final NumberFormatException e) {
                return Double.NaN;
            }
        }

        return negative ? -value : value;
    }

    /**
     * Converts an Object to long.
     *
     * <p>
     * Note that this returns {@link java.lang.Long#MAX_VALUE} or {@link java.lang.Long#MIN_VALUE}
     * for double values that exceed the long range, including positive and negative Infinity. It is
     * the caller's responsibility to handle such values correctly.
     * </p>
     *
     * @param obj an object
     * @return a long
     */
    public static long toLong(final Object obj) {
        return obj instanceof Long ? ((Long)obj) : toLong(toNumber(obj));
    }

    /**
     * Converts a double to long.
     *
     * @param num the double to convert
     * @return the converted long value
     */
    public static long toLong(final double num) {
        return (long) num;
    }

    /**
     * JavaScript compliant Object to int32 conversion See ECMA 9.5 ToInt32
     *
     * @param obj an object
     * @return an int32
     */
    public static int toInt32(final Object obj) {
        return toInt32(toNumber(obj));
    }

    /**
     * JavaScript compliant number to int32 conversion
     *
     * @param num a number
     * @return an int32
     */
    public static int toInt32(final double num) {
        return (int) doubleToInt32(num);
    }

    /**
     * JavaScript compliant Object to uint32 conversion
     *
     * @param obj an object
     * @return a uint32
     */
    public static long toUint32(final Object obj) {
        return toUint32(toNumber(obj));
    }

    /**
     * JavaScript compliant number to uint32 conversion
     *
     * @param num a number
     * @return a uint32
     */
    public static long toUint32(final double num) {
        return doubleToInt32(num) & MAX_UINT;
    }

    private static long doubleToInt32(final double num) {
        final int exponent = Math.getExponent(num);
        if (exponent < 31) {
            return (long) num;  // Fits into 32 bits
        }
        if (exponent >= 84) {
            // Either infinite or NaN or so large that shift / modulo will produce 0
            // (52 bit mantissa + 32 bit target width).
            return 0;
        }
        // This is rather slow and could probably be sped up using bit-fiddling.
        final double d = num >= 0 ? Math.floor(num) : Math.ceil(num);
        return (long) (d % INT32_LIMIT);
    }

    private static double parseRadix(final char[] chars, final int start, final int length, final int radix) {
        int pos = 0;

        for (int i = start; i < length; i++) {
            if (digit(chars[i], radix) == -1) {
                return Double.NaN;
            }
            pos++;
        }

        if (pos == 0) {
            return Double.NaN;
        }

        double value = 0.0;
        for (int i = start; i < start + pos; i++) {
            value *= radix;
            value += digit(chars[i], radix);
        }

        return value;
    }

    private static double toNumberGeneric(final Object obj) {
        if (obj == null) {
            return +0.0;
        }

        if (obj instanceof String) {
            return toNumber((String) obj);
        }

        if (obj instanceof Boolean) {
            return (Boolean) obj ? 1 : +0.0;
        }

        return Double.NaN;
    }
}

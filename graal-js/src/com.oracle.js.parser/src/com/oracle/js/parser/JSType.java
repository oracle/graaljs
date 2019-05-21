/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser;

/**
 * Representation for ECMAScript types - this maps directly to the ECMA script standard
 */
public final class JSType {
    private JSType() {
    }

    /**
     * Returns true if double number can be represented as an int. Note that it returns true for
     * negative zero. If you need to exclude negative zero, use
     * {@link #isStrictlyRepresentableAsInt(double)}.
     *
     * @param number a double to inspect
     *
     * @return true for int representable doubles
     */
    public static boolean isRepresentableAsInt(final double number) {
        return (int) number == number;
    }

    /**
     * Returns true if double number can be represented as an int. Note that it returns false for
     * negative zero. If you don't need to distinguish negative zero, use
     * {@link #isRepresentableAsInt(double)}.
     *
     * @param number a double to inspect
     *
     * @return true for int representable doubles
     */
    public static boolean isStrictlyRepresentableAsInt(final double number) {
        return isRepresentableAsInt(number) && isNotNegativeZero(number);
    }

    /**
     * Returns true if double number can be represented as a long. Note that it returns true for
     * negative zero. If you need to exclude negative zero, use
     * {@link #isStrictlyRepresentableAsLong(double)}.
     *
     * @param number a double to inspect
     * @return true for long representable doubles
     */
    public static boolean isRepresentableAsLong(final double number) {
        return (long) number == number;
    }

    /**
     * Returns true if double number can be represented as a long. Note that it returns false for
     * negative zero. If you don't need to distinguish negative zero, use
     * {@link #isRepresentableAsLong(double)}.
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
     *
     * @param number the number to test
     * @return true if it is not the negative zero, false otherwise.
     */
    private static boolean isNotNegativeZero(final double number) {
        return Double.doubleToRawLongBits(number) != 0x8000000000000000L;
    }
}

/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.intl;

import java.math.BigDecimal;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;

public abstract class ToIntlMathematicalValue extends JavaScriptBaseNode {
    /**
     * Determines whether the result is part of a range. If so then the result is converted to
     * {@code BigDecimal} eagerly, if possible (to simplify the validation of the bounds of the
     * range).
     */
    final boolean partOfRange;

    protected ToIntlMathematicalValue(boolean partOfRange) {
        this.partOfRange = partOfRange;
    }

    @NeverDefault
    public static ToIntlMathematicalValue create(boolean partOfRange) {
        return ToIntlMathematicalValueNodeGen.create(partOfRange);
    }

    public abstract Number executeNumber(Object value);

    @TruffleBoundary
    @Specialization
    protected Number doDouble(double value) {
        if (partOfRange && Double.isFinite(value) && !JSRuntime.isNegativeZero(value)) {
            return BigDecimal.valueOf(value);
        }
        return value;
    }

    @TruffleBoundary
    @Specialization
    protected Number doBigInt(BigInt value) {
        return new BigDecimal(value.bigIntegerValue());
    }

    @TruffleBoundary
    @Specialization
    protected Number doLong(long value) {
        return new BigDecimal(value);
    }

    @TruffleBoundary
    @Specialization
    protected Number doString(TruffleString value) {
        return parseStringNumericLiteral(Strings.toJavaString(JSRuntime.trimJSWhiteSpace(value)));
    }

    @Specialization
    protected Number doBoolean(boolean value) {
        return value ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    @Specialization(guards = {"isUndefined(value)"})
    protected Number doUndefined(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

    @Specialization(guards = "isJSNull(value)")
    protected Number doNull(@SuppressWarnings("unused") Object value) {
        return BigDecimal.ZERO;
    }

    @Specialization()
    protected Number doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization(replaces = {"doDouble", "doBigInt", "doString", "doBoolean", "doUndefined", "doNull", "doSymbol"})
    protected Number doGeneric(Object value,
                    @Cached JSToPrimitiveNode toPrimitiveNode,
                    @Cached("create(partOfRange)") ToIntlMathematicalValue nestedToIntlMVNode) {
        Object primValue = toPrimitiveNode.executeHintNumber(value);
        return nestedToIntlMVNode.executeNumber(primValue);
    }

    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal EIGHT = BigDecimal.valueOf(8);
    private static final BigDecimal SIXTEEN = BigDecimal.valueOf(16);

    private static Number parseStringNumericLiteral(String s) {
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            Number result = parseStrNumericLiteral(s);
            return (result == null) ? Double.NaN : result;
        } catch (ArithmeticException | NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private static Number parseStrNumericLiteral(String s) {
        assert s.length() >= 1;
        char ch0 = s.charAt(0);
        switch (ch0) {
            case '+':
                return parseStrUnsignedDecimalLiteral(s.substring(1));
            case '-':
                Number o = parseStrUnsignedDecimalLiteral(s.substring(1));
                if (o instanceof BigDecimal) {
                    if (((BigDecimal) o).signum() == 0) {
                        return -0d;
                    }
                    return ((BigDecimal) o).negate();
                } else if (o instanceof Double) {
                    return -(Double) o;
                }
                assert (o == null);
                return null;
            case '0':
                if (s.length() == 1) {
                    return BigDecimal.ZERO;
                } else {
                    char ch1 = s.charAt(1);
                    switch (ch1) {
                        case 'b':
                        case 'B':
                            return parseBinaryIntegerLiteral(s.substring(2));
                        case 'o':
                        case 'O':
                            return parseOctalIntegerLiteral(s.substring(2));
                        case 'x':
                        case 'X':
                            return parseHexIntegerLiteral(s.substring(2));
                    }
                }
        }
        return parseStrUnsignedDecimalLiteral(s);
    }

    private static BigDecimal parseBinaryIntegerLiteral(String s) {
        if (s.isEmpty()) {
            return null;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ('0' <= c && c <= '1') {
                result = result.multiply(TWO).add(BigDecimal.valueOf(c - '0'));
            } else {
                return null;
            }
        }
        return result;
    }

    private static BigDecimal parseOctalIntegerLiteral(String s) {
        if (s.isEmpty()) {
            return null;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ('0' <= c && c <= '7') {
                result = result.multiply(EIGHT).add(BigDecimal.valueOf(c - '0'));
            } else {
                return null;
            }
        }
        return result;
    }

    private static BigDecimal parseHexIntegerLiteral(String s) {
        if (s.isEmpty()) {
            return null;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int digit;
            if ('0' <= c && c <= '9') {
                digit = c - '0';
            } else if ('a' <= c && c <= 'f') {
                digit = 10 + (c - 'a');
            } else if ('A' <= c && c <= 'F') {
                digit = 10 + (c - 'A');
            } else {
                return null;
            }
            result = result.multiply(SIXTEEN).add(BigDecimal.valueOf(digit));
        }
        return result;
    }

    private static Number parseStrUnsignedDecimalLiteral(String s) {
        if (s.isEmpty()) {
            return null;
        }

        if ("Infinity".equals(s)) {
            return Double.POSITIVE_INFINITY;
        }

        int dotIndex = s.indexOf('.');
        int exponentIndex = Math.max(s.indexOf('e', dotIndex + 1), s.indexOf('E', dotIndex + 1));

        int fractionalPartLength;
        String digits;
        if (dotIndex == -1) {
            fractionalPartLength = 0;
            if (exponentIndex == -1) {
                digits = s;
            } else {
                digits = s.substring(0, exponentIndex);
            }
        } else {
            String integerPart = s.substring(0, dotIndex);
            String fractionalPart;
            if (exponentIndex == -1) {
                fractionalPart = s.substring(dotIndex + 1);
            } else {
                fractionalPart = s.substring(dotIndex + 1, exponentIndex);
            }
            fractionalPartLength = fractionalPart.length();
            digits = integerPart + fractionalPart;
        }

        BigDecimal result = parseDecimalDigits(digits);
        if (result == null) {
            return null;
        }
        result = result.movePointLeft(fractionalPartLength);

        if (exponentIndex != -1) {
            String exponentPart = s.substring(exponentIndex + 1);
            int exponent = parseSignedInteger(exponentPart);
            result = result.movePointRight(exponent);
        }

        return result;
    }

    private static BigDecimal parseDecimalDigits(String s) {
        if (s.isEmpty()) {
            return null;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ('0' <= c && c <= '9') {
                result = result.multiply(BigDecimal.TEN).add(BigDecimal.valueOf(c - '0'));
            } else {
                return null;
            }
        }
        return result;
    }

    private static int parseSignedInteger(String s) {
        return Integer.parseInt(s);
    }

}

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
package com.oracle.truffle.js.nodes.cast;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNodeGen.JSStringToNumberWithTrimNodeGen;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * This implements ECMA 9.3.1 ToNumber applied to the String Type.
 *
 */
public abstract class JSStringToNumberNode extends JavaScriptBaseNode {

    static final int PREFIX_LENGTH = 2;
    static final int SAFE_HEX_DIGITS = PREFIX_LENGTH + 13;
    static final int SAFE_OCTAL_DIGITS = PREFIX_LENGTH + 17;
    static final int SAFE_BINARY_DIGITS = PREFIX_LENGTH + 53;

    /** Sign + at most 16 digits, i.e. {@code Number.MIN_SAFE_INTEGER.toString().length}. */
    static final int MAX_SAFE_INTEGER_LENGTH = 17;
    static final int SMALL_INT_LENGTH = 9;

    public abstract double execute(String operand);

    protected static final boolean startsWithI(String input) {
        return input.length() >= JSRuntime.INFINITY_STRING.length() && input.length() <= (JSRuntime.INFINITY_STRING.length() + 1) && (input.charAt(0) == 'I' || input.charAt(1) == 'I');
    }

    /**
     * First two chars are valid finite double. Implies
     * {@code !startsWithI && !isHex && !isOctal && !isBinary}.
     */
    protected static final boolean startsWithValidDouble(String input) {
        char firstChar = input.charAt(0);
        if (JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '.' || firstChar == '+') {
            if (input.length() >= 2) {
                char secondChar = input.charAt(1);
                return JSRuntime.isAsciiDigit(secondChar) || secondChar == '.' || secondChar == 'e' || secondChar == 'E';
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    protected static final boolean startsWithValidInt(String input) {
        char firstChar = input.charAt(0);
        return (JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '+') && (input.length() < 2 || JSRuntime.isAsciiDigit(input.charAt(1)));
    }

    protected static final boolean allDigits(String input, int maxLength) {
        assert input.length() <= maxLength;
        for (int i = 0; i < maxLength; i++) {
            if (i >= input.length()) {
                return true;
            } else if (!JSRuntime.isAsciiDigit(input.charAt(i))) {
                return false;
            }
        }
        return false;
    }

    protected static final boolean isHex(String input) {
        return input.length() >= 2 && input.charAt(0) == '0' && (input.charAt(1) == 'x' || input.charAt(1) == 'X');
    }

    protected static final boolean isOctal(String input) {
        return input.length() >= 2 && input.charAt(0) == '0' && (input.charAt(1) == 'o' || input.charAt(1) == 'O');
    }

    protected static final boolean isBinary(String input) {
        return input.length() >= 2 && input.charAt(0) == '0' && (input.charAt(1) == 'b' || input.charAt(1) == 'B');
    }

    @Specialization(guards = "input.length() == 0")
    protected double doLengthIsZero(@SuppressWarnings("unused") String input) {
        return 0;
    }

    @Specialization(guards = "startsWithI(input)")
    protected double doInfinity(String input,
                    @Cached("createBinaryProfile()") ConditionProfile endsWithInfinity) {
        if (endsWithInfinity.profile(input.endsWith(JSRuntime.INFINITY_STRING))) {
            return JSRuntime.identifyInfinity(input, input.charAt(0));
        } else {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"input.length() > 0", "!startsWithI(input)", "!startsWithValidDouble(input)", "!isHex(input)", "!isOctal(input)", "!isBinary(input)"})
    protected double doNaN(@SuppressWarnings("unused") String input) {
        return Double.NaN;
    }

    @Specialization(guards = {"isHex(input)", "input.length() <= SAFE_HEX_DIGITS"})
    @TruffleBoundary
    protected double doHexSafe(String input) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, input.length(), 16));
    }

    @Specialization(guards = {"isHex(input)", "input.length() > SAFE_HEX_DIGITS"})
    @TruffleBoundary
    protected double doHex(String input) {
        try {
            return new BigInteger(input.substring(2), 16).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isOctal(input)", "input.length() <= SAFE_OCTAL_DIGITS"})
    @TruffleBoundary
    protected double doOctalSafe(String input) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, input.length(), 8));
    }

    @Specialization(guards = {"isOctal(input)", "input.length() > SAFE_OCTAL_DIGITS"})
    @TruffleBoundary
    protected double doOctal(String input) {
        try {
            return new BigInteger(input.substring(2), 8).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isBinary(input)", "input.length() <= SAFE_BINARY_DIGITS"})
    @TruffleBoundary
    protected double doBinarySafe(String input) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, input.length(), 2));
    }

    @Specialization(guards = {"isBinary(input)", "input.length() > SAFE_BINARY_DIGITS"})
    @TruffleBoundary
    protected double doBinary(String input) {
        try {
            return new BigInteger(input.substring(2), 2).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"input.length() > 0", "input.length() <= SMALL_INT_LENGTH", "allDigits(input, SMALL_INT_LENGTH)"})
    protected double doSmallPosInt(String input) {
        int result = 0;
        int pos = 0;
        int len = input.length();
        while (pos < len) {
            char c = input.charAt(pos);
            assert JSRuntime.isAsciiDigit(c);
            result *= 10;
            result += c - '0';
            pos++;
        }
        assert result >= 0 && checkLongResult(result, input);
        return result;
    }

    @Specialization(guards = {"input.length() > 0", "input.length() <= MAX_SAFE_INTEGER_LENGTH", "startsWithValidInt(input)"}, rewriteOn = SlowPathException.class, replaces = "doSmallPosInt")
    protected double doInteger(String input) throws SlowPathException {
        long result = JSRuntime.parseSafeInteger(input);
        if (result == JSRuntime.INVALID_SAFE_INTEGER) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new SlowPathException();
        }
        assert checkLongResult(result, input);
        return result;
    }

    @TruffleBoundary
    @Specialization(guards = {"input.length() > 0", "startsWithValidDouble(input)"}, replaces = "doInteger")
    protected double doDouble(String input) {
        return JSRuntime.parseDoubleOrNaN(input);
    }

    private static double safeIntegerToDouble(long result) {
        if (result == JSRuntime.INVALID_SAFE_INTEGER) {
            return Double.NaN;
        }
        assert JSRuntime.isSafeInteger(result);
        return result;
    }

    @TruffleBoundary
    private static boolean checkLongResult(long result, String input) {
        return Double.compare(result, JSRuntime.parseDoubleOrNaN(input)) == 0;
    }

    public abstract static class JSStringToNumberWithTrimNode extends JavaScriptBaseNode {

        @Child private JSStringToNumberNode stringToNumberNode;
        @Child private JSTrimWhitespaceNode trimWhitespaceNode;

        public static JSStringToNumberWithTrimNode create() {
            return JSStringToNumberWithTrimNodeGen.create();
        }

        public abstract double executeString(String operand);

        @Specialization
        protected double stringToNumber(String input) {
            if (stringToNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringToNumberNode = insert(JSStringToNumberNodeGen.create());
                trimWhitespaceNode = insert(JSTrimWhitespaceNode.create());
            }
            return stringToNumberNode.execute(trimWhitespaceNode.executeString(input));
        }

    }
}

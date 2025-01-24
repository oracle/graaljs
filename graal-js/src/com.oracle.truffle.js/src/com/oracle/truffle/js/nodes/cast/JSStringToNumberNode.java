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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;

/**
 * This implements ToNumber applied to the String type.
 */
@GenerateUncached
public abstract class JSStringToNumberNode extends JavaScriptBaseNode {

    public abstract double execute(TruffleString input);

    @Specialization
    protected static double trimmedStringToNumber(TruffleString input,
                    @Cached JSTrimWhitespaceNode trimWhitespaceNode,
                    @Cached JSStringToNumberNoTrimNode stringToNumberNode) {
        return stringToNumberNode.executeNoTrim(trimWhitespaceNode.executeString(input));
    }

    @NeverDefault
    public static JSStringToNumberNode create() {
        return JSStringToNumberNodeGen.create();
    }
}

@GenerateUncached
abstract class JSStringToNumberNoTrimNode extends JavaScriptBaseNode {

    static final int PREFIX_LENGTH = 2;
    static final int SAFE_HEX_DIGITS = PREFIX_LENGTH + 13;
    static final int SAFE_OCTAL_DIGITS = PREFIX_LENGTH + 17;
    static final int SAFE_BINARY_DIGITS = PREFIX_LENGTH + 53;

    /** Sign + at most 16 digits, i.e. {@code Number.MIN_SAFE_INTEGER.toString().length}. */
    static final int MAX_SAFE_INTEGER_LENGTH = 17;
    static final int SMALL_INT_LENGTH = 9;

    abstract double executeNoTrim(TruffleString input);

    protected static char charAt(TruffleString s, int i, TruffleString.ReadCharUTF16Node readChar) {
        return Strings.charAt(readChar, s, i);
    }

    protected static boolean startsWithI(TruffleString input, TruffleString.ReadCharUTF16Node readChar) {
        return Strings.length(input) >= Strings.length(Strings.INFINITY) && Strings.length(input) <= (Strings.length(Strings.INFINITY) + 1) &&
                        (charAt(input, 0, readChar) == 'I' || charAt(input, 1, readChar) == 'I');
    }

    /**
     * First two chars are valid finite double. Implies
     * {@code !startsWithI && !isHex && !isOctal && !isBinary}.
     */
    protected static boolean startsWithValidDouble(TruffleString input, TruffleString.ReadCharUTF16Node readChar) {
        char firstChar = charAt(input, 0, readChar);
        if (JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '.' || firstChar == '+') {
            if (Strings.length(input) >= 2) {
                char secondChar = charAt(input, 1, readChar);
                return JSRuntime.isAsciiDigit(secondChar) || secondChar == '.' || secondChar == 'e' || secondChar == 'E';
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    protected static boolean startsWithValidInt(TruffleString input, TruffleString.ReadCharUTF16Node readChar) {
        char firstChar = charAt(input, 0, readChar);
        return (JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '+') && (Strings.length(input) < 2 || JSRuntime.isAsciiDigit(charAt(input, 1, readChar)));
    }

    protected static boolean allDigits(TruffleString input, int maxLength, TruffleString.ReadCharUTF16Node readChar) {
        assert Strings.length(input) <= maxLength;
        for (int i = 0; i < maxLength; i++) {
            if (i >= Strings.length(input)) {
                return true;
            } else if (!JSRuntime.isAsciiDigit(charAt(input, i, readChar))) {
                return false;
            }
        }
        return false;
    }

    protected static boolean isHex(TruffleString input, TruffleString.ReadCharUTF16Node readChar) {
        return Strings.length(input) >= 2 && charAt(input, 0, readChar) == '0' && (charAt(input, 1, readChar) == 'x' || charAt(input, 1, readChar) == 'X');
    }

    protected static boolean isOctal(TruffleString input, TruffleString.ReadCharUTF16Node readChar) {
        return Strings.length(input) >= 2 && charAt(input, 0, readChar) == '0' && (charAt(input, 1, readChar) == 'o' || charAt(input, 1, readChar) == 'O');
    }

    protected static boolean isBinary(TruffleString input, TruffleString.ReadCharUTF16Node readChar) {
        return Strings.length(input) >= 2 && charAt(input, 0, readChar) == '0' && (charAt(input, 1, readChar) == 'b' || charAt(input, 1, readChar) == 'B');
    }

    @Specialization(guards = "stringLength(input) == 0")
    protected static double doLengthIsZero(@SuppressWarnings("unused") TruffleString input) {
        return 0;
    }

    @Specialization(guards = "startsWithI(input, readChar)")
    protected static double doInfinity(TruffleString input,
                    @Bind Node node,
                    @Cached InlinedConditionProfile endsWithInfinity,
                    @Cached TruffleString.RegionEqualByteIndexNode regionEqualsNode,
                    @Cached @Shared TruffleString.ReadCharUTF16Node readChar) {
        if (endsWithInfinity.profile(node, Strings.endsWith(regionEqualsNode, input, Strings.INFINITY))) {
            return JSRuntime.identifyInfinity(charAt(input, 0, readChar), Strings.length(input));
        } else {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"stringLength(input) > 0",
                    "!startsWithI(input, readChar)",
                    "!startsWithValidDouble(input, readChar)",
                    "!isHex(input, readChar)", "!isOctal(input, readChar)", "!isBinary(input, readChar)"})
    protected static double doNaN(@SuppressWarnings("unused") TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
        return Double.NaN;
    }

    @Specialization(guards = {"isHex(input, readChar)", "stringLength(input) <= SAFE_HEX_DIGITS"})
    @TruffleBoundary
    protected static double doHexSafe(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, Strings.length(input), 16));
    }

    @Specialization(guards = {"isHex(input, readChar)", "stringLength(input) > SAFE_HEX_DIGITS"})
    @TruffleBoundary
    protected static double doHex(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
        try {
            return Strings.parseBigInteger(Strings.lazySubstring(input, 2), 16).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isOctal(input, readChar)", "stringLength(input) <= SAFE_OCTAL_DIGITS"})
    @TruffleBoundary
    protected static double doOctalSafe(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, Strings.length(input), 8));
    }

    @Specialization(guards = {"isOctal(input, readChar)", "stringLength(input) > SAFE_OCTAL_DIGITS"})
    @TruffleBoundary
    protected static double doOctal(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
        try {
            return Strings.parseBigInteger(Strings.lazySubstring(input, 2), 8).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isBinary(input, readChar)", "stringLength(input) <= SAFE_BINARY_DIGITS"})
    @TruffleBoundary
    protected static double doBinarySafe(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, Strings.length(input), 2));
    }

    @Specialization(guards = {"isBinary(input, readChar)", "stringLength(input) > SAFE_BINARY_DIGITS"})
    @TruffleBoundary
    protected static double doBinary(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
        try {
            return Strings.parseBigInteger(Strings.lazySubstring(input, 2), 2).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"stringLength(input) > 0",
                    "stringLength(input) <= SMALL_INT_LENGTH",
                    "allDigits(input, SMALL_INT_LENGTH, readChar)"})
    protected static double doSmallPosInt(TruffleString input,
                    @Cached @Shared TruffleString.ReadCharUTF16Node readChar) {
        int result = 0;
        int pos = 0;
        int len = Strings.length(input);
        while (pos < len) {
            char c = charAt(input, pos, readChar);
            assert JSRuntime.isAsciiDigit(c);
            result *= 10;
            result += c - '0';
            pos++;
        }
        assert result >= 0 && checkLongResult(result, input);
        return result;
    }

    @Specialization(guards = {"stringLength(input) > 0",
                    "stringLength(input) <= MAX_SAFE_INTEGER_LENGTH",
                    "startsWithValidInt(input, readChar)"}, rewriteOn = SlowPathException.class, replaces = "doSmallPosInt")
    protected static double doInteger(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) throws SlowPathException {
        long result = JSRuntime.parseSafeInteger(input);
        if (result == JSRuntime.INVALID_SAFE_INTEGER) {
            throw JSNodeUtil.slowPathException();
        }
        assert checkLongResult(result, input);
        return result;
    }

    @TruffleBoundary
    @Specialization(guards = {"stringLength(input) > 0",
                    "startsWithValidDouble(input, readChar)"}, replaces = "doInteger")
    protected static double doDouble(TruffleString input,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readChar) {
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
    private static boolean checkLongResult(long result, TruffleString input) {
        return Double.compare(result, JSRuntime.parseDoubleOrNaN(input)) == 0;
    }
}

/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;

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

    @Child private TruffleString.ReadCharUTF16Node stringReadNode;
    @Child private JSTrimWhitespaceNode trimWhitespaceNode;

    public final double executeString(TruffleString input) {
        if (trimWhitespaceNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            trimWhitespaceNode = insert(JSTrimWhitespaceNode.create());
        }
        return executeNoTrim(trimWhitespaceNode.executeString(input));
    }

    protected char charAt(TruffleString s, int i) {
        if (stringReadNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stringReadNode = insert(TruffleString.ReadCharUTF16Node.create());
        }
        return Strings.charAt(stringReadNode, s, i);
    }

    protected abstract double executeNoTrim(TruffleString input);

    public static JSStringToNumberNode create() {
        return JSStringToNumberNodeGen.create();
    }

    protected final boolean startsWithI(TruffleString input) {
        return Strings.length(input) >= Strings.length(Strings.INFINITY) && Strings.length(input) <= (Strings.length(Strings.INFINITY) + 1) &&
                        (charAt(input, 0) == 'I' || charAt(input, 1) == 'I');
    }

    /**
     * First two chars are valid finite double. Implies
     * {@code !startsWithI && !isHex && !isOctal && !isBinary}.
     */
    protected final boolean startsWithValidDouble(TruffleString input) {
        char firstChar = charAt(input, 0);
        if (JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '.' || firstChar == '+') {
            if (Strings.length(input) >= 2) {
                char secondChar = charAt(input, 1);
                return JSRuntime.isAsciiDigit(secondChar) || secondChar == '.' || secondChar == 'e' || secondChar == 'E';
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    protected final boolean startsWithValidInt(TruffleString input) {
        char firstChar = charAt(input, 0);
        return (JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '+') && (Strings.length(input) < 2 || JSRuntime.isAsciiDigit(charAt(input, 1)));
    }

    protected final boolean allDigits(TruffleString input, int maxLength) {
        assert Strings.length(input) <= maxLength;
        for (int i = 0; i < maxLength; i++) {
            if (i >= Strings.length(input)) {
                return true;
            } else if (!JSRuntime.isAsciiDigit(charAt(input, i))) {
                return false;
            }
        }
        return false;
    }

    protected final boolean isHex(TruffleString input) {
        return Strings.length(input) >= 2 && charAt(input, 0) == '0' && (charAt(input, 1) == 'x' || charAt(input, 1) == 'X');
    }

    protected final boolean isOctal(TruffleString input) {
        return Strings.length(input) >= 2 && charAt(input, 0) == '0' && (charAt(input, 1) == 'o' || charAt(input, 1) == 'O');
    }

    protected final boolean isBinary(TruffleString input) {
        return Strings.length(input) >= 2 && charAt(input, 0) == '0' && (charAt(input, 1) == 'b' || charAt(input, 1) == 'B');
    }

    @Specialization(guards = "stringLength(input) == 0")
    protected double doLengthIsZero(@SuppressWarnings("unused") TruffleString input) {
        return 0;
    }

    @Specialization(guards = "startsWithI(input)")
    protected double doInfinity(TruffleString input,
                    @Cached ConditionProfile endsWithInfinity,
                    @Cached TruffleString.RegionEqualByteIndexNode regionEqualsNode) {
        if (endsWithInfinity.profile(Strings.endsWith(regionEqualsNode, input, Strings.INFINITY))) {
            return JSRuntime.identifyInfinity(charAt(input, 0), Strings.length(input));
        } else {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"stringLength(input) > 0", "!startsWithI(input)", "!startsWithValidDouble(input)", "!isHex(input)", "!isOctal(input)", "!isBinary(input)"})
    protected double doNaN(@SuppressWarnings("unused") TruffleString input) {
        return Double.NaN;
    }

    @Specialization(guards = {"isHex(input)", "stringLength(input) <= SAFE_HEX_DIGITS"})
    @TruffleBoundary
    protected double doHexSafe(TruffleString input) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, Strings.length(input), 16));
    }

    @Specialization(guards = {"isHex(input)", "stringLength(input) > SAFE_HEX_DIGITS"})
    @TruffleBoundary
    protected double doHex(TruffleString input) {
        try {
            return Strings.parseBigInteger(Strings.substring(input, 2), 16).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isOctal(input)", "stringLength(input) <= SAFE_OCTAL_DIGITS"})
    @TruffleBoundary
    protected double doOctalSafe(TruffleString input) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, Strings.length(input), 8));
    }

    @Specialization(guards = {"isOctal(input)", "stringLength(input) > SAFE_OCTAL_DIGITS"})
    @TruffleBoundary
    protected double doOctal(TruffleString input) {
        try {
            return Strings.parseBigInteger(Strings.substring(input, 2), 8).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isBinary(input)", "stringLength(input) <= SAFE_BINARY_DIGITS"})
    @TruffleBoundary
    protected double doBinarySafe(TruffleString input) {
        return safeIntegerToDouble(JSRuntime.parseSafeInteger(input, 2, Strings.length(input), 2));
    }

    @Specialization(guards = {"isBinary(input)", "stringLength(input) > SAFE_BINARY_DIGITS"})
    @TruffleBoundary
    protected double doBinary(TruffleString input) {
        try {
            return Strings.parseBigInteger(Strings.substring(input, 2), 2).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"stringLength(input) > 0", "stringLength(input) <= SMALL_INT_LENGTH", "allDigits(input, SMALL_INT_LENGTH)"})
    protected double doSmallPosInt(TruffleString input) {
        int result = 0;
        int pos = 0;
        int len = Strings.length(input);
        while (pos < len) {
            char c = charAt(input, pos);
            assert JSRuntime.isAsciiDigit(c);
            result *= 10;
            result += c - '0';
            pos++;
        }
        assert result >= 0 && checkLongResult(result, input);
        return result;
    }

    @Specialization(guards = {"stringLength(input) > 0", "stringLength(input) <= MAX_SAFE_INTEGER_LENGTH",
                    "startsWithValidInt(input)"}, rewriteOn = SlowPathException.class, replaces = "doSmallPosInt")
    protected double doInteger(TruffleString input) throws SlowPathException {
        long result = JSRuntime.parseSafeInteger(input);
        if (result == JSRuntime.INVALID_SAFE_INTEGER) {
            throw JSNodeUtil.slowPathException();
        }
        assert checkLongResult(result, input);
        return result;
    }

    @TruffleBoundary
    @Specialization(guards = {"stringLength(input) > 0", "startsWithValidDouble(input)"}, replaces = "doInteger")
    protected double doDouble(TruffleString input) {
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

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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNodeGen.JSStringToNumberWithTrimNodeGen;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * This implements ECMA 9.3.1 ToNumber applied to the String Type.
 *
 */
public abstract class JSStringToNumberNode extends JavaScriptBaseNode {

    private final ConditionProfile potentiallySeenInfinity = ConditionProfile.createBinaryProfile();
    private final ConditionProfile potentiallySeenInfinitySigned = ConditionProfile.createBinaryProfile();

    public abstract double execute(Object operand);

    protected final boolean containsInfinity(String input) {
        if (input.length() >= JSRuntime.INFINITY_STRING.length() && input.length() <= (JSRuntime.INFINITY_STRING.length() + 1)) {
            if (potentiallySeenInfinity.profile(input.charAt(0) == 'I')) {
                return input.equals(JSRuntime.INFINITY_STRING);
            } else if (potentiallySeenInfinitySigned.profile(input.charAt(1) == 'I')) {
                return input.endsWith(JSRuntime.INFINITY_STRING);
            }
        }
        return false;
    }

    protected static final boolean firstCharValid(String input) {
        char firstChar = input.charAt(0);
        return JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '.' || firstChar == '+';
    }

    @TruffleBoundary
    protected static final boolean isSci(String input) {
        if (isHex(input)) {
            return false;
        }
        int index = JSRuntime.firstExpIndexInString(input);
        return 0 <= index && index < (input.length() - 1);
    }

    @TruffleBoundary
    protected static final boolean isLong(String input) {
        return input.length() <= 18 && input.indexOf('.') == -1;
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

    @Specialization(guards = "containsInfinity(input)")
    protected double doInfinity(String input) {
        return JSRuntime.identifyInfinity(input, input.charAt(0));
    }

    @Specialization(guards = {"input.length() > 0", "!containsInfinity(input)", "!firstCharValid(input)"})
    protected double doFirstCharInvalid(@SuppressWarnings("unused") String input) {
        return Double.NaN;
    }

    @Specialization(guards = {"input.length() > 0", "!containsInfinity(input)", "firstCharValid(input)", "isSci(input)"})
    @TruffleBoundary
    protected double doSci(String input) {
        try {
            return JSRuntime.stringToNumberSci(input);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isHex(input)"})
    @TruffleBoundary
    protected double doHex(String input) {
        try {
            return new BigInteger(input.substring(2), 16).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isOctal(input)"})
    @TruffleBoundary
    protected double doOctal(String input) {
        try {
            return new BigInteger(input.substring(2), 8).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"isBinary(input)"})
    @TruffleBoundary
    protected double doBinary(String input) {
        try {
            return new BigInteger(input.substring(2), 2).doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"input.length() > 0", "!containsInfinity(input)", "firstCharValid(input)", "!isSci(input)", "isLong(input)", "!isHex(input)", "!isOctal(input)", "!isBinary(input)"})
    @TruffleBoundary
    protected double doLongDec(String input) {
        try {
            return JSRuntime.doubleValue(JSRuntime.stringToNumberLong(input));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @Specialization(guards = {"input.length() > 0", "!containsInfinity(input)", "firstCharValid(input)", "!isHex(input)", "!isOctal(input)", "!isBinary(input)", "!isSci(input)"})
    protected double doDouble(String input) {
        return JSRuntime.parseDoubleOrNaN(input);
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

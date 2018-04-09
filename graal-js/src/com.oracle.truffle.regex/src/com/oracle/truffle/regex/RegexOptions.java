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
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;

import java.util.Arrays;

public final class RegexOptions {

    private static final int U180E_WHITESPACE = 1;
    private static final int REGRESSION_TEST_MODE = 1 << 1;

    public static final RegexOptions DEFAULT = new RegexOptions(0);

    private final int options;

    private RegexOptions(int options) {
        this.options = options;
    }

    @CompilerDirectives.TruffleBoundary
    public static RegexOptions parse(String optionsString) throws RegexSyntaxException {
        int options = 0;
        for (String propValue : optionsString.split(",")) {
            if (propValue.isEmpty()) {
                continue;
            }
            int eqlPos = propValue.indexOf('=');
            if (eqlPos < 0) {
                throw optionsSyntaxError(optionsString, propValue + " is not in form 'key=value'");
            }
            String key = propValue.substring(0, eqlPos);
            String value = propValue.substring(eqlPos + 1);
            switch (key) {
                case "U180EWhitespace":
                    options = parseBooleanOption(optionsString, options, key, value, U180E_WHITESPACE);
                    break;
                case "RegressionTestMode":
                    options = parseBooleanOption(optionsString, options, key, value, REGRESSION_TEST_MODE);
                    break;
                default:
                    throw optionsSyntaxError(optionsString, "unexpected option " + key);
            }
        }
        return new RegexOptions(options);
    }

    private static int parseBooleanOption(String optionsString, int options, String key, String value, int flag) throws RegexSyntaxException {
        if (value.equals("true")) {
            return options | flag;
        } else if (!value.equals("false")) {
            throw optionsSyntaxErrorUnexpectedValue(optionsString, key, value, "true", "false");
        }
        return options;
    }

    private static RegexSyntaxException optionsSyntaxErrorUnexpectedValue(String optionsString, String key, String value, String... expectedValues) {
        return optionsSyntaxError(optionsString, String.format("unexpected value '%s' for option '%s', expected one of %s", value, key, Arrays.toString(expectedValues)));
    }

    private static RegexSyntaxException optionsSyntaxError(String optionsString, String msg) {
        return new RegexSyntaxException(String.format("Invalid options syntax in '%s': %s", optionsString, msg));
    }

    private boolean isBitSet(int bit) {
        return (options & bit) != 0;
    }

    public boolean isU180EWhitespace() {
        return isBitSet(U180E_WHITESPACE);
    }

    public boolean isRegressionTestMode() {
        return isBitSet(REGRESSION_TEST_MODE);
    }

    @Override
    public int hashCode() {
        return options;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof RegexOptions && options == ((RegexOptions) obj).options;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isU180EWhitespace()) {
            sb.append("U180EWhitespace");
        }
        if (isRegressionTestMode()) {
            if (isU180EWhitespace()) {
                sb.append(",");
            }
            sb.append("RegressionTestMode");
        }
        return sb.toString();
    }
}

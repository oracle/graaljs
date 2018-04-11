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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexSyntaxException;
import com.oracle.truffle.regex.nashorn.regexp.RegExpScanner;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RegexCompilerInterface {
    private static final String REPEATED_REG_EXP_FLAG_MSG = "Repeated RegExp flag: %c";
    private static final String UNSUPPORTED_REG_EXP_FLAG_MSG = "Invalid regular expression flags";
    private static final String UNSUPPORTED_REG_EXP_FLAG_MSG_NASHORN = "Unsupported RegExp flag: %c";

    public static Node createExecuteCompilerNode() {
        return JSInteropUtil.createCall();
    }

    public static TruffleObject compile(String pattern, String flags, JSContext context) {
        return compile(pattern, flags, context, createExecuteCompilerNode());
    }

    @TruffleBoundary
    public static TruffleObject compile(String pattern, String flags, JSContext context, Node executeCompilerNode) {
        try {
            // RegexLanguage does its own validation of the flags. This call to validateFlags only
            // serves the purpose of mimicking the error messages of Nashorn and V8.
            validateFlags(flags, context.getEcmaScriptVersion());
            return (TruffleObject) ForeignAccess.sendExecute(executeCompilerNode, context.getRegexEngine(), pattern, flags);
        } catch (RegexSyntaxException syntaxException) {
            throw Errors.createSyntaxError(syntaxException.getMessage());
        } catch (InteropException ex) {
            throw ex.raise();
        }
    }

    @TruffleBoundary
    public static void validate(String pattern, String flags, int ecmaScriptVersion) {
        // We cannot use the TRegex parser in Nashorn compatibility mode, since the Nashorn
        // parser produces different error messages.
        if (JSTruffleOptions.NashornCompatibilityMode) {
            try {
                try {
                    RegExpScanner.scan(pattern);
                } catch (final PatternSyntaxException e) {
                    // refine the exception with a better syntax error, if this
                    // passes, just rethrow what we have
                    Pattern.compile(pattern, 0);
                    throw e;
                }
            } catch (final PatternSyntaxException e) {
                throw Errors.createSyntaxError(e.getMessage());
            }
            if (!flags.isEmpty()) {
                validateFlags(flags, ecmaScriptVersion);
            }
        } else {
            try {
                RegexLanguage.validateRegex(pattern, flags);
            } catch (final RegexSyntaxException e) {
                throw Errors.createSyntaxError(e.getMessage());
            }
        }
    }

    @SuppressWarnings("fallthrough")
    @TruffleBoundary
    public static void validateFlags(String flags, int ecmaScriptVersion) {
        boolean ignoreCase = false;
        boolean multiline = false;
        boolean global = false;
        boolean sticky = false;
        boolean unicode = false;
        boolean dotAll = false;

        for (int i = 0; i < flags.length(); i++) {
            char ch = flags.charAt(i);
            boolean repeated;
            switch (ch) {
                case 'i':
                    repeated = ignoreCase;
                    ignoreCase = true;
                    break;
                case 'm':
                    repeated = multiline;
                    multiline = true;
                    break;
                case 'g':
                    repeated = global;
                    global = true;
                    break;
                case 'y':
                    if (ecmaScriptVersion >= 6) {
                        repeated = sticky;
                        sticky = true;
                        break;
                    }
                    // fallthrough
                case 'u':
                    if (ecmaScriptVersion >= 6) {
                        repeated = unicode;
                        unicode = true;
                        break;
                    }
                    // fallthrough
                case 's':
                    if (ecmaScriptVersion >= 9) {
                        repeated = dotAll;
                        dotAll = true;
                        break;
                    }
                    // fallthrough
                default:
                    if (JSTruffleOptions.NashornCompatibilityMode) {
                        throw throwFlagError(UNSUPPORTED_REG_EXP_FLAG_MSG_NASHORN, ch);
                    } else {
                        throw throwFlagError(UNSUPPORTED_REG_EXP_FLAG_MSG);
                    }
            }
            if (repeated) {
                throw throwFlagError(REPEATED_REG_EXP_FLAG_MSG, ch);
            }
        }
    }

    @TruffleBoundary
    private static RuntimeException throwFlagError(String msg, char flag) {
        throw Errors.createSyntaxError(String.format(msg, flag));
    }

    @TruffleBoundary
    private static RuntimeException throwFlagError(String msg) {
        throw Errors.createSyntaxError(msg);
    }
}

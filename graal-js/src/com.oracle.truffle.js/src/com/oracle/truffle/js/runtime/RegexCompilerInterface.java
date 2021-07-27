/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

public final class RegexCompilerInterface {
    private static final String REPEATED_REG_EXP_FLAG_MSG = "Repeated RegExp flag: %c";
    private static final String UNSUPPORTED_REG_EXP_FLAG_MSG = "Invalid regular expression flags";
    private static final String UNSUPPORTED_REG_EXP_FLAG_MSG_NASHORN = "Unsupported RegExp flag: %c";

    private RegexCompilerInterface() {
    }

    public static Object compile(String pattern, String flags, JSContext context) {
        return compile(pattern, flags, context, TRegexUtil.InteropIsNullNode.getUncached());
    }

    public static Object compile(String pattern, String flags, JSContext context, TRegexUtil.InteropIsNullNode isCompiledRegexNullNode) {
        // RegexLanguage does its own validation of the flags. This call to validateFlags only
        // serves the purpose of mimicking the error messages of Nashorn and V8.
        validateFlags(flags, context.getEcmaScriptVersion(), context.isOptionNashornCompatibilityMode(), context.isOptionRegexpMatchIndices());
        Object compiledRegex;
        try {
            compiledRegex = JSRealm.get(null).getEnv().parseInternal(createRegexSource(pattern, flags, context.getRegexOptions())).call();
        } catch (AbstractTruffleException e) {
            throw rethrowAsSyntaxError(e);
        }
        if (isCompiledRegexNullNode.execute(compiledRegex)) {
            throw Errors.createSyntaxError("regular expression not supported");
        }
        return compiledRegex;
    }

    @TruffleBoundary
    private static Source createRegexSource(String pattern, String flags, String options) {
        String regexStr = options + '/' + pattern + '/' + flags;
        return Source.newBuilder("regex", regexStr, regexStr).mimeType("application/tregex").internal(true).build();
    }

    @TruffleBoundary
    public static void validate(JSContext context, String pattern, String flags, int ecmaScriptVersion) {
        if (context.isOptionNashornCompatibilityMode() && !flags.isEmpty()) {
            validateFlags(flags, ecmaScriptVersion, true, context.isOptionRegexpMatchIndices());
        }
        try {
            JSRealm.get(null).getEnv().parseInternal(createRegexSource(pattern, flags, context.getRegexValidateOptions())).call();
        } catch (AbstractTruffleException e) {
            throw rethrowAsSyntaxError(e);
        }
    }

    @TruffleBoundary
    public static void validateFlags(String flags, int ecmaScriptVersion, boolean nashornCompat, boolean allowHasIndices) {
        boolean ignoreCase = false;
        boolean multiline = false;
        boolean global = false;
        boolean sticky = false;
        boolean unicode = false;
        boolean dotAll = false;
        boolean hasIndices = false;

        for (int i = 0; i < flags.length(); i++) {
            char ch = flags.charAt(i);
            boolean recognized = false;
            boolean repeated = false;
            switch (ch) {
                case 'i':
                    recognized = true;
                    repeated = ignoreCase;
                    ignoreCase = true;
                    break;
                case 'm':
                    recognized = true;
                    repeated = multiline;
                    multiline = true;
                    break;
                case 'g':
                    recognized = true;
                    repeated = global;
                    global = true;
                    break;
                case 'y':
                    if (ecmaScriptVersion >= 6) {
                        recognized = true;
                        repeated = sticky;
                        sticky = true;
                    }
                    break;
                case 'u':
                    if (ecmaScriptVersion >= 6) {
                        recognized = true;
                        repeated = unicode;
                        unicode = true;
                    }
                    break;
                case 's':
                    if (ecmaScriptVersion >= JSConfig.ECMAScript2018) {
                        recognized = true;
                        repeated = dotAll;
                        dotAll = true;
                    }
                    break;
                case 'd':
                    if (allowHasIndices) {
                        recognized = true;
                        repeated = hasIndices;
                        hasIndices = true;
                    }
                    break;
            }
            if (!recognized) {
                throw unsupportedFlagError(ch, nashornCompat);
            }
            if (repeated) {
                throw throwFlagError(REPEATED_REG_EXP_FLAG_MSG, ch);
            }
        }
    }

    @TruffleBoundary
    private static RuntimeException unsupportedFlagError(char ch, boolean nashornCompat) {
        if (nashornCompat) {
            throw throwFlagError(UNSUPPORTED_REG_EXP_FLAG_MSG_NASHORN, ch);
        } else {
            throw Errors.createSyntaxError(UNSUPPORTED_REG_EXP_FLAG_MSG);
        }
    }

    @TruffleBoundary
    private static RuntimeException throwFlagError(String msg, char flag) {
        throw Errors.createSyntaxError(String.format(msg, flag));
    }

    @TruffleBoundary
    private static AbstractTruffleException rethrowAsSyntaxError(AbstractTruffleException e) {
        ExceptionType exceptionType;
        try {
            exceptionType = InteropLibrary.getUncached().getExceptionType(e);
        } catch (UnsupportedMessageException ume) {
            throw Errors.shouldNotReachHere();
        }
        if (exceptionType == ExceptionType.PARSE_ERROR) {
            throw Errors.createSyntaxError(e.getMessage());
        }
        throw e;
    }
}

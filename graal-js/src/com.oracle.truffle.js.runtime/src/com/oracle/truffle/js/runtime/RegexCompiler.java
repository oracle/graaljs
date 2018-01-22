/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexSyntaxException;
import com.oracle.truffle.regex.UnsupportedRegexException;
import com.oracle.truffle.regex.nashorn.regexp.RegExpScanner;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RegexCompiler {
    private static final String REPEATED_REG_EXP_FLAG_MSG = "Repeated RegExp flag: %c";
    private static final String UNSUPPORTED_REG_EXP_FLAG_MSG = "Invalid regular expression flags";
    private static final String UNSUPPORTED_REG_EXP_FLAG_MSG_NASHORN = "Unsupported RegExp flag: %c";

    private static Source createRegexLanguageSource(String pattern, String flags) {
        StringBuilder src = new StringBuilder(pattern.length() + 30);
        if (JSTruffleOptions.U180EWhitespace) {
            src.append("U180EWhitespace=true");
        }
        if (JSTruffleOptions.RegexEngine.equals("joni") || JSTruffleOptions.RegexEngine.equals("tregex")) {
            if (JSTruffleOptions.U180EWhitespace) {
                src.append(",");
            }
            src.append("Engine=").append(JSTruffleOptions.RegexEngine);
        }
        src.append("/").append(pattern).append("/").append(flags);
        return Source.newBuilder(src.toString()).name(pattern).mimeType("application/js-regex").build();
    }

    @TruffleBoundary
    public static TruffleObject compile(String pattern, String flags, JSContext context) {
        try {
            // RegexLanguage does its own validation of the flags. This call to validateFlags only
            // serves the purpose of mimicking the error messages of Nashorn and V8.
            validateFlags(flags, context.getEcmaScriptVersion());
            final Source source = createRegexLanguageSource(pattern, flags);
            return (TruffleObject) context.getEnv().parse(source).call();
        } catch (RuntimeException runtimeException) {
            if (runtimeException.getCause() instanceof RegexSyntaxException) {
                throw Errors.createSyntaxError(runtimeException.getCause().getMessage());
            }
            throw runtimeException;
        }
    }

    @TruffleBoundary
    public static void validate(String pattern, String flags, int ecmaScriptVersion) {
        boolean validated = false;
        // We cannot use the TRegex parser in Nashorn compatibility mode, since the Nashorn
        // parser produces different error messages.
        if (!JSTruffleOptions.NashornCompatibilityMode) {
            try {
                RegexLanguage.tRegexValidate(createRegexLanguageSource(pattern, flags));
                validated = true;
            } catch (final RegexSyntaxException e) {
                throw Errors.createSyntaxError(e.getMessage());
            } catch (UnsupportedRegexException e) {
                // validated is false now
            }
        }
        if (!validated) {
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

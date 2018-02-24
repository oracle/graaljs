/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class RegexCompilerWithFallback extends RegexCompiler {

    private final RegexCompiler mainCompiler;
    private final RegexCompiler fallbackCompiler;

    private final DebugUtil.Timer timer = DebugUtil.LOG_TOTAL_COMPILATION_TIME ? new DebugUtil.Timer() : null;

    public RegexCompilerWithFallback(RegexCompiler mainCompiler, RegexCompiler fallbackCompiler) {
        this.mainCompiler = mainCompiler;
        this.fallbackCompiler = fallbackCompiler;
    }

    @Override
    public CompiledRegex compile(RegexSource regexSource) throws RegexSyntaxException {
        CompiledRegex regex;
        long elapsedTimeMain = 0;
        long elapsedTimeFallback = 0;
        try {
            if (DebugUtil.LOG_TOTAL_COMPILATION_TIME) {
                timer.start();
            }
            regex = mainCompiler.compile(regexSource);
            if (DebugUtil.LOG_TOTAL_COMPILATION_TIME) {
                elapsedTimeMain = timer.getElapsed();
            }
        } catch (UnsupportedRegexException mainBailout) {
            try {
                if (DebugUtil.LOG_TOTAL_COMPILATION_TIME) {
                    timer.start();
                }
                regex = fallbackCompiler.compile(regexSource);
                if (DebugUtil.LOG_TOTAL_COMPILATION_TIME) {
                    elapsedTimeFallback = timer.getElapsed();
                }
            } catch (UnsupportedRegexException fallbackBailout) {
                throw new UnsupportedRegexException(String.format("%s; %s", mainBailout.getMessage(), fallbackBailout.getMessage()));
            }
        }
        if (DebugUtil.LOG_TOTAL_COMPILATION_TIME) {
            logCompilationTime(regexSource, elapsedTimeMain, elapsedTimeFallback);
        }
        return regex;
    }

    private static void logCompilationTime(RegexSource regexSource, long elapsedTimeTRegex, long elapsedTimeJoni) {
        System.out.println(String.format("%s, %s, %s, %s",
                        DebugUtil.Timer.elapsedToString(elapsedTimeTRegex + elapsedTimeJoni),
                        DebugUtil.Timer.elapsedToString(elapsedTimeTRegex),
                        DebugUtil.Timer.elapsedToString(elapsedTimeJoni),
                        DebugUtil.jsStringEscape(regexSource.toString())));
    }
}

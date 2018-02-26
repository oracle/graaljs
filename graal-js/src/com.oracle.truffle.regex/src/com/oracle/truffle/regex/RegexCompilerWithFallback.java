/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class RegexCompilerWithFallback extends RegexCompiler {

    private final RegexCompiler mainCompiler;
    private final RegexCompiler fallbackCompiler;

    private final DebugUtil.Timer timer = DebugUtil.LOG_TOTAL_COMPILATION_TIME ? new DebugUtil.Timer() : null;

    public RegexCompilerWithFallback(TruffleObject mainCompiler, TruffleObject fallbackCompiler) {
        this.mainCompiler = ForeignRegexCompiler.importRegexCompiler(mainCompiler);
        this.fallbackCompiler = ForeignRegexCompiler.importRegexCompiler(fallbackCompiler);
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public TruffleObject compile(RegexSource regexSource) throws RegexSyntaxException {
        TruffleObject regex;
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

    private static void logCompilationTime(RegexSource regexSource, long elapsedTimeMain, long elapsedTimeFallback) {
        System.out.println(String.format("%s, %s, %s, %s",
                        DebugUtil.Timer.elapsedToString(elapsedTimeMain + elapsedTimeFallback),
                        DebugUtil.Timer.elapsedToString(elapsedTimeMain),
                        DebugUtil.Timer.elapsedToString(elapsedTimeFallback),
                        DebugUtil.jsStringEscape(regexSource.toString())));
    }
}

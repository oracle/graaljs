/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class RegexEngineWithFallback extends RegexEngine {

    private final RegexEngine mainEngine;
    private final RegexEngine fallbackEngine;

    private final DebugUtil.Timer timer = DebugUtil.LOG_TOTAL_COMPILATION_TIME ? new DebugUtil.Timer() : null;

    public RegexEngineWithFallback(RegexEngine mainEngine, RegexEngine fallbackEngine) {
        this.mainEngine = mainEngine;
        this.fallbackEngine = fallbackEngine;
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
            regex = mainEngine.compile(regexSource);
            if (DebugUtil.LOG_TOTAL_COMPILATION_TIME) {
                elapsedTimeMain = timer.getElapsed();
            }
        } catch (UnsupportedRegexException mainBailout) {
            try {
                if (DebugUtil.LOG_TOTAL_COMPILATION_TIME) {
                    timer.start();
                }
                regex = fallbackEngine.compile(regexSource);
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

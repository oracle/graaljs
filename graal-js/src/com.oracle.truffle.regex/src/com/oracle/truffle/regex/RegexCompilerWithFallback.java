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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class RegexCompilerWithFallback extends RegexCompiler {

    private final RegexCompiler mainCompiler;
    private final RegexCompiler fallbackCompiler;

    private final DebugUtil.DebugLogger logBailout = new DebugUtil.DebugLogger("Bailout: ", DebugUtil.LOG_BAILOUT_MESSAGES);
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
            logBailout.log(mainBailout.getMessage() + ": " + regexSource);
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

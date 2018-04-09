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

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.TRegexCompiler;

/**
 * {@link RegexEngineBuilder} is the entry point into using {@link RegexLanguage}. It is an
 * executable {@link TruffleObject} that configures and returns a {@link RegexEngine}. When
 * executing a {@link RegexEngineBuilder}, it accepts the following arguments:
 * <ol>
 * <li>{@link String} {@code options} (optional): a comma-separated list of options for the engine,
 * the currently supported options include:</li>
 * <ul>
 * <li>{@code U180EWhitespace}: the U+180E Unicode character (MONGOLIAN VOWEL SEPARATOR) is to be
 * treated as whitespace (Unicode versions before 6.3.0)</li>
 * <li>{@code RegressionTestMode}: all compilation is done eagerly, so as to detect errors early
 * during testing</li>
 * </ul>
 * <li>{@link RegexCompiler} {@code fallbackCompiler} (optional): an optional {@link RegexCompiler}
 * to be used when compilation by {@link TRegexCompiler}, the native compiler of
 * {@link RegexLanguage}, fails with an {@link UnsupportedRegexException}; {@code fallbackCompiler}
 * does not have to be an instance of {@link RegexCompiler}, it can also be a {@link TruffleObject}
 * with the same interop semantics as {@link RegexCompiler}</li>
 * </ol>
 */
public class RegexEngineBuilder implements RegexLanguageObject {

    private final RegexLanguage language;

    public RegexEngineBuilder(RegexLanguage language) {
        this.language = language;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexCompiler;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexEngineBuilderMessageResolutionForeign.ACCESS;
    }

    @MessageResolution(receiverType = RegexEngineBuilder.class)
    static class RegexEngineBuilderMessageResolution {

        @Resolve(message = "EXECUTE")
        abstract static class RegexEngineBuilderExecuteNode extends Node {

            private Node isExecutableNode = Message.IS_EXECUTABLE.createNode();

            public Object access(RegexEngineBuilder receiver, Object[] args) {
                if (args.length > 2) {
                    throw ArityException.raise(2, args.length);
                }
                RegexOptions options = RegexOptions.DEFAULT;
                if (args.length >= 1) {
                    if (!(args[0] instanceof String)) {
                        throw UnsupportedTypeException.raise(args);
                    }
                    options = RegexOptions.parse((String) args[0]);
                }
                TruffleObject fallbackCompiler = null;
                if (args.length >= 2) {
                    if (!(args[1] instanceof TruffleObject && ForeignAccess.sendIsExecutable(isExecutableNode, (TruffleObject) args[1]))) {
                        throw UnsupportedTypeException.raise(args);
                    }
                    fallbackCompiler = (TruffleObject) args[1];
                }
                if (fallbackCompiler != null) {
                    return new RegexEngine(new CachingRegexCompiler(new RegexCompilerWithFallback(new TRegexCompiler(receiver.language, options), fallbackCompiler)), options.isRegressionTestMode());
                } else {
                    return new RegexEngine(new CachingRegexCompiler(new TRegexCompiler(receiver.language, options)), options.isRegressionTestMode());
                }
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class RegexEngineBuilderIsExecutableNode extends Node {

            @SuppressWarnings("unused")
            public boolean access(RegexEngineBuilder receiver) {
                return true;
            }
        }
    }
}

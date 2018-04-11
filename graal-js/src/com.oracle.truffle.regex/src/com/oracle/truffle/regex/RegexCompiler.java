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
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;

/**
 * {@link RegexCompiler} is an executable {@link TruffleObject} that compiles regular expressions
 * into {@link CompiledRegexObject}s or compatible {@link TruffleObject}s. It takes the following
 * arguments:
 * <ol>
 * <li>{@link String} {@code pattern}: the source of the regular expression to be compiled</li>
 * <li>{@link String} {@code flags} (optional): a textual representation of the flags to be passed
 * to the compiler (one letter per flag), see {@link RegexFlags} for the supported flags</li>
 * </ol>
 * Executing the {@link RegexCompiler} can also lead to the following exceptions:
 * <ul>
 * <li>{@link RegexSyntaxException}: if the input regular expression is malformed</li>
 * <li>{@link UnsupportedRegexException}: if the input regular expression cannot be compiled by this
 * compiler</li>
 * </ul>
 * <p>
 * {@link RegexCompiler}s are very similar to {@link RegexEngine}s. {@link RegexEngine}s produce
 * {@link RegexObject}s, which contain metadata about the compiled regular expression and lazy
 * access to a {@link CompiledRegexObject}s. When the {@link CompiledRegexObject} of a
 * {@link RegexObject} is needed, the {@link RegexEngine} delegates to a {@link RegexCompiler} to do
 * the actual work. A {@link RegexCompiler} then (eagerly) compiles its input and returns a
 * {@link CompiledRegexObject}. {@link RegexCompiler}s exist because they are easier to compose
 * {@link RegexCompiler}s using combinators such as {@link RegexCompilerWithFallback} and because
 * they are easier to provide by third-party RegExp engines. {@link RegexEngine}s exist because they
 * provide features that are desired by users of {@link RegexLanguage} (e.g. lazy compilation).
 */
public abstract class RegexCompiler implements RegexLanguageObject {

    /**
     * Uses the compiler to try and compile the regular expression described in {@code source}.
     * 
     * @return a {@link CompiledRegexObject} or a compatible {@link TruffleObject}
     * @throws RegexSyntaxException if the engine discovers a syntax error in the regular expression
     * @throws UnsupportedRegexException if the regular expression is not supported by the engine
     */
    public abstract TruffleObject compile(RegexSource source) throws RegexSyntaxException;

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexCompiler;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexCompilerMessageResolutionForeign.ACCESS;
    }

    @MessageResolution(receiverType = RegexCompiler.class)
    static class RegexCompilerMessageResolution {

        @Resolve(message = "EXECUTE")
        abstract static class RegexCompilerExecuteNode extends Node {

            public Object access(RegexCompiler receiver, Object[] args) {
                if (!(args.length == 1 || args.length == 2)) {
                    throw ArityException.raise(2, args.length);
                }
                if (!(args[0] instanceof String)) {
                    throw UnsupportedTypeException.raise(args);
                }
                String pattern = (String) args[0];
                String flags = "";
                if (args.length == 2) {
                    if (!(args[1] instanceof String)) {
                        throw UnsupportedTypeException.raise(args);
                    }
                    flags = (String) args[1];
                }
                RegexSource regexSource = new RegexSource(pattern, RegexFlags.parseFlags(flags));
                return receiver.compile(regexSource);
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class RegexCompilerIsExecutableNode extends Node {

            @SuppressWarnings("unused")
            public boolean access(RegexCompiler receiver) {
                return true;
            }
        }
    }
}

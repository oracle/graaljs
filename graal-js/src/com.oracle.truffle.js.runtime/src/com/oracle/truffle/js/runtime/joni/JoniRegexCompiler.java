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
package com.oracle.truffle.js.runtime.joni;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.CompiledRegexObject;
import com.oracle.truffle.regex.RegexCompiler;
import com.oracle.truffle.regex.RegexFlags;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexRootNode;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.UnsupportedRegexException;
import com.oracle.truffle.regex.nashorn.regexp.RegExpScanner;
import com.oracle.truffle.regex.nashorn.regexp.joni.Option;
import com.oracle.truffle.regex.nashorn.regexp.joni.Regex;
import com.oracle.truffle.regex.nashorn.regexp.joni.Syntax;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.SyntaxException;

import java.util.regex.PatternSyntaxException;

public final class JoniRegexCompiler extends RegexCompiler {

    private final RegexLanguage language;
    // For Joni, we want to share call targets to avoid excessive splitting.
    private CallTarget searchSimpleCallTarget;
    private CallTarget searchGroupCallTarget;
    private CallTarget matchSimpleCallTarget;
    private CallTarget matchGroupCallTarget;

    public JoniRegexCompiler(RegexLanguage language) {
        this.language = language;
    }

    private CallTarget searchSimpleCallTarget() {
        if (searchSimpleCallTarget == null) {
            searchSimpleCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, new JoniRegexExecRootNode.Simple(language, false)));
        }
        return searchSimpleCallTarget;
    }

    private CallTarget searchGroupCallTarget() {
        if (searchGroupCallTarget == null) {
            searchGroupCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, new JoniRegexExecRootNode.Groups(language, false)));
        }
        return searchGroupCallTarget;
    }

    private CallTarget matchSimpleCallTarget() {
        if (matchSimpleCallTarget == null) {
            matchSimpleCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, new JoniRegexExecRootNode.Simple(language, true)));
        }
        return matchSimpleCallTarget;
    }

    private CallTarget matchGroupCallTarget() {
        if (matchGroupCallTarget == null) {
            matchGroupCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, new JoniRegexExecRootNode.Groups(language, true)));
        }
        return matchGroupCallTarget;
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public TruffleObject compile(RegexSource source) throws UnsupportedRegexException {
        try {
            Regex implementation = createJoniRegex(source.getPattern(), source.getFlags());
            CallTarget callTarget;
            boolean group = PatternAnalyzer.containsGroup(source.getPattern());
            if (source.getFlags().isSticky()) {
                callTarget = group ? matchGroupCallTarget() : matchSimpleCallTarget();
            } else {
                callTarget = group ? searchGroupCallTarget() : searchSimpleCallTarget();
            }
            return new CompiledRegexObject(new JoniCompiledRegex(implementation, callTarget));
        } catch (UnsupportedRegexException e) {
            e.setReason("Joni: " + e.getReason());
            e.setRegex(source);
            throw e;
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static Regex createJoniRegex(String pattern, RegexFlags flags) throws UnsupportedRegexException {
        try {
            char[] chars = RegExpScanner.scan(pattern).getJavaPattern().toCharArray();
            return new Regex(chars, 0, chars.length, getOptions(flags), Syntax.JAVASCRIPT);
        } catch (PatternSyntaxException | SyntaxException e) {
            // We get a PatternSyntaxException if the preprocessor in RegExpScanner believes
            // the pattern is malformed (this either means a bug in the preprocessor or an
            // unsupported feature encountered by the preprocessor). Joni's SyntaxExceptions signal
            // syntax errors in the pattern (since we are using a preprocessor, these would
            // either mean a bug in the preprocessor or an unsupported feature encountered by Joni).
            throw new UnsupportedRegexException(e.getMessage(), e);
        }
    }

    private static int getOptions(RegexFlags flags) throws UnsupportedRegexException {
        int option = Option.SINGLELINE;
        if (flags.isUnicode()) {
            throw new UnsupportedRegexException("unicode mode not supported");
        }
        if (flags.isIgnoreCase()) {
            option |= Option.IGNORECASE;
        }
        if (flags.isMultiline()) {
            option &= ~Option.SINGLELINE;
            option |= Option.NEGATE_SINGLELINE;
        }
        if (flags.isDotAll()) {
            option |= Option.MULTILINE;
        }
        return option;
    }
}

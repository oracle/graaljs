/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.joni;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.regex.RegexEngine;
import com.oracle.truffle.regex.RegexFlags;
import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.RegexSyntaxException;
import com.oracle.truffle.regex.nashorn.regexp.RegExpScanner;
import com.oracle.truffle.regex.nashorn.regexp.joni.Option;
import com.oracle.truffle.regex.nashorn.regexp.joni.Regex;
import com.oracle.truffle.regex.nashorn.regexp.joni.Syntax;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.JOniException;

import java.util.regex.PatternSyntaxException;

public final class JoniRegexEngine implements RegexEngine {

    private final RegexLanguage language;
    // For Joni, we want to share call targets to avoid excessive splitting.
    private CallTarget searchSimpleCallTarget;
    private CallTarget searchGroupCallTarget;
    private CallTarget matchSimpleCallTarget;
    private CallTarget matchGroupCallTarget;

    public JoniRegexEngine(RegexLanguage language) {
        this.language = language;
    }

    private CallTarget searchSimpleCallTarget() {
        if (searchSimpleCallTarget == null) {
            searchSimpleCallTarget = Truffle.getRuntime().createCallTarget(new JoniRegexRootNode.Simple(language, false));
        }
        return searchSimpleCallTarget;
    }

    private CallTarget searchGroupCallTarget() {
        if (searchGroupCallTarget == null) {
            searchGroupCallTarget = Truffle.getRuntime().createCallTarget(new JoniRegexRootNode.Groups(language, false));
        }
        return searchGroupCallTarget;
    }

    private CallTarget matchSimpleCallTarget() {
        if (matchSimpleCallTarget == null) {
            matchSimpleCallTarget = Truffle.getRuntime().createCallTarget(new JoniRegexRootNode.Simple(language, true));
        }
        return matchSimpleCallTarget;
    }

    private CallTarget matchGroupCallTarget() {
        if (matchGroupCallTarget == null) {
            matchGroupCallTarget = Truffle.getRuntime().createCallTarget(new JoniRegexRootNode.Groups(language, true));
        }
        return matchGroupCallTarget;
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public CompiledRegex compile(RegexSource source) throws RegexSyntaxException {
        Regex implementation = createJoniRegex(source.getPattern(), source.getFlags());
        CallTarget callTarget;
        boolean group = PatternAnalyzer.containsGroup(source.getPattern());
        if (source.getFlags().isSticky()) {
            callTarget = group ? matchGroupCallTarget() : matchSimpleCallTarget();
        } else {
            callTarget = group ? searchGroupCallTarget() : searchSimpleCallTarget();
        }
        return new JoniCompiledRegex(implementation, callTarget);
    }

    @CompilerDirectives.TruffleBoundary
    private static Regex createJoniRegex(String pattern, RegexFlags flags) throws RegexSyntaxException {
        try {
            char[] chars = RegExpScanner.scan(pattern).getJavaPattern().toCharArray();
            return new Regex(chars, 0, chars.length, getOptions(flags), Syntax.JAVASCRIPT);
        } catch (JOniException | PatternSyntaxException e) {
            throw new RegexSyntaxException(pattern, flags, e.getMessage(), e);
        }
    }

    private static int getOptions(RegexFlags flags) {
        int option = Option.SINGLELINE;
        if (flags.isIgnoreCase()) {
            option |= Option.IGNORECASE;
        }
        if (flags.isMultiline()) {
            option &= ~Option.SINGLELINE;
            option |= Option.NEGATE_SINGLELINE;
        }
        return option;
    }
}

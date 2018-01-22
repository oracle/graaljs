/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.joni;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.RegexRootNode;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.nashorn.regexp.joni.Matcher;
import com.oracle.truffle.regex.nashorn.regexp.joni.Regex;
import com.oracle.truffle.regex.nashorn.regexp.joni.Region;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.result.SingleResult;
import com.oracle.truffle.regex.result.StartsEndsIndexArrayResult;
import com.oracle.truffle.regex.tregex.nodes.input.InputToStringNode;

/**
 * These nodes are instantiated only once and used for all Joni-RegExp. Therefore, we do not gain
 * anything from using ConditionProfiles.
 */
public abstract class JoniRegexRootNode extends RegexRootNode {

    @Child private InputToStringNode toStringNode = InputToStringNode.create();

    private final boolean sticky;

    public JoniRegexRootNode(RegexLanguage language, RegexSource source, boolean sticky) {
        super(language, source);
        this.sticky = sticky;
    }

    @Override
    public RegexResult execute(VirtualFrame frame, RegexObject regexObject, Object input, int fromIndex) {
        Regex impl = ((JoniCompiledRegex) regexObject.getCompiledRegex()).getJoniRegex();
        Matcher matcher = sticky ? match(impl, toStringNode.execute(input), fromIndex) : search(impl, toStringNode.execute(input), fromIndex);

        return (matcher != null) ? getMatchResult(regexObject, input, matcher) : RegexResult.NO_MATCH;
    }

    @TruffleBoundary
    private static Matcher search(Regex regex, String input, int fromIndex) {
        Matcher matcher = regex.matcher(input);
        boolean isMatch = matcher.search(fromIndex, input.length(), regex.getOptions()) > -1;
        // If there was no match, matcher.getBegin() returns 0 (instead of -1). This means that
        // Matcher can only be used if the return value of matcher.search(..) indicates a match.
        return isMatch ? matcher : null;
    }

    @TruffleBoundary
    private static Matcher match(Regex regex, String input, int fromIndex) {
        Matcher matcher = regex.matcher(input);
        boolean isMatch = matcher.match(fromIndex, input.length(), regex.getOptions()) > -1;
        // If there was no match, matcher.getBegin() returns 0 (instead of -1). This means that
        // Matcher can only be used if the return value of matcher.search(..) indicates a match.
        return isMatch ? matcher : null;
    }

    protected abstract RegexResult getMatchResult(RegexObject regex, Object input, Matcher matcher);

    @Override
    protected String getEngineLabel() {
        return "joni";
    }

    public static class Simple extends JoniRegexRootNode {
        public Simple(RegexLanguage language, boolean sticky) {
            super(language, null, sticky);
        }

        @Override
        protected RegexResult getMatchResult(RegexObject regex, Object input, Matcher matcher) {
            return new SingleResult(regex, input, matcher.getBegin(), matcher.getEnd());
        }
    }

    public static class Groups extends JoniRegexRootNode {
        public Groups(RegexLanguage language, boolean sticky) {
            super(language, null, sticky);
        }

        @Override
        protected RegexResult getMatchResult(RegexObject regex, Object input, Matcher matcher) {
            Region reg = matcher.getRegion();
            return new StartsEndsIndexArrayResult(regex, input, reg.beg, reg.end);
        }
    }
}

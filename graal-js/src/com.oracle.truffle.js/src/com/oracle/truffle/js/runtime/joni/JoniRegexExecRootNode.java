/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.joni.result.JoniNoMatchResult;
import com.oracle.truffle.js.runtime.joni.result.JoniRegexResult;
import com.oracle.truffle.js.runtime.joni.result.JoniSingleResult;
import com.oracle.truffle.js.runtime.joni.result.JoniStartsEndsIndexArrayResult;
import com.oracle.truffle.regex.nashorn.regexp.joni.Matcher;
import com.oracle.truffle.regex.nashorn.regexp.joni.Regex;
import com.oracle.truffle.regex.nashorn.regexp.joni.Region;

/**
 * These nodes are instantiated only once and used for all Joni-RegExp. Therefore, we do not gain
 * anything from using ConditionProfiles.
 */
public abstract class JoniRegexExecRootNode extends RootNode {

    private final SourceSection pseudoSource;
    private final boolean sticky;

    private static final FrameDescriptor SHARED_EMPTY_FRAMEDESCRIPTOR = new FrameDescriptor();

    public JoniRegexExecRootNode(TruffleLanguage<?> language, SourceSection pseudoSource, boolean sticky) {
        super(language, SHARED_EMPTY_FRAMEDESCRIPTOR);
        this.pseudoSource = pseudoSource;
        this.sticky = sticky;
    }

    @Override
    public final JoniRegexResult execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        assert args.length == 3;

        JoniCompiledRegex compiledRegex = (JoniCompiledRegex) args[0];
        String input = (String) args[1];
        int fromIndex = (int) args[2];
        Regex impl = compiledRegex.getJoniRegex();
        Matcher matcher = sticky ? match(impl, input, fromIndex) : search(impl, input, fromIndex);

        return (matcher != null) ? getMatchResult(matcher) : JoniNoMatchResult.getInstance();
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

    @Override
    public SourceSection getSourceSection() {
        return pseudoSource;
    }

    @Override
    public String toString() {
        return "joni regex " + pseudoSource;
    }

    protected abstract JoniRegexResult getMatchResult(Matcher matcher);

    private static SourceSection createPseudoSource(String name) {
        String patternSrc = "/[" + name + "]/";
        return Source.newBuilder("regex", patternSrc, patternSrc).build().createSection(0, patternSrc.length());
    }

    public static class Simple extends JoniRegexExecRootNode {
        public Simple(TruffleLanguage<?> language, boolean sticky) {
            super(language, createPseudoSource("JONI_SINGLETON_ROOT_NODE_" + (sticky ? "STICKY_" : "") + "SIMPLE"), sticky);
        }

        @Override
        protected JoniRegexResult getMatchResult(Matcher matcher) {
            return new JoniSingleResult(matcher.getBegin(), matcher.getEnd());
        }
    }

    public static class Groups extends JoniRegexExecRootNode {
        public Groups(TruffleLanguage<?> language, boolean sticky) {
            super(language, createPseudoSource("JONI_SINGLETON_ROOT_NODE_" + (sticky ? "STICKY_" : "") + "GROUPS"), sticky);
        }

        @Override
        protected JoniRegexResult getMatchResult(Matcher matcher) {
            Region reg = matcher.getRegion();
            return new JoniStartsEndsIndexArrayResult(reg.beg, reg.end);
        }
    }
}

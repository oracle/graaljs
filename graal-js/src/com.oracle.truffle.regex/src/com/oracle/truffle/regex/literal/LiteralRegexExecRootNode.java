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
package com.oracle.truffle.regex.literal;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexExecRootNode;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.RegexRootNode;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.result.PreCalculatedResultFactory;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.tregex.nodes.input.InputEndsWithNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputEqualsNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputIndexOfNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputLengthNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputRegionMatchesNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputStartsWithNode;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.PreCalcResultVisitor;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public abstract class LiteralRegexExecRootNode extends RegexExecRootNode implements CompiledRegex {

    protected final String literal;
    protected final PreCalculatedResultFactory resultFactory;
    private final CallTarget regexCallTarget;

    public LiteralRegexExecRootNode(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
        super(language, source);
        this.literal = preCalcResultVisitor.getLiteral();
        this.resultFactory = preCalcResultVisitor.getResultFactory();
        regexCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, this));
    }

    @Override
    protected final String getEngineLabel() {
        return "literal";
    }

    @Override
    public CallTarget getRegexCallTarget() {
        return regexCallTarget;
    }

    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("LiteralRegexNode",
                        new DebugUtil.Value("method", getImplName()),
                        new DebugUtil.Value("literal", DebugUtil.escapeString(literal)),
                        new DebugUtil.Value("factory", resultFactory));
    }

    protected abstract String getImplName();

    public static final class EmptyIndexOf extends LiteralRegexExecRootNode {

        public EmptyIndexOf(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyIndexOf";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            return resultFactory.createFromStart(regex, input, fromIndex);
        }
    }

    public static final class EmptyStartsWith extends LiteralRegexExecRootNode {

        public EmptyStartsWith(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyStartsWith";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            return fromIndex == 0 ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class EmptyEndsWith extends LiteralRegexExecRootNode {

        @Child InputLengthNode lengthNode = InputLengthNode.create();

        public EmptyEndsWith(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyEndsWith";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            assert fromIndex <= lengthNode.execute(input);
            return resultFactory.createFromEnd(regex, input, lengthNode.execute(input));
        }
    }

    public static final class EmptyEquals extends LiteralRegexExecRootNode {

        @Child InputLengthNode lengthNode = InputLengthNode.create();

        public EmptyEquals(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyEquals";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            assert fromIndex <= lengthNode.execute(input);
            return lengthNode.execute(input) == 0 ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class IndexOfChar extends LiteralRegexExecRootNode {

        private final char c;
        @Child InputIndexOfNode indexOfNode = InputIndexOfNode.create();
        @Child InputLengthNode lengthNode = InputLengthNode.create();

        public IndexOfChar(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
            assert literal.length() == 1;
            c = literal.charAt(0);
        }

        @Override
        protected String getImplName() {
            return "indexOfChar";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            int start = indexOfNode.execute(input, c, fromIndex, lengthNode.execute(input));
            if (start == -1) {
                return RegexResult.NO_MATCH;
            }
            return resultFactory.createFromStart(regex, input, start);
        }
    }

    public static final class StartsWith extends LiteralRegexExecRootNode {

        @Child InputStartsWithNode startsWithNode = InputStartsWithNode.create();

        public StartsWith(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "startsWith";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            return fromIndex == 0 && startsWithNode.execute(input, literal) ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class EndsWith extends LiteralRegexExecRootNode {

        @Child InputLengthNode lengthNode = InputLengthNode.create();
        @Child InputEndsWithNode endsWithNode = InputEndsWithNode.create();

        public EndsWith(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "endsWith";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            return fromIndex <= lengthNode.execute(input) - literal.length() && endsWithNode.execute(input, literal) ? resultFactory.createFromEnd(regex, input, lengthNode.execute(input))
                            : RegexResult.NO_MATCH;
        }
    }

    public static final class Equals extends LiteralRegexExecRootNode {

        @Child InputEqualsNode equalsNode = InputEqualsNode.create();

        public Equals(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "equals";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            return fromIndex == 0 && equalsNode.execute(input, literal) ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class RegionMatches extends LiteralRegexExecRootNode {

        @Child InputRegionMatchesNode regionMatchesNode = InputRegionMatchesNode.create();

        public RegionMatches(RegexLanguage language, RegexSource source, PreCalcResultVisitor preCalcResultVisitor) {
            super(language, source, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "regionMatches";
        }

        @Override
        protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
            return regionMatchesNode.execute(input, literal, fromIndex) ? resultFactory.createFromStart(regex, input, fromIndex) : RegexResult.NO_MATCH;
        }
    }
}

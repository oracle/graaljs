/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.literal;

import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexNode;
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

public abstract class LiteralRegexNode extends RegexNode {

    private final String pattern;
    protected final String literal;
    protected final PreCalculatedResultFactory resultFactory;

    public LiteralRegexNode(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
        this.pattern = pattern;
        this.literal = preCalcResultVisitor.getLiteral();
        this.resultFactory = preCalcResultVisitor.getResultFactory();
    }

    @Override
    protected final String getPatternSource() {
        return pattern;
    }

    @Override
    protected final String getEngineLabel() {
        return "literal";
    }

    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("LiteralRegexNode",
                        new DebugUtil.Value("method", getImplName()),
                        new DebugUtil.Value("literal", DebugUtil.escapeString(literal)),
                        new DebugUtil.Value("factory", resultFactory));
    }

    protected abstract String getImplName();

    public static final class EmptyIndexOf extends LiteralRegexNode {

        public EmptyIndexOf(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyIndexOf";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            return resultFactory.createFromStart(regex, input, fromIndex);
        }
    }

    public static final class EmptyStartsWith extends LiteralRegexNode {

        public EmptyStartsWith(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyStartsWith";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            return fromIndex == 0 ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class EmptyEndsWith extends LiteralRegexNode {

        @Child InputLengthNode lengthNode = InputLengthNode.create();

        public EmptyEndsWith(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyEndsWith";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            assert fromIndex <= lengthNode.execute(input);
            return resultFactory.createFromEnd(regex, input, lengthNode.execute(input));
        }
    }

    public static final class EmptyEquals extends LiteralRegexNode {

        @Child InputLengthNode lengthNode = InputLengthNode.create();

        public EmptyEquals(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "emptyEquals";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            assert fromIndex <= lengthNode.execute(input);
            return lengthNode.execute(input) == 0 ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class IndexOfChar extends LiteralRegexNode {

        private final char c;
        @Child InputIndexOfNode indexOfNode = InputIndexOfNode.create();
        @Child InputLengthNode lengthNode = InputLengthNode.create();

        public IndexOfChar(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
            assert literal.length() == 1;
            c = literal.charAt(0);
        }

        @Override
        protected String getImplName() {
            return "indexOfChar";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            int start = indexOfNode.execute(input, c, fromIndex, lengthNode.execute(input));
            if (start == -1) {
                return RegexResult.NO_MATCH;
            }
            return resultFactory.createFromStart(regex, input, start);
        }
    }

    public static final class StartsWith extends LiteralRegexNode {

        @Child InputStartsWithNode startsWithNode = InputStartsWithNode.create();

        public StartsWith(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "startsWith";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            return fromIndex == 0 && startsWithNode.execute(input, literal) ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class EndsWith extends LiteralRegexNode {

        @Child InputLengthNode lengthNode = InputLengthNode.create();
        @Child InputEndsWithNode endsWithNode = InputEndsWithNode.create();

        public EndsWith(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "endsWith";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            return fromIndex <= lengthNode.execute(input) - literal.length() && endsWithNode.execute(input, literal) ? resultFactory.createFromEnd(regex, input, lengthNode.execute(input))
                            : RegexResult.NO_MATCH;
        }
    }

    public static final class Equals extends LiteralRegexNode {

        @Child InputEqualsNode equalsNode = InputEqualsNode.create();

        public Equals(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "equals";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            return fromIndex == 0 && equalsNode.execute(input, literal) ? resultFactory.createFromStart(regex, input, 0) : RegexResult.NO_MATCH;
        }
    }

    public static final class RegionMatches extends LiteralRegexNode {

        @Child InputRegionMatchesNode regionMatchesNode = InputRegionMatchesNode.create();

        public RegionMatches(String pattern, PreCalcResultVisitor preCalcResultVisitor) {
            super(pattern, preCalcResultVisitor);
        }

        @Override
        protected String getImplName() {
            return "regionMatches";
        }

        @Override
        protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
            return regionMatchesNode.execute(input, literal, fromIndex) ? resultFactory.createFromStart(regex, input, fromIndex) : RegexResult.NO_MATCH;
        }
    }
}

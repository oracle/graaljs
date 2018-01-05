/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.literal;

import com.oracle.truffle.regex.RegexNode;
import com.oracle.truffle.regex.tregex.parser.RegexProperties;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.PreCalcResultVisitor;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public final class LiteralRegexEngine {

    public static RegexNode createNode(RegexAST ast) {
        RegexProperties p = ast.getProperties();
        if (p.hasAlternations() || p.hasCharClasses() || p.hasLookAroundAssertions() || p.hasLoops()) {
            return null;
        }
        PreCalcResultVisitor preCalcResultVisitor = PreCalcResultVisitor.run(ast, true);
        final boolean caret = ast.getRoot().startsWithCaret();
        final boolean dollar = ast.getRoot().endsWithDollar();
        if ((caret || dollar) && ast.getSource().getFlags().isMultiline()) {
            return null;
        }
        final LiteralRegexNode literalNode = createLiteralNode(ast, caret, dollar, preCalcResultVisitor);
        if (DebugUtil.DEBUG) {
            if (literalNode != null) {
                System.out.println(literalNode.toTable());
            }
        }
        return literalNode;
    }

    private static LiteralRegexNode createLiteralNode(RegexAST ast, boolean caret, boolean dollar, PreCalcResultVisitor preCalcResultVisitor) {
        String pattern = ast.getSource().getPattern();
        if (preCalcResultVisitor.getLiteral().length() == 0) {
            if (caret) {
                if (dollar) {
                    return new LiteralRegexNode.EmptyEquals(pattern, preCalcResultVisitor);
                }
                return new LiteralRegexNode.EmptyStartsWith(pattern, preCalcResultVisitor);
            }
            if (dollar) {
                return new LiteralRegexNode.EmptyEndsWith(pattern, preCalcResultVisitor);
            }
            return new LiteralRegexNode.EmptyIndexOf(pattern, preCalcResultVisitor);
        }
        if (caret) {
            if (dollar) {
                return new LiteralRegexNode.Equals(pattern, preCalcResultVisitor);
            }
            return new LiteralRegexNode.StartsWith(pattern, preCalcResultVisitor);
        }
        if (dollar) {
            return new LiteralRegexNode.EndsWith(pattern, preCalcResultVisitor);
        }
        if (ast.getSource().getFlags().isSticky()) {
            return new LiteralRegexNode.RegionMatches(pattern, preCalcResultVisitor);
        }
        if (preCalcResultVisitor.getLiteral().length() == 1) {
            return new LiteralRegexNode.IndexOfChar(pattern, preCalcResultVisitor);
        }
        return null;
    }
}

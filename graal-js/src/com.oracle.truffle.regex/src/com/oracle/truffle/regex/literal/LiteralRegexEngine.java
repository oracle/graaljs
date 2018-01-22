/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.literal;

import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.tregex.parser.RegexProperties;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.PreCalcResultVisitor;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public final class LiteralRegexEngine {

    public static LiteralRegexRootNode createNode(RegexLanguage language, RegexAST ast) {
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
        final LiteralRegexRootNode literalNode = createLiteralNode(language, ast, caret, dollar, preCalcResultVisitor);
        if (DebugUtil.DEBUG) {
            if (literalNode != null) {
                System.out.println(literalNode.toTable());
            }
        }
        return literalNode;
    }

    private static LiteralRegexRootNode createLiteralNode(RegexLanguage language, RegexAST ast, boolean caret, boolean dollar, PreCalcResultVisitor preCalcResultVisitor) {
        RegexSource source = ast.getSource();
        if (preCalcResultVisitor.getLiteral().length() == 0) {
            if (caret) {
                if (dollar) {
                    return new LiteralRegexRootNode.EmptyEquals(language, source, preCalcResultVisitor);
                }
                return new LiteralRegexRootNode.EmptyStartsWith(language, source, preCalcResultVisitor);
            }
            if (dollar) {
                return new LiteralRegexRootNode.EmptyEndsWith(language, source, preCalcResultVisitor);
            }
            return new LiteralRegexRootNode.EmptyIndexOf(language, source, preCalcResultVisitor);
        }
        if (caret) {
            if (dollar) {
                return new LiteralRegexRootNode.Equals(language, source, preCalcResultVisitor);
            }
            return new LiteralRegexRootNode.StartsWith(language, source, preCalcResultVisitor);
        }
        if (dollar) {
            return new LiteralRegexRootNode.EndsWith(language, source, preCalcResultVisitor);
        }
        if (source.getFlags().isSticky()) {
            return new LiteralRegexRootNode.RegionMatches(language, source, preCalcResultVisitor);
        }
        if (preCalcResultVisitor.getLiteral().length() == 1) {
            return new LiteralRegexRootNode.IndexOfChar(language, source, preCalcResultVisitor);
        }
        return null;
    }
}

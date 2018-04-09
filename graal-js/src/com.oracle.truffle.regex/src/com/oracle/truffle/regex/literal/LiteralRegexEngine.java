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

import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.tregex.parser.RegexProperties;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.PreCalcResultVisitor;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public final class LiteralRegexEngine {

    public static LiteralRegexExecRootNode createNode(RegexLanguage language, RegexAST ast) {
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
        final LiteralRegexExecRootNode literalNode = createLiteralNode(language, ast, caret, dollar, preCalcResultVisitor);
        if (DebugUtil.DEBUG) {
            if (literalNode != null) {
                System.out.println(literalNode.toTable());
            }
        }
        return literalNode;
    }

    private static LiteralRegexExecRootNode createLiteralNode(RegexLanguage language, RegexAST ast, boolean caret, boolean dollar, PreCalcResultVisitor preCalcResultVisitor) {
        RegexSource source = ast.getSource();
        if (preCalcResultVisitor.getLiteral().length() == 0) {
            if (caret) {
                if (dollar) {
                    return new LiteralRegexExecRootNode.EmptyEquals(language, source, preCalcResultVisitor);
                }
                return new LiteralRegexExecRootNode.EmptyStartsWith(language, source, preCalcResultVisitor);
            }
            if (dollar) {
                return new LiteralRegexExecRootNode.EmptyEndsWith(language, source, preCalcResultVisitor);
            }
            return new LiteralRegexExecRootNode.EmptyIndexOf(language, source, preCalcResultVisitor);
        }
        if (caret) {
            if (dollar) {
                return new LiteralRegexExecRootNode.Equals(language, source, preCalcResultVisitor);
            }
            return new LiteralRegexExecRootNode.StartsWith(language, source, preCalcResultVisitor);
        }
        if (dollar) {
            return new LiteralRegexExecRootNode.EndsWith(language, source, preCalcResultVisitor);
        }
        if (source.getFlags().isSticky()) {
            return new LiteralRegexExecRootNode.RegionMatches(language, source, preCalcResultVisitor);
        }
        if (preCalcResultVisitor.getLiteral().length() == 1) {
            return new LiteralRegexExecRootNode.IndexOfChar(language, source, preCalcResultVisitor);
        }
        return null;
    }
}

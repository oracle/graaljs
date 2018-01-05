/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.analysis;

import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.RegexSyntaxException;
import com.oracle.truffle.regex.tregex.parser.RegexLexer;
import com.oracle.truffle.regex.tregex.parser.Token;

/**
 * Generates a "unified" regular expression representation where all single characters are replaced
 * by "x" and all character classes are replaced by "[c]". The result is supposed to represent the
 * expression's general structure and complexity, and enable the user to find structurally
 * equivalent expressions. Example: /(.*yui[a-xU-Y](,|\w))/ -> /([c]*xxx[c](x|[c]))/
 */
public final class RegexUnifier {

    private final RegexSource source;
    private final RegexLexer lexer;

    private final StringBuilder dump;

    public RegexUnifier(RegexSource source) {
        this.source = source;
        this.lexer = new RegexLexer(source);
        this.dump = new StringBuilder(source.getPattern().length());
    }

    public String getUnifiedPattern() throws RegexSyntaxException {
        dump.append("/");
        while (lexer.hasNext()) {
            Token token = lexer.next();
            switch (token.kind) {
                case caret:
                    dump.append("^");
                    break;
                case dollar:
                    dump.append("$");
                    break;
                case wordBoundary:
                    dump.append("\\b");
                    break;
                case nonWordBoundary:
                    dump.append("\\B");
                    break;
                case backReference:
                    dump.append("\\").append(((Token.BackReference) token).getGroupNr());
                    break;
                case quantifier:
                    final Token.Quantifier quantifier = (Token.Quantifier) token;
                    if (quantifier.getMin() == 0 && quantifier.getMax() == 1) {
                        dump.append("?");
                    } else if (quantifier.getMin() == 0 && quantifier.isInfiniteLoop()) {
                        dump.append("*");
                    } else if (quantifier.getMin() == 1 && quantifier.isInfiniteLoop()) {
                        dump.append("+");
                    } else {
                        dump.append("{").append(quantifier.getMin());
                        if (quantifier.getMax() != quantifier.getMin()) {
                            dump.append(",");
                            if (!quantifier.isInfiniteLoop()) {
                                dump.append(quantifier.getMax());
                            }
                        }
                        dump.append("}");
                    }
                    if (!quantifier.isGreedy()) {
                        dump.append("?");
                    }
                    break;
                case alternation:
                    dump.append("|");
                    break;
                case captureGroupBegin:
                    dump.append("(");
                    break;
                case nonCaptureGroupBegin:
                    dump.append("(?:");
                    break;
                case lookAheadAssertionBegin:
                    dump.append("(?=");
                    break;
                case lookBehindAssertionBegin:
                    dump.append("(?<=");
                    break;
                case negativeLookAheadAssertionBegin:
                    dump.append("(?!");
                    break;
                case groupEnd:
                    dump.append(")");
                    break;
                case charClass:
                    if (((Token.CharacterClass) token).getCodePointSet().matchesSingleChar()) {
                        dump.append("x");
                    } else {
                        dump.append("[c]");
                    }
                    break;
            }
        }
        dump.append("/");
        dump.append(source.getFlags());
        return dump.toString();
    }
}

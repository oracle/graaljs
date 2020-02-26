/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.helper;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.runtime.Boundaries;

/**
 * Helper for parsing replacement value parameters of String.prototype.[@@replace] and
 * RegExp.prototype.[@@replace].
 */
public final class ReplaceStringParser<T> {

    public interface Consumer<T> {

        void literal(T node, int start, int end);

        void match(T node);

        void matchHead(T node);

        void matchTail(T node);

        void captureGroup(T node, int groupNumber, int literalStart, int literalEnd);

        void namedCaptureGroup(T node, String groupName);
    }

    private static final class TokenConsumer implements Consumer<Void> {

        private final ArrayList<Token> tokens = new ArrayList<>();

        @Override
        public void literal(Void node, int start, int end) {
            tokens.add(new LiteralToken(start, end));
        }

        @Override
        public void match(Void node) {
            tokens.add(new Token(Token.Kind.match));
        }

        @Override
        public void matchHead(Void node) {
            tokens.add(new Token(Token.Kind.matchHead));
        }

        @Override
        public void matchTail(Void node) {
            tokens.add(new Token(Token.Kind.matchTail));
        }

        @Override
        public void captureGroup(Void node, int groupNumber, int literalStart, int literalEnd) {
            tokens.add(new CaptureGroupToken(groupNumber, literalStart, literalEnd));
        }

        @Override
        public void namedCaptureGroup(Void node, String groupName) {
            tokens.add(new NamedCaptureGroupToken(groupName));
        }

        public Token[] getTokens() {
            return tokens.toArray(new Token[0]);
        }
    }

    public static class Token {

        public enum Kind {
            literal,
            match,
            matchHead,
            matchTail,
            captureGroup,
            namedCaptureGroup,
        }

        private final Kind kind;

        public Token(Kind kind) {
            this.kind = kind;
        }

        public Kind getKind() {
            return kind;
        }
    }

    public static class LiteralToken extends Token {

        private final int start;
        private final int end;

        public LiteralToken(int start, int end) {
            super(Kind.literal);
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    public static class CaptureGroupToken extends Token {

        private final int groupNumber;
        private final int literalStart;
        private final int literalEnd;

        public CaptureGroupToken(int groupNumber, int literalStart, int literalEnd) {
            super(Kind.captureGroup);
            this.groupNumber = groupNumber;
            this.literalStart = literalStart;
            this.literalEnd = literalEnd;
        }

        public int getGroupNumber() {
            return groupNumber;
        }

        public int getLiteralStart() {
            return literalStart;
        }

        public int getLiteralEnd() {
            return literalEnd;
        }
    }

    public static class NamedCaptureGroupToken extends Token {

        private final String groupName;

        public NamedCaptureGroupToken(String groupName) {
            super(Kind.namedCaptureGroup);
            this.groupName = groupName;
        }

        public String getGroupName() {
            return groupName;
        }
    }

    private final String replaceStr;
    private final int maxGroupNumber; // exclusive
    private final boolean parseNamedCaptureGroups;
    private int index = 0;

    private ReplaceStringParser(String replaceStr, int maxGroupNumber, boolean parseNamedCaptureGroups) {
        this.replaceStr = replaceStr;
        this.maxGroupNumber = maxGroupNumber;
        this.parseNamedCaptureGroups = parseNamedCaptureGroups;
    }

    public static <T> void process(String replaceStr, int maxGroupNumber, boolean parseNamedCaptureGroups, BranchProfile hasDollarProfile, Consumer<T> consumer, T node) {
        new ReplaceStringParser<T>(replaceStr, maxGroupNumber, parseNamedCaptureGroups).process(consumer, node, hasDollarProfile);
    }

    @TruffleBoundary
    public static Token[] parse(String replaceStr, int maxGroupNumber, boolean parseNamedCaptureGroups) {
        TokenConsumer consumer = new TokenConsumer();
        new ReplaceStringParser<Void>(replaceStr, maxGroupNumber, parseNamedCaptureGroups).process(consumer, null, BranchProfile.create());
        return consumer.getTokens();
    }

    public static <T> void processParsed(Token[] tokens, Consumer<T> consumer, T node) {
        for (Token t : tokens) {
            switch (t.getKind()) {
                case literal:
                    consumer.literal(node, ((LiteralToken) t).getStart(), ((LiteralToken) t).getEnd());
                    break;
                case match:
                    consumer.match(node);
                    break;
                case matchHead:
                    consumer.matchHead(node);
                    break;
                case matchTail:
                    consumer.matchTail(node);
                    break;
                case captureGroup:
                    consumer.captureGroup(node, ((CaptureGroupToken) t).getGroupNumber(), ((CaptureGroupToken) t).getLiteralStart(), ((CaptureGroupToken) t).getLiteralEnd());
                    break;
                case namedCaptureGroup:
                    consumer.namedCaptureGroup(node, ((NamedCaptureGroupToken) t).getGroupName());
                    break;
            }
        }
    }

    public void process(Consumer<T> consumer, T node, BranchProfile hasDollarProfile) {
        while (hasNext()) {
            parseNextDollar(consumer, node, hasDollarProfile);
        }
    }

    private boolean hasNext() {
        return index < replaceStr.length();
    }

    private void parseNextDollar(Consumer<T> consumer, T node, BranchProfile hasDollarProfile) {
        assert hasNext();
        int dollarPos = replaceStr.indexOf('$', index);
        if (dollarPos < 0 || dollarPos + 1 == replaceStr.length()) {
            literal(consumer, node, replaceStr.length(), replaceStr.length());
            return;
        }
        hasDollarProfile.enter();
        char ch = replaceStr.charAt(dollarPos + 1);
        switch (ch) {
            case '$':
                literal(consumer, node, dollarPos + 1, dollarPos + 2);
                return;
            case '&':
                match(consumer, node, dollarPos, dollarPos + 2);
                return;
            case '`':
                matchHead(consumer, node, dollarPos, dollarPos + 2);
                return;
            case '\'':
                matchTail(consumer, node, dollarPos, dollarPos + 2);
                return;
            case '<':
                if (parseNamedCaptureGroups) {
                    int groupNameStart = dollarPos + 2;
                    int groupNameEnd = replaceStr.indexOf('>', groupNameStart);
                    if (groupNameEnd >= 0) {
                        namedCaptureGroup(consumer, node, dollarPos, Boundaries.substring(replaceStr, groupNameStart, groupNameEnd), groupNameEnd + 1);
                        return;
                    }
                }
                break;
            default:
                if (isDigit(ch)) {
                    int firstDigit = ch - '0';
                    if (replaceStr.length() > (dollarPos + 2) && isDigit(replaceStr.charAt(dollarPos + 2))) {
                        int groupNumber = firstDigit * 10 + (replaceStr.charAt(dollarPos + 2) - '0');
                        if (0 < groupNumber && groupNumber < maxGroupNumber) {
                            captureGroup(consumer, node, dollarPos, groupNumber, dollarPos + 3);
                            return;
                        }
                    }
                    if (0 < firstDigit && firstDigit < maxGroupNumber) {
                        captureGroup(consumer, node, dollarPos, firstDigit, dollarPos + 2);
                        return;
                    }
                }
                break;
        }
        literal(consumer, node, dollarPos + 2, dollarPos + 2);
    }

    private void literal(Consumer<T> consumer, T node, int literalEnd, int nextIndex) {
        consumer.literal(node, index, literalEnd);
        index = nextIndex;
    }

    private void match(Consumer<T> consumer, T node, int literalEnd, int nextIndex) {
        consumer.literal(node, index, literalEnd);
        consumer.match(node);
        index = nextIndex;
    }

    private void matchHead(Consumer<T> consumer, T node, int literalEnd, int nextIndex) {
        consumer.literal(node, index, literalEnd);
        consumer.matchHead(node);
        index = nextIndex;
    }

    private void matchTail(Consumer<T> consumer, T node, int literalEnd, int nextIndex) {
        consumer.literal(node, index, literalEnd);
        consumer.matchTail(node);
        index = nextIndex;
    }

    private void captureGroup(Consumer<T> consumer, T node, int literalEnd, int groupNumber, int nextIndex) {
        consumer.literal(node, index, literalEnd);
        consumer.captureGroup(node, groupNumber, literalEnd, nextIndex);
        index = nextIndex;
    }

    private void namedCaptureGroup(Consumer<T> consumer, T node, int literalEnd, String groupName, int nextIndex) {
        consumer.literal(node, index, literalEnd);
        consumer.namedCaptureGroup(node, groupName);
        index = nextIndex;
    }

    private boolean isDigit(char ch) {
        return maxGroupNumber > 0 && '0' <= ch && ch <= '9';
    }
}

/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class Token {

    public static Token create(Kind kind) {
        return new Token(kind);
    }

    public static Token createBackReference(int groupNr) {
        return new BackReference(groupNr);
    }

    public static Token createQuantifier(int min, int max, boolean greedy) {
        return new Quantifier(min, max, greedy);
    }

    public static Token createCharClass(CodePointSet codePointSet) {
        return new CharacterClass(codePointSet);
    }

    public enum Kind {
        caret,
        dollar,
        wordBoundary,
        nonWordBoundary,
        backReference,
        quantifier,
        alternation,
        captureGroupBegin,
        nonCaptureGroupBegin,
        lookAheadAssertionBegin,
        lookBehindAssertionBegin,
        negativeLookAheadAssertionBegin,
        groupEnd,
        charClass
    }

    public final Kind kind;

    public Token(Kind kind) {
        this.kind = kind;
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("Token", new DebugUtil.Value("kind", kind.name()));
    }

    public static final class Quantifier extends Token {

        private final int min;
        private final int max;
        private boolean greedy;

        public Quantifier(int min, int max, boolean greedy) {
            super(Kind.quantifier);
            this.min = min;
            this.max = max;
            this.greedy = greedy;
        }

        public boolean isInfiniteLoop() {
            return getMax() == -1;
        }

        /**
         * The minimum number of times the quantified element must appear. Can be -1 to represent a
         * virtually infinite number of occurrences are necessary (e.g. as in
         * <code>a{1111111111111111111,}</code>). Any number which is larger than the maximum size
         * of the platform's String data type is considered "virtually infinite".
         */
        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public boolean isGreedy() {
            return greedy;
        }

        public void setGreedy(boolean greedy) {
            this.greedy = greedy;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public DebugUtil.Table toTable() {
            return super.toTable().append(
                            new DebugUtil.Value("min", getMin()),
                            new DebugUtil.Value("max", getMax()),
                            new DebugUtil.Value("greedy", isGreedy()));
        }
    }

    public static final class CharacterClass extends Token {

        private final CodePointSet codePointSet;

        public CharacterClass(CodePointSet codePointSet) {
            super(Kind.charClass);
            this.codePointSet = codePointSet;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public DebugUtil.Table toTable() {
            return super.toTable().append(new DebugUtil.Value("codePointSet", codePointSet));
        }

        public CodePointSet getCodePointSet() {
            return codePointSet;
        }
    }

    public static final class BackReference extends Token {

        private final int groupNr;

        public BackReference(int groupNr) {
            super(Kind.backReference);
            this.groupNr = groupNr;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public DebugUtil.Table toTable() {
            return super.toTable().append(new DebugUtil.Value("groupNr", groupNr));
        }

        public int getGroupNr() {
            return groupNr;
        }
    }
}

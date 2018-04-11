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

/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;

/**
 * Matcher for a single character range.
 */
public final class SingleRangeMatcher extends ProfiledCharMatcher {

    private final char lo;
    private final char hi;

    /**
     * Constructs a new {@link SingleRangeMatcher}.
     * 
     * @param invert see {@link ProfiledCharMatcher}.
     * @param lo inclusive lower bound of range to match.
     * @param hi inclusive upper bound of range to match.
     */
    public SingleRangeMatcher(boolean invert, char lo, char hi) {
        super(invert);
        this.lo = lo;
        this.hi = hi;
    }

    /**
     * @return inclusive lower bound of range to match.
     */
    public char getLo() {
        return lo;
    }

    /**
     * @return inclusive upper bound of range to match.
     */
    public char getHi() {
        return hi;
    }

    @Override
    public boolean matchChar(char c) {
        return lo <= c && hi >= c;
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return modifiersToString() + MatcherBuilder.rangeToString(lo, hi);
    }
}

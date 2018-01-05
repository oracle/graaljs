/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;

public final class SingleRangeMatcher extends ProfiledCharMatcher {

    private final char lo;
    private final char hi;

    public SingleRangeMatcher(boolean invert, char lo, char hi) {
        super(invert);
        this.lo = lo;
        this.hi = hi;
    }

    public char getHi() {
        return hi;
    }

    public char getLo() {
        return lo;
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

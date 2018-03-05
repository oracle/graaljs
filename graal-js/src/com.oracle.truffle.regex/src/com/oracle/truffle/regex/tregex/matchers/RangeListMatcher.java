/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ExplodeLoop;

/**
 * Character range matcher using a sorted list of ranges.
 */
public final class RangeListMatcher extends ProfiledCharMatcher {

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final char[] ranges;

    /**
     * Constructs a new {@link RangeListMatcher}.
     * 
     * @param invert see {@link ProfiledCharMatcher}.
     * @param ranges a sorted array of character ranges in the form [lower inclusive bound of range
     *            0, higher inclusive bound of range 0, lower inclusive bound of range 1, higher
     *            inclusive bound of range 1, ...]. The array contents are not modified by this
     *            method.
     */
    public RangeListMatcher(boolean invert, char[] ranges) {
        super(invert);
        this.ranges = ranges;
    }

    @Override
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    public boolean matchChar(char c) {
        for (int i = 0; i < ranges.length; i += 2) {
            final char lo = ranges[i];
            final char hi = ranges[i + 1];
            if (lo <= c) {
                if (hi >= c) {
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return "list " + modifiersToString() + MatcherBuilder.rangesToString(ranges);
    }
}

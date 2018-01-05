/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class RangeListMatcher extends ProfiledCharMatcher {

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final char[] ranges;

    public RangeListMatcher(boolean invert, char[] ranges) {
        super(invert);
        this.ranges = ranges;
    }

    @Override
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE)
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
        return modifiersToString() + MatcherBuilder.rangesToString(ranges);
    }
}

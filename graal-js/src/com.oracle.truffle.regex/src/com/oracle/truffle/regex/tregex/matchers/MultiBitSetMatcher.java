/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

import java.util.Arrays;

/**
 * Character matcher that uses an array of 256 bit sets to fully cover the 16 bit character space.
 * This matcher can take up to 8 kilobytes of space, so it should be used only when the set of
 * characters to match is very large and sparse.
 */
public final class MultiBitSetMatcher extends ProfiledCharMatcher {

    private static final int BYTE_RANGE = 256;
    private static final int BYTE_MAX_VALUE = 255;
    private static final int BYTE_MIN_VALUE = 0;

    private static final CompilationFinalBitSet MATCH_NONE = new CompilationFinalBitSet(BYTE_RANGE);
    private static final CompilationFinalBitSet MATCH_ALL = new CompilationFinalBitSet(BYTE_RANGE);

    static {
        MATCH_ALL.invert();
    }

    /**
     * Constructs a new {@link MultiBitSetMatcher}.
     * 
     * @param inverse see {@link ProfiledCharMatcher}.
     * @param ranges a sorted array of character ranges in the form [lower inclusive bound of range
     *            0, higher inclusive bound of range 0, lower inclusive bound of range 1, higher
     *            inclusive bound of range 1, ...]. The array contents are not modified by this
     *            method.
     * @return a new {@link MultiBitSetMatcher}.
     */
    public static MultiBitSetMatcher fromRanges(boolean inverse, char[] ranges) {
        CompilationFinalBitSet[] bitSets = new CompilationFinalBitSet[BYTE_RANGE];
        Arrays.fill(bitSets, MATCH_NONE);
        CompilationFinalBitSet cur = new CompilationFinalBitSet(BYTE_RANGE);
        int curByte = highByte(ranges[0]);
        for (int i = 0; i < ranges.length; i += 2) {
            char rangeLo = ranges[i];
            char rangeHi = ranges[i + 1];
            if (highByte(rangeLo) > curByte) {
                bitSets[curByte] = cur;
                cur = new CompilationFinalBitSet(BYTE_RANGE);
                curByte = highByte(rangeLo);
            }
            if (highByte(rangeLo) == highByte(rangeHi)) {
                cur.setRange(lowByte(rangeLo), lowByte(rangeHi));
            } else {
                cur.setRange(lowByte(rangeLo), BYTE_MAX_VALUE);
                bitSets[curByte] = cur;
                for (int j = highByte(rangeLo) + 1; j < highByte(rangeHi); j++) {
                    bitSets[j] = MATCH_ALL;
                }
                cur = new CompilationFinalBitSet(BYTE_RANGE);
                curByte = highByte(rangeHi);
                cur.setRange(BYTE_MIN_VALUE, lowByte(rangeHi));
            }
        }
        bitSets[curByte] = cur;
        return new MultiBitSetMatcher(inverse, bitSets);
    }

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final CompilationFinalBitSet[] bitSets;

    private MultiBitSetMatcher(boolean invert, CompilationFinalBitSet[] bitSets) {
        super(invert);
        this.bitSets = bitSets;
    }

    @Override
    protected boolean matchChar(char c) {
        return bitSets[highByte(c)].get(lowByte(c));
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        StringBuilder sb = new StringBuilder(modifiersToString()).append("[\n");
        for (int i = 0; i < bitSets.length; i++) {
            sb.append(String.format("    %02x: ", i)).append(bitSets[i]).append("\n");
        }
        return sb.append("  ]").toString();
    }
}

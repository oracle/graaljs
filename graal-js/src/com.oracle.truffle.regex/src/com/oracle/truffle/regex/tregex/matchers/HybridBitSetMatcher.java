/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

/**
 * Character matcher that uses a sorted list of bit sets (like {@link BitSetMatcher}) in conjunction
 * with another {@link CharMatcher} to cover all characters not covered by the bit sets.
 */
public final class HybridBitSetMatcher extends ProfiledCharMatcher {

    @CompilationFinal(dimensions = 1) private final byte[] highBytes;
    @CompilationFinal(dimensions = 1) private final CompilationFinalBitSet[] bitSets;
    private final CharMatcher restMatcher;

    /**
     * Constructs a new {@link HybridBitSetMatcher}.
     * 
     * @param invert see {@link ProfiledCharMatcher}.
     * @param highBytes the respective high bytes of the bit sets.
     * @param bitSets the bit sets that match the low bytes if the character under inspection has
     *            the corresponding high byte.
     * @param restMatcher any {@link CharMatcher}, to cover the characters not covered by the bit
     *            sets.
     */
    public HybridBitSetMatcher(boolean invert, byte[] highBytes, CompilationFinalBitSet[] bitSets, CharMatcher restMatcher) {
        super(invert);
        this.highBytes = highBytes;
        this.bitSets = bitSets;
        this.restMatcher = restMatcher;
        assert isSortedUnsigned(highBytes);
    }

    @Override
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    protected boolean matchChar(char c) {
        final int highByte = highByte(c);
        for (int i = 0; i < highBytes.length; i++) {
            int bitSetHighByte = Byte.toUnsignedInt(highBytes[i]);
            if (highByte == bitSetHighByte) {
                return bitSets[i].get(lowByte(c));
            }
            if (highByte < bitSetHighByte) {
                break;
            }
        }
        return restMatcher.match(c);
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        StringBuilder sb = new StringBuilder(modifiersToString()).append("hybrid [\n");
        for (int i = 0; i < highBytes.length; i++) {
            sb.append(String.format("  %02x: ", Byte.toUnsignedInt(highBytes[i]))).append(bitSets[i]).append("\n");
        }
        return sb.append("  rest: ").append(restMatcher).append("\n]").toString();
    }

    private static boolean isSortedUnsigned(byte[] array) {
        int prev = Integer.MIN_VALUE;
        for (byte b : array) {
            int i = Byte.toUnsignedInt(b);
            if (prev > i) {
                return false;
            }
            prev = i;
        }
        return true;
    }
}

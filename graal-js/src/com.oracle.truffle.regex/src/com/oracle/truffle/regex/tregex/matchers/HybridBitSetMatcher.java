/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

public class HybridBitSetMatcher extends ProfiledCharMatcher {

    @CompilationFinal(dimensions = 1) private final byte[] highBytes;
    @CompilationFinal(dimensions = 1) private final CompilationFinalBitSet[] bitSets;
    private final CharMatcher restMatcher;

    public HybridBitSetMatcher(boolean invert, byte[] highBytes, CompilationFinalBitSet[] bitSets, CharMatcher restMatcher) {
        super(invert);
        this.highBytes = highBytes;
        this.bitSets = bitSets;
        this.restMatcher = restMatcher;
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
}

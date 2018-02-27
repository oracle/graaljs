/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.util.DebugUtil;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

/**
 * Matcher that matches multiple characters with a common high byte using a bit set.<br>
 * Example: characters {@code \u1010, \u1020, \u1030} have a common high byte {@code 0x10}, so they
 * are matched by this high byte and a bit set that matches {@code 0x10}, {@code 0x20} and
 * {@code 0x30}.
 */
public final class BitSetMatcher extends ProfiledCharMatcher {

    private final int highByte;
    private final CompilationFinalBitSet bitSet;

    private BitSetMatcher(boolean invert, int highByte, CompilationFinalBitSet bitSet) {
        super(invert);
        this.highByte = highByte;
        this.bitSet = bitSet;
    }

    /**
     * Constructs a new bit-set-based character matcher.
     * 
     * @param invert see {@link ProfiledCharMatcher}.
     * @param highByte the high byte common to all characters to match.
     * @param bitSet the bit set to match the low byte of the characters to match.
     * @return a new {@link BitSetMatcher} or a {@link NullHighByteBitSetMatcher}.
     */
    public static ProfiledCharMatcher create(boolean invert, int highByte, CompilationFinalBitSet bitSet) {
        if (highByte == 0) {
            return new NullHighByteBitSetMatcher(invert, bitSet);
        }
        return new BitSetMatcher(invert, highByte, bitSet);
    }

    @Override
    public boolean matchChar(char c) {
        return highByte(c) == highByte && bitSet.get(lowByte(c));
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return modifiersToString() + "{hi " + DebugUtil.charToString(highByte) + " lo " + bitSet + "}";
    }

    public CompilationFinalBitSet getBitSet() {
        return bitSet;
    }
}

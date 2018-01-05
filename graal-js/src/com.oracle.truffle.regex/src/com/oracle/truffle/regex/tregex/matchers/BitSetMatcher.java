/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.util.DebugUtil;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

public final class BitSetMatcher extends ProfiledCharMatcher {

    private final int highByte;
    private final CompilationFinalBitSet bitSet;

    public BitSetMatcher(boolean invert, int highByte, CompilationFinalBitSet bitSet) {
        super(invert);
        this.highByte = highByte;
        this.bitSet = bitSet;
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

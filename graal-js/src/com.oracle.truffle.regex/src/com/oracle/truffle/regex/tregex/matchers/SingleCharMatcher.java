/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * Matcher that matches a single character.
 */
public final class SingleCharMatcher extends ProfiledCharMatcher {

    private final char c;

    /**
     * Constructs a new {@link SingleCharMatcher}.
     * 
     * @param invert see {@link ProfiledCharMatcher}.
     * @param c character to match.
     */
    public SingleCharMatcher(boolean invert, char c) {
        super(invert);
        this.c = c;
    }

    /**
     * @return the character to match.
     */
    public char getChar() {
        return c;
    }

    @Override
    public boolean matchChar(char m) {
        return c == m;
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return modifiersToString() + DebugUtil.charToString(c);
    }
}

/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * Matcher that matches two characters. Used for things like dot (.) or ignore-case.
 */
public final class TwoCharMatcher extends ProfiledCharMatcher {

    private final char c1;
    private final char c2;

    /**
     * Constructs a new {@link TwoCharMatcher}.
     * 
     * @param invert see {@link ProfiledCharMatcher}.
     * @param c1 first character to match.
     * @param c2 second character to match.
     */
    public TwoCharMatcher(boolean invert, char c1, char c2) {
        super(invert);
        this.c1 = c1;
        this.c2 = c2;
    }

    @Override
    public boolean matchChar(char m) {
        return m == c1 || m == c2;
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return modifiersToString() + DebugUtil.charToString(c1) + "||" + DebugUtil.charToString(c2);
    }
}

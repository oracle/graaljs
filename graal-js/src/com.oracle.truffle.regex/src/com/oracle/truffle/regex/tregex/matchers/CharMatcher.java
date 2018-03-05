/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

public interface CharMatcher {

    CharMatcher[] EMPTY = {};

    /**
     * Check if a given character matches this {@link CharMatcher}.
     * 
     * @param c any character.
     * @return {@code true} if the character matches.
     */
    boolean match(char c);
}

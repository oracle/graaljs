/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

public final class AnyMatcher implements CharMatcher {

    private AnyMatcher() {
    }

    public static final AnyMatcher INSTANCE = new AnyMatcher();

    public static final CharMatcher[] INSTANCE_ARRAY = new CharMatcher[]{INSTANCE};

    public static CharMatcher create() {
        return INSTANCE;
    }

    public static CharMatcher create(boolean invert) {
        return invert ? EmptyMatcher.create() : create();
    }

    @Override
    public boolean match(char c) {
        return true;
    }

    @Override
    public String toString() {
        return "any";
    }
}

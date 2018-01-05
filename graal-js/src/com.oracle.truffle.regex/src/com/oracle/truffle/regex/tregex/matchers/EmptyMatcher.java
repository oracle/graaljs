/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.matchers;

public final class EmptyMatcher implements CharMatcher {

    private EmptyMatcher() {
    }

    private static final EmptyMatcher INSTANCE = new EmptyMatcher();

    public static EmptyMatcher create() {
        return INSTANCE;
    }

    public static CharMatcher create(boolean invert) {
        return invert ? AnyMatcher.create() : create();
    }

    @Override
    public boolean match(char c) {
        return false;
    }

    @Override
    public String toString() {
        return "empty";
    }
}

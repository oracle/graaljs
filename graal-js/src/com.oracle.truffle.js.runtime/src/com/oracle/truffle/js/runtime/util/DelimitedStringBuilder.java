/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.*;

/**
 * A special implementation (wrapper) of a StringBuilder. Provides some additional support required
 * for Truffle/JS, e.g. checking the string length (and throwing a RangeError), and TruffleBoundary
 * annotations.
 */
public final class DelimitedStringBuilder {

    private final StringBuilder builder;

    public DelimitedStringBuilder() {
        this.builder = new StringBuilder();
    }

    @TruffleBoundary
    public DelimitedStringBuilder(int capacity) {
        this.builder = new StringBuilder(Math.max(16, Math.min(capacity, JSTruffleOptions.StringLengthLimit)));
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return builder.toString();
    }

    @TruffleBoundary
    public void append(String str) {
        if ((builder.length() + str.length()) > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(str);
    }

    @TruffleBoundary
    public void append(char c) {
        if (builder.length() > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(c);
    }

    @TruffleBoundary
    public void append(int intValue) {
        if (builder.length() > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(intValue);
    }

    @TruffleBoundary
    public void append(long longValue) {
        if (builder.length() > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(longValue);
    }

    @TruffleBoundary
    public void append(String charSequence, int start, int end) {
        assert start <= end;
        if (builder.length() + (end - start) > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(charSequence, start, end);
    }

    public int length() {
        return builder.length();
    }
}

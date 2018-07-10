/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    @TruffleBoundary(allowInlining = true)
    public DelimitedStringBuilder(int capacity) {
        this.builder = new StringBuilder(Math.max(16, Math.min(capacity, JSTruffleOptions.StringLengthLimit)));
    }

    @Override
    @TruffleBoundary(allowInlining = true)
    public String toString() {
        return builder.toString();
    }

    @TruffleBoundary(allowInlining = true)
    public void append(String str) {
        if ((builder.length() + str.length()) > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(str);
    }

    @TruffleBoundary(allowInlining = true)
    public void append(char c) {
        if (builder.length() > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(c);
    }

    @TruffleBoundary(allowInlining = true)
    public void append(int intValue) {
        if (builder.length() > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(intValue);
    }

    @TruffleBoundary(allowInlining = true)
    public void append(long longValue) {
        if (builder.length() > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        builder.append(longValue);
    }

    @TruffleBoundary(allowInlining = true)
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

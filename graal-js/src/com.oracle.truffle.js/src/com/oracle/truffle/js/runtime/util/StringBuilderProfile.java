/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.nodes.NodeCloneable;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;

/**
 * A wrapper around StringBuilder methods that takes care of profiling and checking that the string
 * length does not exceed the allowed limit.
 */
public final class StringBuilderProfile extends NodeCloneable {
    private static final int MAX_INT_STRING_LENGTH = 11;
    private static final int MAX_LONG_STRING_LENGTH = 20;

    private final int stringLengthLimit;
    private final BranchProfile errorBranch;

    private StringBuilderProfile(int stringLengthLimit) {
        this.stringLengthLimit = stringLengthLimit;
        this.errorBranch = BranchProfile.create();
    }

    public static StringBuilderProfile create(int stringLengthLimit) {
        return new StringBuilderProfile(stringLengthLimit);
    }

    @SuppressWarnings("static-method")
    public StringBuilder newStringBuilder() {
        return new StringBuilder();
    }

    public StringBuilder newStringBuilder(int capacity) {
        return new StringBuilder(Math.max(16, Math.min(capacity, stringLengthLimit)));
    }

    @SuppressWarnings("static-method")
    public String toString(StringBuilder builder) {
        return Boundaries.builderToString(builder);
    }

    public void append(StringBuilder builder, String str) {
        if ((builder.length() + str.length()) > stringLengthLimit) {
            errorBranch.enter();
            throw Errors.createRangeErrorInvalidStringLength();
        }
        Boundaries.builderAppend(builder, str);
    }

    public void append(StringBuilder builder, char c) {
        if (builder.length() + 1 > stringLengthLimit) {
            errorBranch.enter();
            throw Errors.createRangeErrorInvalidStringLength();
        }
        Boundaries.builderAppend(builder, c);
    }

    public void append(StringBuilder builder, int intValue) {
        if (builder.length() + MAX_INT_STRING_LENGTH > stringLengthLimit) {
            errorBranch.enter();
            throw Errors.createRangeErrorInvalidStringLength();
        }
        Boundaries.builderAppend(builder, intValue);
    }

    public void append(StringBuilder builder, long longValue) {
        if (builder.length() + MAX_LONG_STRING_LENGTH > stringLengthLimit) {
            errorBranch.enter();
            throw Errors.createRangeErrorInvalidStringLength();
        }
        Boundaries.builderAppend(builder, longValue);
    }

    public void append(StringBuilder builder, String charSequence, int start, int end) {
        assert start <= end;
        if (builder.length() + (end - start) > stringLengthLimit) {
            errorBranch.enter();
            throw Errors.createRangeErrorInvalidStringLength();
        }
        Boundaries.builderAppend(builder, charSequence, start, end);
    }

    @Override
    protected Object clone() {
        return new StringBuilderProfile(stringLengthLimit);
    }
}

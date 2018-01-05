/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.regex.CompiledRegex;

public abstract class RegexResult {

    public static final RegexResult NO_MATCH = new RegexResult(null, "NULL", 0) {
    };

    private final CompiledRegex regex;
    private final Object input;
    private final int groupCount;

    public RegexResult(CompiledRegex regex, Object input, int groupCount) {
        this.regex = regex;
        this.input = input;
        this.groupCount = groupCount;
    }

    public final CompiledRegex getCompiledRegex() {
        return regex;
    }

    public final Object getInput() {
        return input;
    }

    public final int getGroupCount() {
        return groupCount;
    }
}

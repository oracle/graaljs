/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.regex.RegexCompiledRegex;

public final class SingleResult extends RegexResult {

    private final int start;
    private final int end;

    public SingleResult(RegexCompiledRegex regex, Object input, int start, int end) {
        super(regex, input, 1);
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

}

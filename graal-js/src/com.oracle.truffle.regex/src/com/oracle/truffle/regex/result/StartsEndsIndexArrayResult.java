/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.regex.RegexObject;

public final class StartsEndsIndexArrayResult extends RegexResult {

    private final int[] starts;
    private final int[] ends;

    public StartsEndsIndexArrayResult(RegexObject regex, Object input, int[] starts, int[] ends) {
        super(regex, input, starts.length);
        this.starts = starts;
        this.ends = ends;
    }

    public int[] getStarts() {
        return starts;
    }

    public int[] getEnds() {
        return ends;
    }

}

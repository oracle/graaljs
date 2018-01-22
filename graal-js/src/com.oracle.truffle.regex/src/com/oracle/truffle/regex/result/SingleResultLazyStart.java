/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.regex.RegexObject;

public final class SingleResultLazyStart extends RegexResult {

    private final int fromIndex;
    private int start = -1;
    private final int end;
    private final CallTarget findStartCallTarget;

    public SingleResultLazyStart(RegexObject regex, Object input, int fromIndex, int end, CallTarget findStartCallTarget) {
        super(regex, input, 1);
        this.fromIndex = fromIndex;
        this.end = end;
        this.findStartCallTarget = findStartCallTarget;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public CallTarget getFindStartCallTarget() {
        return findStartCallTarget;
    }

}

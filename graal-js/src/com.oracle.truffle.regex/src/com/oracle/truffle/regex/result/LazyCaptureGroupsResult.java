/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.regex.RegexCompiledRegex;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

public final class LazyCaptureGroupsResult extends RegexResult {

    private final int fromIndex;
    private final int end;
    private int[] result = null;
    private final CallTarget findStartCallTarget;
    private final CallTarget captureGroupCallTarget;

    public LazyCaptureGroupsResult(RegexCompiledRegex regex,
                    Object input,
                    int fromIndex,
                    int end,
                    int numberOfCaptureGroups,
                    CallTarget findStartCallTarget,
                    CallTarget captureGroupCallTarget) {
        super(regex, input, numberOfCaptureGroups);
        this.fromIndex = fromIndex;
        this.end = end;
        this.findStartCallTarget = findStartCallTarget;
        this.captureGroupCallTarget = captureGroupCallTarget;
    }

    public LazyCaptureGroupsResult(RegexCompiledRegex regex, Object input, int[] result) {
        this(regex, input, -1, -1, result.length / 2, null, null);
        this.result = result;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getEnd() {
        return end;
    }

    public void setResult(int[] result) {
        this.result = result;
    }

    public int[] getResult() {
        return result;
    }

    public CallTarget getFindStartCallTarget() {
        return findStartCallTarget;
    }

    public CallTarget getCaptureGroupCallTarget() {
        return captureGroupCallTarget;
    }

    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("NFAExecutorResult",
                        new DebugUtil.Value("input", getInput()),
                        new DebugUtil.Value("fromIndex", fromIndex),
                        new DebugUtil.Value("end", end),
                        new DebugUtil.Value("result", Arrays.toString(result)),
                        new DebugUtil.Value("captureGroupCallTarget", captureGroupCallTarget));
    }
}

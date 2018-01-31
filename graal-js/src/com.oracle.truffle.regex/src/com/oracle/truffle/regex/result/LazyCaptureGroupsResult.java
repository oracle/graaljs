/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.tregex.nodes.TRegexLazyCaptureGroupsRootNode;
import com.oracle.truffle.regex.tregex.nodes.TRegexLazyFindStartRootNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

public final class LazyCaptureGroupsResult extends RegexResult {

    private final int fromIndex;
    private final int end;
    private int[] result = null;
    private final CallTarget findStartCallTarget;
    private final CallTarget captureGroupCallTarget;

    public LazyCaptureGroupsResult(RegexObject regex,
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

    public LazyCaptureGroupsResult(RegexObject regex, Object input, int[] result) {
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

    /**
     * Creates an arguments array suitable for the lazy calculation of this result's starting index.
     * @return an arguments array suitable for calling the {@link TRegexLazyFindStartRootNode} contained in
     * {@link #getFindStartCallTarget()}.
     */
    public Object[] createArgsFindStart() {
        return new Object[]{getInput(), getEnd() - 1, getFromIndex()};
    }

    /**
     * Creates an arguments array suitable for the lazy calculation of this result's capture group boundaries.
     * @param start The value returned by the call to the {@link TRegexLazyFindStartRootNode} contained in
     *              {@link #getFindStartCallTarget()}.
     * @return an arguments array suitable for calling the {@link TRegexLazyCaptureGroupsRootNode} contained in
     * {@link #getCaptureGroupCallTarget()}.
     */
    public Object[] createArgsCG(int start) {
        return new Object[]{this, start + 1, getEnd()};
    }

    /**
     * Creates an arguments array suitable for the lazy calculation of this result's capture group boundaries if there is
     * no find-start call target (this is the case when the expression is sticky or starts with "^").
     * @return an arguments array suitable for calling the {@link TRegexLazyCaptureGroupsRootNode} contained in
     * {@link #getCaptureGroupCallTarget()}.
     */
    public Object[] createArgsCGNoFindStart() {
        assert findStartCallTarget == null;
        return new Object[]{this, getFromIndex(), getEnd()};
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

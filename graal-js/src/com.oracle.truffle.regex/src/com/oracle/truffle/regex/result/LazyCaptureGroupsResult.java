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
     * 
     * @return an arguments array suitable for calling the {@link TRegexLazyFindStartRootNode}
     *         contained in {@link #getFindStartCallTarget()}.
     */
    public Object[] createArgsFindStart() {
        return new Object[]{getInput(), getEnd() - 1, getFromIndex()};
    }

    /**
     * Creates an arguments array suitable for the lazy calculation of this result's capture group
     * boundaries.
     * 
     * @param start The value returned by the call to the {@link TRegexLazyFindStartRootNode}
     *            contained in {@link #getFindStartCallTarget()}.
     * @return an arguments array suitable for calling the {@link TRegexLazyCaptureGroupsRootNode}
     *         contained in {@link #getCaptureGroupCallTarget()}.
     */
    public Object[] createArgsCG(int start) {
        return new Object[]{this, start + 1, getEnd()};
    }

    /**
     * Creates an arguments array suitable for the lazy calculation of this result's capture group
     * boundaries if there is no find-start call target (this is the case when the expression is
     * sticky or starts with "^").
     * 
     * @return an arguments array suitable for calling the {@link TRegexLazyCaptureGroupsRootNode}
     *         contained in {@link #getCaptureGroupCallTarget()}.
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

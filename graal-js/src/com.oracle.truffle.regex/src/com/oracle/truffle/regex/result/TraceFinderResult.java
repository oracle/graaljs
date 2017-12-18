/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.regex.RegexCompiledRegex;

public final class TraceFinderResult extends RegexResult {

    private final int fromIndex;
    private final int end;
    private final int[] indices;
    private final CallTarget traceFinderCallTarget;
    @CompilationFinal(dimensions = 1) private final PreCalculatedResultFactory[] preCalculatedResults;
    private boolean resultCalculated = false;

    public TraceFinderResult(RegexCompiledRegex regex, Object input, int fromIndex, int end, CallTarget traceFinderCallTarget, PreCalculatedResultFactory[] preCalculatedResults) {
        super(regex, input, preCalculatedResults[0].getNumberOfGroups());
        this.fromIndex = fromIndex;
        this.end = end;
        this.indices = new int[preCalculatedResults[0].getNumberOfGroups() * 2];
        this.traceFinderCallTarget = traceFinderCallTarget;
        this.preCalculatedResults = preCalculatedResults;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getEnd() {
        return end;
    }

    public int[] getIndices() {
        return indices;
    }

    public CallTarget getTraceFinderCallTarget() {
        return traceFinderCallTarget;
    }

    public PreCalculatedResultFactory[] getPreCalculatedResults() {
        return preCalculatedResults;
    }

    public boolean isResultCalculated() {
        return resultCalculated;
    }

    public void setResultCalculated() {
        this.resultCalculated = true;
    }

}

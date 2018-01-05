/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.regex.tregex.matchers.CharMatcher;

public class TraceFinderDFAStateNode extends BackwardDFAStateNode {

    public static final byte NO_PRE_CALC_RESULT = (byte) 0xff;

    private final byte preCalculatedUnAnchoredResult;
    private final byte preCalculatedAnchoredResult;

    public TraceFinderDFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean loopToSelf,
                    boolean findSingleChar,
                    short[] successors,
                    CharMatcher[] matchers,
                    short backwardPrefixState,
                    byte preCalculatedUnAnchoredResult,
                    byte preCalculatedAnchoredResult) {
        super(id, finalState, anchoredFinalState, loopToSelf, findSingleChar, successors, matchers, backwardPrefixState);
        this.preCalculatedUnAnchoredResult = preCalculatedUnAnchoredResult;
        this.preCalculatedAnchoredResult = preCalculatedAnchoredResult;
    }

    private TraceFinderDFAStateNode(TraceFinderDFAStateNode copy, short copyID) {
        super(copy, copyID);
        this.preCalculatedUnAnchoredResult = copy.preCalculatedUnAnchoredResult;
        this.preCalculatedAnchoredResult = copy.preCalculatedAnchoredResult;
    }

    @Override
    public DFAStateNode createNodeSplitCopy(short copyID) {
        return new TraceFinderDFAStateNode(this, copyID);
    }

    public boolean hasPreCalculatedUnAnchoredResult() {
        return preCalculatedUnAnchoredResult != NO_PRE_CALC_RESULT;
    }

    public int getPreCalculatedUnAnchoredResult() {
        return Byte.toUnsignedInt(preCalculatedUnAnchoredResult);
    }

    public boolean hasPreCalculatedAnchoredResult() {
        return preCalculatedAnchoredResult != NO_PRE_CALC_RESULT;
    }

    public int getPreCalculatedAnchoredResult() {
        return Byte.toUnsignedInt(preCalculatedAnchoredResult);
    }
}

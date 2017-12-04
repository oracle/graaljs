/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;
import com.oracle.truffle.regex.tregex.nodes.input.InputIterator;

public class TraceFinderDFAStateNode extends BackwardDFAStateNode {

    public static final byte NO_PRE_CALC_RESULT = (byte) 0xff;

    private final byte preCalculatedUnAnchoredResult;
    private final byte preCalculatedAnchoredResult;

    public TraceFinderDFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean findSingleChar, short loopToSelf,
                    short[] successors,
                    CharMatcher[] matchers,
                    byte preCalculatedUnAnchoredResult,
                    byte preCalculatedAnchoredResult) {
        super(id, finalState, anchoredFinalState, findSingleChar, loopToSelf, successors, matchers);
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

    @Override
    protected void storeResult(VirtualFrame frame, InputIterator inputIterator, int index, boolean anchored) {
        if (anchored) {
            assert hasPreCalculatedAnchoredResult();
            if (hasPreCalculatedUnAnchoredResult() && getPreCalculatedUnAnchoredResult() < getPreCalculatedAnchoredResult()) {
                inputIterator.setResultInt(frame, getPreCalculatedUnAnchoredResult());
            } else {
                inputIterator.setResultInt(frame, getPreCalculatedAnchoredResult());
            }
        } else {
            assert hasPreCalculatedUnAnchoredResult();
            inputIterator.setResultInt(frame, getPreCalculatedUnAnchoredResult());
        }
    }
}

/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;
import com.oracle.truffle.regex.tregex.nodes.input.InputIterator;

public class BackwardDFAStateNode extends DFAStateNode {

    public BackwardDFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean findSingleChar,
                    short loopToSelf,
                    short[] successors,
                    CharMatcher[] matchers) {
        super(id, finalState, anchoredFinalState, findSingleChar, loopToSelf, successors, matchers);
    }

    protected BackwardDFAStateNode(BackwardDFAStateNode copy, short copyID) {
        super(copy, copyID);
    }

    @Override
    public DFAStateNode createNodeSplitCopy(short copyID) {
        return new BackwardDFAStateNode(this, copyID);
    }

    private boolean hasBackwardPrefixState() {
        return getSuccessors().length > getMatchers().length;
    }

    private int getBackwardPrefixStateIndex() {
        assert hasBackwardPrefixState();
        return getSuccessors().length - 1;
    }

    @Override
    protected int prevIndex(VirtualFrame frame, InputIterator inputIterator) {
        return inputIterator.getIndex(frame) + 1;
    }

    @Override
    protected int atEnd1(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        super.atEnd1(frame, inputIterator, transitions, d);
        return switchToPrefixState(inputIterator, frame);
    }

    @Override
    protected int atEnd2(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        super.atEnd2(frame, inputIterator, transitions, d);
        return switchToPrefixState(inputIterator, frame);
    }

    @Override
    protected int atEnd3(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int preLoopIndex) {
        super.atEnd3(frame, inputIterator, transitions, d, preLoopIndex);
        return switchToPrefixState(inputIterator, frame);
    }

    private int switchToPrefixState(InputIterator inputIterator, VirtualFrame frame) {
        if (inputIterator.getIndex(frame) == inputIterator.getFromIndex(frame) - 1 && inputIterator.getFromIndex(frame) - 1 > inputIterator.getMaxIndex(frame) && hasBackwardPrefixState()) {
            inputIterator.setCurMaxIndex(frame, inputIterator.getMaxIndex(frame));
            return getBackwardPrefixStateIndex();
        }
        return FS_RESULT_NO_SUCCESSOR;
    }
}

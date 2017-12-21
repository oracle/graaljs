/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;

public class BackwardDFAStateNode extends DFAStateNode {

    public BackwardDFAStateNode(short id, boolean finalState, boolean anchoredFinalState, boolean findSingleChar, short loopToSelf, short[] successors, CharMatcher[] matchers) {
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
    protected int prevIndex(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        return executor.getIndex(frame) + 1;
    }

    @Override
    protected int atEnd1(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        super.atEnd1(frame, executor);
        return switchToPrefixState(executor, frame);
    }

    @Override
    protected int atEnd2(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        super.atEnd2(frame, executor);
        return switchToPrefixState(executor, frame);
    }

    @Override
    protected int atEnd3(VirtualFrame frame, TRegexDFAExecutorNode executor, int preLoopIndex) {
        super.atEnd3(frame, executor, preLoopIndex);
        return switchToPrefixState(executor, frame);
    }

    private int switchToPrefixState(TRegexDFAExecutorNode executor, VirtualFrame frame) {
        if (executor.getIndex(frame) == executor.getFromIndex(frame) - 1 && executor.getFromIndex(frame) - 1 > executor.getMaxIndex(frame) && hasBackwardPrefixState()) {
            executor.setCurMaxIndex(frame, executor.getMaxIndex(frame));
            return getBackwardPrefixStateIndex();
        }
        return FS_RESULT_NO_SUCCESSOR;
    }
}

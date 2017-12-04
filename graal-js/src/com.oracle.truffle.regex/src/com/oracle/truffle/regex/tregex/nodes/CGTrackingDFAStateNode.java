/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;
import com.oracle.truffle.regex.tregex.nodes.input.InputIterator;

public class CGTrackingDFAStateNode extends DFAStateNode {

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] captureGroupTransitions;

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] precedingCaptureGroupTransitions;

    @Child private DFACaptureGroupPartialTransitionDispatchNode transitionDispatchNode;

    public CGTrackingDFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean findSingleChar,
                    short loopToSelf,
                    short[] successors,
                    CharMatcher[] matchers,
                    short[] captureGroupTransitions,
                    short[] precedingCaptureGroupTransitions) {
        super(id, finalState, anchoredFinalState, findSingleChar, loopToSelf, successors, matchers);
        this.captureGroupTransitions = captureGroupTransitions;
        this.precedingCaptureGroupTransitions = precedingCaptureGroupTransitions;
        transitionDispatchNode = precedingCaptureGroupTransitions.length > 1 ? DFACaptureGroupPartialTransitionDispatchNode.create(precedingCaptureGroupTransitions) : null;
    }

    private CGTrackingDFAStateNode(CGTrackingDFAStateNode copy, short copyID) {
        super(copy, copyID);
        this.captureGroupTransitions = copy.captureGroupTransitions;
        this.precedingCaptureGroupTransitions = copy.precedingCaptureGroupTransitions;
        transitionDispatchNode = precedingCaptureGroupTransitions.length > 1 ? DFACaptureGroupPartialTransitionDispatchNode.create(precedingCaptureGroupTransitions) : null;
    }

    @Override
    public DFAStateNode createNodeSplitCopy(short copyID) {
        return new CGTrackingDFAStateNode(this, copyID);
    }

    @Override
    protected void beforeFindSuccessor(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
    }

    @Override
    protected void successorFound1(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int i) {
        CompilerAsserts.partialEvaluationConstant(this);
        CompilerAsserts.partialEvaluationConstant(i);
        if (precedingCaptureGroupTransitions.length == 1) {
            transitions[precedingCaptureGroupTransitions[0]].getPartialTransitions()[i].apply(d, inputIterator.getIndex(frame));
        } else {
            transitionDispatchNode.applyPartialTransition(transitions, inputIterator.getLastTransition(frame), i, d, inputIterator.getIndex(frame));
        }
        inputIterator.setLastTransition(frame, captureGroupTransitions[i]);
    }

    @Override
    protected int atEnd1(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
        int currentIndex = inputIterator.getIndex(frame) + 1;
        if (isAnchoredFinalState() && inputIterator.atEnd(frame)) {
            if (precedingCaptureGroupTransitions.length == 1) {
                transitions[precedingCaptureGroupTransitions[0]].getTransitionToAnchoredFinalState().apply(d, currentIndex);
            } else {
                transitionDispatchNode.applyAnchoredFinalTransition(transitions, inputIterator.getLastTransition(frame), d, currentIndex, true);
            }
        } else {
            assert isFinalState();
            if (precedingCaptureGroupTransitions.length == 1) {
                transitions[precedingCaptureGroupTransitions[0]].getTransitionToFinalState().apply(d, currentIndex);
            } else {
                transitionDispatchNode.applyFinalTransition(transitions, inputIterator.getLastTransition(frame), d, currentIndex, true);
            }
        }
        inputIterator.setResultObject(frame, d.results[d.currentResultOrder[DFACaptureGroupPartialTransitionNode.FINAL_STATE_RESULT_INDEX]]);
        return FS_RESULT_NO_SUCCESSOR;
    }

    @Override
    protected void successorFound2(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int i) {
        CompilerAsserts.partialEvaluationConstant(this);
        CompilerAsserts.partialEvaluationConstant(i);
        assert inputIterator.getLastTransition(frame) == captureGroupTransitions[loopToSelf];
        transitions[captureGroupTransitions[loopToSelf]].getPartialTransitions()[i].apply(d, inputIterator.getIndex(frame));
        inputIterator.setLastTransition(frame, captureGroupTransitions[i]);
    }

    @Override
    protected void noSuccessor2(VirtualFrame frame, InputIterator inputIterator) {
        throw new IllegalStateException();
    }

    @Override
    protected int atEnd2(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
        return atEndLoop(frame, inputIterator, transitions, d);
    }

    @Override
    protected void successorFound3(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int i, int preLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        CompilerAsserts.partialEvaluationConstant(i);
        applyLoopTransitions(transitions, d, preLoopIndex, inputIterator.getIndex(frame) - 1);
        transitions[captureGroupTransitions[loopToSelf]].getPartialTransitions()[i].apply(d, inputIterator.getIndex(frame));
        inputIterator.setLastTransition(frame, captureGroupTransitions[i]);
    }

    @Override
    protected void noSuccessor3(VirtualFrame frame, InputIterator inputIterator) {
        throw new IllegalStateException();
    }

    @Override
    protected int atEnd3(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int preLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        applyLoopTransitions(transitions, d, preLoopIndex, inputIterator.getIndex(frame));
        return atEndLoop(frame, inputIterator, transitions, d);
    }

    private void applyLoopTransitions(DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int preLoopIndex, int postLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        DFACaptureGroupPartialTransitionNode transition = transitions[captureGroupTransitions[loopToSelf]].getPartialTransitions()[loopToSelf];
        if (transition.doesReorderResults()) {
            for (int i = preLoopIndex; i <= postLoopIndex; i++) {
                transition.apply(d, i);
            }
        } else {
            transition.apply(d, postLoopIndex);
        }
    }

    private int atEndLoop(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
        assert inputIterator.getLastTransition(frame) == captureGroupTransitions[loopToSelf];
        int currentIndex = inputIterator.getIndex(frame) + 1;
        if (isAnchoredFinalState() && inputIterator.atEnd(frame)) {
            transitions[captureGroupTransitions[loopToSelf]].getTransitionToAnchoredFinalState().apply(d, currentIndex);
        } else {
            assert isFinalState();
            transitions[captureGroupTransitions[loopToSelf]].getTransitionToFinalState().apply(d, currentIndex);
        }
        inputIterator.setResultObject(frame, d.results[d.currentResultOrder[DFACaptureGroupPartialTransitionNode.FINAL_STATE_RESULT_INDEX]]);
        return FS_RESULT_NO_SUCCESSOR;
    }
}

/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.nodes.input.InputIterator;

public final class TRegexDFAExecutorNode extends Node {

    public static final int NO_MATCH = -2;
    @Child private InputIterator inputIterator;

    /**
     * Entry points in case we start matching at the beginning of the input string.
     */
    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] anchoredEntry;

    /**
     * Entry points in case we do not start matching at the beginning of the input string. If all
     * possible matches must start at the beginning of the input string, entry points may be -1.
     */
    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] unAnchoredEntry;

    @Children private final DFAStateNode[] states;

    @Children private final DFACaptureGroupLazyTransitionNode[] transitions;

    private final boolean trackCaptureGroups;
    private final int maxNumberOfNFAStates;
    private final int numberOfCaptureGroups;

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] anchoredInitCaptureGroupTransition;
    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] unAnchoredInitCaptureGroupTransition;

    public TRegexDFAExecutorNode(
                    InputIterator inputIterator,
                    short[] anchoredEntry,
                    short[] unAnchoredEntry,
                    DFAStateNode[] states,
                    DFACaptureGroupLazyTransitionNode[] transitions,
                    boolean trackCaptureGroups,
                    int maxNumberOfNFAStates,
                    int numberOfCaptureGroups,
                    short[] anchoredInitCaptureGroupTransition,
                    short[] unAnchoredInitCaptureGroupTransition) {
        this.transitions = transitions;
        assert anchoredEntry.length == unAnchoredEntry.length;
        this.inputIterator = inputIterator;
        this.anchoredEntry = anchoredEntry;
        this.unAnchoredEntry = unAnchoredEntry;
        this.states = states;
        this.trackCaptureGroups = trackCaptureGroups;
        this.maxNumberOfNFAStates = maxNumberOfNFAStates;
        this.numberOfCaptureGroups = numberOfCaptureGroups;
        this.anchoredInitCaptureGroupTransition = anchoredInitCaptureGroupTransition;
        this.unAnchoredInitCaptureGroupTransition = unAnchoredInitCaptureGroupTransition;
    }

    public int getPrefixLength() {
        return anchoredEntry.length - 1;
    }

    public boolean hasUnAnchoredEntry() {
        return unAnchoredEntry[0] != -1;
    }

    public int getNumberOfStates() {
        return states.length;
    }

    public int getNumberOfCaptureGroups() {
        return numberOfCaptureGroups;
    }

    /**
     * records position of the END of the match found, or -1 if no match exists.
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected void execute(final VirtualFrame frame) {
        CompilerAsserts.compilationConstant(states.length);
        CompilerAsserts.compilationConstant(anchoredEntry.length);
        CompilerAsserts.compilationConstant(unAnchoredEntry.length);
        if (!inputIterator.validArgs(frame)) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalArgumentException(String.format("Got illegal args! (fromIndex %d, initialIndex %d, maxIndex %d)",
                            inputIterator.getFromIndex(frame), inputIterator.getIndex(frame), inputIterator.getMaxIndex(frame)));
        }
        if (inputIterator.isBackward() && inputIterator.getFromIndex(frame) - 1 > inputIterator.getMaxIndex(frame)) {
            inputIterator.setCurMaxIndex(frame, inputIterator.getFromIndex(frame) - 1);
        } else {
            inputIterator.setCurMaxIndex(frame, inputIterator.getMaxIndex(frame));
        }
        final DFACaptureGroupTrackingData cgData;
        if (trackCaptureGroups) {
            cgData = new DFACaptureGroupTrackingData(maxNumberOfNFAStates, numberOfCaptureGroups);
            initResultOrder(cgData);
            inputIterator.setResultObject(frame, cgData.results[0]);
        } else {
            cgData = null;
            inputIterator.setResultInt(frame, TRegexDFAExecutorNode.NO_MATCH);
        }
        int ip = -1;
        outer: while (true) {
            if (ip == -1) {
                final int lookBehindOffset = trackCaptureGroups ? Math.max(0, Math.min(anchoredEntry.length - 1, inputIterator.getFromIndex(frame) - inputIterator.getIndex(frame)))
                                : inputIterator.rewindUpTo(frame, anchoredEntry.length - 1);
                if (inputIterator.atBegin(frame)) {
                    for (int i = 0; i < anchoredEntry.length; i++) {
                        if (i == lookBehindOffset) {
                            if (trackCaptureGroups) {
                                inputIterator.setLastTransition(frame, anchoredInitCaptureGroupTransition[i]);
                            }
                            ip = anchoredEntry[i];
                            continue outer;
                        }
                    }
                    throw new IllegalStateException();
                } else if (!hasUnAnchoredEntry()) {
                    break;
                } else {
                    for (int i = 0; i < unAnchoredEntry.length; i++) {
                        if (i == lookBehindOffset) {
                            if (trackCaptureGroups) {
                                inputIterator.setLastTransition(frame, unAnchoredInitCaptureGroupTransition[i]);
                            }
                            ip = unAnchoredEntry[i];
                            continue outer;
                        }
                    }
                    throw new IllegalStateException();
                }
            }
            CompilerAsserts.partialEvaluationConstant(ip);
            CompilerAsserts.partialEvaluationConstant(states[ip]);
            final DFAStateNode curState = states[ip];
            CompilerAsserts.partialEvaluationConstant(curState);
            final short[] successors = curState.getSuccessors();
            CompilerAsserts.partialEvaluationConstant(successors);
            final int successorIndex = curState.execute(frame, inputIterator, transitions, cgData);
            for (int i = 0; i < successors.length; i++) {
                if (i == successorIndex) {
                    ip = successors[i];
                    continue outer;
                }
            }
            break;
        }
    }

    @ExplodeLoop
    private void initResultOrder(DFACaptureGroupTrackingData cgData) {
        for (int i = 0; i < maxNumberOfNFAStates; i++) {
            cgData.currentResultOrder[i] = i;
        }
    }
}

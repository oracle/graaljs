/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

public final class DFACaptureGroupPartialTransitionDispatchNode extends Node {

    private static final int EXPLODE_THRESHOLD = 20;

    public static DFACaptureGroupPartialTransitionDispatchNode create(short[] precedingTransitions) {
        return new DFACaptureGroupPartialTransitionDispatchNode(precedingTransitions);
    }

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] precedingTransitions;

    private DFACaptureGroupPartialTransitionDispatchNode(short[] precedingTransitions) {
        this.precedingTransitions = precedingTransitions;
    }

    public void applyPartialTransition(VirtualFrame frame, TRegexDFAExecutorNode executor, short transitionIndex, int partialTransitionIndex, int currentIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (precedingTransitions.length > EXPLODE_THRESHOLD) {
            applyPartialTransitionBoundary(executor, executor.getCGData(frame), transitionIndex, partialTransitionIndex, currentIndex);
        } else {
            applyPartialTransitionExploded(frame, executor, transitionIndex, partialTransitionIndex, currentIndex);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyPartialTransitionBoundary(TRegexDFAExecutorNode executor, DFACaptureGroupTrackingData d, short transitionIndex, int partialTransitionIndex, int currentIndex) {
        executor.getCGTransitions()[transitionIndex].getPartialTransitions()[partialTransitionIndex].apply(d, currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyPartialTransitionExploded(VirtualFrame frame, TRegexDFAExecutorNode executor, short transitionIndex, int partialTransitionIndex, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (transitionIndex == possibleTransition) {
                final DFACaptureGroupPartialTransitionNode[] partialTransitions = executor.getCGTransitions()[possibleTransition].getPartialTransitions();
                for (int i = 0; i < partialTransitions.length; i++) {
                    CompilerAsserts.partialEvaluationConstant(i);
                    if (i == partialTransitionIndex) {
                        partialTransitions[i].apply(executor.getCGData(frame), currentIndex);
                        return;
                    }
                }
                throw new IllegalStateException();
            }
        }
        throw new IllegalStateException();
    }

    public void applyAnchoredFinalTransition(VirtualFrame frame, TRegexDFAExecutorNode executor, short transitionIndex, int currentIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (precedingTransitions.length > EXPLODE_THRESHOLD) {
            applyAnchoredFinalTransitionBoundary(executor, executor.getCGData(frame), transitionIndex, currentIndex);
        } else {
            applyAnchoredFinalTransitionExploded(frame, executor, transitionIndex, currentIndex);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyAnchoredFinalTransitionBoundary(TRegexDFAExecutorNode executor, DFACaptureGroupTrackingData d, short transitionIndex, int currentIndex) {
        executor.getCGTransitions()[transitionIndex].getTransitionToAnchoredFinalState().applyFinalStateTransition(d, executor.isSearching(), currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyAnchoredFinalTransitionExploded(VirtualFrame frame, TRegexDFAExecutorNode executor, short transitionIndex, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (transitionIndex == possibleTransition) {
                executor.getCGTransitions()[possibleTransition].getTransitionToAnchoredFinalState().applyFinalStateTransition(executor.getCGData(frame), executor.isSearching(), currentIndex);
                return;
            }
        }
        throw new IllegalStateException();
    }

    public void applyFinalTransition(VirtualFrame frame, TRegexDFAExecutorNode executor, short transitionIndex, int currentIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (precedingTransitions.length > EXPLODE_THRESHOLD) {
            applyFinalTransitionBoundary(executor, executor.getCGData(frame), transitionIndex, currentIndex);
        } else {
            applyFinalTransitionExploded(frame, executor, transitionIndex, currentIndex);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyFinalTransitionBoundary(TRegexDFAExecutorNode executor, DFACaptureGroupTrackingData d, short transitionIndex, int currentIndex) {
        executor.getCGTransitions()[transitionIndex].getTransitionToFinalState().applyFinalStateTransition(d, executor.isSearching(), currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyFinalTransitionExploded(VirtualFrame frame, TRegexDFAExecutorNode executorNode, short transitionIndex, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (transitionIndex == possibleTransition) {
                executorNode.getCGTransitions()[possibleTransition].getTransitionToFinalState().applyFinalStateTransition(executorNode.getCGData(frame), executorNode.isSearching(), currentIndex);
                return;
            }
        }
        throw new IllegalStateException();
    }
}

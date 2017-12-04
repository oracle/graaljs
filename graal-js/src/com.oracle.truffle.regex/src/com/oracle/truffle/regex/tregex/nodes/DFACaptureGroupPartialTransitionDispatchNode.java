/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
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

    public void applyPartialTransition(DFACaptureGroupLazyTransitionNode[] transitions, short t, int partialTransitionIndex, DFACaptureGroupTrackingData d, int currentIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (precedingTransitions.length > EXPLODE_THRESHOLD) {
            applyPartialTransitionBoundary(transitions, t, partialTransitionIndex, d, currentIndex);
        } else {
            applyPartialTransitionExploded(transitions, t, partialTransitionIndex, d, currentIndex);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyPartialTransitionBoundary(DFACaptureGroupLazyTransitionNode[] transitions, short t, int partialTransitionIndex, DFACaptureGroupTrackingData d, int currentIndex) {
        transitions[t].getPartialTransitions()[partialTransitionIndex].apply(d, currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyPartialTransitionExploded(DFACaptureGroupLazyTransitionNode[] transitions, short t, int partialTransitionIndex, DFACaptureGroupTrackingData d, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (t == possibleTransition) {
                final DFACaptureGroupPartialTransitionNode[] partialTransitions = transitions[possibleTransition].getPartialTransitions();
                for (int i = 0; i < partialTransitions.length; i++) {
                    CompilerAsserts.partialEvaluationConstant(i);
                    if (i == partialTransitionIndex) {
                        partialTransitions[i].apply(d, currentIndex);
                        return;
                    }
                }
                throw new IllegalStateException();
            }
        }
        throw new IllegalStateException();
    }

    public void applyAnchoredFinalTransition(DFACaptureGroupLazyTransitionNode[] transitions, short t, DFACaptureGroupTrackingData d, int currentIndex, boolean isAtEnd) {
        if (isAtEnd) {
            CompilerAsserts.partialEvaluationConstant(this);
            if (precedingTransitions.length > EXPLODE_THRESHOLD) {
                applyAnchoredFinalTransitionBoundary(transitions, t, d, currentIndex);
            } else {
                applyAnchoredFinalTransitionExploded(transitions, t, d, currentIndex);
            }
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyAnchoredFinalTransitionBoundary(DFACaptureGroupLazyTransitionNode[] transitions, short t, DFACaptureGroupTrackingData d, int currentIndex) {
        transitions[t].getTransitionToAnchoredFinalState().apply(d, currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyAnchoredFinalTransitionExploded(DFACaptureGroupLazyTransitionNode[] transitions, short t, DFACaptureGroupTrackingData d, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (t == possibleTransition) {
                transitions[possibleTransition].getTransitionToAnchoredFinalState().apply(d, currentIndex);
                return;
            }
        }
        throw new IllegalStateException();
    }

    public void applyFinalTransition(DFACaptureGroupLazyTransitionNode[] transitions, short t, DFACaptureGroupTrackingData d, int currentIndex, boolean isAtEnd) {
        if (isAtEnd) {
            CompilerAsserts.partialEvaluationConstant(this);
            if (precedingTransitions.length > EXPLODE_THRESHOLD) {
                applyFinalTransitionBoundary(transitions, t, d, currentIndex);
            } else {
                applyFinalTransitionExploded(transitions, t, d, currentIndex);
            }
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyFinalTransitionBoundary(DFACaptureGroupLazyTransitionNode[] transitions, short t, DFACaptureGroupTrackingData d, int currentIndex) {
        transitions[t].getTransitionToFinalState().apply(d, currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyFinalTransitionExploded(DFACaptureGroupLazyTransitionNode[] transitions, short t, DFACaptureGroupTrackingData d, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (t == possibleTransition) {
                transitions[possibleTransition].getTransitionToFinalState().apply(d, currentIndex);
                return;
            }
        }
        throw new IllegalStateException();
    }
}

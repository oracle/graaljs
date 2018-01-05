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

    public void applyPartialTransition(DFACaptureGroupLazyTransitionNode[] transitions, short t, int partialTransitionIndex,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (precedingTransitions.length > EXPLODE_THRESHOLD) {
            applyPartialTransitionBoundary(transitions, t, partialTransitionIndex, results, currentResultOrder, swap, currentIndex);
        } else {
            applyPartialTransitionExploded(transitions, t, partialTransitionIndex, results, currentResultOrder, swap, currentIndex);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyPartialTransitionBoundary(DFACaptureGroupLazyTransitionNode[] transitions, short t, int partialTransitionIndex,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex) {
        transitions[t].getPartialTransitions()[partialTransitionIndex].apply(results, currentResultOrder, swap, currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyPartialTransitionExploded(DFACaptureGroupLazyTransitionNode[] transitions, short t, int partialTransitionIndex,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (t == possibleTransition) {
                final DFACaptureGroupPartialTransitionNode[] partialTransitions = transitions[possibleTransition].getPartialTransitions();
                for (int i = 0; i < partialTransitions.length; i++) {
                    CompilerAsserts.partialEvaluationConstant(i);
                    if (i == partialTransitionIndex) {
                        partialTransitions[i].apply(results, currentResultOrder, swap, currentIndex);
                        return;
                    }
                }
                throw new IllegalStateException();
            }
        }
        throw new IllegalStateException();
    }

    public void applyAnchoredFinalTransition(DFACaptureGroupLazyTransitionNode[] transitions, short t,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex, boolean isAtEnd) {
        if (isAtEnd) {
            CompilerAsserts.partialEvaluationConstant(this);
            if (precedingTransitions.length > EXPLODE_THRESHOLD) {
                applyAnchoredFinalTransitionBoundary(transitions, t, results, currentResultOrder, swap, currentIndex);
            } else {
                applyAnchoredFinalTransitionExploded(transitions, t, results, currentResultOrder, swap, currentIndex);
            }
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyAnchoredFinalTransitionBoundary(DFACaptureGroupLazyTransitionNode[] transitions, short t,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex) {
        transitions[t].getTransitionToAnchoredFinalState().apply(results, currentResultOrder, swap, currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyAnchoredFinalTransitionExploded(DFACaptureGroupLazyTransitionNode[] transitions, short t,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (t == possibleTransition) {
                transitions[possibleTransition].getTransitionToAnchoredFinalState().apply(results, currentResultOrder, swap, currentIndex);
                return;
            }
        }
        throw new IllegalStateException();
    }

    public void applyFinalTransition(DFACaptureGroupLazyTransitionNode[] transitions, short t,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex, boolean isAtEnd) {
        if (isAtEnd) {
            CompilerAsserts.partialEvaluationConstant(this);
            if (precedingTransitions.length > EXPLODE_THRESHOLD) {
                applyFinalTransitionBoundary(transitions, t, results, currentResultOrder, swap, currentIndex);
            } else {
                applyFinalTransitionExploded(transitions, t, results, currentResultOrder, swap, currentIndex);
            }
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static void applyFinalTransitionBoundary(DFACaptureGroupLazyTransitionNode[] transitions, short t,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex) {
        transitions[t].getTransitionToFinalState().apply(results, currentResultOrder, swap, currentIndex);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private void applyFinalTransitionExploded(DFACaptureGroupLazyTransitionNode[] transitions, short t,
                    int[][] results, int[] currentResultOrder, int[] swap, int currentIndex) {
        for (short possibleTransition : precedingTransitions) {
            if (t == possibleTransition) {
                transitions[possibleTransition].getTransitionToFinalState().apply(results, currentResultOrder, swap, currentIndex);
                return;
            }
        }
        throw new IllegalStateException();
    }
}

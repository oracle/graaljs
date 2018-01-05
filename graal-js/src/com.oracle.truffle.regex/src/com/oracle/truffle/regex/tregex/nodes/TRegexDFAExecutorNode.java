/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;
import com.oracle.truffle.regex.tregex.matchers.SingleCharMatcher;
import com.oracle.truffle.regex.tregex.nodes.input.InputIterator;

public final class TRegexDFAExecutorNode extends Node {

    public static final int NO_MATCH = -2;
    @Child private InputIterator inputIterator;
    private final FrameSlot result;
    private final FrameSlot lastTransitionFS;

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
                    FrameSlot result,
                    FrameSlot lastTransitionFS,
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
        this.result = result;
        this.lastTransitionFS = lastTransitionFS;
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
        final int[][] results;
        final int[] currentResultOrder;
        final int[] swap;
        if (trackCaptureGroups) {
            results = new int[maxNumberOfNFAStates][numberOfCaptureGroups * 2];
            currentResultOrder = new int[maxNumberOfNFAStates];
            swap = new int[maxNumberOfNFAStates];
            initResultOrder(currentResultOrder);
            frame.setObject(result, results[0]);
        } else {
            results = null;
            currentResultOrder = null;
            swap = null;
            frame.setInt(result, NO_MATCH);
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
                                setLastTransition(frame, anchoredInitCaptureGroupTransition[i]);
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
                                setLastTransition(frame, unAnchoredInitCaptureGroupTransition[i]);
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
            final CharMatcher[] matchers = curState.getMatchers();
            CompilerAsserts.partialEvaluationConstant(matchers);
            final short[] cgTransitions;
            final short[] cgPrecedingTransitions;
            final DFACaptureGroupPartialTransitionDispatchNode transitionDispatchNode;
            if (trackCaptureGroups) {
                assert curState instanceof CGTrackingDFAStateNode;
                cgTransitions = ((CGTrackingDFAStateNode) curState).getCaptureGroupTransitions();
                cgPrecedingTransitions = ((CGTrackingDFAStateNode) curState).getPrecedingCaptureGroupTransitions();
                transitionDispatchNode = ((CGTrackingDFAStateNode) curState).getTransitionDispatchNode();
            } else {
                cgTransitions = null;
                cgPrecedingTransitions = null;
                transitionDispatchNode = null;
            }
            CompilerAsserts.partialEvaluationConstant(cgTransitions);
            CompilerAsserts.partialEvaluationConstant(cgPrecedingTransitions);
            CompilerAsserts.partialEvaluationConstant(transitionDispatchNode);
            final int partialUnrollCount = curState.isLoopToSelf() && !curState.isFindSingleChar() ? 4 : 1;
            int unroll = 0;
            innerUnroll: while (true) {
                final boolean hasNext = inputIterator.hasNext(frame);
                if (!trackCaptureGroups && curState.isFinalState()) {
                    // there are no intermediate results when tracking capture groups, since begin
                    // and end of the match are known
                    storeResult(frame, curState);
                }
                if (trackCaptureGroups) {
                    if (curState.isAnchoredFinalState() && inputIterator.atEnd(frame)) {
                        transitionDispatchNode.applyAnchoredFinalTransition(transitions, getLastTransition(frame),
                                        results, currentResultOrder, swap, inputIterator.getIndex(frame) + 1, !hasNext);
                    } else if (curState.isFinalState()) {
                        transitionDispatchNode.applyFinalTransition(transitions, getLastTransition(frame),
                                        results, currentResultOrder, swap, inputIterator.getIndex(frame) + 1, !hasNext);
                    }
                }
                final char c;
                if (hasNext) {
                    c = inputIterator.getChar(frame);
                } else {
                    if (trackCaptureGroups) {
                        assert curState.isFinalState() || curState.isAnchoredFinalState();
                        frame.setObject(result, results[currentResultOrder[DFACaptureGroupPartialTransitionNode.FINAL_STATE_RESULT_INDEX]]);
                        break outer;
                    } else if (curState.isAnchoredFinalState() && inputIterator.atEnd(frame)) {
                        storeResult(frame, curState);
                    }
                    break outer;
                }
                if (inputIterator.isBackward()) {
                    assert curState instanceof BackwardDFAStateNode;
                    final int backwardPrefixState = ((BackwardDFAStateNode) curState).getBackwardPrefixState();
                    if (curState.getId() != backwardPrefixState && inputIterator.isPastFromIndex(frame)) {
                        if (backwardPrefixState == -1) {
                            break outer;
                        } else {
                            ip = backwardPrefixState;
                            continue outer;
                        }
                    }
                }
                if (curState.isFindSingleChar()) {
                    final int initialIndex = inputIterator.getIndex(frame);
                    final boolean found = inputIterator.findChar(frame, ((SingleCharMatcher) curState.getMatchers()[0]).getChar());
                    if (trackCaptureGroups) {
                        inputIterator.advance(frame);
                        if (found) {
                            if (inputIterator.distanceTo(frame, initialIndex) > 1) {
                                transitionDispatchNode.applyPartialTransition(transitions, getLastTransition(frame), 1, results, currentResultOrder, swap, initialIndex + 1);
                                if (inputIterator.distanceTo(frame, initialIndex) > 2) {
                                    transitions[cgTransitions[1]].getPartialTransitions()[1].apply(results, currentResultOrder, swap, inputIterator.getIndex(frame) - 1);
                                }
                                transitions[cgTransitions[1]].getPartialTransitions()[0].apply(results, currentResultOrder, swap, inputIterator.getIndex(frame));
                            } else {
                                transitionDispatchNode.applyPartialTransition(transitions, getLastTransition(frame), 0, results, currentResultOrder, swap, inputIterator.getIndex(frame));
                            }
                            setLastTransition(frame, cgTransitions[0]);
                        } else {
                            if (inputIterator.distanceTo(frame, initialIndex) > 0) {
                                transitionDispatchNode.applyPartialTransition(transitions, getLastTransition(frame), 1, results, currentResultOrder, swap, initialIndex + 1);
                                if (inputIterator.distanceTo(frame, initialIndex) > 1) {
                                    transitions[cgTransitions[1]].getPartialTransitions()[1].apply(results, currentResultOrder, swap, inputIterator.getIndex(frame));
                                }
                                setLastTransition(frame, cgTransitions[1]);
                            }
                        }
                        if (found) {
                            ip = successors[0];
                        }
                        continue outer;
                    } else {
                        if (!found) {
                            inputIterator.advance(frame);
                        }
                        if (curState.isFinalState() || (curState.isAnchoredFinalState() && inputIterator.atEnd(frame))) {
                            storeResult(frame, curState);
                        }
                        if (found) {
                            inputIterator.advance(frame);
                        }
                        if (found) {
                            ip = successors[0];
                            continue outer;
                        } else {
                            break outer;
                        }
                    }
                }
                inputIterator.advance(frame);
                for (int i = 0; i < matchers.length; i++) {
                    final CharMatcher m = matchers[i];
                    if (m.match(c)) {
                        if (trackCaptureGroups) {
                            if (cgPrecedingTransitions.length == 1) {
                                CompilerAsserts.partialEvaluationConstant(cgPrecedingTransitions[0]);
                                transitions[cgPrecedingTransitions[0]].getPartialTransitions()[i].apply(results, currentResultOrder, swap, inputIterator.getIndex(frame));
                            } else {
                                transitionDispatchNode.applyPartialTransition(transitions, getLastTransition(frame), i, results, currentResultOrder, swap, inputIterator.getIndex(frame));
                            }
                            setLastTransition(frame, cgTransitions[i]);
                        }
                        if (curState.isLoopToSelf() && successors[i] == ip && unroll++ < partialUnrollCount && inputIterator.hasNext(frame)) {
                            continue innerUnroll;
                        } else {
                            ip = successors[i];
                            continue outer;
                        }
                    }
                }
                break outer;
            }
        }
    }

    @ExplodeLoop
    private void initResultOrder(int[] currentResultOrder) {
        for (int i = 0; i < maxNumberOfNFAStates; i++) {
            currentResultOrder[i] = i;
        }
    }

    private void setLastTransition(VirtualFrame frame, short lastTransition) {
        frame.setInt(lastTransitionFS, lastTransition);
    }

    private short getLastTransition(VirtualFrame frame) {
        return (short) FrameUtil.getIntSafe(frame, lastTransitionFS);
    }

    private void storeResult(VirtualFrame frame, DFAStateNode curState) {
        if (curState instanceof TraceFinderDFAStateNode) {
            TraceFinderDFAStateNode tfState = (TraceFinderDFAStateNode) curState;
            if (tfState.hasPreCalculatedAnchoredResult() && inputIterator.atEnd(frame)) {
                if (tfState.hasPreCalculatedUnAnchoredResult() && tfState.getPreCalculatedUnAnchoredResult() < tfState.getPreCalculatedAnchoredResult()) {
                    frame.setInt(result, tfState.getPreCalculatedUnAnchoredResult());
                } else {
                    frame.setInt(result, tfState.getPreCalculatedAnchoredResult());
                }
            } else {
                assert tfState.hasPreCalculatedUnAnchoredResult();
                frame.setInt(result, tfState.getPreCalculatedUnAnchoredResult());
            }
        } else {
            frame.setInt(result, inputIterator.getIndex(frame));
        }
    }
}

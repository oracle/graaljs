/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;
import com.oracle.truffle.regex.tregex.nodes.input.InputIterator;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

public class DFAStateNode extends Node {

    protected static final int FS_RESULT_NO_SUCCESSOR = -1;

    private static final byte FINAL_STATE_FLAG = 1;
    private static final byte ANCHORED_FINAL_STATE_FLAG = 1 << 1;
    private static final byte FIND_SINGLE_CHAR_FLAG = 1 << 2;

    private final short id;
    private final byte flags;
    protected final short loopToSelf;
    @CompilationFinal(dimensions = 1) private final short[] successors;
    @CompilationFinal(dimensions = 1) protected final CharMatcher[] matchers;

    public DFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean findSingleChar,
                    short loopToSelf,
                    short[] successors,
                    CharMatcher[] matchers) {
        this.id = id;
        byte newFlags = 0;
        if (finalState) {
            newFlags |= FINAL_STATE_FLAG;
        }
        if (anchoredFinalState) {
            newFlags |= ANCHORED_FINAL_STATE_FLAG;
        }
        if (findSingleChar) {
            newFlags |= FIND_SINGLE_CHAR_FLAG;
        }
        this.loopToSelf = loopToSelf;
        this.flags = newFlags;
        this.successors = successors;
        this.matchers = matchers;
    }

    protected DFAStateNode(DFAStateNode nodeSplitCopy, short copyID) {
        this(copyID, nodeSplitCopy.isFinalState(), nodeSplitCopy.isAnchoredFinalState(), nodeSplitCopy.isFindSingleChar(), nodeSplitCopy.loopToSelf,
                        Arrays.copyOf(nodeSplitCopy.getSuccessors(), nodeSplitCopy.getSuccessors().length), nodeSplitCopy.getMatchers());
    }

    /**
     * Creates a copy of this state node, where all attributes are copied shallowly, except for the
     * {@link #successors} array, which is deep-copied, and the node ID, which is replaced by the
     * parameter copyID.
     * 
     * @param copyID new ID for the copy.
     * @return an "almost shallow" copy of this node.
     */
    public DFAStateNode createNodeSplitCopy(short copyID) {
        return new DFAStateNode(this, copyID);
    }

    public short getId() {
        return id;
    }

    public short[] getSuccessors() {
        return successors;
    }

    public CharMatcher[] getMatchers() {
        return matchers;
    }

    public boolean isFinalState() {
        return flagIsSet(FINAL_STATE_FLAG);
    }

    public boolean isAnchoredFinalState() {
        return flagIsSet(ANCHORED_FINAL_STATE_FLAG);
    }

    public boolean isFindSingleChar() {
        return flagIsSet(FIND_SINGLE_CHAR_FLAG);
    }

    private boolean flagIsSet(byte flag) {
        return (flags & flag) != 0;
    }

    public boolean isLoopToSelf() {
        return loopToSelf != -1;
    }

    public int execute(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
        beforeFindSuccessor(frame, inputIterator, transitions, d);
        final int maxIndex = inputIterator.getCurMaxIndex(frame);
        if (!inputIterator.hasNext(frame, maxIndex)) {
            return atEnd1(frame, inputIterator, transitions, d);
        }
        int result = checkMatch1(frame, inputIterator, transitions, d);
        if (isLoopToSelf() && result == loopToSelf) {
            if (inputIterator.hasNext(frame, maxIndex)) {
                result = checkMatch2(frame, inputIterator, transitions, d);
                if (result == FS_RESULT_NO_SUCCESSOR) {
                    noSuccessor2(frame, inputIterator);
                    return FS_RESULT_NO_SUCCESSOR;
                }
                if (result != loopToSelf) {
                    return result;
                }
            } else {
                return atEnd2(frame, inputIterator, transitions, d);
            }
            final int preLoopIndex = inputIterator.getIndex(frame);
            while (inputIterator.hasNext(frame, maxIndex)) {
                result = checkMatch3(frame, inputIterator, transitions, d, preLoopIndex);
                if (result == FS_RESULT_NO_SUCCESSOR) {
                    noSuccessor3(frame, inputIterator);
                    return FS_RESULT_NO_SUCCESSOR;
                }
                if (result != loopToSelf) {
                    return result;
                }
            }
            return atEnd3(frame, inputIterator, transitions, d, preLoopIndex);
        }
        return result;
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private int checkMatch1(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        final char c = inputIterator.getChar(frame);
        inputIterator.advance(frame);
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].match(c)) {
                CompilerAsserts.partialEvaluationConstant(i);
                successorFound1(frame, inputIterator, transitions, d, i);
                return i;
            }
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private int checkMatch2(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        final char c = inputIterator.getChar(frame);
        inputIterator.advance(frame);
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].match(c)) {
                if (i != loopToSelf) {
                    CompilerAsserts.partialEvaluationConstant(i);
                    successorFound2(frame, inputIterator, transitions, d, i);
                }
                return i;
            }
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private int checkMatch3(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int preLoopIndex) {
        final char c = inputIterator.getChar(frame);
        inputIterator.advance(frame);
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].match(c)) {
                if (i != loopToSelf) {
                    CompilerAsserts.partialEvaluationConstant(i);
                    successorFound3(frame, inputIterator, transitions, d, i, preLoopIndex);
                }
                return i;
            }
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    protected void beforeFindSuccessor(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, inputIterator, curIndex(frame, inputIterator), false);
        }
    }

    protected void successorFound1(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int i) {
        CompilerAsserts.partialEvaluationConstant(this);
    }

    protected int atEnd1(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isAnchoredFinalState() && inputIterator.atEnd(frame)) {
            storeResult(frame, inputIterator, curIndex(frame, inputIterator), true);
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    protected void successorFound2(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int i) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, inputIterator, prevIndex(frame, inputIterator), false);
        }
    }

    protected void noSuccessor2(VirtualFrame frame, InputIterator inputIterator) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, inputIterator, prevIndex(frame, inputIterator), false);
        }
    }

    protected int atEnd2(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d) {
        CompilerAsserts.partialEvaluationConstant(this);
        boolean anchored = isAnchoredFinalState() && inputIterator.atEnd(frame);
        if (isFinalState() || anchored) {
            storeResult(frame, inputIterator, curIndex(frame, inputIterator), anchored);
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    protected void successorFound3(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int i, int preLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, inputIterator, prevIndex(frame, inputIterator), false);
        }
    }

    protected void noSuccessor3(VirtualFrame frame, InputIterator inputIterator) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, inputIterator, prevIndex(frame, inputIterator), false);
        }
    }

    protected int atEnd3(VirtualFrame frame, InputIterator inputIterator, DFACaptureGroupLazyTransitionNode[] transitions, DFACaptureGroupTrackingData d, int preLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        boolean anchored = isAnchoredFinalState() && inputIterator.atEnd(frame);
        if (isFinalState() || anchored) {
            storeResult(frame, inputIterator, curIndex(frame, inputIterator), anchored);
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    protected void storeResult(VirtualFrame frame, InputIterator inputIterator, int index, boolean anchored) {
        CompilerAsserts.partialEvaluationConstant(this);
        inputIterator.setResultInt(frame, index);
    }

    protected int curIndex(VirtualFrame frame, InputIterator inputIterator) {
        CompilerAsserts.partialEvaluationConstant(this);
        return inputIterator.getIndex(frame);
    }

    protected int prevIndex(VirtualFrame frame, InputIterator inputIterator) {
        CompilerAsserts.partialEvaluationConstant(this);
        return inputIterator.getIndex(frame) - 1;
    }

    @TruffleBoundary
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DebugUtil.appendNodeId(sb, id).append(": ").append(matchers.length).append(" successors");
        if (isAnchoredFinalState()) {
            sb.append(", AFS");
        }
        if (isFinalState()) {
            sb.append(", FS");
        }
        sb.append(":\n");
        for (int i = 0; i < matchers.length; i++) {
            sb.append("      - ").append(matchers[i]).append(" -> ");
            DebugUtil.appendNodeId(sb, getSuccessors()[i]).append("\n");
        }
        return sb.toString();
    }

    @TruffleBoundary
    protected DebugUtil.Table transitionToTable(int i) {
        return new DebugUtil.Table("Transition",
                        new DebugUtil.Value("matcher", matchers[i]),
                        new DebugUtil.Value("target", DebugUtil.nodeID(successors[i])));
    }

    @TruffleBoundary
    public DebugUtil.Table toTable() {
        DebugUtil.Table table = new DebugUtil.Table(DebugUtil.nodeID(id));
        if (isAnchoredFinalState()) {
            table.append(new DebugUtil.Value("anchoredFinalState", "true"));
        }
        if (isFinalState()) {
            table.append(new DebugUtil.Value("finalState", "true"));
        }
        if (isLoopToSelf()) {
            table.append(new DebugUtil.Value("loopToSelf", "true"));
        }
        for (int i = 0; i < matchers.length; i++) {
            table.append(transitionToTable(i));
        }
        return table;
    }
}

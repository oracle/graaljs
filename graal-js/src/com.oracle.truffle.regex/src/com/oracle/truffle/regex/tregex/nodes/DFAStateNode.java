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
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

public class DFAStateNode extends DFAAbstractStateNode {

    private static final byte FINAL_STATE_FLAG = 1;
    private static final byte ANCHORED_FINAL_STATE_FLAG = 1 << 1;
    private static final byte FIND_SINGLE_CHAR_FLAG = 1 << 2;

    private final short id;
    private final byte flags;
    protected final short loopToSelf;
    @CompilationFinal(dimensions = 1) protected final CharMatcher[] matchers;

    public DFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean findSingleChar,
                    short loopToSelf,
                    short[] successors,
                    CharMatcher[] matchers) {
        super(successors);
        assert id > 0;
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
        this.matchers = matchers;
    }

    protected DFAStateNode(DFAStateNode nodeSplitCopy, short copyID) {
        this(copyID, nodeSplitCopy.isFinalState(), nodeSplitCopy.isAnchoredFinalState(), nodeSplitCopy.isFindSingleChar(), nodeSplitCopy.loopToSelf,
                        Arrays.copyOf(nodeSplitCopy.getSuccessors(), nodeSplitCopy.getSuccessors().length), nodeSplitCopy.getMatchers());
    }

    @Override
    public DFAStateNode createNodeSplitCopy(short copyID) {
        return new DFAStateNode(this, copyID);
    }

    @Override
    public short getId() {
        return id;
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

    @Override
    public void execute(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        beforeFindSuccessor(frame, executor);
        if (!executor.hasNext(frame)) {
            executor.setSuccessorIndex(frame, atEnd1(frame, executor));
            return;
        }
        executor.setSuccessorIndex(frame, checkMatch1(frame, executor));
        if (isLoopToSelf() && executor.getSuccessorIndex(frame) == loopToSelf) {
            if (executor.hasNext(frame)) {
                executor.setSuccessorIndex(frame, checkMatch2(frame, executor));
                if (executor.getSuccessorIndex(frame) == FS_RESULT_NO_SUCCESSOR) {
                    noSuccessor2(frame, executor);
                    return;
                }
                if (executor.getSuccessorIndex(frame) != loopToSelf) {
                    return;
                }
            } else {
                executor.setSuccessorIndex(frame, atEnd2(frame, executor));
                return;
            }
            final int preLoopIndex = executor.getIndex(frame);
            while (executor.hasNext(frame)) {
                executor.setSuccessorIndex(frame, checkMatch3(frame, executor, preLoopIndex));
                if (executor.getSuccessorIndex(frame) == FS_RESULT_NO_SUCCESSOR) {
                    noSuccessor3(frame, executor, preLoopIndex);
                    return;
                }
                if (executor.getSuccessorIndex(frame) != loopToSelf) {
                    return;
                }
            }
            executor.setSuccessorIndex(frame, atEnd3(frame, executor, preLoopIndex));
        }
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private int checkMatch1(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        final char c = executor.getChar(frame);
        executor.advance(frame);
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].match(c)) {
                CompilerAsserts.partialEvaluationConstant(i);
                successorFound1(frame, executor, i);
                return i;
            }
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private int checkMatch2(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        final char c = executor.getChar(frame);
        executor.advance(frame);
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].match(c)) {
                if (i != loopToSelf) {
                    CompilerAsserts.partialEvaluationConstant(i);
                    successorFound2(frame, executor, i);
                }
                return i;
            }
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    private int checkMatch3(VirtualFrame frame, TRegexDFAExecutorNode executor, int preLoopIndex) {
        final char c = executor.getChar(frame);
        executor.advance(frame);
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].match(c)) {
                if (i != loopToSelf) {
                    CompilerAsserts.partialEvaluationConstant(i);
                    successorFound3(frame, executor, i, preLoopIndex);
                }
                return i;
            }
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    protected void beforeFindSuccessor(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, executor, curIndex(frame, executor), false);
        }
    }

    @SuppressWarnings("unused")
    protected void successorFound1(VirtualFrame frame, TRegexDFAExecutorNode executor, int i) {
        CompilerAsserts.partialEvaluationConstant(this);
    }

    protected int atEnd1(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isAnchoredFinalState() && executor.atEnd(frame)) {
            storeResult(frame, executor, curIndex(frame, executor), true);
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    @SuppressWarnings("unused")
    protected void successorFound2(VirtualFrame frame, TRegexDFAExecutorNode executor, int i) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, executor, prevIndex(frame, executor), false);
        }
    }

    protected void noSuccessor2(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, executor, prevIndex(frame, executor), false);
        }
    }

    protected int atEnd2(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        boolean anchored = isAnchoredFinalState() && executor.atEnd(frame);
        if (isFinalState() || anchored) {
            storeResult(frame, executor, curIndex(frame, executor), anchored);
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    @SuppressWarnings("unused")
    protected void successorFound3(VirtualFrame frame, TRegexDFAExecutorNode executor, int i, int preLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, executor, prevIndex(frame, executor), false);
        }
    }

    @SuppressWarnings("unused")
    protected void noSuccessor3(VirtualFrame frame, TRegexDFAExecutorNode executor, int preLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (isFinalState()) {
            storeResult(frame, executor, prevIndex(frame, executor), false);
        }
    }

    @SuppressWarnings("unused")
    protected int atEnd3(VirtualFrame frame, TRegexDFAExecutorNode executor, int preLoopIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        boolean anchored = isAnchoredFinalState() && executor.atEnd(frame);
        if (isFinalState() || anchored) {
            storeResult(frame, executor, curIndex(frame, executor), anchored);
        }
        return FS_RESULT_NO_SUCCESSOR;
    }

    @SuppressWarnings("unused")
    protected void storeResult(VirtualFrame frame, TRegexDFAExecutorNode executor, int index, boolean anchored) {
        CompilerAsserts.partialEvaluationConstant(this);
        executor.setResultInt(frame, index);
    }

    protected int curIndex(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        return executor.getIndex(frame);
    }

    protected int prevIndex(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        return executor.getIndex(frame) - 1;
    }

    protected int nextIndex(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        CompilerAsserts.partialEvaluationConstant(this);
        return executor.getIndex(frame) + 1;
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
    private DebugUtil.Table transitionToTable(int i) {
        return new DebugUtil.Table("Transition",
                        new DebugUtil.Value("matcher", matchers[i]),
                        new DebugUtil.Value("target", DebugUtil.nodeID(successors[i])));
    }

    @TruffleBoundary
    @Override
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

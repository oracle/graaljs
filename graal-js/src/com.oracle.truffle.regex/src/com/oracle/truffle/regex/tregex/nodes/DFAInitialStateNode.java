/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

/**
 * This state node is responsible for selecting a DFA's initial state based on the index the search
 * starts from. Successors are entry points in case we start matching at the beginning of the input
 * string, followed by entry points in case we do not start matching at the beginning of the input
 * string. If possible matches must start at the beginning of the input string, entry points may be
 * -1.
 */
public class DFAInitialStateNode extends DFAAbstractStateNode {

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] captureGroupTransitions;
    private final boolean searching;
    private final boolean trackCaptureGroups;

    public DFAInitialStateNode(short[] successors, short[] captureGroupTransitions, boolean searching, boolean trackCaptureGroups) {
        super(successors);
        this.captureGroupTransitions = captureGroupTransitions;
        this.searching = searching;
        this.trackCaptureGroups = trackCaptureGroups;
    }

    private DFAInitialStateNode(DFAInitialStateNode copy) {
        this(Arrays.copyOf(copy.successors, copy.successors.length), copy.captureGroupTransitions, copy.searching, copy.trackCaptureGroups);
    }

    public int getPrefixLength() {
        return (successors.length / 2) - 1;
    }

    public boolean hasUnAnchoredEntry() {
        return successors[successors.length / 2] != -1;
    }

    /**
     * Creates a node split copy of this initial state as described in {@link DFAAbstractStateNode},
     * but ignores copyID, since having two initial states in a DFA is not supported. Therefore,
     * this method should be used for replacing the original initial state with the copy.
     * 
     * @param copyID new ID for the copy.
     * @return a node split copy of this initial state as described in {@link DFAAbstractStateNode},
     *         ignoring copyID.
     */
    @Override
    public DFAAbstractStateNode createNodeSplitCopy(short copyID) {
        return new DFAInitialStateNode(this);
    }

    @Override
    public short getId() {
        return 0;
    }

    @Override
    public void execute(VirtualFrame frame, TRegexDFAExecutorNode executor) {
        if (searching) {
            executor.setSuccessorIndex(frame, executor.rewindUpTo(frame, getPrefixLength()));
        } else {
            executor.setSuccessorIndex(frame, Math.max(0, Math.min(getPrefixLength(), executor.getFromIndex(frame) - executor.getIndex(frame))));
        }
        if (!executor.atBegin(frame)) {
            executor.setSuccessorIndex(frame, executor.getSuccessorIndex(frame) + (successors.length / 2));
        }
        if (trackCaptureGroups) {
            executor.setLastTransition(frame, captureGroupTransitions[executor.getSuccessorIndex(frame)]);
        }
    }

    @Override
    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("InitialState");
    }
}

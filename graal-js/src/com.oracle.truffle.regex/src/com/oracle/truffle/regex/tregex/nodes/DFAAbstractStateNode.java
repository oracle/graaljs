/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.nodesplitter.DFANodeSplit;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public abstract class DFAAbstractStateNode extends Node {

    static final int FS_RESULT_NO_SUCCESSOR = -1;

    @CompilerDirectives.CompilationFinal(dimensions = 1) protected final short[] successors;

    DFAAbstractStateNode(short[] successors) {
        this.successors = successors;
    }

    /**
     * Creates a copy of this state node, where all attributes are copied shallowly, except for the
     * {@link #successors} array, which is deep-copied, and the node ID, which is replaced by the
     * parameter copyID. Used by {@link DFANodeSplit}.
     *
     * @param copyID new ID for the copy.
     * @return an "almost shallow" copy of this node.
     */
    public abstract DFAAbstractStateNode createNodeSplitCopy(short copyID);

    public abstract short getId();

    public short[] getSuccessors() {
        return successors;
    }

    /**
     * Calculates this state's successor and returns its ID ({@link DFAStateNode#getId()}) via
     * {@link TRegexDFAExecutorNode#setSuccessorIndex(VirtualFrame, int)}. This return value is
     * called "successor index" and may either be an index of the successors array (between 0 and
     * <code>{@link #getSuccessors()}.length</code>) or {@link #FS_RESULT_NO_SUCCESSOR}.
     * 
     * @param frame a virtual frame as described by {@link TRegexDFAExecutorProperties}.
     * @param executor this node's parent {@link TRegexDFAExecutorNode}.
     */
    public abstract void executeFindSuccessor(VirtualFrame frame, TRegexDFAExecutorNode executor);

    public abstract DebugUtil.Table toTable();
}

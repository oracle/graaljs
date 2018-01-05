/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

public class DFAStateNode extends Node {

    private static final byte FINAL_STATE_FLAG = 1;
    private static final byte ANCHORED_FINAL_STATE_FLAG = 1 << 1;
    private static final byte LOOP_TO_SELF_FLAG = 1 << 2;
    private static final byte FIND_SINGLE_CHAR_FLAG = 1 << 3;

    private final short id;
    private final byte flags;
    @CompilationFinal(dimensions = 1) private final short[] successors;
    @CompilationFinal(dimensions = 1) private final CharMatcher[] matchers;

    public DFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean loopToSelf,
                    boolean findSingleChar,
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
        if (loopToSelf) {
            newFlags |= LOOP_TO_SELF_FLAG;
        }
        if (findSingleChar) {
            newFlags |= FIND_SINGLE_CHAR_FLAG;
        }
        this.flags = newFlags;
        this.successors = successors;
        this.matchers = matchers;
    }

    protected DFAStateNode(DFAStateNode nodeSplitCopy, short copyID) {
        this(copyID, nodeSplitCopy.isFinalState(), nodeSplitCopy.isAnchoredFinalState(), nodeSplitCopy.isLoopToSelf(), nodeSplitCopy.isFindSingleChar(),
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

    public boolean isLoopToSelf() {
        return flagIsSet(LOOP_TO_SELF_FLAG);
    }

    public boolean isFindSingleChar() {
        return flagIsSet(FIND_SINGLE_CHAR_FLAG);
    }

    private boolean flagIsSet(byte flag) {
        return (flags & flag) != 0;
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

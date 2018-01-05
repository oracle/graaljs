/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public final class DFACaptureGroupLazyTransitionNode extends Node {

    private final short id;
    @Children private final DFACaptureGroupPartialTransitionNode[] partialTransitions;
    private final DFACaptureGroupPartialTransitionNode transitionToFinalState;
    private final DFACaptureGroupPartialTransitionNode transitionToAnchoredFinalState;

    public DFACaptureGroupLazyTransitionNode(short id,
                    DFACaptureGroupPartialTransitionNode[] partialTransitions,
                    DFACaptureGroupPartialTransitionNode transitionToFinalState,
                    DFACaptureGroupPartialTransitionNode transitionToAnchoredFinalState) {
        this.id = id;
        this.partialTransitions = partialTransitions;
        this.transitionToFinalState = transitionToFinalState;
        this.transitionToAnchoredFinalState = transitionToAnchoredFinalState;
    }

    public short getId() {
        return id;
    }

    public DFACaptureGroupPartialTransitionNode[] getPartialTransitions() {
        return partialTransitions;
    }

    public DFACaptureGroupPartialTransitionNode getTransitionToFinalState() {
        return transitionToFinalState;
    }

    public DFACaptureGroupPartialTransitionNode getTransitionToAnchoredFinalState() {
        return transitionToAnchoredFinalState;
    }

    public DebugUtil.Table toTable() {
        return toTable("DFACaptureGroupLazyTransition");
    }

    public DebugUtil.Table toTable(String name) {
        DebugUtil.Table table = new DebugUtil.Table(name);
        for (int i = 0; i < partialTransitions.length; i++) {
            table.append(partialTransitions[i].toTable("Transition " + i));
        }
        if (transitionToAnchoredFinalState != null) {
            table.append(transitionToAnchoredFinalState.toTable("TransitionAF"));
        }
        if (transitionToFinalState != null) {
            table.append(transitionToFinalState.toTable("TransitionF"));
        }
        return table;
    }
}

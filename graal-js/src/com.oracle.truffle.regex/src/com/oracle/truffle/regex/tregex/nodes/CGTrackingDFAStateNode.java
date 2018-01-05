/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.matchers.CharMatcher;

public class CGTrackingDFAStateNode extends DFAStateNode {

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] captureGroupTransitions;

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final short[] precedingCaptureGroupTransitions;

    @Child private DFACaptureGroupPartialTransitionDispatchNode transitionDispatchNode;

    public CGTrackingDFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean loopToSelf,
                    boolean findSingleChar,
                    short[] successors,
                    CharMatcher[] matchers,
                    short[] captureGroupTransitions,
                    short[] precedingCaptureGroupTransitions) {
        super(id, finalState, anchoredFinalState, loopToSelf, findSingleChar, successors, matchers);
        this.captureGroupTransitions = captureGroupTransitions;
        this.precedingCaptureGroupTransitions = precedingCaptureGroupTransitions;
        transitionDispatchNode = DFACaptureGroupPartialTransitionDispatchNode.create(precedingCaptureGroupTransitions);
    }

    private CGTrackingDFAStateNode(CGTrackingDFAStateNode copy, short copyID) {
        super(copy, copyID);
        this.captureGroupTransitions = copy.captureGroupTransitions;
        this.precedingCaptureGroupTransitions = copy.precedingCaptureGroupTransitions;
        transitionDispatchNode = DFACaptureGroupPartialTransitionDispatchNode.create(precedingCaptureGroupTransitions);
    }

    @Override
    public DFAStateNode createNodeSplitCopy(short copyID) {
        return new CGTrackingDFAStateNode(this, copyID);
    }

    public short[] getCaptureGroupTransitions() {
        return captureGroupTransitions;
    }

    public short[] getPrecedingCaptureGroupTransitions() {
        return precedingCaptureGroupTransitions;
    }

    public DFACaptureGroupPartialTransitionDispatchNode getTransitionDispatchNode() {
        return transitionDispatchNode;
    }
}

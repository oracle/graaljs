/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.regex.tregex.matchers.CharMatcher;

public class BackwardDFAStateNode extends DFAStateNode {

    private final short backwardPrefixState;

    public BackwardDFAStateNode(short id,
                    boolean finalState,
                    boolean anchoredFinalState,
                    boolean loopToSelf,
                    boolean findSingleChar,
                    short[] successors,
                    CharMatcher[] matchers,
                    short backwardPrefixState) {
        super(id, finalState, anchoredFinalState, loopToSelf, findSingleChar, successors, matchers);
        this.backwardPrefixState = backwardPrefixState;
    }

    protected BackwardDFAStateNode(BackwardDFAStateNode copy, short copyID) {
        super(copy, copyID);
        this.backwardPrefixState = copy.backwardPrefixState;
    }

    @Override
    public DFAStateNode createNodeSplitCopy(short copyID) {
        return new BackwardDFAStateNode(this, copyID);
    }

    public int getBackwardPrefixState() {
        return backwardPrefixState;
    }
}

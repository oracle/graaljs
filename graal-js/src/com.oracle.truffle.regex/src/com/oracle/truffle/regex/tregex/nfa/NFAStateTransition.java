/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * Provides information about a transition from one NFAState to another state.
 */
public class NFAStateTransition {

    private final short id;
    private final NFAState source;
    private final NFAState target;
    private final GroupBoundaries groupBoundaries;

    public NFAStateTransition(short id, NFAState source, NFAState target, GroupBoundaries groupBoundaries) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.groupBoundaries = groupBoundaries;
    }

    public short getId() {
        return id;
    }

    public NFAState getSource() {
        return source;
    }

    public NFAState getTarget() {
        return target;
    }

    public NFAState getTarget(boolean forward) {
        return forward ? target : source;
    }

    public NFAState getSource(boolean forward) {
        return forward ? source : target;
    }

    /**
     * groups entered and exited by this transition.
     */
    public GroupBoundaries getGroupBoundaries() {
        return groupBoundaries;
    }

    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("NFATransition",
                        new DebugUtil.Value("target", target.idToString()),
                        groupBoundaries.toTable());
    }
}

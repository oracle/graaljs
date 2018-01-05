/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * {@link MatchFound} nodes are {@link RegexASTNode}s that represent the initial/final states of the
 * non-deterministic finite state automaton generated from the regular expression.
 * <p>
 * Regular expressions are translated into non-deterministic finite state automata, with each
 * {@link RegexASTNode} in the {@link RegexAST} contributing some of the states or transitions. The
 * {@link MatchFound} nodes are those that contribute the final (accepting) states. The root group
 * of every regular expression is linked (using the 'next' pointer) to a single {@link MatchFound}
 * node. Other {@link MatchFound} nodes appear in look-behind and look-ahead assertions, where they
 * contribute the final states of their subautomata (look-around assertions generate subautomata
 * which are then joined with the root automaton using a product construction).
 * <p>
 * {@link MatchFound} nodes are also used as initial states (the initial states of the forward
 * search automaton are the final states of the reverse search automaton). Therefore, there is a set
 * of {@link MatchFound} nodes used as final states in forward search (reachable by 'next' pointers)
 * and as initial states in reverse search and a set of {@link MatchFound} nodes used as final
 * states in reverse search (reachable by 'prev' pointers) and as initial states in forward search.
 * {@link MatchFound} being used as NFA initial states is also why they can have a next-pointer (
 * {@link #getNext()}) themselves (see {@link RegexAST#getNFAUnAnchoredInitialState(int)}).
 */
public class MatchFound extends Term {

    private RegexASTNode next;

    @Override
    public MatchFound copy(RegexAST ast) {
        throw new UnsupportedOperationException();
    }

    public RegexASTNode getNext() {
        return next;
    }

    public void setNext(RegexASTNode next) {
        this.next = next;
    }

    @Override
    public String toString() {
        return "::";
    }

    @Override
    public DebugUtil.Table toTable() {
        return toTable("MatchFound");
    }
}

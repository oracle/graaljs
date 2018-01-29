/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.parser.ast.LookBehindAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NFAMatcherState extends NFAState {

    private final MatcherBuilder matcherBuilder;
    private final Set<LookBehindAssertion> finishedLookBehinds;

    public NFAMatcherState(short id,
                    ASTNodeSet<? extends RegexASTNode> stateSet,
                    MatcherBuilder matcherBuilder,
                    Set<LookBehindAssertion> finishedLookBehinds,
                    boolean hasPrefixStates) {
        this(id, stateSet, new ArrayList<>(), new ArrayList<>(), null, matcherBuilder, finishedLookBehinds, hasPrefixStates);
    }

    private NFAMatcherState(
                    short id,
                    ASTNodeSet<? extends RegexASTNode> stateSet,
                    List<NFAStateTransition> next,
                    List<NFAStateTransition> prev,
                    List<Integer> possibleResults,
                    MatcherBuilder matcherBuilder,
                    Set<LookBehindAssertion> finishedLookBehinds,
                    boolean hasPrefixStates) {
        super(id, stateSet, next, prev, possibleResults);
        this.matcherBuilder = matcherBuilder;
        this.finishedLookBehinds = finishedLookBehinds;
        setHasPrefixStates(hasPrefixStates);
    }

    public MatcherBuilder getMatcherBuilder() {
        return matcherBuilder;
    }

    public Set<LookBehindAssertion> getFinishedLookBehinds() {
        return finishedLookBehinds;
    }

    @Override
    public String toString() {
        return matcherBuilder.toString();
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        return toTable("NFAMatcherState").append(new DebugUtil.Value("matcherBuilder", toString()));
    }
}

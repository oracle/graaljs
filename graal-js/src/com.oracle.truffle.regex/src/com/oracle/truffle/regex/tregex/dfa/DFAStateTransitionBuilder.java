/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.dfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.automaton.TransitionBuilder;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.nfa.NFA;
import com.oracle.truffle.regex.tregex.nfa.NFAStateTransition;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.List;

public class DFAStateTransitionBuilder extends TransitionBuilder<NFATransitionSet> {

    private final NFATransitionSet transitions;
    private MatcherBuilder matcherBuilder;

    DFAStateTransitionBuilder(MatcherBuilder matcherBuilder, List<NFAStateTransition> transitions, NFA nfa, boolean forward, boolean prioritySensitive) {
        this.transitions = NFATransitionSet.create(nfa, forward, prioritySensitive, transitions);
        this.matcherBuilder = matcherBuilder;
    }

    DFAStateTransitionBuilder(MatcherBuilder matcherBuilder, NFATransitionSet transitions) {
        this.transitions = transitions;
        this.matcherBuilder = matcherBuilder;
    }

    @Override
    public MatcherBuilder getMatcherBuilder() {
        return matcherBuilder;
    }

    @Override
    public void setMatcherBuilder(MatcherBuilder matcherBuilder) {
        this.matcherBuilder = matcherBuilder;
    }

    @Override
    public DFAStateTransitionBuilder createMerged(TransitionBuilder<NFATransitionSet> other, MatcherBuilder mergedMatcher) {
        return new DFAStateTransitionBuilder(mergedMatcher, transitions.createMerged(other.getTargetState()));
    }

    @Override
    public void mergeInPlace(TransitionBuilder<NFATransitionSet> other, MatcherBuilder mergedMatcher) {
        transitions.addAll(other.getTargetState());
        matcherBuilder = mergedMatcher;
    }

    @Override
    public NFATransitionSet getTargetState() {
        return transitions;
    }

    @Override
    public String toString() {
        return toTable("DFAStateConnectionBuilder").toString();
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable(String name) {
        return new DebugUtil.Table(name,
                        new DebugUtil.Value("matcherBuilder", getMatcherBuilder()),
                        new DebugUtil.Value("transitions", getTargetState()));
    }
}

/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.regex.tregex.automaton.TransitionBuilder;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class ASTTransitionSetBuilder extends TransitionBuilder<ASTTransitionSet> {

    private final ASTTransitionSet transitionSet;
    private MatcherBuilder matcherBuilder;

    public ASTTransitionSetBuilder(ASTTransitionSet transitionSet, MatcherBuilder matcherBuilder) {
        this.transitionSet = transitionSet;
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
    public ASTTransitionSet getTargetState() {
        return transitionSet;
    }

    @Override
    public ASTTransitionSetBuilder createMerged(TransitionBuilder<ASTTransitionSet> other, MatcherBuilder mergedMatcher) {
        return new ASTTransitionSetBuilder(transitionSet.createMerged(other.getTargetState()), mergedMatcher);
    }

    @Override
    public void mergeInPlace(TransitionBuilder<ASTTransitionSet> other, MatcherBuilder mergedMatcher) {
        transitionSet.mergeInPlace(other);
        matcherBuilder = mergedMatcher;
    }

    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("ASTTransitionSetBuilder",
                        new DebugUtil.Value("matcherBuilder", matcherBuilder),
                        transitionSet.toTable());
    }
}

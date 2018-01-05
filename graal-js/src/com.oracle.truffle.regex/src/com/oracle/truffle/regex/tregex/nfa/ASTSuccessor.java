/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.buffer.CompilationBuffer;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.parser.ast.CharacterClass;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class ASTSuccessor {

    private ArrayList<ASTTransitionSetBuilder> mergedStates = new ArrayList<>();
    private boolean lookAroundsMerged = false;
    private List<ASTStep> lookAheads = Collections.emptyList();
    private List<ASTStep> lookBehinds = Collections.emptyList();

    private final CompilationBuffer compilationBuffer;

    ASTSuccessor(CompilationBuffer compilationBuffer) {
        this.compilationBuffer = compilationBuffer;
    }

    ASTSuccessor(CompilationBuffer compilationBuffer, ASTTransition initialTransition) {
        this.compilationBuffer = compilationBuffer;
        addInitialTransition(initialTransition);
    }

    public void addInitialTransition(ASTTransition transition) {
        MatcherBuilder matcherBuilder = MatcherBuilder.createFull();
        if (transition.getTarget() instanceof CharacterClass) {
            matcherBuilder = ((CharacterClass) transition.getTarget()).getMatcherBuilder();
        }
        mergedStates.add(new ASTTransitionSetBuilder(new ASTTransitionSet(transition), matcherBuilder));
    }

    public void setLookAheads(ArrayList<ASTStep> lookAheads) {
        this.lookAheads = lookAheads;
    }

    public void setLookBehinds(ArrayList<ASTStep> lookBehinds) {
        this.lookBehinds = lookBehinds;
    }

    public void addLookBehinds(Collection<ASTStep> addLookBehinds) {
        if (lookBehinds.isEmpty()) {
            lookBehinds = new ArrayList<>();
        }
        lookBehinds.addAll(addLookBehinds);
    }

    public ArrayList<ASTTransitionSetBuilder> getMergedStates(ASTTransitionCanonicalizer canonicalizer) {
        if (!lookAroundsMerged) {
            mergeLookArounds(canonicalizer);
            lookAroundsMerged = true;
        }
        return mergedStates;
    }

    private void mergeLookArounds(ASTTransitionCanonicalizer canonicalizer) {
        assert mergedStates.size() == 1;
        ASTTransitionSetBuilder successor = mergedStates.get(0);
        for (ASTStep lookBehind : lookBehinds) {
            addAllIntersecting(canonicalizer, successor, lookBehind, mergedStates);
        }
        ASTTransitionSetBuilder[] mergedLookBehinds = canonicalizer.run(mergedStates, compilationBuffer);
        mergedStates.clear();
        Collections.addAll(mergedStates, mergedLookBehinds);
        ArrayList<ASTTransitionSetBuilder> newMergedStates = new ArrayList<>();
        for (ASTStep lookAhead : lookAheads) {
            for (ASTTransitionSetBuilder state : mergedStates) {
                addAllIntersecting(canonicalizer, state, lookAhead, newMergedStates);
            }
            ArrayList<ASTTransitionSetBuilder> tmp = mergedStates;
            mergedStates = newMergedStates;
            newMergedStates = tmp;
            newMergedStates.clear();
        }
    }

    private void addAllIntersecting(ASTTransitionCanonicalizer canonicalizer, ASTTransitionSetBuilder state, ASTStep lookAround, ArrayList<ASTTransitionSetBuilder> result) {
        for (ASTSuccessor successor : lookAround.getSuccessors()) {
            for (ASTTransitionSetBuilder lookAroundState : successor.getMergedStates(canonicalizer)) {
                MatcherBuilder intersection = state.getMatcherBuilder().createIntersectionMatcher(lookAroundState.getMatcherBuilder(), compilationBuffer);
                if (intersection.matchesSomething()) {
                    result.add(state.createMerged(lookAroundState, intersection));
                }
            }
        }
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        DebugUtil.Table table = new DebugUtil.Table("ASTStepSuccessor",
                        new DebugUtil.Value("lookAheads", lookAheads.stream().map(x -> String.valueOf(x.getRoot().toStringWithID())).collect(
                                        Collectors.joining(", ", "[", "]"))),
                        new DebugUtil.Value("lookBehinds", lookBehinds.stream().map(x -> String.valueOf(x.getRoot().toStringWithID())).collect(
                                        Collectors.joining(", ", "[", "]"))));
        for (ASTTransitionSetBuilder mergeBuilder : mergedStates) {
            table.append(mergeBuilder.toTable());
        }
        return table;
    }
}

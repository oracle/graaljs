/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.automaton;

import com.oracle.truffle.regex.tregex.buffer.CompilationBuffer;
import com.oracle.truffle.regex.tregex.buffer.ObjectBuffer;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StateTransitionCanonicalizer<SS, TB extends TransitionBuilder<SS>> {

    private final MatcherBuilder[] intersectionResult = new MatcherBuilder[3];
    private final Map<SS, TB> mergeSameTargetsMap = new HashMap<>();

    public TB[] run(List<TB> transitions, CompilationBuffer compilationBuffer) {
        ObjectBuffer disjointTransitions = calcDisjointTransitions(transitions, compilationBuffer);
        return mergeSameTargets(compilationBuffer, disjointTransitions);
    }

    @SuppressWarnings("unchecked")
    private ObjectBuffer calcDisjointTransitions(List<TB> transitions, CompilationBuffer compilationBuffer) {
        ObjectBuffer disjointTransitions = compilationBuffer.getObjectBuffer1();
        for (TB t : transitions) {
            for (int i = 0; i < disjointTransitions.size(); i++) {
                TB dt = (TB) disjointTransitions.get(i);
                dt.getMatcherBuilder().intersectAndSubtract(t.getMatcherBuilder(), compilationBuffer, intersectionResult);
                MatcherBuilder dtSubtractedMatcher = intersectionResult[0];
                MatcherBuilder tSubtractedMatcher = intersectionResult[1];
                MatcherBuilder intersection = intersectionResult[2];
                if (intersection.matchesSomething()) {
                    if (dtSubtractedMatcher.matchesNothing()) {
                        dt.mergeInPlace(t, intersection);
                    } else {
                        dt.setMatcherBuilder(dtSubtractedMatcher);
                        disjointTransitions.add(dt.createMerged(t, intersection));
                    }
                    t.setMatcherBuilder(tSubtractedMatcher);
                    if (tSubtractedMatcher.matchesNothing()) {
                        break;
                    }
                }
            }
            if (t.getMatcherBuilder().matchesSomething()) {
                disjointTransitions.add(t);
            }
        }
        return disjointTransitions;
    }

    @SuppressWarnings("unchecked")
    private TB[] mergeSameTargets(CompilationBuffer compilationBuffer, ObjectBuffer disjointTransitions) {
        int resultSize = 0;
        for (Object o : disjointTransitions) {
            TB tb = (TB) o;
            if (tb.getMatcherBuilder().matchesNothing()) {
                continue;
            }
            TB existingTransitions = mergeSameTargetsMap.get(tb.getTargetState());
            if (existingTransitions == null) {
                mergeSameTargetsMap.put(tb.getTargetState(), tb);
                resultSize++;
            } else {
                boolean merged = false;
                TB mergeCandidate = existingTransitions;
                do {
                    if (isSameTargetMergeAllowed(tb, mergeCandidate)) {
                        mergeCandidate.setMatcherBuilder(mergeCandidate.getMatcherBuilder().union(tb.getMatcherBuilder(), compilationBuffer));
                        merged = true;
                        break;
                    }
                    mergeCandidate = (TB) mergeCandidate.getNext();
                } while (mergeCandidate != null);
                if (!merged) {
                    tb.setNext(existingTransitions);
                    mergeSameTargetsMap.put(tb.getTargetState(), tb);
                    resultSize++;
                }
            }
        }
        TB[] resultArray = createResultArray(resultSize);
        int i = 0;
        for (TB list : mergeSameTargetsMap.values()) {
            TB tb = list;
            do {
                resultArray[i++] = tb;
                tb = (TB) tb.getNext();
            } while (tb != null);
        }
        mergeSameTargetsMap.clear();
        return resultArray;
    }

    protected abstract boolean isSameTargetMergeAllowed(TB a, TB b);

    protected abstract TB[] createResultArray(int size);
}

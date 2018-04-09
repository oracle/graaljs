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
package com.oracle.truffle.regex.tregex.automaton;

import com.oracle.truffle.regex.tregex.buffer.CompilationBuffer;
import com.oracle.truffle.regex.tregex.buffer.ObjectArrayBuffer;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import org.graalvm.collections.EconomicMap;

import java.util.List;

public abstract class StateTransitionCanonicalizer<SS, TB extends TransitionBuilder<SS>> {

    private final MatcherBuilder[] intersectionResult = new MatcherBuilder[3];
    private final EconomicMap<SS, TB> mergeSameTargetsMap = EconomicMap.create();

    public TB[] run(List<TB> transitions, CompilationBuffer compilationBuffer) {
        ObjectArrayBuffer disjointTransitions = calcDisjointTransitions(transitions, compilationBuffer);
        return mergeSameTargets(compilationBuffer, disjointTransitions);
    }

    @SuppressWarnings("unchecked")
    private ObjectArrayBuffer calcDisjointTransitions(List<TB> transitions, CompilationBuffer compilationBuffer) {
        ObjectArrayBuffer disjointTransitions = compilationBuffer.getObjectBuffer1();
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
    private TB[] mergeSameTargets(CompilationBuffer compilationBuffer, ObjectArrayBuffer disjointTransitions) {
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
        for (TB list : mergeSameTargetsMap.getValues()) {
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

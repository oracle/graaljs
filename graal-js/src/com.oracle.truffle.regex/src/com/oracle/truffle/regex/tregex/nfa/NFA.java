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

import com.oracle.truffle.regex.result.PreCalculatedResultFactory;
import com.oracle.truffle.regex.tregex.automaton.StateIndex;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.parser.Counter;
import com.oracle.truffle.regex.tregex.parser.ast.CharacterClass;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NFA implements StateIndex<NFAState> {

    private final RegexAST ast;
    private final List<NFAAnchoredFinalState> anchoredEntry;
    private final List<NFAFinalState> unAnchoredEntry;
    private final NFAAnchoredFinalState reverseAnchoredEntry;
    private final NFAFinalState reverseUnAnchoredEntry;
    private final NFAState[] states;
    private final NFAStateTransition[] transitions;
    private final Counter.ThresholdCounter stateIDCounter;
    private final Counter.ThresholdCounter transitionIDCounter;
    private final PreCalculatedResultFactory[] preCalculatedResults;

    public NFA(RegexAST ast,
                    List<NFAAnchoredFinalState> anchoredEntry,
                    List<NFAFinalState> unAnchoredEntry,
                    NFAAnchoredFinalState reverseAnchoredEntry,
                    NFAFinalState reverseUnAnchoredEntry,
                    Collection<NFAState> states,
                    Counter.ThresholdCounter stateIDCounter,
                    Counter.ThresholdCounter transitionIDCounter,
                    PreCalculatedResultFactory[] preCalculatedResults) {
        this.ast = ast;
        this.anchoredEntry = anchoredEntry;
        this.unAnchoredEntry = unAnchoredEntry;
        this.reverseAnchoredEntry = reverseAnchoredEntry;
        this.reverseUnAnchoredEntry = reverseUnAnchoredEntry;
        this.stateIDCounter = stateIDCounter;
        this.transitionIDCounter = transitionIDCounter;
        this.preCalculatedResults = preCalculatedResults;
        // reserve last slot for loopBack matcher
        this.states = new NFAState[stateIDCounter.getCount() + 1];
        // reserve last slots for loopBack matcher
        this.transitions = new NFAStateTransition[transitionIDCounter.getCount() + (isTraceFinderNFA() ? 0 : unAnchoredEntry.get(0).getNext().size() + 1)];
        for (NFAState s : states) {
            assert this.states[s.getId()] == null;
            this.states[s.getId()] = s;
            if (s.getNext() == null) {
                continue;
            }
            for (NFAStateTransition t : s.getNext()) {
                assert this.transitions[t.getId()] == null;
                this.transitions[t.getId()] = t;
            }
        }
    }

    public boolean hasReverseUnAnchoredEntry() {
        return reverseUnAnchoredEntry != null && !reverseUnAnchoredEntry.getPrev().isEmpty();
    }

    public RegexAST getAst() {
        return ast;
    }

    public List<NFAAnchoredFinalState> getAnchoredEntry() {
        return anchoredEntry;
    }

    public List<NFAFinalState> getUnAnchoredEntry() {
        return unAnchoredEntry;
    }

    public NFAAnchoredFinalState getReverseAnchoredEntry() {
        return reverseAnchoredEntry;
    }

    public NFAFinalState getReverseUnAnchoredEntry() {
        return reverseUnAnchoredEntry;
    }

    public NFAState[] getStates() {
        return states;
    }

    public NFAStateTransition[] getTransitions() {
        return transitions;
    }

    public PreCalculatedResultFactory[] getPreCalculatedResults() {
        return preCalculatedResults;
    }

    public boolean isTraceFinderNFA() {
        return preCalculatedResults != null;
    }

    @Override
    public int getNumberOfStates() {
        return states.length;
    }

    @Override
    public NFAState getState(int id) {
        return states[id];
    }

    public NFAMatcherState createLoopBackMatcher() {
        if (states[states.length - 1] != null) {
            return (NFAMatcherState) states[states.length - 1];
        }
        ASTNodeSet<RegexASTNode> cc = new ASTNodeSet<>(ast);
        CharacterClass loopBackCC = ast.createLoopBackMatcher();
        cc.add(loopBackCC);
        NFAMatcherState ret = new NFAMatcherState((short) stateIDCounter.inc(), cc, MatcherBuilder.createFull(), Collections.emptySet(), false);
        for (NFAStateTransition t : getUnAnchoredEntry().get(0).getNext()) {
            NFAStateTransition loopBackTransition = new NFAStateTransition((short) transitionIDCounter.inc(), ret, t.getTarget(), t.getGroupBoundaries());
            assert transitions[loopBackTransition.getId()] == null;
            transitions[loopBackTransition.getId()] = loopBackTransition;
            ret.addLoopBackNext(loopBackTransition);
        }
        NFAStateTransition loopBackTransition = new NFAStateTransition((short) transitionIDCounter.inc(), ret, ret, new GroupBoundaries());
        assert transitions[loopBackTransition.getId()] == null;
        transitions[loopBackTransition.getId()] = loopBackTransition;
        ret.addLoopBackNext(loopBackTransition);
        assert states[ret.getId()] == null;
        states[ret.getId()] = ret;
        return ret;
    }
}

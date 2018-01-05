/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
    public NFAState[] getIndex() {
        return getStates();
    }

    public NFAMatcherState createLoopBackMatcher() {
        ASTNodeSet<RegexASTNode> cc = new ASTNodeSet<>(ast);
        CharacterClass loopBackCC = ast.createLoopBackMatcher();
        cc.add(loopBackCC);
        NFAMatcherState ret = new NFAMatcherState(stateIDCounter.inc(), cc, MatcherBuilder.createFull(), Collections.emptySet(), false);
        for (NFAStateTransition t : getUnAnchoredEntry().get(0).getNext()) {
            NFAStateTransition loopBackTransition = new NFAStateTransition((short) transitionIDCounter.inc(), ret, t.getTarget(), new GroupBoundaries());
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

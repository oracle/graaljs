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
import com.oracle.truffle.regex.tregex.automaton.IndexedState;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single state in the NFA form of a regular expression. States may either be matcher
 * states or final states, where a matcher state matches a set of characters and a final state
 * indicates that a match has been found. A state may represent multiple nodes of a regex AST, if it
 * is the result of a product automaton composition of the "regular" regular expression and its
 * lookaround assertions, e.g. the NFA of an expression like /(?=[ab])a/ will contain a state that
 * matches both the 'a' in the lookahead assertion as well as following 'a' in the expression, and
 * therefore will have a state set containing two AST nodes.
 */
public abstract class NFAState implements IndexedState {

    private final short id;
    private final ASTNodeSet<? extends RegexASTNode> stateSet;
    private boolean hasPrefixStates;
    private List<NFAStateTransition> next;
    private List<NFAStateTransition> prev;
    private List<Integer> possibleResults;

    protected NFAState(short id, ASTNodeSet<? extends RegexASTNode> stateSet, List<NFAStateTransition> next, List<NFAStateTransition> prev, List<Integer> possibleResults) {
        this.id = id;
        this.stateSet = stateSet;
        this.next = next;
        this.prev = prev;
        this.possibleResults = possibleResults;
    }

    protected NFAState(short id, ASTNodeSet<? extends RegexASTNode> stateSet) {
        this(id, stateSet, new ArrayList<>(), new ArrayList<>(), null);
    }

    public ASTNodeSet<? extends RegexASTNode> getStateSet() {
        return stateSet;
    }

    public boolean hasPrefixStates() {
        return hasPrefixStates;
    }

    public void setHasPrefixStates(boolean hasPrefixStates) {
        this.hasPrefixStates = hasPrefixStates;
    }

    /**
     * List of possible next states, sorted by priority.
     */
    public List<NFAStateTransition> getNext() {
        return next;
    }

    public void addLoopBackNext(NFAStateTransition transition) {
        // loopBack transitions always have minimal priority, so no sorting is necessary
        next.add(transition);
    }

    public void setNext(ArrayList<NFAStateTransition> transitions, boolean createReverseTransitions) {
        this.next = transitions;
        if (createReverseTransitions) {
            for (NFAStateTransition t : transitions) {
                t.getTarget().prev.add(t);
            }
        }
    }

    public void removeNext(NFAState state) {
        next.removeIf(x -> x.getTarget() == state);
    }

    /**
     * List of possible previous states, unsorted.
     */
    public List<NFAStateTransition> getPrev() {
        return prev;
    }

    public void addPrev(NFAStateTransition transition) {
        prev.add(transition);
        transition.getSource().next.add(transition);
    }

    @Override
    public short getId() {
        return id;
    }

    /**
     * Set of possible pre-calculated result indices as generated by the
     * {@link NFATraceFinderGenerator}. This set must be sorted, since the index values indicate the
     * priority of their respective pre-calculated results. Example: /(a)|([ab])/ will yield two
     * possible results, where the one corresponding to capture group 1 will have the higher
     * priority, so when a single 'a' is encountered when searching for a match, the pre-calculated
     * result corresponding to capture group 1 must be preferred.
     */
    public List<Integer> getPossibleResults() {
        if (possibleResults == null) {
            return Collections.emptyList();
        }
        return possibleResults;
    }

    public boolean hasPossibleResults() {
        return !(possibleResults == null || possibleResults.isEmpty());
    }

    public void addPossibleResult(int index) {
        if (possibleResults == null) {
            possibleResults = new ArrayList<>();
        }
        int searchResult = Collections.binarySearch(possibleResults, index);
        if (searchResult < 0) {
            possibleResults.add((searchResult + 1) * -1, index);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public String idToString() {
        return getStateSet().stream().map(x -> String.valueOf(x.getId())).collect(Collectors.joining(",", "(", ")")) + "[" + id + "]";
    }

    @Override
    public String toString() {
        return idToString();
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        return toTable("NFAState");
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable(String name) {
        DebugUtil.Table table = new DebugUtil.Table("next");
        for (NFAStateTransition transition : next) {
            table.append(transition.toTable());
        }
        return new DebugUtil.Table(name,
                        new DebugUtil.Value("id", id),
                        new DebugUtil.Value("stateSet", idToString()),
                        table);
    }
}

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
package com.oracle.truffle.regex.tregex.dfa;

import com.oracle.truffle.regex.tregex.nfa.NFA;
import com.oracle.truffle.regex.tregex.nfa.NFAAnchoredFinalState;
import com.oracle.truffle.regex.tregex.nfa.NFAFinalState;
import com.oracle.truffle.regex.tregex.nfa.NFAMatcherState;
import com.oracle.truffle.regex.tregex.nfa.NFAState;
import com.oracle.truffle.regex.tregex.nfa.NFAStateTransition;
import com.oracle.truffle.regex.tregex.nodes.TraceFinderDFAStateNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class NFATransitionSet implements Iterable<NFAStateTransition> {

    private static final byte FLAG_FORWARD = 1;
    private static final byte FLAG_PRIORITY_SENSITIVE = 1 << 1;
    private static final byte FLAG_CONTAINS_PREFIX_STATES = 1 << 2;
    private static final byte FLAG_HASH_COMPUTED = 1 << 3;

    private final NFA nfa;
    private final NFAStateSet stateSet;
    private byte flags = 0;
    private short[] transitions;
    private short size = 0;
    private byte preCalculatedUnAnchoredResult = TraceFinderDFAStateNode.NO_PRE_CALC_RESULT;
    private byte preCalculatedAnchoredResult = TraceFinderDFAStateNode.NO_PRE_CALC_RESULT;
    private short finalState = -1;
    private short anchoredFinalState = -1;
    private int cachedHash;

    private NFATransitionSet(NFA nfa, boolean forward, boolean prioritySensitive, short[] transitions) {
        this.nfa = nfa;
        stateSet = new NFAStateSet(nfa);
        if (forward) {
            flags |= FLAG_FORWARD;
        }
        if (prioritySensitive) {
            flags |= FLAG_PRIORITY_SENSITIVE;
        }
        this.transitions = transitions;
    }

    private NFATransitionSet(NFATransitionSet copy, int capacity) {
        this.nfa = copy.nfa;
        this.stateSet = copy.stateSet.copy();
        this.flags = copy.flags;
        this.transitions = new short[capacity];
        System.arraycopy(copy.transitions, 0, this.transitions, 0, copy.size);
        this.size = copy.size;
        this.preCalculatedUnAnchoredResult = copy.preCalculatedUnAnchoredResult;
        this.preCalculatedAnchoredResult = copy.preCalculatedAnchoredResult;
        this.finalState = copy.finalState;
        this.anchoredFinalState = copy.anchoredFinalState;
        this.cachedHash = copy.cachedHash;
    }

    public static NFATransitionSet create(NFA nfa, boolean forward, boolean prioritySensitive) {
        return new NFATransitionSet(nfa, forward, prioritySensitive, new short[20]);
    }

    public static NFATransitionSet create(NFA nfa, boolean forward, boolean prioritySensitive, List<NFAStateTransition> transitions) {
        NFATransitionSet transitionSet = new NFATransitionSet(nfa, forward, prioritySensitive, new short[(transitions.size() * 2) < 20 ? 20 : transitions.size() * 2]);
        transitionSet.addAll(transitions);
        return transitionSet;
    }

    public NFATransitionSet createMerged(NFATransitionSet otherTargetState) {
        NFATransitionSet merged = new NFATransitionSet(this, size() + otherTargetState.size() + 20);
        merged.addAll(otherTargetState);
        return merged;
    }

    private boolean isFlagSet(byte flag) {
        return (flags & flag) != 0;
    }

    private void setFlag(byte flag) {
        flags |= flag;
    }

    private void clearFlag(byte flag) {
        flags &= ~flag;
    }

    private boolean isForward() {
        return isFlagSet(FLAG_FORWARD);
    }

    public boolean isPrioritySensitive() {
        return isFlagSet(FLAG_PRIORITY_SENSITIVE);
    }

    public boolean containsPrefixStates() {
        return isFlagSet(FLAG_CONTAINS_PREFIX_STATES);
    }

    private void setContainsPrefixStates() {
        setFlag(FLAG_CONTAINS_PREFIX_STATES);
    }

    private boolean isHashComputed() {
        return isFlagSet(FLAG_HASH_COMPUTED);
    }

    private void setHashComputed() {
        setFlag(FLAG_HASH_COMPUTED);
    }

    private void clearHashComputed() {
        clearFlag(FLAG_HASH_COMPUTED);
    }

    public NFAFinalState getFinalState() {
        return containsFinalState() ? (NFAFinalState) nfa.getStates()[finalState] : null;
    }

    public NFAAnchoredFinalState getAnchoredFinalState() {
        return containsAnchoredFinalState() ? (NFAAnchoredFinalState) nfa.getStates()[anchoredFinalState] : null;
    }

    public boolean containsAnchoredFinalState() {
        return anchoredFinalState >= 0;
    }

    public boolean containsFinalState() {
        return finalState >= 0;
    }

    public byte getPreCalculatedUnAnchoredResult() {
        return preCalculatedUnAnchoredResult;
    }

    public byte getPreCalculatedAnchoredResult() {
        return preCalculatedAnchoredResult;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void add(NFAStateTransition transition) {
        NFAState target = transition.getTarget(isForward());
        if (isPrioritySensitive() && (containsFinalState() || (target instanceof NFAAnchoredFinalState && containsAnchoredFinalState()))) {
            return;
        }
        if (!stateSet.add(target)) {
            return;
        }
        if (target.hasPrefixStates()) {
            setContainsPrefixStates();
        }
        if (target instanceof NFAMatcherState) {
            appendTransition(transition);
        } else if (target instanceof NFAAnchoredFinalState) {
            anchoredFinalState = target.getId();
            if (target.hasPossibleResults()) {
                updatePreCalcAnchoredResult(target.getPossibleResults().get(0));
            }
            appendTransition(transition);
        } else {
            assert target instanceof NFAFinalState;
            finalState = target.getId();
            if (target.hasPossibleResults()) {
                updatePreCalcUnAnchoredResult(target.getPossibleResults().get(0));
            }
            appendTransition(transition);
        }
    }

    private void ensureCapacity(int newSize) {
        if (newSize < transitions.length) {
            return;
        }
        int newLength = transitions.length * 2;
        while (newLength < newSize) {
            newLength *= 2;
        }
        short[] newTransitions = new short[newLength];
        System.arraycopy(transitions, 0, newTransitions, 0, size);
        transitions = newTransitions;
    }

    private void appendTransition(NFAStateTransition transition) {
        ensureCapacity(size + 1);
        transitions[size] = transition.getId();
        size++;
        clearHashComputed();
    }

    public void addAll(NFATransitionSet other) {
        if (isPrioritySensitive() && containsFinalState()) {
            return;
        }
        ensureCapacity(size + other.size);
        for (int i = 0; i < other.size; i++) {
            add(nfa.getTransitions()[other.transitions[i]]);
        }
    }

    public void addAll(List<NFAStateTransition> addTransitions) {
        if (isPrioritySensitive() && containsFinalState()) {
            return;
        }
        ensureCapacity(size + addTransitions.size());
        for (NFAStateTransition t : addTransitions) {
            add(t);
        }
    }

    private void updatePreCalcUnAnchoredResult(int newResult) {
        if (newResult >= 0) {
            if (preCalculatedUnAnchoredResult == TraceFinderDFAStateNode.NO_PRE_CALC_RESULT || Byte.toUnsignedInt(preCalculatedUnAnchoredResult) > newResult) {
                preCalculatedUnAnchoredResult = (byte) newResult;
            }
        }
    }

    private void updatePreCalcAnchoredResult(int newResult) {
        if (newResult >= 0) {
            if (preCalculatedAnchoredResult == TraceFinderDFAStateNode.NO_PRE_CALC_RESULT || Byte.toUnsignedInt(preCalculatedAnchoredResult) > newResult) {
                preCalculatedAnchoredResult = (byte) newResult;
            }
        }
    }

    @Override
    public int hashCode() {
        if (!isHashComputed()) {
            if (isPrioritySensitive()) {
                cachedHash = 1;
                for (int i = 0; i < size; i++) {
                    cachedHash = 31 * cachedHash + nfa.getTransitions()[transitions[i]].getTarget(isForward()).hashCode();
                }
            } else {
                cachedHash = stateSetHashCode(stateSet);
            }
            setHashComputed();
        }
        return cachedHash;
    }

    private static int stateSetHashCode(NFAStateSet nfaStates) {
        if (nfaStates == null) {
            return 0;
        }
        return nfaStates.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NFATransitionSet)) {
            return false;
        }
        NFATransitionSet o = (NFATransitionSet) obj;
        if (isPrioritySensitive()) {
            if (size() != o.size()) {
                return false;
            }
            for (int i = 0; i < size(); i++) {
                if (!nfa.getTransitions()[transitions[i]].getTarget(isForward()).equals(nfa.getTransitions()[o.transitions[i]].getTarget(isForward()))) {
                    return false;
                }
            }
            return true;
        } else {
            return stateSetEquals(stateSet, o.stateSet);
        }
    }

    private static boolean stateSetEquals(NFAStateSet a, NFAStateSet b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Override
    public Iterator<NFAStateTransition> iterator() {
        return new NFATransitionSetIterator(nfa, transitions, size);
    }

    private static final class NFATransitionSetIterator implements Iterator<NFAStateTransition> {

        private final NFA nfa;
        private final short[] transitions;
        private final short size;
        private short i = 0;

        private NFATransitionSetIterator(NFA nfa, short[] transitions, short size) {
            this.nfa = nfa;
            this.transitions = transitions;
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return i < size;
        }

        @Override
        public NFAStateTransition next() {
            return nfa.getTransitions()[transitions[i++]];
        }
    }

    public Stream<NFAStateTransition> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public String toString() {
        return stream().map(x -> x.getTarget(isForward()).idToString()).collect(Collectors.joining(",", "{", "}"));
    }

    public DebugUtil.Table toTable(String name) {
        DebugUtil.Table table = new DebugUtil.Table(name);
        for (NFAStateTransition transition : this) {
            table.append(transition.toTable());
        }
        return table;
    }
}

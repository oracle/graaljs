/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.automaton.TransitionBuilder;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;
import java.util.Iterator;

public class ASTTransitionSet implements Iterable<ASTTransition> {

    private final ArrayList<ASTTransition> transitions;

    public ASTTransitionSet(ASTTransition transition) {
        this.transitions = new ArrayList<>();
        transitions.add(transition);
    }

    public ASTTransitionSet(ArrayList<ASTTransition> transitions) {
        this.transitions = transitions;
    }

    public ASTTransitionSet createMerged(ASTTransitionSet other) {
        ArrayList<ASTTransition> merged = new ArrayList<>(transitions);
        ASTTransitionSet ret = new ASTTransitionSet(merged);
        ret.merge(other);
        return ret;
    }

    public void mergeInPlace(TransitionBuilder<ASTTransitionSet> other) {
        merge(other.getTargetState());
    }

    private void merge(ASTTransitionSet other) {
        for (ASTTransition t : other) {
            if (!transitions.contains(t)) {
                transitions.add(t);
            }
        }
    }

    @Override
    public Iterator<ASTTransition> iterator() {
        return transitions.iterator();
    }

    @Override
    public int hashCode() {
        return transitions.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof ASTTransitionSet && transitions.equals(((ASTTransitionSet) obj).transitions));
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        DebugUtil.Table table = new DebugUtil.Table("ASTTransitionSet");
        for (ASTTransition t : transitions) {
            table.append(t.toTable());
        }
        return table;
    }
}

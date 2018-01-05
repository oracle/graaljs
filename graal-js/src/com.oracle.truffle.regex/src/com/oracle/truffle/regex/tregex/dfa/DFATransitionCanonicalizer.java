/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.dfa;

import com.oracle.truffle.regex.tregex.automaton.StateTransitionCanonicalizer;
import com.oracle.truffle.regex.tregex.nfa.NFAStateTransition;

import java.util.Iterator;

public class DFATransitionCanonicalizer extends StateTransitionCanonicalizer<NFATransitionSet, DFAStateTransitionBuilder> {

    private final boolean trackCaptureGroups;

    public DFATransitionCanonicalizer(boolean trackCaptureGroups) {
        this.trackCaptureGroups = trackCaptureGroups;
    }

    @Override
    protected boolean isSameTargetMergeAllowed(DFAStateTransitionBuilder a, DFAStateTransitionBuilder b) {
        if (!trackCaptureGroups) {
            return true;
        }
        assert a.getTargetState().equals(b.getTargetState());
        Iterator<NFAStateTransition> ia = a.getTargetState().iterator();
        Iterator<NFAStateTransition> ib = b.getTargetState().iterator();
        while (ia.hasNext()) {
            final NFAStateTransition lastA = ia.next();
            final NFAStateTransition lastB = ib.next();
            if (!(lastA.getTarget().getStateSet().equals(lastB.getTarget().getStateSet()) &&
                            lastA.getSource().getStateSet().equals(lastB.getSource().getStateSet()) &&
                            lastA.getGroupBoundaries().equals(lastB.getGroupBoundaries()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected DFAStateTransitionBuilder[] createResultArray(int size) {
        return new DFAStateTransitionBuilder[size];
    }
}

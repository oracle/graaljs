/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.regex.tregex.automaton.StateTransitionCanonicalizer;

public class ASTTransitionCanonicalizer extends StateTransitionCanonicalizer<ASTTransitionSet, ASTTransitionSetBuilder> {

    @Override
    protected boolean isSameTargetMergeAllowed(ASTTransitionSetBuilder a, ASTTransitionSetBuilder b) {
        return true;
    }

    @Override
    protected ASTTransitionSetBuilder[] createResultArray(int size) {
        return new ASTTransitionSetBuilder[size];
    }
}

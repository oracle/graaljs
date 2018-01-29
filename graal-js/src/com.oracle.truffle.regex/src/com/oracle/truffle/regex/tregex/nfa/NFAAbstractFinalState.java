/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;

public abstract class NFAAbstractFinalState extends NFAState {

    protected NFAAbstractFinalState(short id, ASTNodeSet<? extends RegexASTNode> stateSet, int preCalculatedResultIndex) {
        super(id, stateSet);
        addPossibleResult(preCalculatedResultIndex);
    }

    protected NFAAbstractFinalState(short id, ASTNodeSet<? extends RegexASTNode> stateSet) {
        super(id, stateSet);
    }
}

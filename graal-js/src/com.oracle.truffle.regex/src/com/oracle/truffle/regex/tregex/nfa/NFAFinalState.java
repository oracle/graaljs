/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class NFAFinalState extends NFAAbstractFinalState {

    public NFAFinalState(short id, ASTNodeSet<? extends RegexASTNode> stateSet) {
        super(id, stateSet);
    }

    public NFAFinalState(short id, ASTNodeSet<? extends RegexASTNode> stateSet, int preCalculatedResultIndex) {
        super(id, stateSet, preCalculatedResultIndex);
    }

    @Override
    public String toString() {
        return "end";
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        return toTable("NFAFinalState");
    }
}

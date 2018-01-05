/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.regex.tregex.automaton.StateSet;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;

import java.util.Collection;

public class ASTNodeSet<S extends RegexASTNode> extends StateSet<S> {

    public ASTNodeSet(RegexAST ast) {
        super(ast);
    }

    public ASTNodeSet(RegexAST ast, S node) {
        super(ast);
        add(node);
    }

    public ASTNodeSet(RegexAST ast, Collection<S> initialNodes) {
        super(ast);
        addAll(initialNodes);
    }

    private ASTNodeSet(ASTNodeSet<S> copy) {
        super(copy);
    }

    @Override
    public ASTNodeSet<S> copy() {
        return new ASTNodeSet<>(this);
    }
}

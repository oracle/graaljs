/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast.visitors;

import com.oracle.truffle.regex.tregex.parser.ast.BackReference;
import com.oracle.truffle.regex.tregex.parser.ast.CharacterClass;
import com.oracle.truffle.regex.tregex.parser.ast.Group;
import com.oracle.truffle.regex.tregex.parser.ast.LookAheadAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.LookBehindAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.MatchFound;
import com.oracle.truffle.regex.tregex.parser.ast.PositionAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTSubtreeRootNode;
import com.oracle.truffle.regex.tregex.parser.ast.Sequence;

/**
 * A visitor used to maintain the state of cached {@link RegexAST} fields when removing parts of the
 * AST.
 * <p>
 * Nodes can be removed from the AST simply by mutating the parent/child pointers. However, there
 * are some cached values stored in {@link RegexAST} which would no longer have valid values. The
 * {@link DeleteVisitor} is something you can run on nodes that you are removing from the AST in
 * order to update the relevant fields of {@link RegexAST}.
 *
 * @see RegexAST#getNodeCount()
 * @see RegexAST#getEndPoints()
 * @see RegexAST#getReachableCarets()
 * @see RegexAST#getReachableDollars()
 * @see RegexAST#getLookBehinds()
 */
public class DeleteVisitor extends DepthFirstTraversalRegexASTVisitor {

    private final RegexAST ast;

    public DeleteVisitor(RegexAST ast) {
        this.ast = ast;
    }

    @Override
    protected void visit(BackReference backReference) {
        ast.getNodeCount().dec();
    }

    @Override
    protected void visit(Group group) {
        ast.getNodeCount().dec();
        if (group.getParent() instanceof RegexASTSubtreeRootNode) {
            ast.getNodeCount().dec();
            ast.getEndPoints().remove(group.getSubTreeParent().getMatchFound());
        }
    }

    @Override
    protected void visit(Sequence sequence) {
        ast.getNodeCount().dec();
    }

    @Override
    protected void visit(PositionAssertion assertion) {
        ast.getNodeCount().dec();
        switch (assertion.type) {
            case CARET:
                ast.getReachableCarets().remove(assertion);
                break;
            case DOLLAR:
                ast.getReachableDollars().remove(assertion);
                break;
        }
    }

    @Override
    protected void visit(LookBehindAssertion assertion) {
        ast.getNodeCount().dec();
        ast.getLookBehinds().remove(assertion);
    }

    @Override
    protected void visit(LookAheadAssertion assertion) {
        ast.getNodeCount().dec();
    }

    @Override
    protected void visit(CharacterClass characterClass) {
        ast.getNodeCount().dec();
    }

    @Override
    protected void visit(MatchFound matchFound) {
        throw new IllegalStateException();
    }
}

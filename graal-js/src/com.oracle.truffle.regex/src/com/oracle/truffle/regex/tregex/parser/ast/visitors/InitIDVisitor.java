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
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTSubtreeRootNode;
import com.oracle.truffle.regex.tregex.parser.ast.Sequence;

public final class InitIDVisitor extends DepthFirstTraversalRegexASTVisitor {

    /**
     * ID of the parent node of AST nodes that are not part of a lookaround assertion.
     */
    public static final int REGEX_AST_ROOT_PARENT_ID = 0;

    private final RegexASTNode[] index;
    private int nextID;

    private InitIDVisitor(RegexASTNode[] index, int nextID) {
        this.index = index;
        this.nextID = nextID;
    }

    public static void init(RegexAST ast) {
        // additional reserved slots:
        // - 1 slot for REGEX_AST_ROOT_PARENT_ID
        // - prefix length + 1 anchored initial NFA states
        // - prefix length + 1 unanchored initial NFA states
        // - 1 slot at the end for NFA loopBack matcher
        int initialID = 3 + (ast.getWrappedPrefixLength() * 2);
        InitIDVisitor visitor = new InitIDVisitor(new RegexASTNode[initialID + ast.getNumberOfNodes() + 1], initialID);
        assert ast.getWrappedRoot().getSubTreeParent().getId() == REGEX_AST_ROOT_PARENT_ID;
        visitor.index[REGEX_AST_ROOT_PARENT_ID] = ast.getWrappedRoot().getSubTreeParent();
        visitor.run(ast.getWrappedRoot());
        ast.setIndex(visitor.index);
    }

    private void initID(RegexASTNode node) {
        node.setId(nextID++);
        index[node.getId()] = node;
    }

    @Override
    protected void visit(BackReference backReference) {
        initID(backReference);
    }

    @Override
    protected void visit(Group group) {
        initID(group);
    }

    @Override
    protected void leave(Group group) {
        if (group.getParent() instanceof RegexASTSubtreeRootNode) {
            final MatchFound matchFound = group.getSubTreeParent().getMatchFound();
            if (!matchFound.idInitialized()) {
                initID(matchFound);
            }
        }
    }

    @Override
    protected void visit(Sequence sequence) {
        initID(sequence);
    }

    @Override
    protected void visit(PositionAssertion assertion) {
        initID(assertion);
    }

    @Override
    protected void visit(LookBehindAssertion assertion) {
        initID(assertion);
    }

    @Override
    protected void visit(LookAheadAssertion assertion) {
        initID(assertion);
    }

    @Override
    protected void visit(CharacterClass characterClass) {
        initID(characterClass);
    }

    @Override
    protected void visit(MatchFound matchFound) {
        initID(matchFound);
    }
}

/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.RegexASTVisitorIterable;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * A common supertype to the root node and look-ahead and look-behind assertions. Every AST subtree
 * contains a {@link Group} which contains the syntactic subtree, as well as a {@link MatchFound}
 * node, which is needed for NFA-like traversal of the AST, see
 * {@link com.oracle.truffle.regex.tregex.parser.ast.visitors.NFATraversalRegexASTVisitor}.
 */
public abstract class RegexASTSubtreeRootNode extends Term implements RegexASTVisitorIterable {

    private Group group;
    private MatchFound matchFound;
    private boolean visitorGroupVisited = false;

    RegexASTSubtreeRootNode() {
    }

    RegexASTSubtreeRootNode(RegexASTSubtreeRootNode copy, RegexAST ast) {
        super(copy);
        setGroup(copy.group.copy(ast));
        ast.createEndPoint(this);
    }

    @Override
    public abstract RegexASTSubtreeRootNode copy(RegexAST ast);

    /**
     * Returns the {@link Group} that represents the contents of this subtree.
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Sets the contents of this subtree.
     * <p>
     * This method should be called after creating any instance of this class. Otherwise, methods of
     * this class could throw {@link NullPointerException}s or return {@code null}s.
     */
    public void setGroup(Group group) {
        this.group = group;
        group.setParent(this);
    }

    /**
     * Returns this subtree's corresponding {@link MatchFound} node.
     */
    public MatchFound getMatchFound() {
        return matchFound;
    }

    public void setMatchFound(MatchFound matchFound) {
        this.matchFound = matchFound;
        matchFound.setParent(this);
    }

    /**
     * Marks the node as dead, i.e. unmatchable.
     * <p>
     * Note that using this setter also traverses the ancestors and children of this node and
     * updates their "dead" status as well.
     */
    @Override
    public void markAsDead() {
        super.markAsDead();
        if (!group.isDead()) {
            group.markAsDead();
        }
    }

    @Override
    public boolean visitorHasNext() {
        return !visitorGroupVisited;
    }

    @Override
    public RegexASTNode visitorGetNext(boolean reverse) {
        visitorGroupVisited = true;
        return group;
    }

    @Override
    public void resetVisitorIterator() {
        visitorGroupVisited = false;
    }

    public abstract String getPrefix();

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return "(" + getPrefix() + group.alternativesToString() + ")";
    }

    @Override
    public DebugUtil.Table toTable(String name) {
        return super.toTable(name).append(new DebugUtil.Value("group", astNodeId(group)));
    }
}

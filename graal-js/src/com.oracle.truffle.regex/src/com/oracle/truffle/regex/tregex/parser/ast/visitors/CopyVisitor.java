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
import com.oracle.truffle.regex.tregex.parser.ast.Term;

/**
 * An AST visitor that produces a deep copy of a given {@link Term} and its subtree, and registers
 * all new nodes in the {@link RegexAST} provided at instantiation. This visitor should be preferred
 * over {@link RegexASTNode#copy(RegexAST)} whenever possible, since it is non-recursive. Note that
 * this visitor is not thread-safe!
 * 
 * @see DepthFirstTraversalRegexASTVisitor
 */
public class CopyVisitor extends DepthFirstTraversalRegexASTVisitor {

    private final RegexAST ast;
    private Term copyRoot;
    private RegexASTNode curParent;

    public CopyVisitor(RegexAST ast) {
        this.ast = ast;
    }

    public Term copy(Term term) {
        run(term);
        assert copyRoot != null;
        Term result = copyRoot;
        copyRoot = null;
        return result;
    }

    @Override
    protected void visit(BackReference backReference) {
        addToParent(ast.createBackReference(backReference.getGroupNr()));
    }

    @Override
    protected void visit(Group group) {
        Group copy = group.isCapturing() ? ast.createCaptureGroup(group.getGroupNumber()) : ast.createGroup();
        copy.setLoop(group.isLoop());
        copy.setExpandedQuantifier(group.isExpandedQuantifier());
        copy.setEnclosedCaptureGroupsLow(group.getEnclosedCaptureGroupsLow());
        copy.setEnclosedCaptureGroupsHigh(group.getEnclosedCaptureGroupsHigh());
        addToParent(copy);
        curParent = copy;
    }

    @Override
    protected void leave(Group group) {
        goToUpperParent();
    }

    @Override
    protected void visit(Sequence sequence) {
        curParent = ((Group) curParent).addSequence(ast);
    }

    @Override
    protected void leave(Sequence sequence) {
        goToUpperParent();
    }

    @Override
    protected void visit(PositionAssertion assertion) {
        addToParent(ast.createPositionAssertion(assertion.type));
    }

    @Override
    protected void visit(LookBehindAssertion assertion) {
        LookBehindAssertion copy = ast.createLookBehindAssertion();
        addToParent(copy);
        curParent = copy;
    }

    @Override
    protected void leave(LookBehindAssertion assertion) {
        goToUpperParent();
    }

    @Override
    protected void visit(LookAheadAssertion assertion) {
        LookAheadAssertion copy = ast.createLookAheadAssertion(assertion.isNegated());
        addToParent(copy);
        curParent = copy;
    }

    @Override
    protected void leave(LookAheadAssertion assertion) {
        goToUpperParent();
    }

    @Override
    protected void visit(CharacterClass characterClass) {
        addToParent(ast.createCharacterClass(characterClass.getMatcherBuilder()));
    }

    @Override
    protected void visit(MatchFound matchFound) {
        throw new IllegalStateException();
    }

    private void goToUpperParent() {
        assert curParent != null;
        curParent = curParent.getParent();
    }

    private void addToParent(Term copy) {
        if (curParent == null) {
            assert copyRoot == null;
            copyRoot = copy;
        } else if (curParent instanceof RegexASTSubtreeRootNode) {
            assert copy instanceof Group;
            ((RegexASTSubtreeRootNode) curParent).setGroup((Group) copy);
        } else {
            ((Sequence) curParent).add(copy);
        }
    }
}

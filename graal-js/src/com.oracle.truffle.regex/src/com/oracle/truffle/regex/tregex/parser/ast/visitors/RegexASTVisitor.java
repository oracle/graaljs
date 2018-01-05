/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.parser.ast.Sequence;

public abstract class RegexASTVisitor {

    protected abstract void visit(BackReference backReference);

    protected abstract void visit(Group group);

    protected abstract void leave(Group group);

    protected abstract void visit(Sequence sequence);

    protected abstract void leave(Sequence sequence);

    protected abstract void visit(PositionAssertion assertion);

    protected abstract void visit(LookBehindAssertion assertion);

    protected abstract void leave(LookBehindAssertion assertion);

    protected abstract void visit(LookAheadAssertion assertion);

    protected abstract void leave(LookAheadAssertion assertion);

    protected abstract void visit(CharacterClass characterClass);

    protected abstract void visit(MatchFound matchFound);

    protected void doVisit(RegexASTNode cur) {
        if (cur == null) {
            throw new IllegalStateException();
        }
        if (cur instanceof Group) {
            visit((Group) cur);
        } else if (cur instanceof Sequence) {
            visit((Sequence) cur);
        } else if (cur instanceof PositionAssertion) {
            visit((PositionAssertion) cur);
        } else if (cur instanceof LookBehindAssertion) {
            visit((LookBehindAssertion) cur);
        } else if (cur instanceof LookAheadAssertion) {
            visit((LookAheadAssertion) cur);
        } else if (cur instanceof CharacterClass) {
            visit((CharacterClass) cur);
        } else if (cur instanceof BackReference) {
            visit((BackReference) cur);
        } else if (cur instanceof MatchFound) {
            visit((MatchFound) cur);
        } else {
            throw new IllegalStateException();
        }
    }

    protected void doLeave(RegexASTNode cur) {
        if (cur instanceof Group) {
            leave((Group) cur);
        } else if (cur instanceof Sequence) {
            leave((Sequence) cur);
        } else if (cur instanceof LookBehindAssertion) {
            leave((LookBehindAssertion) cur);
        } else if (cur instanceof LookAheadAssertion) {
            leave((LookAheadAssertion) cur);
        } else {
            throw new IllegalStateException();
        }
    }
}

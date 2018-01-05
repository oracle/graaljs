/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast.visitors;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.parser.ast.BackReference;
import com.oracle.truffle.regex.tregex.parser.ast.CharacterClass;
import com.oracle.truffle.regex.tregex.parser.ast.Group;
import com.oracle.truffle.regex.tregex.parser.ast.LookAheadAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.LookBehindAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.MatchFound;
import com.oracle.truffle.regex.tregex.parser.ast.PositionAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTSubtreeRootNode;
import com.oracle.truffle.regex.tregex.parser.ast.Sequence;

public final class ASTDebugDumpVisitor extends DepthFirstTraversalRegexASTVisitor {

    private ASTDebugDumpVisitor() {
    }

    @CompilerDirectives.TruffleBoundary
    public static String getDump(Group root) {
        ASTDebugDumpVisitor visitor = new ASTDebugDumpVisitor();
        visitor.run(root);
        if (visitor.dead) {
            visitor.dump.append("\u001b[0m");
        }
        return visitor.dump.toString();
    }

    private final StringBuilder dump = new StringBuilder();
    private boolean dead;

    private void append(RegexASTNode node) {
        checkDead(node);
        dump.append(node.toString());
    }

    private void checkDead(RegexASTNode node) {
        if (!dead && node.isDead()) {
            dump.append("\u001b[41m");
            dead = true;
        }
        if (dead && !node.isDead()) {
            dump.append("\u001b[0m");
            dead = false;
        }
    }

    @Override
    protected void visit(BackReference backReference) {
        append(backReference);
    }

    @Override
    protected void visit(Group group) {
        checkDead(group);
        dump.append("(");
        if (group.getParent() instanceof RegexASTSubtreeRootNode) {
            dump.append(((RegexASTSubtreeRootNode) group.getParent()).getPrefix());
        } else if (!group.isCapturing()) {
            dump.append("?:");
        }
    }

    @Override
    protected void leave(Group group) {
        dump.append(")").append(group.loopToString());
    }

    @Override
    protected void visit(Sequence sequence) {
        if (sequence != sequence.getParent().getAlternatives().get(0)) {
            dump.append("|");
        }
    }

    @Override
    protected void visit(PositionAssertion assertion) {
        append(assertion);
    }

    @Override
    protected void visit(LookBehindAssertion assertion) {
    }

    @Override
    protected void visit(LookAheadAssertion assertion) {
    }

    @Override
    protected void visit(CharacterClass characterClass) {
        append(characterClass);
    }

    @Override
    protected void visit(MatchFound matchFound) {
        append(matchFound);
    }
}

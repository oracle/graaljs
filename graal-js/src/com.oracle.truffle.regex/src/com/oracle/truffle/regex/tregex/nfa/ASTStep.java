/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;

public final class ASTStep {

    private final RegexASTNode root;
    private final ArrayList<ASTSuccessor> successors = new ArrayList<>();

    public ASTStep(RegexASTNode root) {
        this.root = root;
    }

    public RegexASTNode getRoot() {
        return root;
    }

    public ArrayList<ASTSuccessor> getSuccessors() {
        return successors;
    }

    public void addSuccessor(ASTSuccessor successor) {
        successors.add(successor);
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        DebugUtil.Table table = new DebugUtil.Table("ASTStep",
                        new DebugUtil.Value("root", root.toStringWithID()));
        for (ASTSuccessor s : successors) {
            s.getMergedStates(new ASTTransitionCanonicalizer());
            table.append(s.toTable());
        }
        return table;
    }
}

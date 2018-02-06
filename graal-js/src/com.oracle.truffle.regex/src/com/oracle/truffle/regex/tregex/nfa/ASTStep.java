/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.UnsupportedRegexException;
import com.oracle.truffle.regex.tregex.TRegexOptions;
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
        // When compiling a regular expression, almost all ASTSuccessors will yield at least 1 NFA
        // transition (the only case when an ASTSuccessor yields no NFA transitions is when the NFA
        // transition would collide with a position assertion such as $ or ^, as in /(?=a$)ab/, see
        // NFAGenerator#createNFATransitions). Furthermore, there exist regular expressions such as
        // (a?|b?|c?|d?|e?|f?|g?)(a?|b?|c?|d?|e?|f?|g?)... The number of ASTSuccessors in a single
        // ASTStep rises exponentially with the number of repetitions of this pattern (there is a
        // different ASTSuccessor for every possible path to a next matching character). If we want
        // to avoid running out of memory in such situations, we have to bailout during the
        // collection of ASTSuccessors in ASTStep, before they are transformed into NFA transitions.
        if (successors.size() > TRegexOptions.TRegexMaxNumberOfASTSuccessorsInOneASTStep) {
            throw new UnsupportedRegexException("TRegex Bailout: ASTSuccessor explosion");
        }
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

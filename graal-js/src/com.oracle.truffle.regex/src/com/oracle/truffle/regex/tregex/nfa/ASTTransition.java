/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.parser.ast.Term;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class ASTTransition {

    private Term target;
    private final GroupBoundaries groupBoundaries = new GroupBoundaries();

    public ASTTransition() {
    }

    public ASTTransition(Term target) {
        this.target = target;
    }

    public Term getTarget() {
        return target;
    }

    public void setTarget(Term target) {
        this.target = target;
    }

    public GroupBoundaries getGroupBoundaries() {
        return groupBoundaries;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ASTTransition && target.equals(((ASTTransition) obj).target);
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("ASTTransition",
                        new DebugUtil.Value("target", target.toStringWithID()),
                        groupBoundaries.toTable());
    }
}

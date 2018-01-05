/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * An assertion that succeeds depending on whether another regular expression can be matched at the
 * current position.
 * <p>
 * Corresponds to the <strong>( ? =</strong> <em>Disjunction</em> <strong>)</strong> and <strong>( ?
 * !</strong> <em>Disjunction</em> <strong>)</strong> right-hand sides of the <em>Assertion</em>
 * goal symbol in the ECMAScript RegExp syntax.
 */
public class LookAheadAssertion extends RegexASTSubtreeRootNode {

    /**
     * Creates a new look-ahead assertion AST node.
     * 
     * Note that for this node to be complete, {@link RegexASTSubtreeRootNode#setGroup(Group)} has
     * to be called with the {@link Group} that represents the contents of this look-ahead
     * assertion.
     * 
     * @param negated whether this look-ahead assertion is negative or not
     */
    LookAheadAssertion(boolean negated) {
        setFlag(FLAG_LOOK_AHEAD_NEGATED, negated);
    }

    private LookAheadAssertion(LookAheadAssertion copy, RegexAST ast) {
        super(copy, ast);
    }

    @Override
    public LookAheadAssertion copy(RegexAST ast) {
        return ast.register(new LookAheadAssertion(this, ast));
    }

    /**
     * Indicates whether this is a negative look-ahead assertion (written as {@code (?!...)}) or
     * positive one (written as {@code (?:...)}).
     * <p>
     * Positive look-ahead assertions match if and only if the text at the current position matches
     * the contents of the assertion. Negative look-ahead assertions match if and only if the text
     * at the current position <em>does not</em> match the contents of the assertion.
     */
    public boolean isNegated() {
        return isFlagSet(FLAG_LOOK_AHEAD_NEGATED);
    }

    @Override
    public String getPrefix() {
        return isNegated() ? "?!" : "?=";
    }

    @Override
    public DebugUtil.Table toTable() {
        return toTable(isNegated() ? "NegativeLookAheadAssertion" : "LookAheadAssertion");
    }
}

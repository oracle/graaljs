/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.nfa.ASTNodeSet;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link Term} that matches characters belonging to a specified set of characters.
 * <p>
 * Corresponds to the right-hand sides <em>PatternCharacter</em>, <strong>.</strong> and
 * <em>CharacterClass</em> of the goal symbol <em>Atom</em> and the right-hand sides
 * <em>CharacterClassEscape</em> and <em>CharacterEscape</em> of the goal symbol <em>AtomEscape</em>
 * in the ECMAScript RegExp syntax.
 * <p>
 * Note that {@link CharacterClass} nodes and the {@link MatcherBuilder}s that they rely on can only
 * match characters from the Basic Multilingual Plane (and whose code point fits into 16-bit
 * integers). Any term which matches characters outside of the Basic Multilingual Plane is expanded
 * by {@link com.oracle.truffle.regex.tregex.parser.RegexParser} into a more complex expression
 * which matches the individual code units that would make up the UTF-16 encoding of those
 * characters.
 */
public class CharacterClass extends Term {

    private final MatcherBuilder matcherBuilder;
    // look-behind groups which might match the same character as this CharacterClass node
    private ASTNodeSet<Group> lookBehindEntries;

    /**
     * Creates a new {@link CharacterClass} node which matches the set of characters specified by
     * the {@code matcherBuilder}.
     */
    CharacterClass(MatcherBuilder matcherBuilder) {
        this.matcherBuilder = matcherBuilder;
    }

    private CharacterClass(CharacterClass copy) {
        super(copy);
        matcherBuilder = copy.matcherBuilder;
    }

    @Override
    public CharacterClass copy(RegexAST ast) {
        return ast.register(new CharacterClass(this));
    }

    /**
     * Returns the {@link MatcherBuilder} representing the set of characters that can be matched by
     * this {@link CharacterClass}.
     */
    public MatcherBuilder getMatcherBuilder() {
        return matcherBuilder;
    }

    public void addLookBehindEntry(RegexAST ast, Group lookBehindEntry) {
        if (lookBehindEntries == null) {
            lookBehindEntries = new ASTNodeSet<>(ast);
        }
        lookBehindEntries.add(lookBehindEntry);
    }

    public boolean hasLookBehindEntries() {
        return lookBehindEntries != null;
    }

    /**
     * Returns the (fixed-length) look-behind assertions whose first characters can match the same
     * character as this node. Note that the set contains the {@link Group} bodies of the
     * {@link LookBehindAssertion} nodes, not the {@link LookBehindAssertion} nodes themselves.
     */
    public Set<Group> getLookBehindEntries() {
        if (lookBehindEntries == null) {
            return Collections.emptySet();
        }
        return lookBehindEntries;
    }

    @Override
    public String toString() {
        return matcherBuilder.toString();
    }

    @Override
    public DebugUtil.Table toTable() {
        final DebugUtil.Table table = toTable("CharacterClass").append(new DebugUtil.Value("matcherBuilder", matcherBuilder));
        if (lookBehindEntries != null) {
            table.append(new DebugUtil.Value("lookBehindEntries", lookBehindEntries.stream().map(RegexASTNode::astNodeId).collect(Collectors.joining(","))));
        }
        return table;
    }
}

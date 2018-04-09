/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

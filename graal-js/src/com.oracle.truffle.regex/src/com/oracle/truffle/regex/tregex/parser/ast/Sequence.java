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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.RegexASTVisitorIterable;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * A Sequence is a concatenation of {@link Term}s.
 * <p>
 * Sequences are used as the alternatives in a {@link Group}. They are the only subtype of
 * {@link RegexASTNode} which does not extend {@link Term}. In order to coerce a Sequence into a
 * {@link Term}, wrap it in a {@link Group} (as its only alternative).
 * <p>
 * Corresponds to the goal symbol <em>Alternative</em> in the ECMAScript RegExp syntax.
 */
public class Sequence extends RegexASTNode implements RegexASTVisitorIterable {

    private final ArrayList<Term> terms = new ArrayList<>();
    private short visitorIterationIndex = 0;

    Sequence() {
    }

    private Sequence(Sequence copy, RegexAST ast) {
        super(copy);
        for (Term t : copy.terms) {
            add(t.copy(ast));
        }
    }

    @Override
    public Sequence copy(RegexAST ast) {
        return ast.register(new Sequence(this, ast));
    }

    @Override
    public Group getParent() {
        return (Group) super.getParent();
    }

    @Override
    public void setParent(RegexASTNode parent) {
        assert parent instanceof Group;
        super.setParent(parent);
    }

    /**
     * Returns the list of terms that constitute this {@link Sequence}.
     * <p>
     * Note that elements should not be added or removed from this list. Use the methods
     * {@link #add(Term)} and {@link #removeLast()} instead.
     */
    public ArrayList<Term> getTerms() {
        return terms;
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    public Term getFirstTerm() {
        return terms.get(0);
    }

    public Term getLastTerm() {
        return terms.get(terms.size() - 1);
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
        if (getParent() == null) {
            return;
        }
        for (Term t : terms) {
            t.markAsDead();
        }
        for (Sequence s : getParent().getAlternatives()) {
            if (!s.isDead()) {
                return;
            }
        }
        getParent().markAsDead();
    }

    /**
     * Adds a {@link Term} to the end of the {@link Sequence}.
     * 
     * @param term
     */
    public void add(Term term) {
        term.setParent(this);
        term.setSeqIndex(terms.size());
        terms.add(term);
    }

    /**
     * Removes the last {@link Term} from this {@link Sequence}.
     */
    public void removeLast() {
        terms.remove(terms.size() - 1);
    }

    public boolean isLiteral() {
        if (isEmpty()) {
            return false;
        }
        for (Term t : terms) {
            if (!(t instanceof CharacterClass)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RegexASTSubtreeRootNode getSubTreeParent() {
        return getParent().getSubTreeParent();
    }

    @Override
    public boolean visitorHasNext() {
        return visitorIterationIndex < terms.size();
    }

    @Override
    public void resetVisitorIterator() {
        visitorIterationIndex = 0;
    }

    @Override
    public RegexASTNode visitorGetNext(boolean reverse) {
        if (reverse) {
            return terms.get(terms.size() - (++visitorIterationIndex));
        }
        return terms.get(visitorIterationIndex++);
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return terms.stream().map(Term::toString).collect(Collectors.joining(""));
    }

    @Override
    public DebugUtil.Table toTable() {
        return toTable("Sequence").append(terms.stream().map(RegexASTNode::toTable));
    }
}

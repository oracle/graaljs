/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser.ir;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * IR representation of an indexed access (brackets operator.)
 */
public final class IndexNode extends BaseNode {
    /** Property index. */
    private final Expression index;

    /**
     * Constructors
     *
     * @param token token
     * @param finish finish
     * @param base base node for access
     * @param index index for access
     */
    public IndexNode(long token, int finish, Expression base, Expression index, boolean isSuper, boolean optional, boolean optionalChain) {
        super(token, finish, base, isSuper, optional, optionalChain);
        this.index = index;
    }

    public IndexNode(final long token, final int finish, final Expression base, final Expression index) {
        this(token, finish, base, index, false, false, false);
    }

    private IndexNode(final IndexNode indexNode, final Expression base, final Expression index, final boolean isSuper) {
        super(indexNode, base, isSuper, indexNode.isOptional(), indexNode.isOptionalChain());
        this.index = index;
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterIndexNode(this)) {
            //@formatter:off
            return visitor.leaveIndexNode(
                setBase((Expression) base.accept(visitor)).
                setIndex((Expression) index.accept(visitor)));
            //@formatter:on
        }
        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterIndexNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        final boolean needsParen = tokenType().needsParens(base.tokenType(), true);

        if (needsParen) {
            sb.append('(');
        }

        base.toString(sb, printType);

        if (needsParen) {
            sb.append(')');
        }

        if (isOptional()) {
            sb.append('?').append('.');
        }
        sb.append('[');
        index.toString(sb, printType);
        sb.append(']');
    }

    /**
     * Get the index expression for this IndexNode
     *
     * @return the index
     */
    public Expression getIndex() {
        return index;
    }

    private IndexNode setBase(final Expression base) {
        if (this.base == base) {
            return this;
        }
        return new IndexNode(this, base, index, isSuper());
    }

    /**
     * Set the index expression for this node
     *
     * @param index new index expression
     * @return a node equivalent to this one except for the requested change.
     */
    public IndexNode setIndex(final Expression index) {
        if (this.index == index) {
            return this;
        }
        return new IndexNode(this, base, index, isSuper());
    }

    @Override
    public IndexNode setIsSuper() {
        if (isSuper()) {
            return this;
        }
        return new IndexNode(this, base, index, true);
    }
}

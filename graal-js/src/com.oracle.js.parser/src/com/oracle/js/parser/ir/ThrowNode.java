/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.js.parser.ir;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation for THROW statements.
 */
public final class ThrowNode extends Statement {
    /** Exception expression. */
    private final Expression expression;

    private final boolean isSyntheticRethrow;

    /**
     * Constructor
     *
     * @param lineNumber line number
     * @param token      token
     * @param finish     finish
     * @param expression expression to throw
     * @param isSyntheticRethrow true if this throw node is part of a synthetic rethrow.
     */
    public ThrowNode(final int lineNumber, final long token, final int finish, final Expression expression, final boolean isSyntheticRethrow) {
        super(lineNumber, token, finish);
        this.expression = expression;
        this.isSyntheticRethrow = isSyntheticRethrow;
    }

    private ThrowNode(final ThrowNode node, final Expression expression, final boolean isSyntheticRethrow) {
        super(node);
        this.expression = expression;
        this.isSyntheticRethrow = isSyntheticRethrow;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    /**
     * Assist in IR navigation.
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterThrowNode(this)) {
            return visitor.leaveThrowNode(setExpression((Expression)expression.accept(visitor)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterThrowNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        sb.append("throw ");

        if (expression != null) {
            expression.toString(sb, printType);
        }
    }

    /**
     * Get the expression that is being thrown by this node
     * @return expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Reset the expression being thrown by this node
     * @param expression new expression
     * @return new or same thrownode
     */
    public ThrowNode setExpression(final Expression expression) {
        if (this.expression == expression) {
            return this;
        }
        return new ThrowNode(this, expression, isSyntheticRethrow);
    }

    /**
     * Is this a throw a synthetic rethrow in a synthetic catch-all block
     * created when inlining finally statements? In that case we never
     * wrap whatever is thrown into an ECMAException, just rethrow it.
     * @return true if synthetic throw node
     */
    public boolean isSyntheticRethrow() {
        return isSyntheticRethrow;
    }

    @Override
    public boolean isCompletionValueNeverEmpty() {
        return true;
    }
}

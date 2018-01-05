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

import static com.oracle.js.parser.TokenType.BIT_NOT;
import static com.oracle.js.parser.TokenType.DECPOSTFIX;
import static com.oracle.js.parser.TokenType.INCPOSTFIX;

import com.oracle.js.parser.Token;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * UnaryNode nodes represent single operand operations.
 */
public final class UnaryNode extends Expression implements Assignment<Expression> {
    /** Right hand side argument. */
    private final Expression expression;

    /**
     * Constructor
     *
     * @param token  token
     * @param rhs    expression
     */
    public UnaryNode(final long token, final Expression rhs) {
        this(token, Math.min(rhs.getStart(), Token.descPosition(token)), Math.max(Token.descPosition(token) + Token.descLength(token), rhs.getFinish()), rhs);
    }

    /**
     * Constructor
     *
     * @param token      token
     * @param start      start
     * @param finish     finish
     * @param expression expression
     */
    public UnaryNode(final long token, final int start, final int finish, final Expression expression) {
        super(token, start, finish);
        this.expression   = expression;
    }


    private UnaryNode(final UnaryNode unaryNode, final Expression expression) {
        super(unaryNode);
        this.expression   = expression;
    }

    /**
     * Is this an assignment - i.e. that mutates something such as a++
     *
     * @return true if assignment
     */
    @Override
    public boolean isAssignment() {
        switch (tokenType()) {
        case DECPOSTFIX:
        case DECPREFIX:
        case INCPOSTFIX:
        case INCPREFIX:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isSelfModifying() {
        return isAssignment();
    }

    @Override
    public Expression getAssignmentDest() {
        return isAssignment() ? getExpression() : null;
    }

    @Override
    public Expression getAssignmentSource() {
        return getAssignmentDest();
    }

    /**
     * Assist in IR navigation.
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterUnaryNode(this)) {
            return visitor.leaveUnaryNode(setExpression((Expression)expression.accept(visitor)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterUnaryNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        final TokenType tokenType = tokenType();
        final String    name      = tokenType.getName();
        final boolean   isPostfix = tokenType == DECPOSTFIX || tokenType == INCPOSTFIX;

        if (tokenType == TokenType.AWAIT) {
            // await expression
            sb.append("await ");
        } else if (tokenType == TokenType.SPREAD_ARRAY || tokenType == TokenType.SPREAD_OBJECT) {
            sb.append("...");
        }

        boolean rhsParen = tokenType.needsParens(getExpression().tokenType(), false);

        if (!isPostfix) {
            if (name == null) {
                sb.append(tokenType.name());
                rhsParen = true;
            } else {
                sb.append(name);

                if (tokenType.ordinal() > BIT_NOT.ordinal()) {
                    sb.append(' ');
                }
            }
        }

        if (rhsParen) {
            sb.append('(');
        }

        getExpression().toString(sb, printType);

        if (rhsParen) {
            sb.append(')');
        }

        if (isPostfix) {
            sb.append(tokenType == DECPOSTFIX ? "--" : "++");
        }
    }

    /**
     * Get the right hand side of this if it is inherited by a binary expression,
     * or just the expression itself if still Unary
     *
     * @see BinaryNode
     *
     * @return right hand side or expression node
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Reset the right hand side of this if it is inherited by a binary expression,
     * or just the expression itself if still Unary
     *
     * @see BinaryNode
     *
     * @param expression right hand side or expression node
     * @return a node equivalent to this one except for the requested change.
     */
    public UnaryNode setExpression(final Expression expression) {
        if (this.expression == expression) {
            return this;
        }
        return new UnaryNode(this, expression);
    }
}

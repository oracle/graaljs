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

import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * BinaryNode nodes represent two operand operations.
 */
public final class BinaryNode extends Expression implements Assignment<Expression> {
    /** Left hand side argument. */
    private final Expression lhs;

    /** Right hand side argument. */
    private final Expression rhs;

    /**
     * Constructor
     *
     * @param token  token
     * @param lhs    left hand side
     * @param rhs    right hand side
     */
    public BinaryNode(final long token, final Expression lhs, final Expression rhs) {
        super(token, Math.min(lhs.getStart(), rhs.getStart()), Math.max(rhs.getFinish(), lhs.getFinish()));
        assert !(isTokenType(TokenType.AND) || isTokenType(TokenType.OR)) || lhs instanceof JoinPredecessorExpression;
        this.lhs   = lhs;
        this.rhs   = rhs;
    }

    private BinaryNode(final BinaryNode binaryNode, final Expression lhs, final Expression rhs) {
        super(binaryNode);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * Returns true if the node is a comparison operation (either equality, inequality, or relational).
     * @return true if the node is a comparison operation.
     */
    public boolean isComparison() {
        switch (tokenType()) {
        case EQ:
        case EQ_STRICT:
        case NE:
        case NE_STRICT:
        case LE:
        case LT:
        case GE:
        case GT:
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns true if the node is a relational operation (less than (or equals), greater than (or equals)).
     * @return true if the node is a relational operation.
     */
    public boolean isRelational() {
        switch (tokenType()) {
        case LT:
        case GT:
        case LE:
        case GE:
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns true if the node is a logical operation.
     * @return true if the node is a logical operation.
     */
    public boolean isLogical() {
        return isLogical(tokenType());
    }

    /**
     * Returns true if the token type represents a logical operation.
     * @param tokenType the token type
     * @return true if the token type represents a logical operation.
     */
    public static boolean isLogical(final TokenType tokenType) {
        switch (tokenType) {
        case AND:
        case OR:
            return true;
        default:
            return false;
        }
    }

    /**
     * Check if this node is an assignment
     *
     * @return true if this node assigns a value
     */
    @Override
    public boolean isAssignment() {
        return tokenType().isAssignment();
    }

    @Override
    public boolean isSelfModifying() {
        return isAssignment() && !isTokenType(TokenType.ASSIGN);
    }

    @Override
    public Expression getAssignmentDest() {
        return isAssignment() ? lhs() : null;
    }

    @Override
    public Expression getAssignmentSource() {
        return rhs();
    }

    /**
     * Assist in IR navigation.
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterBinaryNode(this)) {
            return visitor.leaveBinaryNode(setLHS((Expression)lhs.accept(visitor)).setRHS((Expression)rhs.accept(visitor)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterBinaryNode(this);
    }

    @Override
    public boolean isAlwaysFalse() {
        switch (tokenType()) {
        case COMMALEFT:
            return lhs.isAlwaysFalse();
        case COMMARIGHT:
            return rhs.isAlwaysFalse();
        default:
            return false;
        }
    }

    @Override
    public boolean isAlwaysTrue() {
        switch (tokenType()) {
        case COMMALEFT:
            return lhs.isAlwaysTrue();
        case COMMARIGHT:
            return rhs.isAlwaysTrue();
        default:
            return false;
        }
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        final TokenType tokenType = tokenType();

        final boolean lhsParen = tokenType.needsParens(lhs().tokenType(), true);
        final boolean rhsParen = tokenType.needsParens(rhs().tokenType(), false);

        if (lhsParen) {
            sb.append('(');
        }

        lhs().toString(sb, printType);

        if (lhsParen) {
            sb.append(')');
        }

        sb.append(' ');

        switch (tokenType) {
        case COMMALEFT:
            sb.append(",<");
            break;
        case COMMARIGHT:
            sb.append(",>");
            break;
        case INCPREFIX:
        case DECPREFIX:
            sb.append("++");
            break;
        default:
            sb.append(tokenType.getName());
            break;
        }

        sb.append(' ');

        if (rhsParen) {
            sb.append('(');
        }
        rhs().toString(sb, printType);
        if (rhsParen) {
            sb.append(')');
        }
    }

    /**
     * Get the left hand side expression for this node
     * @return the left hand side expression
     */
    public Expression lhs() {
        return lhs;
    }

    /**
     * Get the right hand side expression for this node
     * @return the left hand side expression
     */
    public Expression rhs() {
        return rhs;
    }

    /**
     * Set the left hand side expression for this node
     * @param lhs new left hand side expression
     * @return a node equivalent to this one except for the requested change.
     */
    public BinaryNode setLHS(final Expression lhs) {
        if (this.lhs == lhs) {
            return this;
        }
        return new BinaryNode(this, lhs, rhs);
    }

    /**
     * Set the right hand side expression for this node
     * @param rhs new left hand side expression
     * @return a node equivalent to this one except for the requested change.
     */
    public BinaryNode setRHS(final Expression rhs) {
        if (this.rhs == rhs) {
            return this;
        }
        return new BinaryNode(this, lhs, rhs);
    }
}

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

/**
 * IR representation for {@code return} statements.
 */
public class ReturnNode extends Statement {
    /** Optional expression. */
    private final Expression expression;

    private boolean inTerminalPosition;

    /**
     * Constructor
     *
     * @param lineNumber line number
     * @param token token
     * @param finish finish
     * @param expression expression to return
     */
    public ReturnNode(final int lineNumber, final long token, final int finish, final Expression expression) {
        super(lineNumber, token, finish);
        this.expression = expression;
    }

    private ReturnNode(final ReturnNode returnNode, final Expression expression) {
        super(returnNode);
        this.expression = expression;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterReturnNode(this)) {
            if (expression != null) {
                return visitor.leaveReturnNode(setExpression((Expression) expression.accept(visitor)));
            }
            return visitor.leaveReturnNode(this);
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterReturnNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        sb.append("return");
        if (expression != null) {
            sb.append(' ');
            expression.toString(sb, printType);
        }
    }

    /**
     * Get the expression this node returns
     *
     * @return return expression, or null if void return
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Reset the expression this node returns
     *
     * @param expression new expression, or null if void return
     * @return new or same return node
     */
    public ReturnNode setExpression(final Expression expression) {
        if (this.expression == expression) {
            return this;
        }
        return new ReturnNode(this, expression);
    }

    public boolean isInTerminalPosition() {
        return inTerminalPosition;
    }

    public void setInTerminalPosition(boolean inTerminalPosition) {
        this.inTerminalPosition = inTerminalPosition;
    }

    @Override
    public boolean isCompletionValueNeverEmpty() {
        return true;
    }
}

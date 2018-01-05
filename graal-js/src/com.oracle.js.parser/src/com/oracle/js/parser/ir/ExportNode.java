/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

public class ExportNode extends Node {

    private final ExportClauseNode exportClause;

    private final FromNode from;

    private final VarNode var;

    private final Expression expression;

    private final boolean isDefault;

    public ExportNode(final long token, final int start, final int finish, final ExportClauseNode exportClause) {
        this(token, start, finish, exportClause, null, null, null, false);
    }

    public ExportNode(final long token, final int start, final int finish, final FromNode from) {
        this(token, start, finish, null, from, null, null, false);
    }

    public ExportNode(final long token, final int start, final int finish, final ExportClauseNode exportClause, final FromNode from) {
        this(token, start, finish, exportClause, from, null, null, false);
    }

    public ExportNode(final long token, final int start, final int finish, final Expression expression, final boolean isDefault) {
        this(token, start, finish, null, null, null, expression, isDefault);
    }

    public ExportNode(final long token, final int start, final int finish, final VarNode var) {
        this(token, start, finish, null, null, var, null, false);
    }

    private ExportNode(final long token, final int start, final int finish, final ExportClauseNode exportClause,
                    final FromNode from, final VarNode var, final Expression expression, final boolean isDefault) {
        super(token, start, finish);
        this.exportClause = exportClause;
        this.from = from;
        this.var = var;
        this.expression = expression;
        this.isDefault = isDefault;
    }

    private ExportNode(final ExportNode node, final ExportClauseNode exportClause,
                    final FromNode from, final VarNode var, final Expression expression) {
        super(node);
        this.isDefault = node.isDefault;

        this.exportClause = exportClause;
        this.from = from;
        this.var = var;
        this.expression = expression;
    }

    public ExportClauseNode getExportClause() {
        return exportClause;
    }

    public FromNode getFrom() {
        return from;
    }

    public VarNode getVar() {
        return var;
    }

    public Expression getExpression() {
        return expression;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public ExportNode setExportClause(ExportClauseNode exportClause) {
        if (this.exportClause == exportClause) {
            return this;
        }
        return new ExportNode(this, exportClause, from, var, expression);
    }

    public ExportNode setFrom(FromNode from) {
        if (this.from == from) {
            return this;
        }
        return new ExportNode(this, exportClause, from, var, expression);
    }

    public ExportNode setVar(VarNode var) {
        if (this.var == var) {
            return this;
        }
        return new ExportNode(this, exportClause, from, var, expression);
    }

    public ExportNode setExpression(Expression expression) {
        if (this.expression == expression) {
            return this;
        }
        return new ExportNode(this, exportClause, from, var, expression);
    }

    @Override
    public Node accept(NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterExportNode(this)) {
            ExportClauseNode newExportClause = exportClause == null ? null
                            : (ExportClauseNode) exportClause.accept(visitor);
            FromNode newFrom = from == null ? null
                            : (FromNode) from.accept(visitor);
            VarNode newVar = var == null ? null
                            : (VarNode) var.accept(visitor);
            Expression newExpression = expression == null ? null
                            : (Expression) expression.accept(visitor);
            return visitor.leaveExportNode(
                            setExportClause(newExportClause).setFrom(newFrom).setVar(newVar).setExpression(newExpression));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterExportNode(this);
    }

    @Override
    public void toString(StringBuilder sb, boolean printType) {
        sb.append("export ");
        if (isDefault) {
            sb.append("default ");
        }
        if (expression != null) {
            expression.toString(sb, printType);
            if (expression.isAssignment()) {
                sb.append(';');
            }
        } else {
            if (exportClause == null) {
                sb.append("* ");
            } else {
                exportClause.toString(sb, printType);
            }
            if (from != null) {
                from.toString(sb, printType);
            }
            sb.append(';');
        }
    }

}

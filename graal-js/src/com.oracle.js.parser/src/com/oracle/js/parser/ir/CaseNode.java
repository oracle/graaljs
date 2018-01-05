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

import java.util.List;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation of {@code case} clause.
 * Case nodes are not {@link BreakableNode}s, but the {@link SwitchNode} is.
 */
public final class CaseNode extends Node implements Terminal {
    /** Test expression. */
    private final Expression test;

    /** Statements. */
    protected final List<Statement> statements;

    private final boolean terminal;

    /**
     * Constructors
     *
     * @param token    token
     * @param finish   finish
     * @param test     case test node, can be any node in JavaScript
     * @param statements case body statements
     */
    public CaseNode(final long token, final int finish, final Expression test, List<Statement> statements) {
        super(token, finish);

        this.test  = test;
        this.statements  = statements;
        this.terminal = isTerminal(statements);
    }

    CaseNode(final CaseNode caseNode, final int finish, final Expression test, final List<Statement> statements) {
        super(caseNode, finish);

        this.test  = test;
        this.statements = statements;
        this.terminal = isTerminal(statements);
    }

    private static boolean isTerminal(List<Statement> statements) {
        return statements.isEmpty() ? false : statements.get(statements.size() - 1).hasTerminalFlags();
    }

    /**
     * Assist in IR navigation.
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterCaseNode(this)) {
            final Expression newTest = test == null ? null : (Expression)test.accept(visitor);
            List<Statement> newStatements = Node.accept(visitor, statements);
            return visitor.leaveCaseNode(setTest(newTest).setStatements(newStatements));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterCaseNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printTypes) {
        if (test != null) {
            sb.append("case ");
            test.toString(sb, printTypes);
            sb.append(':');
        } else {
            sb.append("default:");
        }
    }

    @Override
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Get the body for this case node
     * @return the body
     */
    public List<Statement> getStatements() {
        return statements;
    }

    /**
     * Get the test expression for this case node
     * @return the test
     */
    public Expression getTest() {
        return test;
    }

    /**
     * Reset the test expression for this case node
     * @param test new test expression
     * @return new or same CaseNode
     */
    public CaseNode setTest(final Expression test) {
        if (this.test == test) {
            return this;
        }
        return new CaseNode(this, finish, test, statements);
    }

    public CaseNode setStatements(final List<Statement> statements) {
        if (this.statements == statements) {
            return this;
        }
        int lastFinish = 0;
        if (!statements.isEmpty()) {
            lastFinish = statements.get(statements.size() - 1).getFinish();
        }
        return new CaseNode(this, lastFinish, test, statements);
    }
}

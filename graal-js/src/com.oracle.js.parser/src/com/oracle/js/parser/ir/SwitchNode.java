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

import java.util.Collections;
import java.util.List;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation of a SWITCH statement.
 */
public final class SwitchNode extends BreakableStatement {
    /** Switch expression. */
    private final Expression expression;

    /** Switch cases. */
    private final List<CaseNode> cases;

    /** Switch default index. */
    private final int defaultCaseIndex;

    /** Tag symbol. */
    private Symbol tag;

    /**
     * Constructor
     *
     * @param lineNumber  lineNumber
     * @param token       token
     * @param finish      finish
     * @param expression  switch expression
     * @param cases       cases
     * @param defaultCaseIndex the default case index; -1 if none, otherwise has to be present in cases list
     */
    public SwitchNode(final int lineNumber, final long token, final int finish, final Expression expression, final List<CaseNode> cases, final int defaultCaseIndex) {
        super(lineNumber, token, finish);
        this.expression       = expression;
        this.cases            = cases;
        this.defaultCaseIndex = defaultCaseIndex;
        assert defaultCaseIndex == -1 || cases.get(defaultCaseIndex).getTest() == null;
    }

    private SwitchNode(final SwitchNode switchNode, final Expression expression, final List<CaseNode> cases, final int defaultCaseIndex) {
        super(switchNode);
        this.expression       = expression;
        this.cases            = cases;
        this.defaultCaseIndex = defaultCaseIndex;
        this.tag              = switchNode.getTag(); //TODO are symbols inhereted as references?
    }

    @Override
    public boolean isTerminal() {
        //there must be a default case, and that including all other cases must terminate
        if (!cases.isEmpty() && defaultCaseIndex != -1) {
            for (final CaseNode caseNode : cases) {
                if (!caseNode.isTerminal()) {
                    return false;
                }
            }
            return true;
        }
        return false;

    }

    @Override
    public Node accept(final LexicalContext lc, final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterSwitchNode(this)) {
            return visitor.leaveSwitchNode(
                setExpression(lc, (Expression)expression.accept(visitor)).
                setCases(lc, Node.accept(visitor, cases), defaultCaseIndex));
        }

        return this;
    }

    @Override
    public <R> R accept(LexicalContext lc, TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterSwitchNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        sb.append("switch (");
        expression.toString(sb, printType);
        sb.append(')');
    }

    /**
     * Return the case node that is default case
     * @return default case or null if none
     */
    public CaseNode getDefaultCase() {
        return defaultCaseIndex == -1 ? null : cases.get(defaultCaseIndex);
    }

    /**
     * Get the cases in this switch
     * @return a list of case nodes
     */
    public List<CaseNode> getCases() {
        return Collections.unmodifiableList(cases);
    }

    private SwitchNode setCases(final LexicalContext lc, final List<CaseNode> cases, final int defaultCaseIndex) {
        if (this.cases == cases) {
            return this;
        }
        return Node.replaceInLexicalContext(lc, this, new SwitchNode(this, expression, cases, defaultCaseIndex));
    }

    /**
     * Return the expression to switch on
     * @return switch expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Set or reset the expression to switch on
     * @param lc lexical context
     * @param expression switch expression
     * @return new switch node or same if no state was changed
     */
    public SwitchNode setExpression(final LexicalContext lc, final Expression expression) {
        if (this.expression == expression) {
            return this;
        }
        return Node.replaceInLexicalContext(lc, this, new SwitchNode(this, expression, cases, defaultCaseIndex));
    }

    /**
     * Get the tag symbol for this switch. The tag symbol is where
     * the switch expression result is stored
     * @return tag symbol
     */
    public Symbol getTag() {
        return tag;
    }

    /**
     * Set the tag symbol for this switch. The tag symbol is where
     * the switch expression result is stored
     * @param tag a symbol
     */
    public void setTag(final Symbol tag) {
        this.tag = tag;
    }

    /**
     * @return true if this switch has a default case.
     */
    public boolean hasDefaultCase() {
        return defaultCaseIndex != -1;
    }

    @Override
    public boolean isCompletionValueNeverEmpty() {
        return true;
    }
}

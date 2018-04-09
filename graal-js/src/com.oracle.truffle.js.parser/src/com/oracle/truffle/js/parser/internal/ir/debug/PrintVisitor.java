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
package com.oracle.truffle.js.parser.internal.ir.debug;

import static com.oracle.js.parser.TokenType.BIT_NOT;
import static com.oracle.js.parser.TokenType.DECPOSTFIX;
import static com.oracle.js.parser.TokenType.INCPOSTFIX;

import java.util.List;

import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.BinaryNode;
import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.BlockStatement;
import com.oracle.js.parser.ir.BreakNode;
import com.oracle.js.parser.ir.CaseNode;
import com.oracle.js.parser.ir.CatchNode;
import com.oracle.js.parser.ir.ContinueNode;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.ForNode;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.IfNode;
import com.oracle.js.parser.ir.JoinPredecessorExpression;
import com.oracle.js.parser.ir.LabelNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.Node;
import com.oracle.js.parser.ir.Statement;
import com.oracle.js.parser.ir.SwitchNode;
import com.oracle.js.parser.ir.ThrowNode;
import com.oracle.js.parser.ir.TryNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.js.parser.ir.WhileNode;
import com.oracle.js.parser.ir.WithNode;
import com.oracle.js.parser.ir.visitor.NodeVisitor;

/**
 * Print out the AST as human readable source code. This works both on lowered and unlowered ASTs
 *
 * see the flags --print-parse and --print-lower-parse
 */
public final class PrintVisitor extends NodeVisitor<LexicalContext> {
    /** Tab width. */
    private static final int TABWIDTH = 4;

    /** Composing buffer. */
    private final StringBuilder sb;

    /** Indentation factor. */
    private int indent;

    /** Line separator. */
    private static final String EOLN = "\n";

    /** Print line numbers. */
    private final boolean printLineNumbers;

    /** Print inferred and optimistic types. */
    private final boolean printTypes;

    private int lastLineNumber = -1;

    /**
     * Constructor.
     */
    public PrintVisitor() {
        this(true, true);
    }

    /**
     * Constructor.
     *
     * @param printLineNumbers should line number nodes be included in the output?
     * @param printTypes should we print optimistic and inferred types?
     */
    public PrintVisitor(final boolean printLineNumbers, final boolean printTypes) {
        super(new LexicalContext());
        this.sb = new StringBuilder();
        this.printLineNumbers = printLineNumbers;
        this.printTypes = printTypes;
    }

    /**
     * Constructor.
     *
     * @param root a node from which to start printing code
     */
    public PrintVisitor(final Node root) {
        this(root, true, true);
    }

    /**
     * Constructor.
     *
     * @param root a node from which to start printing code
     * @param printLineNumbers should line numbers nodes be included in the output?
     * @param printTypes should we print optimistic and inferred types?
     */
    public PrintVisitor(final Node root, final boolean printLineNumbers, final boolean printTypes) {
        this(printLineNumbers, printTypes);
        visit(root);
    }

    private void visit(final Node root) {
        root.accept(this);
    }

    @Override
    public String toString() {
        return sb.append(EOLN).toString();
    }

    /**
     * Insert spaces before a statement.
     */
    private void indent() {
        for (int i = indent; i > 0; i--) {
            sb.append(' ');
        }
    }

    /*
     * Visits.
     */

    @Override
    public boolean enterDefault(final Node node) {
        node.toString(sb, printTypes);
        return false;
    }

    @Override
    public boolean enterContinueNode(final ContinueNode node) {
        node.toString(sb, printTypes);
        return false;
    }

    @Override
    public boolean enterBreakNode(final BreakNode node) {
        node.toString(sb, printTypes);
        return false;
    }

    @Override
    public boolean enterThrowNode(final ThrowNode node) {
        node.toString(sb, printTypes);
        return false;
    }

    @Override
    public boolean enterBlock(final Block block) {
        sb.append(' ');
        sb.append('{');

        indent += TABWIDTH;

        final List<Statement> statements = block.getStatements();
        printStatements(statements);

        indent -= TABWIDTH;

        sb.append(EOLN);
        indent();

        sb.append('}');

        return false;
    }

    private void printStatements(final List<Statement> statements) {
        for (final Statement statement : statements) {
            if (printLineNumbers) {
                final int lineNumber = statement.getLineNumber();
                sb.append(EOLN);
                if (lineNumber != lastLineNumber) {
                    indent();
                    sb.append("[|").append(lineNumber).append("|];").append(EOLN);
                }
                lastLineNumber = lineNumber;
            }
            indent();

            statement.accept(this);

            int lastIndex = sb.length() - 1;
            char lastChar = sb.charAt(lastIndex);
            while (Character.isWhitespace(lastChar) && lastIndex >= 0) {
                lastChar = sb.charAt(--lastIndex);
            }

            if (lastChar != '}' && lastChar != ';') {
                sb.append(';');
            }

            if (statement.hasGoto()) {
                sb.append(" [GOTO]");
            }

            if (statement.isTerminal()) {
                sb.append(" [TERMINAL]");
            }
        }
    }

    @Override
    public boolean enterBlockStatement(final BlockStatement statement) {
        statement.getBlock().accept(this);
        return false;
    }

    @Override
    public boolean enterBinaryNode(final BinaryNode binaryNode) {
        binaryNode.lhs().accept(this);
        sb.append(' ');
        sb.append(binaryNode.tokenType());
        sb.append(' ');
        binaryNode.rhs().accept(this);
        return false;
    }

    @Override
    public boolean enterJoinPredecessorExpression(final JoinPredecessorExpression expr) {
        expr.getExpression().accept(this);
        return false;
    }

    @Override
    public boolean enterIdentNode(final IdentNode identNode) {
        identNode.toString(sb, printTypes);
        return true;
    }

    @Override
    public boolean enterUnaryNode(final UnaryNode unaryNode) {
        final TokenType tokenType = unaryNode.tokenType();
        final String name = tokenType.getName();
        final boolean isPostfix = tokenType == DECPOSTFIX || tokenType == INCPOSTFIX;

        boolean rhsParen = tokenType.needsParens(unaryNode.getExpression().tokenType(), false);

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

        unaryNode.getExpression().toString(sb, printTypes);

        if (rhsParen) {
            sb.append(')');
        }

        if (isPostfix) {
            sb.append(tokenType == DECPOSTFIX ? "--" : "++");
        }
        return false;
    }

    @Override
    public boolean enterExpressionStatement(final ExpressionStatement expressionStatement) {
        expressionStatement.getExpression().accept(this);
        return false;
    }

    @Override
    public boolean enterForNode(final ForNode forNode) {
        forNode.toString(sb, printTypes);
        forNode.getBody().accept(this);
        return false;
    }

    @Override
    public boolean enterFunctionNode(final FunctionNode functionNode) {
        functionNode.toString(sb, printTypes);
        enterBlock(functionNode.getBody());
        return false;
    }

    @Override
    public boolean enterIfNode(final IfNode ifNode) {
        ifNode.toString(sb, printTypes);
        ifNode.getPass().accept(this);

        final Block fail = ifNode.getFail();

        if (fail != null) {
            sb.append(" else ");
            fail.accept(this);
        }
        return false;
    }

    @Override
    public boolean enterLabelNode(final LabelNode labeledNode) {
        indent -= TABWIDTH;
        indent();
        indent += TABWIDTH;
        labeledNode.toString(sb, printTypes);
        labeledNode.getBody().accept(this);
        return false;
    }

    @Override
    public boolean enterSwitchNode(final SwitchNode switchNode) {
        switchNode.toString(sb, printTypes);
        sb.append(" {");

        final List<CaseNode> cases = switchNode.getCases();

        for (final CaseNode caseNode : cases) {
            sb.append(EOLN);
            indent();
            caseNode.toString(sb, printTypes);
            indent += TABWIDTH;
            printStatements(caseNode.getStatements());
            indent -= TABWIDTH;
        }
        sb.append(EOLN);
        indent();
        sb.append("}");

        return false;
    }

    @Override
    public boolean enterTryNode(final TryNode tryNode) {
        tryNode.toString(sb, printTypes);
        tryNode.getBody().accept(this);

        for (final CatchNode catchNode : tryNode.getCatches()) {
            catchNode.toString(sb, printTypes);
            catchNode.getBody().accept(this);
        }

        final Block finallyBody = tryNode.getFinallyBody();

        if (finallyBody != null) {
            sb.append(" finally ");
            finallyBody.accept(this);
        }
        return false;
    }

    @Override
    public boolean enterVarNode(final VarNode varNode) {
        sb.append(varNode.tokenType().getName()).append(' ');
        varNode.getName().toString(sb, printTypes);
        final Node init = varNode.getInit();
        if (init != null) {
            sb.append(" = ");
            init.accept(this);
        }

        return false;
    }

    @Override
    public boolean enterWhileNode(final WhileNode whileNode) {
        if (whileNode.isDoWhile()) {
            sb.append("do");
            whileNode.getBody().accept(this);
            sb.append(' ');
            whileNode.toString(sb, printTypes);
        } else {
            whileNode.toString(sb, printTypes);
            whileNode.getBody().accept(this);
        }

        return false;
    }

    @Override
    public boolean enterWithNode(final WithNode withNode) {
        withNode.toString(sb, printTypes);
        withNode.getBody().accept(this);

        return false;
    }

}

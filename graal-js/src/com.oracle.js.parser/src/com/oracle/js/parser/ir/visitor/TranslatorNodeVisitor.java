/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser.ir.visitor;

import com.oracle.js.parser.ir.AccessNode;
import com.oracle.js.parser.ir.BinaryNode;
import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.BlockStatement;
import com.oracle.js.parser.ir.BreakNode;
import com.oracle.js.parser.ir.CallNode;
import com.oracle.js.parser.ir.CaseNode;
import com.oracle.js.parser.ir.CatchNode;
import com.oracle.js.parser.ir.ClassNode;
import com.oracle.js.parser.ir.BlockExpression;
import com.oracle.js.parser.ir.ContinueNode;
import com.oracle.js.parser.ir.DebuggerNode;
import com.oracle.js.parser.ir.EmptyNode;
import com.oracle.js.parser.ir.ErrorNode;
import com.oracle.js.parser.ir.ExportClauseNode;
import com.oracle.js.parser.ir.ExportNode;
import com.oracle.js.parser.ir.ExportSpecifierNode;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.ForNode;
import com.oracle.js.parser.ir.FromNode;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.IfNode;
import com.oracle.js.parser.ir.ImportClauseNode;
import com.oracle.js.parser.ir.ImportNode;
import com.oracle.js.parser.ir.ImportSpecifierNode;
import com.oracle.js.parser.ir.IndexNode;
import com.oracle.js.parser.ir.JoinPredecessorExpression;
import com.oracle.js.parser.ir.LabelNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.NameSpaceImportNode;
import com.oracle.js.parser.ir.NamedImportsNode;
import com.oracle.js.parser.ir.Node;
import com.oracle.js.parser.ir.ObjectNode;
import com.oracle.js.parser.ir.ParameterNode;
import com.oracle.js.parser.ir.PropertyNode;
import com.oracle.js.parser.ir.ReturnNode;
import com.oracle.js.parser.ir.RuntimeNode;
import com.oracle.js.parser.ir.SwitchNode;
import com.oracle.js.parser.ir.TernaryNode;
import com.oracle.js.parser.ir.ThrowNode;
import com.oracle.js.parser.ir.TryNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.js.parser.ir.WhileNode;
import com.oracle.js.parser.ir.WithNode;

// @formatter:off
/**
 * Visitor used to navigate the IR.
 * @param <T> lexical context class used by this visitor
 * @param <R> return type
 */
public abstract class TranslatorNodeVisitor<T extends LexicalContext, R> {
    /** lexical context in use */
    protected final T lc;

    /**
     * Constructor
     *
     * @param lc a custom lexical context
     */
    public TranslatorNodeVisitor(final T lc) {
        this.lc = lc;
    }

    /**
     * Get the lexical context of this node visitor
     * @return lexical context
     */
    public final T getLexicalContext() {
        return lc;
    }

    /**
     * Override this method to do a double inheritance pattern, e.g. avoid
     * using
     * <p>
     * if (x instanceof NodeTypeA) {
     *    ...
     * } else if (x instanceof NodeTypeB) {
     *    ...
     * } else {
     *    ...
     * }
     * <p>
     * Use a NodeVisitor instead, and this method contents forms the else case.
     *
     * @param node the node to visit
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    protected R enterDefault(final Node node) {
        throw new AssertionError(String.format("should not reach here. %s(%s)", node.getClass().getSimpleName(), node));
    }

    /**
     * Callback for entering an AccessNode
     *
     * @param  accessNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterAccessNode(final AccessNode accessNode) {
        return enterDefault(accessNode);
    }

    /**
     * Callback for entering a Block
     *
     * @param  block     the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterBlock(final Block block) {
        return enterDefault(block);
    }

    /**
     * Callback for entering a BinaryNode
     *
     * @param  binaryNode  the node
     * @return processed   node
     */
    public R enterBinaryNode(final BinaryNode binaryNode) {
        return enterDefault(binaryNode);
    }

    /**
     * Callback for entering a BreakNode
     *
     * @param  breakNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterBreakNode(final BreakNode breakNode) {
        return enterDefault(breakNode);
    }

    /**
     * Callback for entering a CallNode
     *
     * @param  callNode  the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterCallNode(final CallNode callNode) {
        return enterDefault(callNode);
    }

    /**
     * Callback for entering a CaseNode
     *
     * @param  caseNode  the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterCaseNode(final CaseNode caseNode) {
        return enterDefault(caseNode);
    }

    /**
     * Callback for entering a CatchNode
     *
     * @param  catchNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterCatchNode(final CatchNode catchNode) {
        return enterDefault(catchNode);
    }

    /**
     * Callback for entering a ContinueNode
     *
     * @param  continueNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterContinueNode(final ContinueNode continueNode) {
        return enterDefault(continueNode);
    }

    /**
     * Callback for entering a DebuggerNode
     *
     * @param  debuggerNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterDebuggerNode(final DebuggerNode debuggerNode) {
        return enterDefault(debuggerNode);
    }

    /**
     * Callback for entering an EmptyNode
     *
     * @param  emptyNode   the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterEmptyNode(final EmptyNode emptyNode) {
        return enterDefault(emptyNode);
    }

    /**
     * Callback for entering an ErrorNode
     *
     * @param  errorNode   the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterErrorNode(final ErrorNode errorNode) {
        return enterDefault(errorNode);
    }

    public R enterExportClauseNode(final ExportClauseNode exportClauseNode) {
        return enterDefault(exportClauseNode);
    }

    public R enterExportNode(final ExportNode exportNode) {
        return enterDefault(exportNode);
    }

    public R enterExportSpecifierNode(final ExportSpecifierNode exportSpecifierNode) {
        return enterDefault(exportSpecifierNode);
    }

    /**
     * Callback for entering an ExpressionStatement
     *
     * @param  expressionStatement the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterExpressionStatement(final ExpressionStatement expressionStatement) {
        return enterDefault(expressionStatement);
    }

    /**
     * Callback for entering a BlockStatement
     *
     * @param  blockStatement the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterBlockStatement(final BlockStatement blockStatement) {
        return enterDefault(blockStatement);
    }

    /**
     * Callback for entering a ForNode
     *
     * @param  forNode   the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterForNode(final ForNode forNode) {
        return enterDefault(forNode);
    }

    public R enterFromNode(final FromNode fromNode) {
        return enterDefault(fromNode);
    }

    /**
     * Callback for entering a FunctionNode
     *
     * @param  functionNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterFunctionNode(final FunctionNode functionNode) {
        return enterDefault(functionNode);
    }

    /**
     * Callback for entering an IdentNode
     *
     * @param  identNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterIdentNode(final IdentNode identNode) {
        return enterDefault(identNode);
    }

    /**
     * Callback for entering an IfNode
     *
     * @param  ifNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterIfNode(final IfNode ifNode) {
        return enterDefault(ifNode);
    }

    public R enterImportClauseNode(final ImportClauseNode importClauseNode) {
        return enterDefault(importClauseNode);
    }

    public R enterImportNode(final ImportNode importNode) {
        return enterDefault(importNode);
    }

    public R enterImportSpecifierNode(final ImportSpecifierNode importSpecifierNode) {
        return enterDefault(importSpecifierNode);
    }

    /**
     * Callback for entering an IndexNode
     *
     * @param  indexNode  the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterIndexNode(final IndexNode indexNode) {
        return enterDefault(indexNode);
    }

    /**
     * Callback for entering a LabelNode
     *
     * @param  labelNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterLabelNode(final LabelNode labelNode) {
        return enterDefault(labelNode);
    }

    /**
     * Callback for entering a LiteralNode
     *
     * @param  literalNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterLiteralNode(final LiteralNode<?> literalNode) {
        return enterDefault(literalNode);
    }

    public R enterNameSpaceImportNode(final NameSpaceImportNode nameSpaceImportNode) {
        return enterDefault(nameSpaceImportNode);
    }

    public R enterNamedImportsNode(final NamedImportsNode namedImportsNode) {
        return enterDefault(namedImportsNode);
    }

    /**
     * Callback for entering an ObjectNode
     *
     * @param  objectNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterObjectNode(final ObjectNode objectNode) {
        return enterDefault(objectNode);
    }

    /**
     * Callback for entering a PropertyNode
     *
     * @param  propertyNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterPropertyNode(final PropertyNode propertyNode) {
        return enterDefault(propertyNode);
    }

    /**
     * Callback for entering a ReturnNode
     *
     * @param  returnNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterReturnNode(final ReturnNode returnNode) {
        return enterDefault(returnNode);
    }

    /**
     * Callback for entering a RuntimeNode
     *
     * @param  runtimeNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterRuntimeNode(final RuntimeNode runtimeNode) {
        return enterDefault(runtimeNode);
    }

    /**
     * Callback for entering a SwitchNode
     *
     * @param  switchNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterSwitchNode(final SwitchNode switchNode) {
        return enterDefault(switchNode);
    }

    /**
     * Callback for entering a TernaryNode
     *
     * @param  ternaryNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterTernaryNode(final TernaryNode ternaryNode) {
        return enterDefault(ternaryNode);
    }

    /**
     * Callback for entering a ThrowNode
     *
     * @param  throwNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterThrowNode(final ThrowNode throwNode) {
        return enterDefault(throwNode);
    }

    /**
     * Callback for entering a TryNode
     *
     * @param  tryNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterTryNode(final TryNode tryNode) {
        return enterDefault(tryNode);
    }

    /**
     * Callback for entering a UnaryNode
     *
     * @param  unaryNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterUnaryNode(final UnaryNode unaryNode) {
        return enterDefault(unaryNode);
    }

    /**
     * Callback for entering a {@link JoinPredecessorExpression}.
     *
     * @param  expr the join predecessor expression
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterJoinPredecessorExpression(final JoinPredecessorExpression expr) {
        return enterDefault(expr);
    }

    /**
     * Callback for entering a VarNode
     *
     * @param  varNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterVarNode(final VarNode varNode) {
        return enterDefault(varNode);
    }

    /**
     * Callback for entering a WhileNode
     *
     * @param  whileNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterWhileNode(final WhileNode whileNode) {
        return enterDefault(whileNode);
    }

    /**
     * Callback for entering a WithNode
     *
     * @param  withNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterWithNode(final WithNode withNode) {
        return enterDefault(withNode);
    }

    /**
     * Callback for entering a ClassNode
     *
     * @param  classNode the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterClassNode(ClassNode classNode) {
        return enterDefault(classNode);
    }

    /**
     * Callback for entering a BlockExpression
     *
     * @param  blockExpression the node
     * @return true if traversal should continue and node children be traversed, false otherwise
     */
    public R enterBlockExpression(BlockExpression blockExpression) {
        return enterDefault(blockExpression);
    }

    /**
     * Callback for entering a ParameterNode
     *
     * @param  paramNode the node
     */
    public R enterParameterNode(final ParameterNode paramNode) {
        return enterDefault(paramNode);
    }
}

/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.ArrayList;
import java.util.Arrays;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class AbstractBlockNode extends StatementNode implements SequenceNode, ResumableNode {
    @Children protected final JavaScriptNode[] statements;

    protected AbstractBlockNode(JavaScriptNode[] statements) {
        this.statements = statements;
    }

    @Override
    public final JavaScriptNode[] getStatements() {
        return statements;
    }

    @ExplodeLoop
    @Override
    public final void executeVoid(VirtualFrame frame) {
        JavaScriptNode[] stmts = statements;
        for (int i = 0; i < stmts.length; ++i) {
            stmts[i].executeVoid(frame);
        }
    }

    /**
     * Filter out empty statements, unwrap void nodes, and inline block nodes.
     *
     * If creating an expression block, for the last statement, only inline expression block nodes.
     */
    protected static JavaScriptNode filterStatements(JavaScriptNode[] originalStatements, boolean exprBlock) {
        ArrayList<JavaScriptNode> filteredStatements = null;
        boolean returnExprBlock = exprBlock;
        for (int i = 0; i < originalStatements.length; i++) {
            JavaScriptNode statement = originalStatements[i];
            if (statement instanceof EmptyNode || statement instanceof AbstractBlockNode || statement instanceof VoidNode || statement instanceof JSConstantUndefinedNode) {
                if (filteredStatements == null) {
                    filteredStatements = newListFromRange(originalStatements, 0, i);
                }
                if (statement instanceof AbstractBlockNode) {
                    filteredStatements.addAll(Arrays.asList(((AbstractBlockNode) statement).getStatements()));
                } else if (statement instanceof VoidNode) {
                    VoidNode voidNode = (VoidNode) statement;
                    filteredStatements.add(voidNode.getOperand());
                    transferSourceSection(statement, voidNode.getOperand());
                } else {
                    assert statement instanceof EmptyNode || statement instanceof JSConstantUndefinedNode;
                }
                if (exprBlock && i == originalStatements.length - 1 && !(statement instanceof ExprBlockNode || statement instanceof EmptyNode)) {
                    // last statement would have returned undefined, so we need a void block node
                    returnExprBlock = false;
                }
            } else {
                if (filteredStatements != null) {
                    filteredStatements.add(statement);
                }
            }
        }

        JavaScriptNode[] finalStatements = filteredStatements == null ? originalStatements : filteredStatements.toArray(new JavaScriptNode[filteredStatements.size()]);

        if (returnExprBlock) {
            if (finalStatements.length == 0) {
                return new EmptyNode();
            } else if (finalStatements.length == 1) {
                return finalStatements[0];
            } else {
                return new ExprBlockNode(finalStatements);
            }
        } else {
            if (finalStatements.length == 0) {
                return JSConstantNode.createUndefined();
            } else if (finalStatements.length == 1) {
                return VoidNode.create(finalStatements[0]);
            } else {
                return new BlockNode(finalStatements);
            }
        }
    }

    protected static ArrayList<JavaScriptNode> newListFromRange(JavaScriptNode[] statements, int from, int to) {
        return new ArrayList<>(Arrays.asList(statements).subList(from, to));
    }
}

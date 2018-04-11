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

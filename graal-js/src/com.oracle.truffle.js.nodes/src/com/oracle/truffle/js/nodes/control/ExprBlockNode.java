/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;

@NodeInfo(cost = NodeCost.NONE)
public final class ExprBlockNode extends AbstractBlockNode implements SequenceNode, ResumableNode {
    ExprBlockNode(JavaScriptNode[] statements) {
        super(statements);
        assert statements.length >= 1 : "block must contain at least 1 statement";
    }

    public static JavaScriptNode createExprBlock(JavaScriptNode[] statements) {
        return filterStatements(statements, true);
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].execute(frame);
    }

    @ExplodeLoop
    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].executeBoolean(frame);
    }

    @ExplodeLoop
    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].executeInt(frame);
    }

    @ExplodeLoop
    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].executeDouble(frame);
    }

    @ExplodeLoop
    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsIntAndReset(frame);
        JavaScriptNode[] stmts = statements;
        assert index < stmts.length;
        int last = stmts.length - 1;
        for (int i = 0; i < stmts.length; ++i) {
            if (i < index) {
                continue;
            }
            try {
                if (i != last) {
                    stmts[i].executeVoid(frame);
                } else {
                    return stmts[last].execute(frame);
                }
            } catch (YieldException e) {
                setState(frame, i);
                throw e;
            }
        }
        throw Errors.shouldNotReachHere();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new ExprBlockNode(cloneUninitialized(statements));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return statements[statements.length - 1].isResultAlwaysOfType(clazz);
    }
}

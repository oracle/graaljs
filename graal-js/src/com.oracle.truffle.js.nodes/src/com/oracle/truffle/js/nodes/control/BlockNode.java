/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

@NodeInfo(cost = NodeCost.NONE)
public final class BlockNode extends AbstractBlockNode implements SequenceNode, ResumableNode {
    BlockNode(JavaScriptNode[] statements) {
        super(statements);
    }

    public static JavaScriptNode createVoidBlock(JavaScriptNode... statements) {
        return filterStatements(statements, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return EMPTY;
    }

    @ExplodeLoop
    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsIntAndReset(frame);
        JavaScriptNode[] stmts = statements;
        for (int i = 0; i < stmts.length; i++) {
            if (i < index) {
                continue;
            }
            try {
                stmts[i].executeVoid(frame);
            } catch (YieldException e) {
                setState(frame, i);
                throw e;
            }
        }
        return EMPTY;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new BlockNode(cloneUninitialized(statements));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }
}

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.AbstractBlockNode;
import com.oracle.truffle.js.nodes.control.BlockNode;
import com.oracle.truffle.js.nodes.control.ExprBlockNode;
import com.oracle.truffle.js.nodes.control.ResumableNode;
import com.oracle.truffle.js.nodes.control.SequenceNode;
import com.oracle.truffle.js.nodes.control.YieldException;

@NodeInfo(cost = NodeCost.NONE)
public class DualNode extends JavaScriptNode implements SequenceNode, ResumableNode {

    @Child private JavaScriptNode left;
    @Child private JavaScriptNode right;

    public DualNode(JavaScriptNode left, JavaScriptNode right) {
        this.left = left;
        this.right = right;
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        if (left instanceof DualNode || left instanceof AbstractBlockNode || right instanceof DualNode || right instanceof AbstractBlockNode) {
            final int len = getLen(left) + getLen(right);
            JavaScriptNode[] arr = new JavaScriptNode[len];
            int pos = flatten(arr, 0, left);
            flatten(arr, pos, right);
            return right instanceof BlockNode ? BlockNode.createVoidBlock(arr) : ExprBlockNode.createExprBlock(arr);
        }
        return new DualNode(left, right);
    }

    private static int flatten(JavaScriptNode[] arr, int pos, JavaScriptNode node) {
        if (node instanceof DualNode) {
            DualNode dual = (DualNode) node;
            arr[pos] = dual.left;
            arr[pos + 1] = dual.right;
            return pos + 2;
        } else if (node instanceof AbstractBlockNode) {
            AbstractBlockNode block = (AbstractBlockNode) node;
            int len = block.getStatements().length;
            System.arraycopy(block.getStatements(), 0, arr, pos, len);
            return pos + len;
        } else {
            arr[pos] = node;
            return pos + 1;
        }
    }

    private static int getLen(JavaScriptNode node) {
        return node instanceof DualNode ? 2 : node instanceof AbstractBlockNode ? ((AbstractBlockNode) node).getStatements().length : 1;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        left.executeVoid(frame);
        return right.execute(frame);
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        left.executeVoid(frame);
        return right.executeInt(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        left.executeVoid(frame);
        return right.executeDouble(frame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        left.executeVoid(frame);
        return right.executeBoolean(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        left.executeVoid(frame);
        right.executeVoid(frame);
    }

    @Override
    public JavaScriptNode[] getStatements() {
        return new JavaScriptNode[]{left, right};
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int state = getStateAsIntAndReset(frame);
        if (state == 0) {
            left.executeVoid(frame);
            try {
                return right.execute(frame);
            } catch (YieldException e) {
                setState(frame, 1);
                throw e;
            }
        } else {
            assert state == 1;
            try {
                return right.execute(frame);
            } catch (YieldException e) {
                setState(frame, 1);
                throw e;
            }
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return right.isResultAlwaysOfType(clazz);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new DualNode(cloneUninitialized(left), cloneUninitialized(right));
    }
}

/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;

/**
 * @see BreakNode
 */
public class DirectBreakTargetNode extends StatementNode {

    @Child protected JavaScriptNode block;

    DirectBreakTargetNode(JavaScriptNode block) {
        this.block = block;
    }

    public static DirectBreakTargetNode create(JavaScriptNode block) {
        return new DirectBreakTargetNode(block);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return block.execute(frame);
        } catch (DirectBreakException ex) {
            return EMPTY;
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            block.executeVoid(frame);
        } catch (DirectBreakException ex) {
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(block));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return block.isResultAlwaysOfType(clazz);
    }
}

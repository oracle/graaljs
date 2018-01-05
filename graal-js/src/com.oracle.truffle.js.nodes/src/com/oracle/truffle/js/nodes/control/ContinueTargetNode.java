/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * @see ContinueNode
 */
public final class ContinueTargetNode extends StatementNode {

    @Child private JavaScriptNode block;
    private final ContinueTarget target;

    ContinueTargetNode(JavaScriptNode block, ContinueTarget target) {
        this.target = target;
        this.block = block;
    }

    public static ContinueTargetNode create(JavaScriptNode block, ContinueTarget target) {
        return new ContinueTargetNode(block, target);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return block.execute(frame);
        } catch (ContinueException ex) {
            if (!ex.matchTarget(target)) {
                throw ex;
            }
            return Undefined.instance;
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            block.executeVoid(frame);
        } catch (ContinueException ex) {
            if (!ex.matchTarget(target)) {
                throw ex;
            }
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(block), target);
    }
}

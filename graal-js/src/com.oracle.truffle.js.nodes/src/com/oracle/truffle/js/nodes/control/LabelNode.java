/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class LabelNode extends StatementNode {

    @Child private JavaScriptNode block;
    private final BreakTarget target;

    LabelNode(JavaScriptNode block, BreakTarget target) {
        this.block = block;
        this.target = target;
    }

    public static LabelNode create(JavaScriptNode block, BreakTarget target) {
        return new LabelNode(block, target);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return block.execute(frame);
        } catch (LabelBreakException ex) {
            if (!ex.matchTarget(target)) {
                throw ex;
            }
        }
        return EMPTY;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(block), target);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }
}

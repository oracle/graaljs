/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.3 Empty Statement.
 */
@NodeInfo(cost = NodeCost.NONE)
public final class EmptyNode extends StatementNode implements RepeatableNode {
    EmptyNode() {
    }

    public static EmptyNode create() {
        return new EmptyNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return EMPTY;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return copy();
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }
}

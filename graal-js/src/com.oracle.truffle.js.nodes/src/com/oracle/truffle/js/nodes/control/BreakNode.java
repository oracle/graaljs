/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;

/**
 * 12.8 The break Statement.
 */
@NodeInfo(shortName = "break")
public final class BreakNode extends StatementNode {

    private final BreakException breakException;

    BreakNode(BreakTarget breakTarget) {
        this.breakException = breakTarget.getBreakException();
    }

    public static BreakNode create(BreakTarget breakTarget) {
        return new BreakNode(breakTarget);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw breakException;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw breakException;
    }

    public boolean isDirectBreak() {
        return breakException instanceof DirectBreakException;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return copy();
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return true;
    }
}

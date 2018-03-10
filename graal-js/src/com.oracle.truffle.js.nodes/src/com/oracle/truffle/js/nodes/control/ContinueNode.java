/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchStatementTag;

/**
 * 12.7 The continue Statement.
 */
@NodeInfo(shortName = "continue")
public final class ContinueNode extends StatementNode {

    private final ContinueException continueException;

    ContinueNode(ContinueTarget continueTarget) {
        this.continueException = continueTarget.getContinueException();
    }

    public static ContinueNode create(ContinueTarget continueTarget) {
        return new ContinueNode(continueTarget);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ControlFlowBranchStatementTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        descriptor.addProperty("kind", "continue");
        return descriptor;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw continueException;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw continueException;
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

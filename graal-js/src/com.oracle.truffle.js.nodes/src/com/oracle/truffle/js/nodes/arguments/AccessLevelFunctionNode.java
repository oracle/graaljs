/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.runtime.JSArguments;

public final class AccessLevelFunctionNode extends JavaScriptNode implements RepeatableNode {

    @Child private ScopeFrameNode accessFrame;

    private AccessLevelFunctionNode(ScopeFrameNode accessFrame) {
        this.accessFrame = accessFrame;
    }

    public static JavaScriptNode create(int frameLevel) {
        return new AccessLevelFunctionNode(ScopeFrameNode.create(frameLevel, 0));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Frame parentFrame = accessFrame.executeFrame(frame);
        return JSArguments.getFunctionObject(parentFrame.getArguments());
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AccessLevelFunctionNode(NodeUtil.cloneNode(accessFrame));
    }
}

/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;

/**
 * This node provides the "this" object, that might a primitive value.
 */
@NodeInfo(shortName = "this")
public final class AccessThisNode extends JavaScriptNode implements RepeatableNode {

    AccessThisNode() {
    }

    public static AccessThisNode create() {
        return new AccessThisNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return JSFrameUtil.getThisObj(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create();
    }
}

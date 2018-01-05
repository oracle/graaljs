/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public final class AccessFunctionNode extends JavaScriptNode implements RepeatableNode {

    AccessFunctionNode() {
    }

    public static AccessFunctionNode create() {
        return new AccessFunctionNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return JSFrameUtil.getFunctionObject(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create();
    }
}

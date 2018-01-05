/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.js.nodes.JavaScriptNode;

public class JSInteropReceiverNode extends JavaScriptNode {

    @Override
    public Object execute(VirtualFrame frame) {
        return ForeignAccess.getReceiver(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return copy();
    }
}

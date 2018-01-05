/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;

@NodeInfo(cost = NodeCost.NONE)
public final class FunctionBodyNode extends AbstractBodyNode {
    @Child private JavaScriptNode body;

    public FunctionBodyNode(JavaScriptNode body) {
        this.body = body;
    }

    public static FunctionBodyNode create(JavaScriptNode body) {
        return new FunctionBodyNode(body);
    }

    public JavaScriptNode getBody() {
        return body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(body));
    }
}

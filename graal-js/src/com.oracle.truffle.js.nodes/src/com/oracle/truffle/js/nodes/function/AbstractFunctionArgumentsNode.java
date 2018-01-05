/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;

public abstract class AbstractFunctionArgumentsNode extends JavaScriptBaseNode {

    public abstract int getCount(VirtualFrame frame);

    public abstract Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta);

    protected abstract AbstractFunctionArgumentsNode copyUninitialized();

    @SuppressWarnings("unchecked")
    public static <T extends AbstractFunctionArgumentsNode> T cloneUninitialized(T node) {
        return node == null ? null : (T) node.copyUninitialized();
    }
}

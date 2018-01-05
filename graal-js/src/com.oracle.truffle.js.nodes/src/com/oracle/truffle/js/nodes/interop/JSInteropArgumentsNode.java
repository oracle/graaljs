/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;

public class JSInteropArgumentsNode extends AbstractFunctionArgumentsNode {

    private final int offset;
    @Children private final JSInteropArgumentNode[] arguments;

    public JSInteropArgumentsNode(int args, int offset) {
        this.offset = offset;
        this.arguments = new JSInteropArgumentNode[args];
        for (int i = offset; i < args + offset; i++) {
            arguments[i - offset] = new JSInteropArgumentNode(i, true);
        }
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return arguments.length;
    }

    @Override
    @ExplodeLoop
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] args, int delta) {
        for (int i = 0; i < arguments.length; i++) {
            args[i + delta] = arguments[i].execute(frame);
        }
        return args;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSInteropArgumentsNode(arguments.length, offset);
    }
}

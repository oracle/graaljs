/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser.env;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;

/**
 * Read-only environment based on a frame descriptor used to give debugger code access to the
 * lexical environment it's to be evaluated in.
 */
public class DebugEnvironment extends Environment {
    private final FrameDescriptor frameDescriptor;

    public DebugEnvironment(Environment parent, NodeFactory factory, JSContext context, FrameDescriptor frameDescriptor) {
        super(parent, factory, context);
        this.frameDescriptor = frameDescriptor;
    }

    @Override
    protected FrameSlot findBlockFrameSlot(String name) {
        return frameDescriptor.findFrameSlot(name);
    }

    @Override
    public FunctionEnvironment function() {
        return null;
    }

    @Override
    public boolean isStrictMode() {
        return true;
    }
}

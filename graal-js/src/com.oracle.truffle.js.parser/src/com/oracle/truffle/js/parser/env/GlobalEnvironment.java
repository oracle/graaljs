/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser.env;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;

public class GlobalEnvironment extends Environment {
    private final FunctionEnvironment functionEnvironment;
    private final FrameDescriptor blockFrameDescriptor;

    public GlobalEnvironment(Environment parent, NodeFactory factory, JSContext context) {
        super(parent, factory, context);
        this.functionEnvironment = parent.function();
        this.blockFrameDescriptor = context.getRealm().getGlobalScope().getFrameDescriptor();
    }

    @Override
    public FunctionEnvironment function() {
        return functionEnvironment;
    }

    @Override
    public FrameSlot findBlockFrameSlot(String name) {
        return blockFrameDescriptor.findFrameSlot(name);
    }

    @Override
    public FrameDescriptor getBlockFrameDescriptor() {
        return blockFrameDescriptor;
    }
}

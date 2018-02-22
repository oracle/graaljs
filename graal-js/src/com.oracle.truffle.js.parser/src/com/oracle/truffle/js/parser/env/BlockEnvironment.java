/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser.env;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.nodes.access.*;
import com.oracle.truffle.js.runtime.*;

public class BlockEnvironment extends Environment {
    private final FunctionEnvironment functionEnvironment;
    private final FrameDescriptor blockFrameDescriptor;
    private final FrameSlot parentSlot;
    private final int scopeLevel;

    public BlockEnvironment(Environment parent, NodeFactory factory, JSContext context) {
        super(parent, factory, context);
        this.functionEnvironment = parent.function();
        this.blockFrameDescriptor = factory.createBlockFrameDescriptor();
        this.parentSlot = ScopeFrameNode.PARENT_SCOPE_SLOT;
        this.scopeLevel = parent.getScopeLevel() + 1;
    }

    @Override
    public FunctionEnvironment function() {
        return functionEnvironment;
    }

    @Override
    public FrameSlot findBlockFrameSlot(String name) {
        FrameSlot frameSlot = getBlockFrameDescriptor().findFrameSlot(name);
        return frameSlot;
    }

    @Override
    public FrameDescriptor getBlockFrameDescriptor() {
        return blockFrameDescriptor;
    }

    public FrameSlot getParentSlot() {
        return parentSlot;
    }

    @Override
    public int getScopeLevel() {
        return scopeLevel;
    }
}

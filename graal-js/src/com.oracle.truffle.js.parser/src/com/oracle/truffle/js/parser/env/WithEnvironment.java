/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser.env;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;

/**
 *
 * This environment stores "with" blocks. Actually, this is just a marker in the chain (tree) of
 * environments.
 */
public class WithEnvironment extends Environment {

    /**
     * Name of the frame slot that contains the with object.
     */
    private final String withVarName;

    public WithEnvironment(Environment parent, NodeFactory factory, JSContext context, String withVarName) {
        super(parent, factory, context);
        this.withVarName = withVarName;
        assert parent.hasLocalVar(withVarName);
    }

    public String getWithVarName() {
        return withVarName;
    }

    @Override
    public FunctionEnvironment function() {
        return getParent().function();
    }

    @Override
    public int getScopeLevel() {
        return getParent().getScopeLevel();
    }

    @Override
    protected FrameSlot findBlockFrameSlot(String name) {
        return null;
    }

    @Override
    public boolean isDynamicScopeContext() {
        return true;
    }
}

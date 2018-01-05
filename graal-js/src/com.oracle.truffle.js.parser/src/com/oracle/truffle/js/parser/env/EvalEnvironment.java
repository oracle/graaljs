/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser.env;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;

public class EvalEnvironment extends Environment {
    private final boolean isDirectEval;

    public EvalEnvironment(Environment parent, NodeFactory factory, JSContext context, boolean isDirectEval) {
        super(parent, factory, context);
        assert parent == null || parent.function() == null || parent.function().isDeepFrozen();
        this.isDirectEval = isDirectEval;
    }

    public boolean isDirectEval() {
        return isDirectEval;
    }

    @Override
    public FunctionEnvironment function() {
        return null;
    }

    @Override
    protected FrameSlot findBlockFrameSlot(String name) {
        throw new UnsupportedOperationException();
    }
}

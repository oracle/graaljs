/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * IteratorComplete(iterResult) unary expression.
 */
public class IteratorCompleteUnaryNode extends JavaScriptNode {
    @Child private PropertyGetNode getDoneNode;
    @Child private JavaScriptNode iterResultNode;
    @Child private JSToBooleanNode toBooleanNode;

    protected IteratorCompleteUnaryNode(JSContext context, JavaScriptNode iterResultNode) {
        this.iterResultNode = iterResultNode;
        this.getDoneNode = PropertyGetNode.create(JSRuntime.DONE, false, context);
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode iterResultNode) {
        return new IteratorCompleteUnaryNode(context, iterResultNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        Object iterResult = iterResultNode.execute(frame);
        Object done = getDoneNode.getValue(iterResult);
        if (done instanceof Boolean) {
            return (boolean) done;
        }
        if (toBooleanNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toBooleanNode = insert(JSToBooleanNode.create());
        }
        return toBooleanNode.executeBoolean(done);
    }

    @Override
    public final boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(getDoneNode.getContext(), cloneUninitialized(iterResultNode));
    }
}

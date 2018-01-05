/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;

/**
 * 12.9 The return Statement.
 */
@NodeInfo(shortName = "return")
public class ReturnNode extends StatementNode {

    @Child protected JavaScriptNode expression;

    protected ReturnNode(JavaScriptNode expression) {
        this.expression = expression;
    }

    public static ReturnNode create(JavaScriptNode expression) {
        if (expression instanceof JSConstantNode) {
            return new ConstantReturnNode(expression);
        } else {
            assert !(expression instanceof EmptyNode);
            return new ReturnNode(expression);
        }
    }

    public static ReturnNode createFrameReturn(JavaScriptNode expression) {
        return new FrameReturnNode(expression);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new ReturnException(expression.execute(frame));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new ReturnException(expression.execute(frame));
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        ReturnNode copy = (ReturnNode) copy();
        copy.expression = cloneUninitialized(expression);
        return copy;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return true;
    }

    private static class ConstantReturnNode extends ReturnNode {

        private final ReturnException exception;

        protected ConstantReturnNode(JavaScriptNode expression) {
            super(expression);
            this.exception = new ReturnException(expression.execute(null));
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw exception;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            throw exception;
        }
    }

    public static class FrameReturnNode extends ReturnNode {

        private static final ReturnException RETURN_EXCEPTION = new ReturnException(null);

        protected FrameReturnNode(JavaScriptNode expression) {
            super(expression);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            expression.executeVoid(frame);
            throw RETURN_EXCEPTION;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            expression.executeVoid(frame);
            throw RETURN_EXCEPTION;
        }
    }
}

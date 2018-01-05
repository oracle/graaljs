/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;

public class ReturnTargetNode extends JavaScriptNode {

    @Child protected JavaScriptNode body;

    protected ReturnTargetNode(JavaScriptNode child) {
        this.body = child;
    }

    public static ReturnTargetNode create(JavaScriptNode child) {
        return new ReturnTargetNode(child);
    }

    public static FrameReturnTargetNode createFrameReturnTarget(JavaScriptNode body, JavaScriptNode returnValue) {
        return new FrameReturnTargetNode(body, returnValue);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (ReturnException ex) {
            return ex.getResult();
        }
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return body.executeInt(frame);
        } catch (ReturnException ex) {
            return JSTypesGen.expectInteger(ex.getResult());
        }
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return body.executeDouble(frame);
        } catch (ReturnException ex) {
            return JSTypesGen.expectDouble(ex.getResult());
        }
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return body.executeBoolean(frame);
        } catch (ReturnException ex) {
            return JSTypesGen.expectBoolean(ex.getResult());
        }
    }

    public final JavaScriptNode getBody() {
        return body;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new ReturnTargetNode(cloneUninitialized(body));
    }

    public static class FrameReturnTargetNode extends ReturnTargetNode {

        @Child private JavaScriptNode returnValue;

        protected FrameReturnTargetNode(JavaScriptNode body, JavaScriptNode returnValue) {
            super(body);
            this.returnValue = returnValue;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return body.execute(frame);
            } catch (ReturnException ex) {
                return returnValue.execute(frame);
            }
        }

        @Override
        public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
            try {
                return body.executeInt(frame);
            } catch (ReturnException ex) {
                return returnValue.executeInt(frame);
            }
        }

        @Override
        public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
            try {
                return body.executeDouble(frame);
            } catch (ReturnException ex) {
                return returnValue.executeDouble(frame);
            }
        }

        @Override
        public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
            try {
                return body.executeBoolean(frame);
            } catch (ReturnException ex) {
                return returnValue.executeBoolean(frame);
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new FrameReturnTargetNode(cloneUninitialized(body), cloneUninitialized(returnValue));
        }
    }
}

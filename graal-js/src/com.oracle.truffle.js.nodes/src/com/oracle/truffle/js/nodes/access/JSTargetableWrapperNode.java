/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;

public class JSTargetableWrapperNode extends JSTargetableNode {

    @Child private JavaScriptNode delegate;
    @Child private JavaScriptNode targetNode;

    protected JSTargetableWrapperNode(JavaScriptNode delegate, JavaScriptNode targetNode) {
        this.delegate = delegate;
        this.targetNode = targetNode;
    }

    public static JSTargetableNode create(JavaScriptNode delegate, JavaScriptNode target) {
        return new JSTargetableWrapperNode(delegate, target);
    }

    public JavaScriptNode getDelegate() {
        return delegate;
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        return delegate.execute(frame);
    }

    @Override
    public int executeIntWithTarget(VirtualFrame frame, Object target) throws UnexpectedResultException {
        return delegate.executeInt(frame);
    }

    @Override
    public double executeDoubleWithTarget(VirtualFrame frame, Object target) throws UnexpectedResultException {
        return delegate.executeDouble(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return delegate.execute(frame);
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return delegate.executeInt(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return delegate.executeDouble(frame);
    }

    @Override
    public Object evaluateTarget(VirtualFrame frame) {
        return targetNode.execute(frame);
    }

    @Override
    public JavaScriptNode getTarget() {
        return targetNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new JSTargetableWrapperNode(cloneUninitialized(delegate), cloneUninitialized(targetNode));
    }
}

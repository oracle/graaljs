/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class GeneratorWrapperNode extends JavaScriptNode implements RepeatingNode {
    @Child private JavaScriptNode childNode;
    @Child private JavaScriptNode stateNode;
    @Child private WriteNode writeStateNode;

    private GeneratorWrapperNode(JavaScriptNode childNode, JavaScriptNode stateNode, WriteNode writeStateNode) {
        assert !(childNode instanceof GeneratorWrapperNode);
        this.childNode = childNode;
        this.stateNode = stateNode;
        this.writeStateNode = writeStateNode;
    }

    public static JavaScriptNode createWrapper(JavaScriptNode child, JavaScriptNode state, WriteNode writeStateNode) {
        return new GeneratorWrapperNode(child, state, writeStateNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Node child = childNode;
        if (child instanceof WrapperNode) {
            child = ((WrapperNode) child).getDelegateNode();
        }
        if (child instanceof ResumableNode) {
            return ((ResumableNode) child).resume(frame);
        } else {
            assert false : child.getClass();
            throw Errors.shouldNotReachHere();
        }
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
        assert childNode instanceof ResumableNode && childNode instanceof RepeatingNode : childNode.getClass();
        return (boolean) execute(frame);
    }

    public Object isResuming(VirtualFrame frame) {
        return stateNode.execute(frame) != Undefined.instance;
    }

    public Object getState(VirtualFrame frame) {
        return stateNode.execute(frame);
    }

    public int getStateAsInt(VirtualFrame frame) {
        Object value = stateNode.execute(frame);
        return (value instanceof Integer) ? (int) value : 0;
    }

    public void setState(VirtualFrame frame, Object resumeState) {
        writeStateNode.executeWrite(frame, resumeState);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return createWrapper(cloneUninitialized(childNode), cloneUninitialized(stateNode), (WriteNode) cloneUninitialized((JavaScriptNode) writeStateNode));
    }
}

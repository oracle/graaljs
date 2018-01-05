/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode.RequireObjectCoercibleWrapperNode;

public final class SuperPropertyReferenceNode extends JSTargetableNode implements RepeatableNode {

    @Child private JavaScriptNode baseValueNode;
    @Child private JavaScriptNode thisValueNode;

    private SuperPropertyReferenceNode(JavaScriptNode baseNode, JavaScriptNode thisValueNode) {
        this.baseValueNode = baseNode;
        this.thisValueNode = thisValueNode;
    }

    public static JSTargetableNode create(JavaScriptNode baseNode, JavaScriptNode thisValueNode) {
        assert baseNode instanceof RepeatableNode && thisValueNode instanceof RepeatableNode;
        return new SuperPropertyReferenceNode(RequireObjectCoercibleWrapperNode.create(baseNode), thisValueNode);
    }

    public JavaScriptNode getBaseValue() {
        return baseValueNode;
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        return baseValueNode.execute(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return baseValueNode.execute(frame);
    }

    @Override
    public Object evaluateTarget(VirtualFrame frame) {
        return thisValueNode.execute(frame);
    }

    public JavaScriptNode getThisValue() {
        return thisValueNode;
    }

    @Override
    public JavaScriptNode getTarget() {
        return thisValueNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new SuperPropertyReferenceNode(cloneUninitialized(baseValueNode), cloneUninitialized(thisValueNode));
    }
}

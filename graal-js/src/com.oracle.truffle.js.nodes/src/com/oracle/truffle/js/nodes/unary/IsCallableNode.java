/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Represents abstract operation IsCallable.
 *
 * @see JSRuntime#isCallable(Object)
 */
public abstract class IsCallableNode extends JSUnaryNode {

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

    public abstract boolean executeBoolean(Object operand);

    @SuppressWarnings("unused")
    @Specialization(guards = "isJSFunction(function)")
    protected static boolean doJSFunction(DynamicObject function) {
        return true;
    }

    @Specialization(guards = "isJSProxy(proxy)")
    protected static boolean doJSProxy(DynamicObject proxy) {
        return JSRuntime.isCallableProxy(proxy);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static boolean doOther(Object other) {
        return false;
    }

    public static IsCallableNode create(JavaScriptNode operand) {
        return IsCallableNodeGen.create(operand);
    }

    public static IsCallableNode create() {
        return IsCallableNodeGen.create(null);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return IsCallableNode.create(cloneUninitialized(getOperand()));
    }
}

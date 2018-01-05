/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.JSContext;

@NodeInfo(cost = NodeCost.NONE)
public class GlobalObjectNode extends JavaScriptNode implements RepeatableNode {
    private final JSContext context;

    protected GlobalObjectNode(JSContext context) {
        this.context = context;
    }

    public static GlobalObjectNode create(JSContext context) {
        return new GlobalObjectNode(context);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        return executeDynamicObject();
    }

    @Override
    public DynamicObject executeDynamicObject(VirtualFrame frame) {
        return executeDynamicObject();
    }

    public DynamicObject executeDynamicObject() {
        return getGlobalObject(context);
    }

    public static DynamicObject getGlobalObject(JSContext context) {
        return context.getRealm().getGlobalObject();
    }

    final JSContext getContext() {
        return context;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context);
    }
}

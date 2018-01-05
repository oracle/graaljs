/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * ES6 7.4.7 CreateIterResultObject (value, done).
 */
public abstract class CreateIterResultObjectNode extends JavaScriptBaseNode {
    @Child private CreateObjectNode createObjectNode;
    @Child private CreateDataPropertyNode createValuePropertyNode;
    @Child private CreateDataPropertyNode createDonePropertyNode;

    protected CreateIterResultObjectNode(JSContext context) {
        this.createObjectNode = CreateObjectNode.create(context);
        this.createValuePropertyNode = CreateDataPropertyNode.create(context, JSRuntime.VALUE);
        this.createDonePropertyNode = CreateDataPropertyNode.create(context, JSRuntime.DONE);
    }

    public static CreateIterResultObjectNode create(JSContext context) {
        return CreateIterResultObjectNodeGen.create(context);
    }

    @Specialization
    protected DynamicObject doCreateIterResultObject(VirtualFrame frame, Object value, boolean done) {
        DynamicObject iterResult = createObjectNode.execute(frame);
        createValuePropertyNode.executeVoid(iterResult, value);
        createDonePropertyNode.executeVoid(iterResult, done);
        return iterResult;
    }

    public abstract DynamicObject execute(VirtualFrame frame, Object value, boolean done);
}

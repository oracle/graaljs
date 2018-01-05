/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * ES6 7.4.3 IteratorComplete(iterResult).
 */
public abstract class IteratorCompleteNode extends JavaScriptBaseNode {
    @Child private PropertyNode getDoneNode;
    @Child private JSToBooleanNode toBooleanNode;

    protected IteratorCompleteNode(JSContext context) {
        NodeFactory factory = NodeFactory.getInstance(context);
        this.getDoneNode = factory.createProperty(context, null, JSRuntime.DONE);
        this.toBooleanNode = JSToBooleanNode.create();
    }

    public static IteratorCompleteNode create(JSContext context) {
        return IteratorCompleteNodeGen.create(context);
    }

    @Specialization
    protected boolean doIteratorNext(DynamicObject iterResult) {
        return toBooleanNode.executeBoolean(getDoneNode.executeWithTarget(iterResult));
    }

    public abstract boolean execute(DynamicObject iterResult);
}

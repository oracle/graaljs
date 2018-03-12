/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;

/**
 * ES6 7.4.4 IteratorValue(iterResult).
 */
public abstract class IteratorValueNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode iterResultNode;
    @Child private PropertyGetNode getValueNode;

    protected IteratorValueNode(JSContext context, JavaScriptNode iterResultNode) {
        this.iterResultNode = iterResultNode;
        this.getValueNode = PropertyGetNode.create("value", false, context);
    }

    public static IteratorValueNode create(JSContext context) {
        return create(context, null);
    }

    public static IteratorValueNode create(JSContext context, JavaScriptNode iterResult) {
        return IteratorValueNodeGen.create(context, iterResult);
    }

    @Specialization
    protected Object doIteratorNext(DynamicObject iterResult) {
        return getValueNode.getValue(iterResult);
    }

    public abstract Object execute(DynamicObject iterResult);

    @Override
    protected JavaScriptNode copyUninitialized() {
        return IteratorValueNodeGen.create(getValueNode.getContext(), cloneUninitialized(iterResultNode));
    }
}

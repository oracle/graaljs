/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * ES6 7.4.3 IteratorComplete(iterResult).
 */
public class IteratorCompleteNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getDoneNode;
    @Child private JSToBooleanNode toBooleanNode;

    protected IteratorCompleteNode(JSContext context) {
        this.getDoneNode = PropertyGetNode.create(JSRuntime.DONE, false, context);
        this.toBooleanNode = JSToBooleanNode.create();
    }

    public static IteratorCompleteNode create(JSContext context) {
        return new IteratorCompleteNode(context);
    }

    public boolean execute(DynamicObject iterResult) {
        return toBooleanNode.executeBoolean(getDoneNode.getValue(iterResult));
    }
}

/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES6 7.4.5 IteratorStep(iterator).
 */
@NodeChild(value = "iterator", type = JavaScriptNode.class)
public abstract class IteratorStepNode extends JavaScriptNode {
    @Child private IteratorNextNode iteratorNextNode;
    @Child private IteratorCompleteNode iteratorCompleteNode;
    private final JSContext context;

    protected IteratorStepNode(JSContext context) {
        this.context = context;
    }

    public static IteratorStepNode create(JSContext context) {
        return create(context, null);
    }

    public static IteratorStepNode create(JSContext context, JavaScriptNode iterator) {
        return IteratorStepNodeGen.create(context, iterator);
    }

    @Specialization
    protected Object doIteratorStep(DynamicObject iterator) {
        if (iteratorNextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            iteratorNextNode = insert(IteratorNextNode.create(context));
        }
        if (iteratorCompleteNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            iteratorCompleteNode = insert(IteratorCompleteNode.create(context));
        }
        // passing undefined might be wrong, we should NOT pass "value"
        Object result = iteratorNextNode.execute(iterator, Undefined.instance);
        Object done = iteratorCompleteNode.execute((DynamicObject) result);
        if (done instanceof Boolean && ((Boolean) done) == Boolean.TRUE) {
            return false;
        }
        return result;
    }

    public abstract Object execute(DynamicObject iterator);

    abstract JavaScriptNode getIterator();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return IteratorStepNodeGen.create(context, cloneUninitialized(getIterator()));
    }
}

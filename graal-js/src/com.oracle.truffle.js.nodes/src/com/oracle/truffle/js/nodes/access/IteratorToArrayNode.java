/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.ExprBlockNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArray;

/**
 * Absorb iterator to new array.
 */
@NodeChild(value = "iterator", type = JavaScriptNode.class)
public abstract class IteratorToArrayNode extends JavaScriptNode {
    private final JSContext context;
    @Child private IteratorStepSpecialNode iteratorStepNode;

    protected IteratorToArrayNode(JSContext context, JavaScriptNode writeDone) {
        this.context = context;
        this.iteratorStepNode = IteratorStepSpecialNode.create(context, null, ExprBlockNode.createExprBlock(new JavaScriptNode[]{writeDone, JSConstantNode.create(null)}), true);
    }

    protected IteratorToArrayNode(JSContext context, IteratorStepSpecialNode iteratorStepNode) {
        this.context = context;
        this.iteratorStepNode = iteratorStepNode;
    }

    public static IteratorToArrayNode create(JSContext context, JavaScriptNode iterator, JavaScriptNode writeDone) {
        return IteratorToArrayNodeGen.create(context, writeDone, iterator);
    }

    @Specialization
    protected Object doIterator(VirtualFrame frame, DynamicObject iterator) {
        List<Object> elements = new ArrayList<>();
        Object value;
        while ((value = iteratorStepNode.execute(frame, iterator)) != null) {
            Boundaries.listAdd(elements, value);
        }
        return JSArray.createZeroBasedObjectArray(context, Boundaries.listToArray(elements));
    }

    public abstract Object execute(VirtualFrame frame, DynamicObject iterator);

    abstract JavaScriptNode getIterator();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return IteratorToArrayNodeGen.create(context, cloneUninitialized(iteratorStepNode), cloneUninitialized(getIterator()));
    }
}

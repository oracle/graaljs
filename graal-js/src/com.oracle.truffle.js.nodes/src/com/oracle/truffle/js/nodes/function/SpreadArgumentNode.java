/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorStepSpecialNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;

public final class SpreadArgumentNode extends JavaScriptNode {
    @Child private GetIteratorNode getIteratorNode;
    @Child private IteratorStepSpecialNode iteratorStepNode;

    private SpreadArgumentNode(JSContext context, JavaScriptNode arg) {
        this.getIteratorNode = GetIteratorNode.create(context, arg);
        this.iteratorStepNode = IteratorStepSpecialNode.create(context, null, JSConstantNode.create(null), false);
    }

    public static SpreadArgumentNode create(JSContext context, JavaScriptNode arg) {
        return new SpreadArgumentNode(context, arg);
    }

    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        DynamicObject iterator = getIteratorNode.execute(frame);
        Object[] args = arguments;
        int i = 0;
        for (;;) {
            Object nextArg = iteratorStepNode.execute(frame, iterator);
            if (nextArg == null) {
                break;
            }
            if (delta + i >= args.length) {
                args = Arrays.copyOf(args, args.length + (args.length + 1) / 2);
            }
            args[delta + i++] = nextArg;
        }
        return delta + i == args.length ? args : Arrays.copyOf(args, delta + i);
    }

    @Override
    public Object[] executeObjectArray(VirtualFrame frame) {
        DynamicObject iterator = getIteratorNode.execute(frame);
        Object[] args = new Object[0];
        for (int i = 0;; i++) {
            Object nextArg = iteratorStepNode.execute(frame, iterator);
            if (nextArg == null) {
                break;
            }
            if (i >= args.length) {
                args = Arrays.copyOf(args, args.length + 1);
            }
            args[i] = nextArg;
        }
        return args;
    }

    @Override
    public Object[] execute(VirtualFrame frame) {
        return executeObjectArray(frame);
    }

    public void executeToList(VirtualFrame frame, List<Object> argList) {
        DynamicObject iterator = getIteratorNode.execute(frame);
        for (;;) {
            Object nextArg = iteratorStepNode.execute(frame, iterator);
            if (nextArg == null) {
                break;
            }
            Boundaries.listAdd(argList, nextArg);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        SpreadArgumentNode copy = (SpreadArgumentNode) copy();
        copy.getIteratorNode = cloneUninitialized(getIteratorNode);
        copy.iteratorStepNode = cloneUninitialized(iteratorStepNode);
        return copy;
    }
}

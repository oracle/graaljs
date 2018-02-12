/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.AwaitNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Utility node implementing ForIn/OfBodyEvaluation handling of async iterators.
 *
 * @see IteratorNextUnaryNode
 */
public class AsyncIteratorNextNode extends AwaitNode {
    private final JSContext context;
    @Child private PropertyGetNode getNextNode;
    @Child private JSFunctionCallNode methodCallNode;

    protected AsyncIteratorNextNode(JSContext context, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        super(context, iterator, asyncContextNode, asyncResultNode);
        this.context = context;
        this.getNextNode = PropertyGetNode.create(JSRuntime.NEXT, false, context);
        this.methodCallNode = JSFunctionCallNode.createCall();
    }

    public static AwaitNode create(JSContext context, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return new AsyncIteratorNextNode(context, iterator, asyncContextNode, asyncResultNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        DynamicObject iterator = (DynamicObject) expression.execute(frame);
        Object next = getNextNode.getValue(iterator);
        Object nextResult = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, next));
        setState(frame, 1);
        return suspendAwait(frame, nextResult);
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsInt(frame);
        if (index == 0) {
            return execute(frame);
        } else {
            setState(frame, 0);
            Object result = resumeAwait(frame);
            if (!JSObject.isJSObject(result)) {
                throw Errors.createTypeError("Iterator Result not an object");
            }
            return result;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AsyncIteratorNextNode(context, cloneUninitialized(expression), cloneUninitialized(readAsyncContextNode), cloneUninitialized(readAsyncResultNode));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == TruffleObject.class;
    }
}

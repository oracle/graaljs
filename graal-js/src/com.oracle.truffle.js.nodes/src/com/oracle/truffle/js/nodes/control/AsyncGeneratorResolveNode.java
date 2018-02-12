/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.ArrayDeque;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncGeneratorResolveNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getAsyncGeneratorQueueNode;
    @Child private CreateIterResultObjectNode createIterResultObjectNode;
    @Child private PropertyGetNode getPromiseResolve;
    @Child private JSFunctionCallNode callResolveNode;
    @Child private AsyncGeneratorResumeNextNode asyncGeneratorResumeNextNode;

    protected AsyncGeneratorResolveNode(JSContext context) {
        this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        this.getAsyncGeneratorQueueNode = PropertyGetNode.create(JSFunction.ASYNC_GENERATOR_QUEUE_ID, false, context);
        this.getPromiseResolve = PropertyGetNode.create("resolve", false, context);
        this.callResolveNode = JSFunctionCallNode.createCall();
    }

    public static AsyncGeneratorResolveNode create(JSContext context) {
        return new AsyncGeneratorResolveNode(context);
    }

    public Object execute(VirtualFrame frame, DynamicObject generator, Object value, boolean done) {
        performResolve(frame, generator, value, done);
        if (asyncGeneratorResumeNextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.asyncGeneratorResumeNextNode = insert(AsyncGeneratorResumeNextNode.create(getContext()));
        }
        asyncGeneratorResumeNextNode.execute(frame, generator);
        return Undefined.instance;
    }

    @SuppressWarnings("unchecked")
    void performResolve(VirtualFrame frame, DynamicObject generator, Object value, boolean done) {
        ArrayDeque<AsyncGeneratorRequest> queue = (ArrayDeque<AsyncGeneratorRequest>) getAsyncGeneratorQueueNode.getValue(generator);
        assert !queue.isEmpty();
        AsyncGeneratorRequest next = queue.removeFirst();
        DynamicObject promiseCapability = next.getPromiseCapability();
        DynamicObject iteratorResult = createIterResultObjectNode.execute(frame, value, done);
        Object resolve = getPromiseResolve.getValue(promiseCapability);
        callResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, resolve, iteratorResult));
    }

    private JSContext getContext() {
        return getPromiseResolve.getContext();
    }
}

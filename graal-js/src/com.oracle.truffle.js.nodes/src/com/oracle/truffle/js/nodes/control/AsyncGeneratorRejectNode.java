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

public class AsyncGeneratorRejectNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getAsyncGeneratorQueueNode;
    @Child private CreateIterResultObjectNode createIterResultObjectNode;
    @Child private PropertyGetNode getPromiseReject;
    @Child private JSFunctionCallNode callRejectNode;
    @Child private AsyncGeneratorResumeNextNode asyncGeneratorResumeNextNode;

    protected AsyncGeneratorRejectNode(JSContext context) {
        this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        this.getAsyncGeneratorQueueNode = PropertyGetNode.create(JSFunction.ASYNC_GENERATOR_QUEUE_ID, false, context);
        this.getPromiseReject = PropertyGetNode.create("reject", false, context);
        this.callRejectNode = JSFunctionCallNode.createCall();
    }

    public static AsyncGeneratorRejectNode create(JSContext context) {
        return new AsyncGeneratorRejectNode(context);
    }

    public Object execute(VirtualFrame frame, DynamicObject generator, Object exception) {
        performReject(frame, generator, exception);
        if (asyncGeneratorResumeNextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.asyncGeneratorResumeNextNode = insert(AsyncGeneratorResumeNextNode.create(getContext()));
        }
        asyncGeneratorResumeNextNode.execute(frame, generator);
        return Undefined.instance;
    }

    @SuppressWarnings({"unchecked", "unused"})
    void performReject(VirtualFrame frame, DynamicObject generator, Object exception) {
        ArrayDeque<AsyncGeneratorRequest> queue = (ArrayDeque<AsyncGeneratorRequest>) getAsyncGeneratorQueueNode.getValue(generator);
        assert !queue.isEmpty();
        AsyncGeneratorRequest next = queue.removeFirst();
        DynamicObject promiseCapability = next.getPromiseCapability();
        Object resolve = getPromiseReject.getValue(promiseCapability);
        callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, resolve, exception));
    }

    private JSContext getContext() {
        return getPromiseReject.getContext();
    }
}

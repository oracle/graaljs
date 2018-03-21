/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.ArrayDeque;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.AsyncGeneratorState;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncGeneratorEnqueueNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getGeneratorState;
    @Child private PropertyGetNode getAsyncGeneratorQueueNode;
    @Child private HasHiddenKeyCacheNode hasAsyncGeneratorInternalSlots;
    @Child private PropertyGetNode getPromiseReject;
    @Child private JSFunctionCallNode callPromiseRejectNode;
    @Child private JSFunctionCallNode createPromiseCapability;
    @Child private PropertyGetNode getPromise;
    @Child private AsyncGeneratorResumeNextNode asyncGeneratorResumeNextNode;
    private final JSContext context;

    protected AsyncGeneratorEnqueueNode(JSContext context) {
        this.context = context;
        this.getGeneratorState = PropertyGetNode.create(JSFunction.GENERATOR_STATE_ID, false, context);
        this.getAsyncGeneratorQueueNode = PropertyGetNode.create(JSFunction.ASYNC_GENERATOR_QUEUE_ID, false, context);
        this.hasAsyncGeneratorInternalSlots = HasHiddenKeyCacheNode.create(JSFunction.ASYNC_GENERATOR_QUEUE_ID);
        this.getPromiseReject = PropertyGetNode.create("reject", false, context);
        this.callPromiseRejectNode = JSFunctionCallNode.createCall();
        this.createPromiseCapability = JSFunctionCallNode.createCall();
        this.getPromise = PropertyGetNode.create("promise", false, context);
        this.asyncGeneratorResumeNextNode = AsyncGeneratorResumeNextNode.create(context);
    }

    public static AsyncGeneratorEnqueueNode create(JSContext context) {
        return new AsyncGeneratorEnqueueNode(context);
    }

    @SuppressWarnings("unchecked")
    public Object execute(VirtualFrame frame, DynamicObject generator, Completion completion) {
        DynamicObject promiseCapability = newPromiseCapability();
        if (!JSGuards.isJSObject(generator) || !hasAsyncGeneratorInternalSlots.executeHasHiddenKey(generator)) {
            Object badGeneratorError = Errors.createTypeErrorAsyncGeneratorObjectExpected().getErrorObjectEager(context);
            Object reject = getPromiseReject.getValue(generator);
            callPromiseRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, reject, badGeneratorError));
            return getPromise.getValue(promiseCapability);
        }
        ArrayDeque<AsyncGeneratorRequest> queue = (ArrayDeque<AsyncGeneratorRequest>) getAsyncGeneratorQueueNode.getValue(generator);
        AsyncGeneratorRequest request = AsyncGeneratorRequest.create(completion, promiseCapability);
        queueAdd(queue, request);
        AsyncGeneratorState state = (AsyncGeneratorState) getGeneratorState.getValue(generator);
        if (state != AsyncGeneratorState.Executing) {
            asyncGeneratorResumeNextNode.execute(frame, generator);
        }
        return getPromise.getValue(promiseCapability);
    }

    private DynamicObject newPromiseCapability() {
        return (DynamicObject) createPromiseCapability.executeCall(JSArguments.createZeroArg(Undefined.instance, context.getRealm().getAsyncFunctionPromiseCapabilityConstructor()));
    }

    @TruffleBoundary
    private static void queueAdd(ArrayDeque<AsyncGeneratorRequest> queue, AsyncGeneratorRequest request) {
        queue.addLast(request);
    }
}

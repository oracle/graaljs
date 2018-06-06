/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.ArrayDeque;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.AsyncGeneratorState;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncGeneratorEnqueueNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getGeneratorState;
    @Child private PropertyGetNode getAsyncGeneratorQueueNode;
    @Child private HasHiddenKeyCacheNode hasAsyncGeneratorInternalSlots;
    @Child private JSFunctionCallNode callPromiseRejectNode;
    @Child private NewPromiseCapabilityNode newPromiseCapability;
    @Child private AsyncGeneratorResumeNextNode asyncGeneratorResumeNextNode;
    private final ConditionProfile notExecutingProf = ConditionProfile.createBinaryProfile();
    private final JSContext context;

    protected AsyncGeneratorEnqueueNode(JSContext context) {
        this.context = context;
        this.getGeneratorState = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
        this.getAsyncGeneratorQueueNode = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_QUEUE_ID, context);
        this.hasAsyncGeneratorInternalSlots = HasHiddenKeyCacheNode.create(JSFunction.ASYNC_GENERATOR_QUEUE_ID);
        this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
        this.asyncGeneratorResumeNextNode = AsyncGeneratorResumeNextNode.create(context);
    }

    public static AsyncGeneratorEnqueueNode create(JSContext context) {
        return new AsyncGeneratorEnqueueNode(context);
    }

    @SuppressWarnings("unchecked")
    public Object execute(VirtualFrame frame, Object generator, Completion completion) {
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        if (!JSGuards.isJSObject(generator) || !hasAsyncGeneratorInternalSlots.executeHasHiddenKey(generator)) {
            enterErrorBranch();
            return badGeneratorError(promiseCapability);
        }
        ArrayDeque<AsyncGeneratorRequest> queue = (ArrayDeque<AsyncGeneratorRequest>) getAsyncGeneratorQueueNode.getValue(generator);
        AsyncGeneratorRequest request = AsyncGeneratorRequest.create(completion, promiseCapability);
        queueAdd(queue, request);
        AsyncGeneratorState state = (AsyncGeneratorState) getGeneratorState.getValue(generator);
        if (notExecutingProf.profile(state != AsyncGeneratorState.Executing)) {
            asyncGeneratorResumeNextNode.execute(frame, (DynamicObject) generator);
        }
        return promiseCapability.getPromise();
    }

    private PromiseCapabilityRecord newPromiseCapability() {
        return newPromiseCapability.executeDefault();
    }

    @TruffleBoundary
    private static void queueAdd(ArrayDeque<AsyncGeneratorRequest> queue, AsyncGeneratorRequest request) {
        queue.addLast(request);
    }

    private void enterErrorBranch() {
        if (callPromiseRejectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callPromiseRejectNode = insert(JSFunctionCallNode.createCall());
        }
    }

    private Object badGeneratorError(PromiseCapabilityRecord promiseCapability) {
        Object badGeneratorError = Errors.createTypeErrorAsyncGeneratorObjectExpected().getErrorObjectEager(context);
        Object reject = promiseCapability.getReject();
        callPromiseRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, reject, badGeneratorError));
        return promiseCapability.getPromise();
    }
}

/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DisposeCapability;
import com.oracle.truffle.js.runtime.util.DisposeCapability.DisposableResource;

public final class AsyncDisposeResourcesNode extends AbstractDisposeResourcesNode {
    private static final HiddenKey ASYNC_DISPOSE_STATE_ID = new HiddenKey("AsyncDisposeState");
    private static final HiddenKey PROMISE_CAPABILITY_ID = new HiddenKey("PromiseCapability");

    static final class AsyncDisposeState {
        private final DisposeCapability capability;
        private Object errorObject;
        private boolean needsAwait;
        private boolean hasAwaited;

        private AsyncDisposeState(DisposeCapability capability, Object errorObject) {
            this.capability = capability;
            this.errorObject = errorObject;
        }
    }

    @Child private PromiseResolveNode promiseResolveNode;
    @Child private PerformPromiseThenNode performPromiseThenNode;
    @Child private JSFunctionCallNode callNode;
    @Child private PropertySetNode setAsyncDisposeState;
    @Child private PropertySetNode setPromiseCapability;

    private final JSContext context;

    private AsyncDisposeResourcesNode(JSContext context) {
        this.context = context;
        this.promiseResolveNode = PromiseResolveNode.create(context);
        this.performPromiseThenNode = PerformPromiseThenNode.create();
        this.callNode = JSFunctionCallNode.createCall();
        this.setAsyncDisposeState = PropertySetNode.createSetHidden(ASYNC_DISPOSE_STATE_ID, context);
        this.setPromiseCapability = PropertySetNode.createSetHidden(PROMISE_CAPABILITY_ID, context);
    }

    public static AsyncDisposeResourcesNode create(JSContext context) {
        return new AsyncDisposeResourcesNode(context);
    }

    static AsyncDisposeState createState(DisposeCapability capability, Object currentError) {
        return new AsyncDisposeState(capability, currentError);
    }

    /**
     * Disposes resources synchronously until either disposal is complete or a value must be
     * awaited. Returns {@code null} when disposal is complete, otherwise the value to await.
     */
    Object disposeUntilAwait(AsyncDisposeState state) {
        DisposeCapability capability = state.capability;
        Object errorObject = state.errorObject;
        boolean needsAwait = state.needsAwait;
        boolean hasAwaited = state.hasAwaited;
        DisposableResource resource;
        while ((resource = capability.popResource()) != null) {
            if (!resource.isAsyncDispose() && needsAwait && !hasAwaited) {
                capability.pushResourceUnchecked(resource);
                return awaitResult(state, errorObject, false, false, Undefined.instance);
            }

            if (resource.getDisposeMethod() == Undefined.instance) {
                assert resource.isAsyncDispose();
                needsAwait = true;
                continue;
            }

            Object promiseOrValue;
            try {
                promiseOrValue = callDisposeMethod(resource);
            } catch (Throwable throwable) {
                errorObject = combineDisposeErrors(captureDisposeError(throwable), errorObject);
                continue;
            }

            if (resource.isAsyncDispose()) {
                return awaitResult(state, errorObject, needsAwait, true, promiseOrValue);
            }
        }
        if (needsAwait && !hasAwaited) {
            return awaitResult(state, errorObject, false, false, Undefined.instance);
        }
        state.errorObject = errorObject;
        return null;
    }

    private static Object awaitResult(AsyncDisposeState state, Object errorObject, boolean needsAwait, boolean hasAwaited, Object promiseOrValue) {
        state.errorObject = errorObject;
        state.needsAwait = needsAwait;
        state.hasAwaited = hasAwaited;
        return promiseOrValue;
    }

    void rejectAwait(AsyncDisposeState state, Object reason) {
        state.errorObject = combineDisposeErrors(reason, state.errorObject);
    }

    void complete(AsyncDisposeState state) {
        if (state.errorObject != DisposeCapability.NO_ERROR) {
            throwError(state.errorObject);
        }
    }

    public void execute(DisposeCapability capability, Object currentError, PromiseCapabilityRecord promiseCapability) {
        continueDispose(createState(capability, currentError), promiseCapability);
    }

    private void continueDispose(AsyncDisposeState state, PromiseCapabilityRecord promiseCapability) {
        Object promiseOrValue = disposeUntilAwait(state);
        if (promiseOrValue == null) {
            complete(state, promiseCapability);
        } else {
            scheduleAwait(state, promiseCapability, promiseOrValue);
        }
    }

    private void scheduleAwait(AsyncDisposeState state, PromiseCapabilityRecord promiseCapability, Object promiseOrValue) {
        JSPromiseObject promise = promiseResolveNode.executeDefault(promiseOrValue);
        JSFunctionObject onFulfilled = createContinuationFunction(state, promiseCapability, false);
        JSFunctionObject onRejected = createContinuationFunction(state, promiseCapability, true);
        performPromiseThenNode.execute(promise, onFulfilled, onRejected);
    }

    private JSFunctionObject createContinuationFunction(AsyncDisposeState state, PromiseCapabilityRecord promiseCapability, boolean rejected) {
        BuiltinFunctionKey key = rejected ? BuiltinFunctionKey.AsyncDisposeResourcesReject : BuiltinFunctionKey.AsyncDisposeResourcesContinue;
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(key, ctx -> createContinuationFunctionData(ctx, rejected));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setAsyncDisposeState.setValue(function, state);
        setPromiseCapability.setValue(function, promiseCapability);
        return function;
    }

    private void complete(AsyncDisposeState state, PromiseCapabilityRecord promiseCapability) {
        Object callback = state.errorObject == DisposeCapability.NO_ERROR ? promiseCapability.getResolve() : promiseCapability.getReject();
        Object value = state.errorObject == DisposeCapability.NO_ERROR ? Undefined.instance : state.errorObject;
        callNode.executeCall(JSArguments.createOneArg(Undefined.instance, callback, value));
    }

    private static JSFunctionData createContinuationFunctionData(JSContext context, boolean rejected) {
        return JSFunctionData.createCallOnly(context, new AsyncDisposeResourcesRootNode(context, rejected).getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static final class AsyncDisposeResourcesRootNode extends JavaScriptRootNode {
        @Child private JavaScriptNode valueNode;
        @Child private AsyncDisposeResourcesNode asyncDisposeResourcesNode;
        @Child private PropertyGetNode getAsyncDisposeState;
        @Child private PropertyGetNode getPromiseCapability;

        private final boolean rejected;

        private AsyncDisposeResourcesRootNode(JSContext context, boolean rejected) {
            this.rejected = rejected;
            this.valueNode = rejected ? AccessIndexedArgumentNode.create(0) : null;
            this.asyncDisposeResourcesNode = AsyncDisposeResourcesNode.create(context);
            this.getAsyncDisposeState = PropertyGetNode.createGetHidden(ASYNC_DISPOSE_STATE_ID, context);
            this.getPromiseCapability = PropertyGetNode.createGetHidden(PROMISE_CAPABILITY_ID, context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
            AsyncDisposeState state = (AsyncDisposeState) getAsyncDisposeState.getValue(functionObject);
            PromiseCapabilityRecord promiseCapability = (PromiseCapabilityRecord) getPromiseCapability.getValue(functionObject);
            if (rejected) {
                asyncDisposeResourcesNode.rejectAwait(state, valueNode.execute(frame));
            }
            asyncDisposeResourcesNode.continueDispose(state, promiseCapability);
            return Undefined.instance;
        }
    }
}

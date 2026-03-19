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
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSArguments;
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
    private static final HiddenKey CAPABILITY_ID = new HiddenKey("DisposeCapability");
    private static final HiddenKey ERROR_ID = new HiddenKey("DisposeError");
    private static final HiddenKey PROMISE_CAPABILITY_ID = new HiddenKey("PromiseCapability");
    private static final HiddenKey NEEDS_AWAIT_ID = new HiddenKey("NeedsAwait");
    private static final HiddenKey HAS_AWAITED_ID = new HiddenKey("HasAwaited");

    @Child private PromiseResolveNode promiseResolveNode;
    @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
    @Child private PerformPromiseThenNode performPromiseThenNode;
    @Child private JSFunctionCallNode callNode;
    @Child private PropertySetNode setCapability;
    @Child private PropertySetNode setError;
    @Child private PropertySetNode setPromiseCapability;
    @Child private PropertySetNode setNeedsAwait;
    @Child private PropertySetNode setHasAwaited;

    private final JSContext context;

    private AsyncDisposeResourcesNode(JSContext context) {
        this.context = context;
        this.promiseResolveNode = PromiseResolveNode.create(context);
        this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
        this.performPromiseThenNode = PerformPromiseThenNode.create(context);
        this.callNode = JSFunctionCallNode.createCall();
        this.setCapability = PropertySetNode.createSetHidden(CAPABILITY_ID, context);
        this.setError = PropertySetNode.createSetHidden(ERROR_ID, context);
        this.setPromiseCapability = PropertySetNode.createSetHidden(PROMISE_CAPABILITY_ID, context);
        this.setNeedsAwait = PropertySetNode.createSetHidden(NEEDS_AWAIT_ID, context);
        this.setHasAwaited = PropertySetNode.createSetHidden(HAS_AWAITED_ID, context);
    }

    public static AsyncDisposeResourcesNode create(JSContext context) {
        return new AsyncDisposeResourcesNode(context);
    }

    public Object execute(DisposeCapability capability, Object currentError) {
        return disposeRemaining(capability, currentError, null, false, false);
    }

    Object resumeRejected(DisposeCapability capability, Object currentError, Object rejection, boolean needsAwait, boolean hasAwaited) {
        return disposeRemaining(capability, combineDisposeErrors(rejection, currentError), null, needsAwait, hasAwaited);
    }

    public void execute(DisposeCapability capability, Object currentError, PromiseCapabilityRecord promiseCapability) {
        disposeRemaining(capability, currentError, promiseCapability, false, false);
    }

    void execute(DisposeCapability capability, Object currentError, PromiseCapabilityRecord promiseCapability, boolean needsAwait, boolean hasAwaited) {
        disposeRemaining(capability, currentError, promiseCapability, needsAwait, hasAwaited);
    }

    void resumeRejected(DisposeCapability capability, Object currentError, Object rejection, PromiseCapabilityRecord promiseCapability, boolean needsAwait, boolean hasAwaited) {
        disposeRemaining(capability, combineDisposeErrors(rejection, currentError), promiseCapability, needsAwait, hasAwaited);
    }

    private Object disposeRemaining(DisposeCapability capability, Object currentError, PromiseCapabilityRecord promiseCapability, boolean needsAwaitParam, boolean hasAwaited) {
        Object errorObject = currentError;
        DisposableResource resource;
        boolean needsAwait = needsAwaitParam;
        while ((resource = capability.popResource()) != null) {
            if (!resource.isAsyncDispose() && needsAwait && !hasAwaited) {
                capability.pushResourceUnchecked(resource);
                return awaitResult(capability, errorObject, promiseCapability, false, hasAwaited, Undefined.instance);
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
                try {
                    return awaitResult(capability, errorObject, promiseCapability, needsAwait, true, promiseOrValue);
                } catch (Throwable throwable) {
                    errorObject = combineDisposeErrors(captureDisposeError(throwable), errorObject);
                }
            }
        }
        if (needsAwait && !hasAwaited) {
            return awaitResult(capability, errorObject, promiseCapability, false, false, Undefined.instance);
        }
        if (promiseCapability != null) {
            if (errorObject != DisposeCapability.NO_ERROR) {
                rejectPromise(promiseCapability, errorObject);
            } else {
                resolvePromise(promiseCapability, Undefined.instance);
            }
            return Undefined.instance;
        }
        if (errorObject != DisposeCapability.NO_ERROR) {
            throwError(errorObject);
        }
        return Undefined.instance;
    }

    private Object awaitResult(DisposeCapability capability, Object errorObject, PromiseCapabilityRecord promiseCapability, boolean needsAwait, boolean hasAwaited, Object promiseOrValue) {
        JSPromiseObject promise = promiseResolveNode.executeDefault(promiseOrValue);
        JSFunctionObject onFulfilled = createContinuationFunction(capability, errorObject, false, promiseCapability, needsAwait, hasAwaited);
        JSFunctionObject onRejected = createContinuationFunction(capability, errorObject, true, promiseCapability, needsAwait, hasAwaited);
        if (promiseCapability == null) {
            return performPromiseThenNode.execute(promise, onFulfilled, onRejected, newPromiseCapabilityNode.executeDefault());
        }
        performPromiseThenNode.execute(promise, onFulfilled, onRejected);
        return Undefined.instance;
    }

    private JSFunctionObject createContinuationFunction(DisposeCapability capability, Object errorObject, boolean rejected, PromiseCapabilityRecord promiseCapability,
                    boolean needsAwait, boolean hasAwaited) {
        BuiltinFunctionKey key = rejected ? BuiltinFunctionKey.AsyncDisposeResourcesReject : BuiltinFunctionKey.AsyncDisposeResourcesContinue;
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(key, ctx -> createContinuationFunctionData(ctx, rejected));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setCapability.setValue(function, capability);
        setError.setValue(function, errorObject);
        setPromiseCapability.setValue(function, promiseCapability == null ? Undefined.instance : promiseCapability);
        setNeedsAwait.setValue(function, needsAwait);
        setHasAwaited.setValue(function, hasAwaited);
        return function;
    }

    private void resolvePromise(PromiseCapabilityRecord promiseCapability, Object value) {
        callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getResolve(), value));
    }

    private void rejectPromise(PromiseCapabilityRecord promiseCapability, Object error) {
        callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getReject(), error));
    }

    private static JSFunctionData createContinuationFunctionData(JSContext context, boolean rejected) {
        return JSFunctionData.createCallOnly(context, new AsyncDisposeResourcesRootNode(context, rejected).getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static final class AsyncDisposeResourcesRootNode extends JavaScriptRootNode {
        @Child private JavaScriptNode valueNode;
        @Child private AsyncDisposeResourcesNode asyncDisposeResourcesNode;
        @Child private PropertyGetNode getCapability;
        @Child private PropertyGetNode getError;
        @Child private PropertyGetNode getPromiseCapability;
        @Child private PropertyGetNode getNeedsAwait;
        @Child private PropertyGetNode getHasAwaited;

        private final boolean rejected;

        private AsyncDisposeResourcesRootNode(JSContext context, boolean rejected) {
            this.rejected = rejected;
            this.valueNode = rejected ? AccessIndexedArgumentNode.create(0) : null;
            this.asyncDisposeResourcesNode = AsyncDisposeResourcesNode.create(context);
            this.getCapability = PropertyGetNode.createGetHidden(CAPABILITY_ID, context);
            this.getError = PropertyGetNode.createGetHidden(ERROR_ID, context);
            this.getPromiseCapability = PropertyGetNode.createGetHidden(PROMISE_CAPABILITY_ID, context);
            this.getNeedsAwait = PropertyGetNode.createGetHidden(NEEDS_AWAIT_ID, context);
            this.getHasAwaited = PropertyGetNode.createGetHidden(HAS_AWAITED_ID, context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
            DisposeCapability capability = (DisposeCapability) getCapability.getValue(functionObject);
            Object errorObject = getError.getValue(functionObject);
            Object promiseCapability = getPromiseCapability.getValue(functionObject);
            boolean needsAwait = (boolean) getNeedsAwait.getValue(functionObject);
            boolean hasAwaited = (boolean) getHasAwaited.getValue(functionObject);
            boolean completesPromise = promiseCapability != Undefined.instance;
            if (rejected) {
                if (completesPromise) {
                    asyncDisposeResourcesNode.resumeRejected(capability, errorObject, valueNode.execute(frame), (PromiseCapabilityRecord) promiseCapability, needsAwait, hasAwaited);
                    return Undefined.instance;
                }
                return asyncDisposeResourcesNode.resumeRejected(capability, errorObject, valueNode.execute(frame), needsAwait, hasAwaited);
            }
            if (completesPromise) {
                asyncDisposeResourcesNode.execute(capability, errorObject, (PromiseCapabilityRecord) promiseCapability, needsAwait, hasAwaited);
                return Undefined.instance;
            }
            return asyncDisposeResourcesNode.disposeRemaining(capability, errorObject, null, needsAwait, hasAwaited);
        }
    }
}

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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DisposeCapability;

public abstract class AddDisposableResourceNode extends JavaScriptBaseNode {
    private static final HiddenKey SYNC_DISPOSE_METHOD_ID = new HiddenKey("SyncDisposeMethod");

    @Child private IsObjectNode isObjectNode;
    @Child private GetMethodNode getDisposeMethodNode;
    @Child private GetMethodNode getAsyncDisposeMethodNode;
    @Child private PropertySetNode setSyncDisposeMethodNode;
    private final BranchProfile errorProfile;
    private final JSContext context;
    private final boolean asyncDispose;

    protected AddDisposableResourceNode(JSContext context, boolean asyncDispose) {
        this.context = context;
        this.asyncDispose = asyncDispose;
        this.isObjectNode = IsObjectNode.create();
        this.getDisposeMethodNode = GetMethodNode.create(context, Symbol.SYMBOL_DISPOSE);
        this.getAsyncDisposeMethodNode = asyncDispose ? GetMethodNode.create(context, Symbol.SYMBOL_ASYNC_DISPOSE) : null;
        this.setSyncDisposeMethodNode = asyncDispose ? PropertySetNode.createSetHidden(SYNC_DISPOSE_METHOD_ID, context) : null;
        this.errorProfile = BranchProfile.create();
    }

    public static AddDisposableResourceNode create(JSContext context, boolean asyncDispose) {
        return AddDisposableResourceNodeGen.create(context, asyncDispose);
    }

    public final void execute(DisposeCapability capability, Object value) {
        executeImpl(capability, value);
    }

    // AddDisposableResource without method
    protected abstract void executeImpl(DisposeCapability capability, Object value);

    @Specialization
    protected void doResource(DisposeCapability capability, Object value,
                    @Cached InlinedBranchProfile growProfile) {
        if (JSRuntime.isNullish(value)) {
            if (asyncDispose) {
                capability.pushResource(DisposeCapability.forResource(Undefined.instance, true, Undefined.instance), this, growProfile);
            }
            return;
        }

        Object disposeMethod = getDisposeMethod(value);
        if (disposeMethod == Undefined.instance) {
            errorProfile.enter();
            throw Errors.createTypeError("Object is not disposable", this);
        }
        capability.pushResource(DisposeCapability.forResource(value, asyncDispose, disposeMethod), this, growProfile);
    }

    // AddDisposableResource with method
    public static void addCallback(DisposeCapability capability, Object disposeMethod, Object argument, boolean asyncDispose, Node node, InlinedBranchProfile growProfile) {
        capability.pushResource(DisposeCapability.forCallback(disposeMethod, argument, asyncDispose), node, growProfile);
    }

    private Object getDisposeMethod(Object value) {
        if (!isObjectNode.executeBoolean(value)) {
            errorProfile.enter();
            throw Errors.createTypeErrorNotAnObject(value, this);
        }
        if (!asyncDispose) {
            return getDisposeMethodNode.executeWithTarget(value);
        }
        Object asyncDisposeMethod = getAsyncDisposeMethodNode.executeWithTarget(value);
        if (asyncDisposeMethod != Undefined.instance) {
            return asyncDisposeMethod;
        }
        Object syncDisposeMethod = getDisposeMethodNode.executeWithTarget(value);
        if (syncDisposeMethod == Undefined.instance) {
            return Undefined.instance;
        }
        return createAsyncDisposeFromSyncDisposeMethod(syncDisposeMethod);
    }

    private JSFunctionObject createAsyncDisposeFromSyncDisposeMethod(Object syncDisposeMethod) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.AsyncDisposeFromSyncDisposeMethod,
                        AddDisposableResourceNode::createAsyncDisposeFromSyncDisposeMethodData);
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setSyncDisposeMethodNode.setValue(function, syncDisposeMethod);
        return function;
    }

    private static JSFunctionData createAsyncDisposeFromSyncDisposeMethodData(JSContext context) {
        return JSFunctionData.createCallOnly(context, new AsyncDisposeFromSyncDisposeMethodRootNode(context).getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    private static final class AsyncDisposeFromSyncDisposeMethodRootNode extends JavaScriptRootNode {
        @Child private PropertyGetNode getSyncDisposeMethodNode;
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private JSFunctionCallNode callNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        private AsyncDisposeFromSyncDisposeMethodRootNode(JSContext context) {
            this.getSyncDisposeMethodNode = PropertyGetNode.createGetHidden(SYNC_DISPOSE_METHOD_ID, context);
            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.callNode = JSFunctionCallNode.createCall();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
            Object syncDisposeMethod = getSyncDisposeMethodNode.getValue(functionObject);
            Object thisObj = JSFrameUtil.getThisObj(frame);
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            try {
                callNode.executeCall(JSArguments.createZeroArg(thisObj, syncDisposeMethod));
            } catch (Throwable throwable) {
                rejectPromise(promiseCapability, captureDisposeError(throwable));
                return promiseCapability.getPromise();
            }
            resolvePromise(promiseCapability, Undefined.instance);
            return promiseCapability.getPromise();
        }

        private void resolvePromise(PromiseCapabilityRecord promiseCapability, Object value) {
            callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getResolve(), value));
        }

        private void rejectPromise(PromiseCapabilityRecord promiseCapability, Object error) {
            callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getReject(), error));
        }

        private Object captureDisposeError(Throwable throwable) {
            if (throwable instanceof AbstractTruffleException ex) {
                return getErrorObjectNode().execute(ex, true);
            } else if (throwable instanceof StackOverflowError err) {
                return getErrorObjectNode().execute(err, true);
            } else {
                throw JSRuntime.rethrow(throwable);
            }
        }

        private TryCatchNode.GetErrorObjectNode getErrorObjectNode() {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create());
            }
            return getErrorObjectNode;
        }
    }
}

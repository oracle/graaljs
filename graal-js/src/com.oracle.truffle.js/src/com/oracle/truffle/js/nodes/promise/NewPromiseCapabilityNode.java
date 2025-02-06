/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class NewPromiseCapabilityNode extends JavaScriptBaseNode {
    public static final HiddenKey PROMISE_CAPABILITY_KEY = new HiddenKey("PromiseCapability");

    private final JSContext context;
    @Child private IsConstructorNode isConstructor;
    @Child private JSFunctionCallNode newPromise;
    @Child private IsCallableNode isCallable;
    @Child private PropertySetNode setPromiseCapability;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected NewPromiseCapabilityNode(JSContext context) {
        this.context = context;
        this.isConstructor = IsConstructorNode.create();
        this.newPromise = JSFunctionCallNode.createNew();
        this.isCallable = IsCallableNode.create();
        this.setPromiseCapability = PropertySetNode.createSetHidden(PROMISE_CAPABILITY_KEY, context);
    }

    @NeverDefault
    public static NewPromiseCapabilityNode create(JSContext context) {
        return new NewPromiseCapabilityNode(context);
    }

    public PromiseCapabilityRecord executeDefault() {
        return execute(getRealm().getPromiseConstructor());
    }

    @TruffleBoundary
    public static PromiseCapabilityRecord createDefault(JSRealm realm) {
        JSFunctionObject constructor = realm.getPromiseConstructor();
        JSContext context = realm.getContext();
        assert JSFunction.isConstructor(constructor);
        PromiseCapabilityRecord promiseCapability = PromiseCapabilityRecord.create(Undefined.instance, Undefined.instance, Undefined.instance);
        JSFunctionObject executor = getCapabilitiesExecutor(context, realm, promiseCapability);
        JSPromiseObject promise = (JSPromiseObject) JSFunction.construct(constructor, new Object[]{executor});
        assert JSFunction.isJSFunction(promiseCapability.getResolve()) && JSFunction.isJSFunction(promiseCapability.getReject());
        promiseCapability.setPromise(promise);
        return promiseCapability;
    }

    public PromiseCapabilityRecord execute(Object constructor) {
        if (!isConstructor.executeBoolean(constructor)) {
            errorBranch.enter();
            throw Errors.createTypeErrorNotAConstructor(constructor, context);
        }
        PromiseCapabilityRecord promiseCapability = PromiseCapabilityRecord.create(Undefined.instance, Undefined.instance, Undefined.instance);
        JSFunctionObject executor = getCapabilitiesExecutor(promiseCapability);
        Object promise = newPromise.executeCall(JSArguments.create(Undefined.instance, constructor, executor));
        if (!(promise instanceof JSDynamicObject)) {
            errorBranch.enter();
            throw Errors.createTypeError("Promise cannot be a foreign object");
        }
        if (!isCallable.executeBoolean(promiseCapability.getResolve()) || !isCallable.executeBoolean(promiseCapability.getReject())) {
            errorBranch.enter();
            throw Errors.createTypeError("Promise resolve or reject function is not callable");
        }
        promiseCapability.setPromise((JSDynamicObject) promise);
        return promiseCapability;
    }

    private JSFunctionObject getCapabilitiesExecutor(PromiseCapabilityRecord promiseCapability) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseGetCapabilitiesExecutor, (c) -> createGetCapabilitiesExecutorImpl(c));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setPromiseCapability.setValue(function, promiseCapability);
        return function;
    }

    private static JSFunctionObject getCapabilitiesExecutor(JSContext context, JSRealm realm, PromiseCapabilityRecord promiseCapability) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseGetCapabilitiesExecutor, (c) -> createGetCapabilitiesExecutorImpl(c));
        JSFunctionObject function = JSFunction.create(realm, functionData);
        JSObjectUtil.putHiddenProperty(function, PROMISE_CAPABILITY_KEY, promiseCapability);
        return function;
    }

    private static JSFunctionData createGetCapabilitiesExecutorImpl(JSContext context) {
        class GetCapabilitiesExecutorNode extends JavaScriptRootNode {
            @Child private JavaScriptNode resolveNode = AccessIndexedArgumentNode.create(0);
            @Child private JavaScriptNode rejectNode = AccessIndexedArgumentNode.create(1);
            @Child private PropertyGetNode getPromiseCapability = PropertyGetNode.createGetHidden(PROMISE_CAPABILITY_KEY, context);
            private final BranchProfile errorBranch = BranchProfile.create();

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
                PromiseCapabilityRecord capability = (PromiseCapabilityRecord) getPromiseCapability.getValue(functionObject);
                if (capability.getResolve() != Undefined.instance || capability.getReject() != Undefined.instance) {
                    errorBranch.enter();
                    throw Errors.createTypeError("error while creating capability!");
                }
                Object resolve = resolveNode.execute(frame);
                Object reject = rejectNode.execute(frame);
                capability.setResolve(resolve);
                capability.setReject(reject);
                return Undefined.instance;
            }
        }
        return JSFunctionData.createCallOnly(context, new GetCapabilitiesExecutorNode().getCallTarget(), 2, Strings.EMPTY_STRING);
    }
}

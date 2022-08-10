/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.AsyncHandlerRootNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncIteratorCloseNode extends JavaScriptBaseNode {
    private static final HiddenKey PROMISE_ID = new HiddenKey("promise");
    private static final HiddenKey COMPLETION_ID = new HiddenKey("completion");

    @Child private GetMethodNode getReturnNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private PropertyGetNode getConstructorNode;
    @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
    @Child private JSFunctionCallNode callNode;
    @Child private PropertySetNode setPromiseNode;
    @Child private PropertySetNode setCompletionNode;
    @Child private PerformPromiseThenNode performPromiseThenNode;

    private final JSContext context;

    protected AsyncIteratorCloseNode(JSContext context) {
        this.context = context;

        this.getReturnNode = GetMethodNode.create(context, Strings.RETURN);
        this.getConstructorNode = PropertyGetNode.create(JSObject.CONSTRUCTOR, context);
        this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
        this.setCompletionNode = PropertySetNode.createSetHidden(COMPLETION_ID, context);
        this.setPromiseNode = PropertySetNode.createSetHidden(PROMISE_ID, context);
        this.callNode = JSFunctionCallNode.createCall();
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.performPromiseThenNode = PerformPromiseThenNode.create(context);
    }

    public static AsyncIteratorCloseNode create(JSContext context) {
        return new AsyncIteratorCloseNode(context);
    }

    public final JSDynamicObject execute(JSDynamicObject iterator) {
        Object returnMethod = getReturnNode.executeWithTarget(iterator);
        if (returnMethod != Undefined.instance) {
            Object innerResult = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
            JSDynamicObject promise = this.toPromise(innerResult);
            return performPromiseThenNode.execute(promise, createCloseFunction(promise), Undefined.instance, newPromiseCapabilityNode.executeDefault());
        }
        return Undefined.instance;
    }

    public final JSDynamicObject executeAbrupt(JSDynamicObject iterator, Object error) {
        PromiseCapabilityRecord capabilityRecord = newPromiseCapabilityNode.executeDefault();
        callNode.executeCall(JSArguments.createOneArg(capabilityRecord.getPromise(), capabilityRecord.getReject(), error));
        JSDynamicObject completion = capabilityRecord.getPromise();

        try {
            Object returnMethod = getReturnNode.executeWithTarget(iterator);
            if (returnMethod != Undefined.instance) {
                Object innerResult = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
                JSDynamicObject promise = this.toPromise(innerResult);
                JSFunctionObject finallyFunction = createCloseAbruptFunction(promise, completion);
                return performPromiseThenNode.execute(promise, finallyFunction, finallyFunction, newPromiseCapabilityNode.executeDefault());
            }
        } catch (AbstractTruffleException e) {
            // re-throw outer exception, see AsyncIteratorClose
        }

        return completion;
    }

    private JSDynamicObject toPromise(Object promiseOrValue) {
        JSDynamicObject promise;
        if (!JSPromise.isJSPromise(promiseOrValue) || getConstructorNode.getValueOrDefault(promiseOrValue, Undefined.instance) != getRealm().getPromiseConstructor()) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getResolve(), promiseOrValue));
            return promiseCapability.getPromise();
        }

        return (JSDynamicObject) promiseOrValue;
    }

    public JSFunctionObject createCloseFunction(JSDynamicObject promise) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncIteratorClose, AsyncIteratorCloseNode::createCloseFunctionImpl);
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setPromiseNode.setValue(function, promise);
        return function;
    }
    public JSFunctionObject createCloseAbruptFunction(JSDynamicObject promise, Object completion) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncIteratorCloseAbrupt, AsyncIteratorCloseNode::createCloseAbruptFunctionImpl);
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setPromiseNode.setValue(function, promise);
        setCompletionNode.setValue(function, completion);
        return function;
    }

    private static JSFunctionData createCloseFunctionImpl(JSContext context) {
        return JSFunctionData.createCallOnly(context, new AsyncIteratorCloseRootNode(context, false).getCallTarget(), 1, Strings.EMPTY_STRING);
    }
    private static JSFunctionData createCloseAbruptFunctionImpl(JSContext context) {
        return JSFunctionData.createCallOnly(context, new AsyncIteratorCloseRootNode(context, true).getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    public static class AsyncIteratorCloseRootNode extends JavaScriptRootNode implements AsyncHandlerRootNode {
        @Child protected JavaScriptNode valueNode;

        @Child protected JSFunctionCallNode callNode;
        @Child private PropertyGetNode getCompletionNode;
        @Child private IsJSObjectNode isObjectNode;

        private final boolean isAbrupt;

        AsyncIteratorCloseRootNode(JSContext context, boolean isAbrupt) {
            valueNode = AccessIndexedArgumentNode.create(0);
            callNode = JSFunctionCallNode.createCall();
            isObjectNode = IsJSObjectNode.create();
            getCompletionNode = PropertyGetNode.createGetHidden(COMPLETION_ID, context);
            this.isAbrupt = isAbrupt;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (isAbrupt) {
                return getCompletionNode.getValue(JSFrameUtil.getFunctionObject(frame));
            } else {
                Object innerResult = valueNode.execute(frame);
                if (!isObjectNode.executeBoolean(innerResult)) {
                    throw Errors.createTypeErrorIterResultNotAnObject(innerResult, this);
                }
                return Undefined.instance;
            }
        }

        @Override
        public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
            assert JSFunction.isJSFunction(handlerFunction) && ((RootCallTarget) JSFunction.getFunctionData(handlerFunction).getCallTarget()).getRootNode() == this;
            JSDynamicObject promise = (JSDynamicObject) JSObjectUtil.getHiddenProperty(handlerFunction, PROMISE_ID);
            return new AsyncStackTraceInfo(promise, null);
        }
    }
}

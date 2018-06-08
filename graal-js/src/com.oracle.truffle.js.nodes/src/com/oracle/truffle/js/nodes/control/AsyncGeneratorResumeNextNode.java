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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.InternalCallNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.AsyncGeneratorState;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncGeneratorResumeNextNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getGeneratorState;
    @Child private PropertySetNode setGeneratorState;
    @Child private PropertyGetNode getAsyncGeneratorQueueNode;
    @Child private JSFunctionCallNode callPromiseResolveNode;
    @Child private PerformPromiseThenNode performPromiseThenNode;
    @Child private NewPromiseCapabilityNode newPromiseCapability;
    @Child private AsyncGeneratorResolveNode asyncGeneratorResolveNode;
    @Child private AsyncGeneratorRejectNode asyncGeneratorRejectNode;
    @Child private PropertySetNode setGenerator;
    @Child private PropertySetNode setPromiseIsHandled;
    private final ConditionProfile abruptProf = ConditionProfile.createBinaryProfile();
    protected final JSContext context;

    static final HiddenKey RETURN_PROCESSOR_GENERATOR = new HiddenKey("Generator");

    protected AsyncGeneratorResumeNextNode(JSContext context) {
        this.context = context;
        this.getGeneratorState = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
        this.setGeneratorState = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
        this.getAsyncGeneratorQueueNode = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_QUEUE_ID, context);
        this.asyncGeneratorResolveNode = AsyncGeneratorResolveNode.create(context);
    }

    public static AsyncGeneratorResumeNextNode create(JSContext context) {
        return new AsyncGeneratorResumeNextNode.WithCall(context);
    }

    public static AsyncGeneratorResumeNextNode createTailCall(JSContext context) {
        return new AsyncGeneratorResumeNextNode(context);
    }

    @SuppressWarnings("unchecked")
    public final Object execute(VirtualFrame frame, DynamicObject generator) {
        for (;;) {
            AsyncGeneratorState state = (AsyncGeneratorState) getGeneratorState.getValue(generator);
            assert state != AsyncGeneratorState.Executing;
            if (state == AsyncGeneratorState.AwaitingReturn) {
                return Undefined.instance;
            }
            ArrayDeque<AsyncGeneratorRequest> queue = (ArrayDeque<AsyncGeneratorRequest>) getAsyncGeneratorQueueNode.getValue(generator);
            if (queue.isEmpty()) {
                return Undefined.instance;
            }
            AsyncGeneratorRequest next = queue.peekFirst();
            if (abruptProf.profile(next.isAbruptCompletion())) {
                if (state == AsyncGeneratorState.SuspendedStart) {
                    setGeneratorState.setValue(generator, state = AsyncGeneratorState.Completed);
                }
                if (state == AsyncGeneratorState.Completed) {
                    if (next.isReturn()) {
                        enterReturnBranch();
                        setGeneratorState.setValue(generator, AsyncGeneratorState.AwaitingReturn);
                        PromiseCapabilityRecord promiseCapability = newPromiseCapability.executeDefault();
                        Object resolve = promiseCapability.getResolve();
                        callPromiseResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, resolve, next.getCompletionValue()));
                        DynamicObject onFulfilled = createAsyncGeneratorReturnProcessorFulfilledFunction(generator);
                        DynamicObject onRejected = createAsyncGeneratorReturnProcessorRejectedFunction(generator);
                        PromiseCapabilityRecord throwawayCapability = newPromiseCapability.executeDefault();
                        setPromiseIsHandled.setValueBoolean(throwawayCapability.getPromise(), true);
                        performPromiseThenNode.execute(promiseCapability.getPromise(), onFulfilled, onRejected, throwawayCapability);
                        return Undefined.instance;
                    } else {
                        assert next.isThrow();
                        enterThrowBranch();
                        // return ! AsyncGeneratorReject(generator, completion.[[Value]]).
                        asyncGeneratorRejectNode.performReject(frame, generator, next.getCompletionValue());
                        continue; // Perform ! AsyncGeneratorResumeNext(generator).
                    }
                }
            } else if (state == AsyncGeneratorState.Completed) {
                // return ! AsyncGeneratorResolve(generator, undefined, true).
                asyncGeneratorResolveNode.performResolve(frame, generator, Undefined.instance, true);
                continue; // Perform ! AsyncGeneratorResumeNext(generator).
            }
            assert state == AsyncGeneratorState.SuspendedStart || state == AsyncGeneratorState.SuspendedYield;
            setGeneratorState.setValue(generator, state = AsyncGeneratorState.Executing);
            return performResumeNext(generator, next.getCompletion());
        }
    }

    protected Object performResumeNext(@SuppressWarnings("unused") DynamicObject generator, Completion completion) {
        return completion;
    }

    private static class WithCall extends AsyncGeneratorResumeNextNode {
        @Child private PropertyGetNode getGeneratorTarget;
        @Child private PropertyGetNode getGeneratorContext;
        @Child private InternalCallNode callNode;

        protected WithCall(JSContext context) {
            super(context);
            this.getGeneratorTarget = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_TARGET_ID, context);
            this.getGeneratorContext = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_CONTEXT_ID, context);
            this.callNode = InternalCallNode.create();
        }

        @Override
        protected Object performResumeNext(DynamicObject generator, Completion completion) {
            CallTarget generatorTarget = (CallTarget) getGeneratorTarget.getValue(generator);
            Object generatorContext = getGeneratorContext.getValue(generator);
            callNode.execute(generatorTarget, new Object[]{generatorContext, generator, completion});
            return Undefined.instance;
        }
    }

    private void enterReturnBranch() {
        if (newPromiseCapability == null || callPromiseResolveNode == null || performPromiseThenNode == null || setGenerator == null || setPromiseIsHandled == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.newPromiseCapability = insert(NewPromiseCapabilityNode.create(context));
            this.callPromiseResolveNode = insert(JSFunctionCallNode.createCall());
            this.performPromiseThenNode = insert(PerformPromiseThenNode.create(context));
            this.setGenerator = insert(PropertySetNode.createSetHidden(RETURN_PROCESSOR_GENERATOR, context));
            this.setPromiseIsHandled = insert(PropertySetNode.createSetHidden(JSPromise.PROMISE_IS_HANDLED, context));
        }
    }

    private void enterThrowBranch() {
        if (asyncGeneratorRejectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.asyncGeneratorRejectNode = insert(AsyncGeneratorRejectNode.create(context));
        }
    }

    private DynamicObject createAsyncGeneratorReturnProcessorFulfilledFunction(DynamicObject generator) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncGeneratorReturnFulfilled, (c) -> createAsyncGeneratorReturnProcessorFulfilledImpl(c));
        DynamicObject function = JSFunction.create(context.getRealm(), functionData);
        setGenerator.setValue(function, generator);
        return function;
    }

    private static JSFunctionData createAsyncGeneratorReturnProcessorFulfilledImpl(JSContext context) {
        class AsyncGeneratorReturnFulfilledRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
            @Child private AsyncGeneratorResolveNode asyncGeneratorResolveNode = AsyncGeneratorResolveNode.create(context);
            @Child private PropertyGetNode getGenerator = PropertyGetNode.createGetHidden(RETURN_PROCESSOR_GENERATOR, context);
            @Child private PropertySetNode setGeneratorState = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                DynamicObject generatorObject = (DynamicObject) getGenerator.getValue(functionObject);
                setGeneratorState.setValue(generatorObject, AsyncGeneratorState.Completed);
                Object value = valueNode.execute(frame);
                return asyncGeneratorResolveNode.execute(frame, generatorObject, value, true);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new AsyncGeneratorReturnFulfilledRootNode());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }

    private DynamicObject createAsyncGeneratorReturnProcessorRejectedFunction(DynamicObject generator) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncGeneratorReturnRejected, (c) -> createAsyncGeneratorReturnProcessorRejectedImpl(c));
        DynamicObject function = JSFunction.create(context.getRealm(), functionData);
        setGenerator.setValue(function, generator);
        return function;
    }

    private static JSFunctionData createAsyncGeneratorReturnProcessorRejectedImpl(JSContext context) {
        class AsyncGeneratorReturnRejectedRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode reasonNode = AccessIndexedArgumentNode.create(0);
            @Child private AsyncGeneratorRejectNode asyncGeneratorRejectNode = AsyncGeneratorRejectNode.create(context);
            @Child private PropertyGetNode getGenerator = PropertyGetNode.createGetHidden(RETURN_PROCESSOR_GENERATOR, context);
            @Child private PropertySetNode setGeneratorState = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                DynamicObject generatorObject = (DynamicObject) getGenerator.getValue(functionObject);
                setGeneratorState.setValue(generatorObject, AsyncGeneratorState.Completed);
                Object reason = reasonNode.execute(frame);
                return asyncGeneratorRejectNode.execute(frame, generatorObject, reason);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new AsyncGeneratorReturnRejectedRootNode());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }
}

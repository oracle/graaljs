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
package com.oracle.truffle.js.nodes.control;

import java.util.ArrayDeque;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
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
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSAsyncGeneratorObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.AsyncGeneratorState;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncGeneratorResumeNextNode extends JavaScriptBaseNode {
    @Child private JSFunctionCallNode callPromiseResolveNode;
    @Child private PerformPromiseThenNode performPromiseThenNode;
    @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
    @Child private AsyncGeneratorResolveNode asyncGeneratorResolveNode;
    @Child private AsyncGeneratorRejectNode asyncGeneratorRejectNode;
    @Child private PropertySetNode setGeneratorNode;
    @Child private PromiseResolveNode promiseResolveNode;
    @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
    private final ConditionProfile abruptProf = ConditionProfile.create();
    protected final JSContext context;

    static final HiddenKey RETURN_PROCESSOR_GENERATOR = new HiddenKey("Generator");

    protected AsyncGeneratorResumeNextNode(JSContext context) {
        this.context = context;
        this.asyncGeneratorResolveNode = AsyncGeneratorResolveNode.create(context);
    }

    public static AsyncGeneratorResumeNextNode create(JSContext context) {
        return new AsyncGeneratorResumeNextNode.WithCall(context);
    }

    public static AsyncGeneratorResumeNextNode createTailCall(JSContext context) {
        return new AsyncGeneratorResumeNextNode(context);
    }

    public final Object execute(JSAsyncGeneratorObject generator) {
        for (;;) {
            AsyncGeneratorState state = generator.getAsyncGeneratorState();
            assert state != AsyncGeneratorState.Executing;
            if (state == AsyncGeneratorState.AwaitingReturn) {
                return Undefined.instance;
            }
            ArrayDeque<AsyncGeneratorRequest> queue = generator.getAsyncGeneratorQueue();
            if (queue.isEmpty()) {
                return Undefined.instance;
            }
            AsyncGeneratorRequest next = queue.peekFirst();
            if (abruptProf.profile(next.isAbruptCompletion())) {
                if (state == AsyncGeneratorState.SuspendedStart) {
                    state = AsyncGeneratorState.Completed;
                    generator.setAsyncGeneratorState(state);
                }
                if (state == AsyncGeneratorState.Completed) {
                    if (next.isReturn()) { // AsyncGeneratorAwaitReturn
                        enterReturnBranch();
                        generator.setAsyncGeneratorState(AsyncGeneratorState.AwaitingReturn);
                        JSPromiseObject promise;
                        try {
                            promise = promiseResolve(next.getCompletionValue());
                        } catch (AbstractTruffleException e) {
                            asyncGeneratorRejectBrokenPromise(generator, e);
                            continue; // Perform AsyncGeneratorDrainQueue(generator).
                        }
                        JSFunctionObject onFulfilled = createAsyncGeneratorReturnProcessorFulfilledFunction(generator);
                        JSFunctionObject onRejected = createAsyncGeneratorReturnProcessorRejectedFunction(generator);
                        PromiseCapabilityRecord throwawayCapability = newThrowawayCapability();
                        performPromiseThenNode.execute(promise, onFulfilled, onRejected, throwawayCapability);
                        return Undefined.instance;
                    } else {
                        assert next.isThrow();
                        enterThrowBranch();
                        // return AsyncGeneratorReject(generator, completion.[[Value]]).
                        asyncGeneratorRejectNode.performReject(generator, next.getCompletionValue());
                        continue; // Perform AsyncGeneratorDrainQueue(generator).
                    }
                }
            } else if (state == AsyncGeneratorState.Completed) {
                // return AsyncGeneratorResolve(generator, undefined, true).
                asyncGeneratorResolveNode.performResolve(generator, Undefined.instance, true);
                continue; // Perform AsyncGeneratorDrainQueue(generator).
            }
            assert state == AsyncGeneratorState.SuspendedStart || state == AsyncGeneratorState.SuspendedYield;
            generator.setAsyncGeneratorState(AsyncGeneratorState.Executing);
            return performResumeNext(generator, next.getCompletion());
        }
    }

    private JSPromiseObject promiseResolve(Object value) {
        if (context.usePromiseResolve()) {
            if (promiseResolveNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseResolveNode = insert(PromiseResolveNode.create(context));
            }
            return (JSPromiseObject) promiseResolveNode.execute(getRealm().getPromiseConstructor(), value);
        } else {
            if (callPromiseResolveNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callPromiseResolveNode = insert(JSFunctionCallNode.createCall());
            }
            PromiseCapabilityRecord promiseCapability = newPromiseCapability();
            callPromiseResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), value));
            return (JSPromiseObject) promiseCapability.getPromise();
        }
    }

    protected Object performResumeNext(@SuppressWarnings("unused") JSAsyncGeneratorObject generator, Completion completion) {
        return completion;
    }

    private static class WithCall extends AsyncGeneratorResumeNextNode {
        @Child private InternalCallNode callNode;

        protected WithCall(JSContext context) {
            super(context);
            this.callNode = InternalCallNode.create();
        }

        @Override
        protected Object performResumeNext(JSAsyncGeneratorObject generator, Completion completion) {
            CallTarget generatorTarget = generator.getAsyncGeneratorTarget();
            Object generatorContext = generator.getAsyncGeneratorContext();
            callNode.execute(generatorTarget, JSArguments.createResumeArguments(generatorContext, generator, completion));
            return Undefined.instance;
        }
    }

    private void enterReturnBranch() {
        if (performPromiseThenNode == null || setGeneratorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.performPromiseThenNode = insert(PerformPromiseThenNode.create(context));
            this.setGeneratorNode = insert(PropertySetNode.createSetHidden(RETURN_PROCESSOR_GENERATOR, context));
        }
    }

    private PromiseCapabilityRecord newPromiseCapability() {
        if (newPromiseCapabilityNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            newPromiseCapabilityNode = insert(NewPromiseCapabilityNode.create(context));
        }
        return newPromiseCapabilityNode.executeDefault();
    }

    private PromiseCapabilityRecord newThrowawayCapability() {
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2019) {
            return null;
        }
        PromiseCapabilityRecord throwawayCapability = newPromiseCapability();
        ((JSPromiseObject) throwawayCapability.getPromise()).setIsHandled(true);
        return throwawayCapability;
    }

    private void enterThrowBranch() {
        if (asyncGeneratorRejectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.asyncGeneratorRejectNode = insert(AsyncGeneratorRejectNode.create());
        }
    }

    private void asyncGeneratorRejectBrokenPromise(JSAsyncGeneratorObject generator, AbstractTruffleException exception) {
        if (getErrorObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
        }
        enterThrowBranch();
        generator.setAsyncGeneratorState(AsyncGeneratorState.Completed);
        Object error = getErrorObjectNode.execute(exception);
        asyncGeneratorRejectNode.performReject(generator, error);
    }

    private JSFunctionObject createAsyncGeneratorReturnProcessorFulfilledFunction(JSDynamicObject generator) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncGeneratorReturnFulfilled, (c) -> createAsyncGeneratorReturnProcessorFulfilledImpl(c));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setGeneratorNode.setValue(function, generator);
        return function;
    }

    private static JSFunctionData createAsyncGeneratorReturnProcessorFulfilledImpl(JSContext context) {
        class AsyncGeneratorReturnFulfilledRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
            @Child private AsyncGeneratorResolveNode asyncGeneratorResolveNode = AsyncGeneratorResolveNode.create(context);
            @Child private PropertyGetNode getGenerator = PropertyGetNode.createGetHidden(RETURN_PROCESSOR_GENERATOR, context);

            @Override
            public Object execute(VirtualFrame frame) {
                JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                JSAsyncGeneratorObject generatorObject = (JSAsyncGeneratorObject) getGenerator.getValue(functionObject);
                generatorObject.setAsyncGeneratorState(AsyncGeneratorState.Completed);
                Object value = valueNode.execute(frame);
                return asyncGeneratorResolveNode.execute(generatorObject, value, true);
            }
        }
        return JSFunctionData.createCallOnly(context, new AsyncGeneratorReturnFulfilledRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private JSFunctionObject createAsyncGeneratorReturnProcessorRejectedFunction(JSDynamicObject generator) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncGeneratorReturnRejected, (c) -> createAsyncGeneratorReturnProcessorRejectedImpl(c));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setGeneratorNode.setValue(function, generator);
        return function;
    }

    private static JSFunctionData createAsyncGeneratorReturnProcessorRejectedImpl(JSContext context) {
        class AsyncGeneratorReturnRejectedRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode reasonNode = AccessIndexedArgumentNode.create(0);
            @Child private AsyncGeneratorRejectNode asyncGeneratorRejectNode = AsyncGeneratorRejectNode.create();
            @Child private PropertyGetNode getGenerator = PropertyGetNode.createGetHidden(RETURN_PROCESSOR_GENERATOR, context);

            @Override
            public Object execute(VirtualFrame frame) {
                JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                JSAsyncGeneratorObject generatorObject = (JSAsyncGeneratorObject) getGenerator.getValue(functionObject);
                generatorObject.setAsyncGeneratorState(AsyncGeneratorState.Completed);
                Object reason = reasonNode.execute(frame);
                return asyncGeneratorRejectNode.execute(generatorObject, reason);
            }
        }
        return JSFunctionData.createCallOnly(context, new AsyncGeneratorReturnRejectedRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }
}

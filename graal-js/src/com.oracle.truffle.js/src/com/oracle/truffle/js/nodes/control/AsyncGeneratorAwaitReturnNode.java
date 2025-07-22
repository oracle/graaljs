/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.builtins.AsyncIteratorPrototypeBuiltins.AsyncIteratorAwaitNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSAsyncGeneratorObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.AsyncGeneratorState;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncGeneratorAwaitReturnNode extends AsyncGeneratorCompleteStepNode {

    protected final JSContext context;
    @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
    @Child private PromiseResolveNode promiseResolveNode;
    @Child private PerformPromiseThenNode performPromiseThenNode;
    @Child private PropertySetNode setGeneratorNode;

    AsyncGeneratorAwaitReturnNode(JSContext context) {
        super(context);
        this.context = context;
    }

    public static AsyncGeneratorAwaitReturnNode create(JSContext context) {
        return new AsyncGeneratorAwaitReturnNode(context);
    }

    public final Object getErrorObject(AbstractTruffleException ex) {
        if (getErrorObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
        }
        return getErrorObjectNode.execute(ex);
    }

    public final void executeAsyncGeneratorAwaitReturn(JSAsyncGeneratorObject generator, ArrayDeque<AsyncGeneratorRequest> queue) {
        generator.setAsyncGeneratorState(AsyncGeneratorState.AwaitingReturn);
        try {
            asyncGeneratorAwaitReturn(generator, queue);
        } catch (AbstractTruffleException ex) {
            // PromiseResolve has thrown an error
            asyncGeneratorRejectBrokenPromise(generator, ex, queue);
        }
    }

    protected final void asyncGeneratorAwaitReturn(Object generator, ArrayDeque<AsyncGeneratorRequest> queue) {
        assert !queue.isEmpty();
        AsyncGeneratorRequest next = queue.peekFirst();
        // PromiseResolve error caught in caller
        JSPromiseObject promise = promiseResolve(next.getCompletionValue());
        if (performPromiseThenNode == null || setGeneratorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.performPromiseThenNode = insert(PerformPromiseThenNode.create(context));
            this.setGeneratorNode = insert(PropertySetNode.createSetHidden(AsyncIteratorAwaitNode.THIS_ID, context));
        }
        JSFunctionObject onFulfilled = createAsyncGeneratorReturnProcessorFulfilledFunction(generator);
        JSFunctionObject onRejected = createAsyncGeneratorReturnProcessorRejectedFunction(generator);
        performPromiseThenNode.execute(promise, onFulfilled, onRejected, null);
    }

    private JSPromiseObject promiseResolve(Object value) {
        if (promiseResolveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseResolveNode = insert(PromiseResolveNode.create(context));
        }
        return (JSPromiseObject) promiseResolveNode.execute(getRealm().getPromiseConstructor(), value);
    }

    protected final void asyncGeneratorRejectBrokenPromise(JSAsyncGeneratorObject generator, AbstractTruffleException exception, ArrayDeque<AsyncGeneratorRequest> queue) {
        generator.setAsyncGeneratorState(JSFunction.AsyncGeneratorState.Completed);
        Object error = getErrorObject(exception);
        asyncGeneratorCompleteStep(Completion.Type.Throw, error, true, queue);
    }

    private JSFunctionObject createAsyncGeneratorReturnProcessorFulfilledFunction(Object generator) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncGeneratorAwaitReturnFulfilled,
                        (c) -> createAsyncGeneratorAwaitReturnProcessorImpl(c, Completion.Type.Normal));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setGeneratorNode.setValue(function, generator);
        return function;
    }

    private JSFunctionObject createAsyncGeneratorReturnProcessorRejectedFunction(Object generator) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncGeneratorAwaitReturnRejected,
                        (c) -> createAsyncGeneratorAwaitReturnProcessorImpl(c, Completion.Type.Throw));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setGeneratorNode.setValue(function, generator);
        return function;
    }

    private static JSFunctionData createAsyncGeneratorAwaitReturnProcessorImpl(JSContext context, Completion.Type completionType) {
        class AsyncGeneratorAwaitReturnProcessorRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorAwaitNode.AsyncIteratorArgs> {
            @Child private AsyncGeneratorDrainQueueNode asyncGeneratorOpNode;

            AsyncGeneratorAwaitReturnProcessorRootNode(JSContext context) {
                super(context);
                this.asyncGeneratorOpNode = AsyncGeneratorDrainQueueNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                var generator = getThis(frame);
                Object result = valueNode.execute(frame);
                asyncGeneratorOpNode.asyncGeneratorCompleteStepAndDrainQueue(generator, completionType, result);
                return Undefined.instance;
            }
        }
        return JSFunctionData.createCallOnly(context, new AsyncGeneratorAwaitReturnProcessorRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
    }
}

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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.builtins.AsyncFromSyncIteratorPrototypeBuiltinsFactory.AsyncFromSyncNextNodeGen;
import com.oracle.truffle.js.builtins.AsyncFromSyncIteratorPrototypeBuiltinsFactory.AsyncFromSyncReturnNodeGen;
import com.oracle.truffle.js.builtins.AsyncFromSyncIteratorPrototypeBuiltinsFactory.AsyncFromSyncThrowNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNodeGen;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in methods of 11.1.3.2 The %AsyncFromSyncIteratorPrototype% Object.
 */
public final class AsyncFromSyncIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncFromSyncIteratorPrototypeBuiltins.GeneratorPrototype> {

    protected AsyncFromSyncIteratorPrototypeBuiltins() {
        super(JSFunction.ASYNC_FROM_SYNC_ITERATOR_PROTOTYPE_NAME, GeneratorPrototype.class);
    }

    public enum GeneratorPrototype implements BuiltinEnum<GeneratorPrototype> {
        next(1),
        return_(1),
        throw_(1);

        private final int length;

        GeneratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GeneratorPrototype builtinEnum) {
        assert context.getEcmaScriptVersion() >= 8;
        switch (builtinEnum) {
            case next:
                return AsyncFromSyncNextNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case return_:
                return AsyncFromSyncReturnNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case throw_:
                return AsyncFromSyncThrowNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            default:
                return null;
        }
    }

    @ImportStatic(JSRuntime.class)
    private abstract static class AsyncFromSyncBaseNode extends JSBuiltinNode {
        static final HiddenKey DONE = new HiddenKey("Done");

        @Child private PropertyGetNode getPromise;
        @Child private PropertyGetNode getPromiseReject;
        @Child private PropertyGetNode getPromiseResolve;

        @Child private JSFunctionCallNode executePromiseMethod;
        @Child private NewPromiseCapabilityNode newPromiseCapability;
        @Child protected PerformPromiseThenNode performPromiseThenNode;

        @Child protected IteratorNextNode iteratorNext;
        @Child protected IteratorValueNode iteratorValue;
        @Child protected IteratorCompleteNode iteratorComplete;

        @Child protected PropertyGetNode getGeneratorTarget;
        @Child private PropertySetNode setDoneNode;

        AsyncFromSyncBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
            this.executePromiseMethod = JSFunctionCallNode.createCall();
            this.iteratorNext = IteratorNextNode.create(context);
            this.iteratorComplete = IteratorCompleteNode.create(context);
            this.iteratorValue = IteratorValueNodeGen.create(context);
            this.getGeneratorTarget = PropertyGetNode.createGetHidden(JSFunction.ASYNC_FROM_SYNC_ITERATOR_KEY, context);
            this.setDoneNode = PropertySetNode.createSetHidden(DONE, context);
            this.performPromiseThenNode = PerformPromiseThenNode.create(context);
        }

        protected PromiseCapabilityRecord createPromiseCapability() {
            return newPromiseCapability.executeDefault();
        }

        protected boolean isAsyncFromSyncIterator(DynamicObject thiz) {
            return thiz != Undefined.instance && getGeneratorTarget.getValue(thiz) != Undefined.instance;
        }

        protected void promiseCapabilityReject(PromiseCapabilityRecord promiseCapability, GraalJSException exception) {
            Object result = exception.getErrorObjectEager(getContext());
            promiseCapabilityRejectImpl(promiseCapability, result);
        }

        protected void promiseCapabilityRejectImpl(PromiseCapabilityRecord promiseCapability, Object result) {
            executePromiseMethod.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), result));
        }

        protected void promiseCapabilityResolve(PromiseCapabilityRecord valueWrapperCapability, Object result) {
            executePromiseMethod.executeCall(JSArguments.createOneArg(Undefined.instance, valueWrapperCapability.getResolve(), result));
        }

        protected Object getPromise(DynamicObject promiseCapability) {
            return getPromise.getValue(promiseCapability);
        }

        /**
         * Async-from-Sync Iterator Value Unwrap Functions.
         */
        protected final DynamicObject createIteratorValueUnwrapFunction(JSRealm realm, boolean done) {
            JSContext context = realm.getContext();
            JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncFromSyncIteratorValueUnwrap, c -> createIteratorValueUnwrapImpl(c));
            DynamicObject function = JSFunction.create(realm, functionData);
            setDoneNode.setValueBoolean(function, done);
            return function;
        }

        private static JSFunctionData createIteratorValueUnwrapImpl(JSContext context) {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode() {
                @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
                @Child private PropertyGetNode isDoneNode = PropertyGetNode.createGetHidden(DONE, context);
                @Child private CreateIterResultObjectNode iterResult = CreateIterResultObjectNodeGen.create(context);

                @Override
                public Object execute(VirtualFrame frame) {
                    DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                    boolean done;
                    try {
                        done = isDoneNode.getValueBoolean(functionObject);
                    } catch (UnexpectedResultException e) {
                        throw Errors.shouldNotReachHere();
                    }
                    return iterResult.execute(frame, valueNode.execute(frame), done);
                }
            });
            return JSFunctionData.createCallOnly(context, callTarget, 1, "Async-from-Sync Iterator Value Unwrap Function");
        }
    }

    public abstract static class AsyncFromSyncNext extends AsyncFromSyncBaseNode {

        public AsyncFromSyncNext(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isObject(thisObj)")
        protected Object next(DynamicObject thisObj, Object value) {
            PromiseCapabilityRecord promiseCapability = createPromiseCapability();
            if (!isAsyncFromSyncIterator(thisObj)) {
                JSException typeError = Errors.createTypeErrorIncompatibleReceiver(thisObj);
                promiseCapabilityReject(promiseCapability, typeError);
                return promiseCapability.getPromise();
            }
            boolean nextDone;
            Object nextValue;
            DynamicObject nextResult;
            DynamicObject syncIterator = (DynamicObject) getGeneratorTarget.getValue(thisObj);
            try {
                nextResult = iteratorNext.execute(syncIterator, value);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            try {
                nextDone = iteratorComplete.execute(nextResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            try {
                nextValue = iteratorValue.execute(nextResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            PromiseCapabilityRecord valueWrapperCapability = createPromiseCapability();
            promiseCapabilityResolve(valueWrapperCapability, nextValue);
            DynamicObject onFulfilled = createIteratorValueUnwrapFunction(getContext().getRealm(), nextDone);
            performPromiseThenNode.execute(valueWrapperCapability.getPromise(), onFulfilled, Undefined.instance, promiseCapability);
            return promiseCapability.getPromise();
        }

    }

    public abstract static class AsyncFromSyncMethod extends AsyncFromSyncBaseNode {

        @Child private JSFunctionCallNode executeReturnMethod;

        public AsyncFromSyncMethod(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.executeReturnMethod = JSFunctionCallNode.createCall();
        }

        protected abstract GetMethodNode getMethod();

        protected abstract Object processUndefinedMethod(VirtualFrame frame, PromiseCapabilityRecord promiseCapability, Object value);

        protected Object doMethod(VirtualFrame frame, DynamicObject thisObj, Object value) {
            PromiseCapabilityRecord promiseCapability = createPromiseCapability();
            if (!isAsyncFromSyncIterator(thisObj)) {
                JSException typeError = Errors.createTypeErrorIncompatibleReceiver(thisObj);
                promiseCapabilityReject(promiseCapability, typeError);
                return promiseCapability.getPromise();
            }
            boolean done;
            DynamicObject syncIterator = (DynamicObject) getGeneratorTarget.getValue(thisObj);
            Object method = getMethod().executeWithTarget(syncIterator);
            if (method == Undefined.instance) {
                return processUndefinedMethod(frame, promiseCapability, value);
            }
            Object returnResult = executeReturnMethod.executeCall(JSArguments.create(syncIterator, method, value));
            if (!JSObject.isJSObject(returnResult)) {
                promiseCapabilityReject(promiseCapability, Errors.createTypeErrorNotAnObject(returnResult));
                return promiseCapability.getPromise();
            }
            try {
                done = iteratorComplete.execute((DynamicObject) returnResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            Object returnValue;
            try {
                returnValue = iteratorValue.execute((DynamicObject) returnResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            PromiseCapabilityRecord valueWrapperCapability = createPromiseCapability();
            promiseCapabilityResolve(valueWrapperCapability, returnValue);
            DynamicObject onFulfilled = createIteratorValueUnwrapFunction(getContext().getRealm(), done);
            performPromiseThenNode.execute(valueWrapperCapability.getPromise(), onFulfilled, Undefined.instance, promiseCapability);
            return promiseCapability.getPromise();
        }
    }

    public abstract static class AsyncFromSyncReturn extends AsyncFromSyncMethod {

        @Child private GetMethodNode getReturn;
        @Child private CreateIterResultObjectNode createIterResult;

        public AsyncFromSyncReturn(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getReturn = GetMethodNode.create(context, null, "return");
            this.createIterResult = CreateIterResultObjectNodeGen.create(getContext());
        }

        @Override
        protected GetMethodNode getMethod() {
            return getReturn;
        }

        @Override
        protected Object processUndefinedMethod(VirtualFrame frame, PromiseCapabilityRecord promiseCapability, Object value) {
            DynamicObject iterResult = createIterResult.execute(frame, value, true);
            promiseCapabilityResolve(promiseCapability, iterResult);
            return promiseCapability.getPromise();
        }

        @Specialization(guards = "isObject(thisObj)")
        protected Object resume(VirtualFrame frame, DynamicObject thisObj, Object value) {
            return doMethod(frame, thisObj, value);
        }
    }

    public abstract static class AsyncFromSyncThrow extends AsyncFromSyncMethod {

        @Child private GetMethodNode getThrow;

        public AsyncFromSyncThrow(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getThrow = GetMethodNode.create(context, null, "throw");
        }

        @Override
        protected GetMethodNode getMethod() {
            return getThrow;
        }

        @Override
        protected Object processUndefinedMethod(VirtualFrame frame, PromiseCapabilityRecord promiseCapability, Object value) {
            promiseCapabilityRejectImpl(promiseCapability, value);
            return promiseCapability.getPromise();
        }

        @Specialization(guards = "isObject(thisObj)")
        protected Object doThrow(VirtualFrame frame, DynamicObject thisObj, Object value) {
            return doMethod(frame, thisObj, value);
        }
    }

}

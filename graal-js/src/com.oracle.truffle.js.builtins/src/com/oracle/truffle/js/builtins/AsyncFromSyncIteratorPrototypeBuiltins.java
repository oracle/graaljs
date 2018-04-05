/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in methods of 11.1.3.2 The %AsyncFromSyncIteratorPrototype% Object.
 */
public final class AsyncFromSyncIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncFromSyncIteratorPrototypeBuiltins.GeneratorPrototype> {

    protected AsyncFromSyncIteratorPrototypeBuiltins() {
        super(JSFunction.ASYNC_FROM_SYNC_ITERATOR_PROTOTYPE_NAME, GeneratorPrototype.class);
    }

    public enum GeneratorPrototype implements BuiltinEnum<GeneratorPrototype> {
        next(0),
        return_(0),
        throw_(0);

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
                return AsyncFromSyncNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case return_:
                return AsyncFromSyncReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case throw_:
                return AsyncFromSyncThrowNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
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
        @Child private JSFunctionCallNode createPromiseCapability;

        @Child protected IteratorNextNode iteratorNext;
        @Child protected IteratorValueNode iteratorValue;
        @Child protected IteratorCompleteNode iteratorComplete;

        @Child protected PropertyGetNode getGeneratorTarget;
        @Child private JSFunctionCallNode performPromiseThenCall;
        @Child private PropertySetNode setDoneNode;

        AsyncFromSyncBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createPromiseCapability = JSFunctionCallNode.createCall();
            this.getPromiseReject = PropertyGetNode.create("reject", false, context);
            this.getPromiseResolve = PropertyGetNode.create("resolve", false, context);
            this.executePromiseMethod = JSFunctionCallNode.createCall();
            this.getPromise = PropertyGetNode.create("promise", false, context);
            this.iteratorNext = IteratorNextNode.create(context);
            this.iteratorComplete = IteratorCompleteNode.create(context);
            this.iteratorValue = IteratorValueNodeGen.create(context);
            this.getGeneratorTarget = PropertyGetNode.create(JSFunction.ASYNC_FROM_SYNC_ITERATOR_KEY, false, context);
            this.performPromiseThenCall = JSFunctionCallNode.createCall();
            this.setDoneNode = PropertySetNode.create(DONE, false, context, false);
        }

        protected DynamicObject createPromiseCapability() {
            return (DynamicObject) createPromiseCapability.executeCall(JSArguments.create(Undefined.instance, getContext().getAsyncFunctionPromiseCapabilityConstructor(), new Object[]{}));
        }

        protected boolean isAsyncFromSyncIterator(DynamicObject thiz) {
            return thiz != Undefined.instance && getGeneratorTarget.getValue(thiz) != Undefined.instance;
        }

        protected void promiseCapabilityReject(DynamicObject promiseCapability, GraalJSException exception) {
            DynamicObject reject = (DynamicObject) getPromiseReject.getValue(promiseCapability);
            Object result = exception.getErrorObjectEager(getContext());
            executePromiseMethod.executeCall(JSArguments.create(Undefined.instance, reject, new Object[]{result}));
        }

        protected void promiseCapabilityResolve(DynamicObject promiseCapability, Object result) {
            DynamicObject resolve = (DynamicObject) getPromiseResolve.getValue(promiseCapability);
            executePromiseMethod.executeCall(JSArguments.create(Undefined.instance, resolve, new Object[]{result}));
        }

        protected Object getPromise(DynamicObject promiseCapability) {
            return getPromise.getValue(promiseCapability);
        }

        protected void performPromiseThen(Object promise, DynamicObject onFulfilled, DynamicObject instance, DynamicObject promiseCapability) {
            performPromiseThenCall.executeCall(JSArguments.create(Undefined.instance, getContext().getPerformPromiseThen(), promise, onFulfilled, instance, promiseCapability));
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
                @Child private PropertyGetNode isDoneNode = PropertyGetNode.create(DONE, false, context);
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
        protected Object next(DynamicObject thisObj) {
            DynamicObject promiseCapability = createPromiseCapability();
            if (!isAsyncFromSyncIterator(thisObj)) {
                JSException typeError = Errors.createTypeErrorIncompatibleReceiver(thisObj);
                promiseCapabilityReject(promiseCapability, typeError);
                return getPromise(promiseCapability);
            }
            boolean nextDone;
            Object nextValue;
            DynamicObject nextResult;
            DynamicObject syncIterator = (DynamicObject) getGeneratorTarget.getValue(thisObj);
            try {
                nextResult = iteratorNext.execute(syncIterator, Undefined.instance);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return getPromise(promiseCapability);
            }
            try {
                nextDone = iteratorComplete.execute(nextResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return getPromise(promiseCapability);
            }
            try {
                nextValue = iteratorValue.execute(nextResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return getPromise(promiseCapability);
            }
            DynamicObject valueWrapperCapability = createPromiseCapability();
            promiseCapabilityResolve(valueWrapperCapability, nextValue);
            DynamicObject onFulfilled = createIteratorValueUnwrapFunction(getContext().getRealm(), nextDone);
            performPromiseThen(getPromise(valueWrapperCapability), onFulfilled, Undefined.instance, promiseCapability);
            return getPromise(promiseCapability);
        }

    }

    public abstract static class AsyncFromSyncMethod extends AsyncFromSyncBaseNode {

        @Child private JSFunctionCallNode executeReturnMethod;
        @Child private CreateIterResultObjectNode createIterResult;

        public AsyncFromSyncMethod(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createIterResult = CreateIterResultObjectNodeGen.create(getContext());
            this.executeReturnMethod = JSFunctionCallNode.createCall();
        }

        protected abstract GetMethodNode getMethod();

        protected Object doMethod(VirtualFrame frame, DynamicObject thisObj) {
            DynamicObject promiseCapability = createPromiseCapability();
            if (!isAsyncFromSyncIterator(thisObj)) {
                JSException typeError = Errors.createTypeErrorIncompatibleReceiver(thisObj);
                promiseCapabilityReject(promiseCapability, typeError);
                return getPromise(promiseCapability);
            }
            boolean done;
            DynamicObject syncIterator = (DynamicObject) getGeneratorTarget.getValue(thisObj);
            Object method = getMethod().executeWithTarget(syncIterator);
            if (method == Undefined.instance) {
                DynamicObject iterResult = createIterResult.execute(frame, Undefined.instance, true);
                promiseCapabilityResolve(promiseCapability, iterResult);
                return getPromise(promiseCapability);
            }
            Object value = JSArguments.getUserArgument(frame.getArguments(), 0);
            DynamicObject returnResult = (DynamicObject) executeReturnMethod.executeCall(JSArguments.create(syncIterator, method, value));
            if (!JSObject.isJSObject(returnResult)) {
                promiseCapabilityReject(promiseCapability, Errors.createTypeErrorNotAnObject(returnResult));
                return getPromise(promiseCapability);
            }
            try {
                done = iteratorComplete.execute(returnResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return getPromise(promiseCapability);
            }
            Object returnValue;
            try {
                returnValue = iteratorValue.execute(returnResult);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return getPromise(promiseCapability);
            }
            DynamicObject valueWrapperCapability = createPromiseCapability();
            promiseCapabilityResolve(valueWrapperCapability, returnValue);
            DynamicObject onFulfilled = createIteratorValueUnwrapFunction(getContext().getRealm(), done);
            performPromiseThen(getPromise(valueWrapperCapability), onFulfilled, Undefined.instance, promiseCapability);
            return getPromise(promiseCapability);
        }
    }

    public abstract static class AsyncFromSyncReturn extends AsyncFromSyncMethod {

        @Child private GetMethodNode getReturn;

        public AsyncFromSyncReturn(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getReturn = GetMethodNode.create(context, null, "return");
        }

        @Override
        protected GetMethodNode getMethod() {
            return getReturn;
        }

        @Specialization(guards = "isObject(thisObj)")
        protected Object resume(VirtualFrame frame, DynamicObject thisObj) {
            return doMethod(frame, thisObj);
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

        @Specialization(guards = "isObject(thisObj)")
        protected Object doThrow(VirtualFrame frame, DynamicObject thisObj) {
            return doMethod(frame, thisObj);
        }
    }

}

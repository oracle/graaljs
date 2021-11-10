/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.AsyncFromSyncIteratorPrototypeBuiltinsFactory.AsyncFromSyncNextNodeGen;
import com.oracle.truffle.js.builtins.AsyncFromSyncIteratorPrototypeBuiltinsFactory.AsyncFromSyncReturnNodeGen;
import com.oracle.truffle.js.builtins.AsyncFromSyncIteratorPrototypeBuiltinsFactory.AsyncFromSyncThrowNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
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
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
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
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in methods of 11.1.3.2 The %AsyncFromSyncIteratorPrototype% Object.
 */
public final class AsyncFromSyncIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncFromSyncIteratorPrototypeBuiltins.GeneratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncFromSyncIteratorPrototypeBuiltins();

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

        @Child private PropertyGetNode getPromiseNode;

        @Child private JSFunctionCallNode executePromiseMethodNode;
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child protected PerformPromiseThenNode performPromiseThenNode;
        @Child private PromiseResolveNode promiseResolveNode;

        @Child protected IteratorNextNode iteratorNextNode;
        @Child protected IteratorValueNode iteratorValueNode;
        @Child protected IteratorCompleteNode iteratorCompleteNode;

        @Child protected PropertyGetNode getSyncIteratorRecordNode;
        @Child private PropertySetNode setDoneNode;

        protected ConditionProfile valuePresenceProfile = ConditionProfile.createBinaryProfile();

        AsyncFromSyncBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.executePromiseMethodNode = JSFunctionCallNode.createCall();
            this.iteratorNextNode = IteratorNextNode.create();
            this.iteratorCompleteNode = IteratorCompleteNode.create(context);
            this.iteratorValueNode = IteratorValueNodeGen.create(context);
            this.getSyncIteratorRecordNode = PropertyGetNode.createGetHidden(JSFunction.ASYNC_FROM_SYNC_ITERATOR_KEY, context);
            this.setDoneNode = PropertySetNode.createSetHidden(DONE, context);
            this.performPromiseThenNode = PerformPromiseThenNode.create(context);
            this.promiseResolveNode = PromiseResolveNode.create(context);
        }

        protected PromiseCapabilityRecord createPromiseCapability() {
            return newPromiseCapabilityNode.executeDefault();
        }

        protected boolean isAsyncFromSyncIterator(DynamicObject thiz) {
            return thiz != Undefined.instance && getSyncIteratorRecordNode.getValue(thiz) != Undefined.instance;
        }

        protected void promiseCapabilityReject(PromiseCapabilityRecord promiseCapability, GraalJSException exception) {
            Object result = exception.getErrorObjectEager(getRealm());
            promiseCapabilityRejectImpl(promiseCapability, result);
        }

        protected void promiseCapabilityRejectImpl(PromiseCapabilityRecord promiseCapability, Object result) {
            executePromiseMethodNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), result));
        }

        protected void promiseCapabilityResolve(PromiseCapabilityRecord valueWrapperCapability, Object result) {
            executePromiseMethodNode.executeCall(JSArguments.createOneArg(Undefined.instance, valueWrapperCapability.getResolve(), result));
        }

        protected Object getPromise(DynamicObject promiseCapability) {
            return getPromiseNode.getValue(promiseCapability);
        }

        protected final Object asyncFromSyncIteratorContinuation(Object result, PromiseCapabilityRecord promiseCapability) {
            boolean done;
            try {
                done = iteratorCompleteNode.execute(result);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            Object returnValue;
            try {
                returnValue = iteratorValueNode.execute(result);
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            JSRealm realm = getRealm();
            DynamicObject valueWrapper;
            if (getContext().usePromiseResolve()) {
                valueWrapper = promiseResolveNode.execute(realm.getPromiseConstructor(), returnValue);
            } else {
                PromiseCapabilityRecord valueWrapperCapability = createPromiseCapability();
                promiseCapabilityResolve(valueWrapperCapability, returnValue);
                valueWrapper = valueWrapperCapability.getPromise();
            }
            DynamicObject onFulfilled = createIteratorValueUnwrapFunction(realm, done);
            performPromiseThenNode.execute(valueWrapper, onFulfilled, Undefined.instance, promiseCapability);
            return promiseCapability.getPromise();
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
            class AsyncFromSyncIteratorValueUnwrapRootNode extends JavaScriptRootNode {
                @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
                @Child private PropertyGetNode isDoneNode = PropertyGetNode.createGetHidden(DONE, context);
                @Child private CreateIterResultObjectNode createIterResult = CreateIterResultObjectNode.create(context);

                @Override
                public Object execute(VirtualFrame frame) {
                    DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                    Object value = valueNode.execute(frame);
                    boolean done;
                    try {
                        done = isDoneNode.getValueBoolean(functionObject);
                    } catch (UnexpectedResultException e) {
                        throw Errors.shouldNotReachHere();
                    }
                    return createIterResult.execute(frame, value, done);
                }
            }
            return JSFunctionData.createCallOnly(context, new AsyncFromSyncIteratorValueUnwrapRootNode().getCallTarget(), 1, "");
        }
    }

    public abstract static class AsyncFromSyncNext extends AsyncFromSyncBaseNode {

        public AsyncFromSyncNext(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isObject(thisObj)")
        protected Object next(VirtualFrame frame, DynamicObject thisObj, Object value) {
            PromiseCapabilityRecord promiseCapability = createPromiseCapability();
            if (!isAsyncFromSyncIterator(thisObj)) {
                JSException typeError = Errors.createTypeErrorIncompatibleReceiver(thisObj);
                promiseCapabilityReject(promiseCapability, typeError);
                return promiseCapability.getPromise();
            }
            Object nextResult;
            IteratorRecord syncIteratorRecord = (IteratorRecord) getSyncIteratorRecordNode.getValue(thisObj);
            try {
                if (valuePresenceProfile.profile(JSArguments.getUserArgumentCount(frame.getArguments()) == 0)) {
                    nextResult = iteratorNextNode.execute(syncIteratorRecord);
                } else {
                    nextResult = iteratorNextNode.execute(syncIteratorRecord, value);
                }
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            return asyncFromSyncIteratorContinuation(nextResult, promiseCapability);
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
            IteratorRecord syncIteratorRecord = (IteratorRecord) getSyncIteratorRecordNode.getValue(thisObj);
            DynamicObject syncIterator = syncIteratorRecord.getIterator();
            Object method = getMethod().executeWithTarget(syncIterator);
            if (method == Undefined.instance) {
                return processUndefinedMethod(frame, promiseCapability, value);
            }
            Object returnResult;
            try {
                if (valuePresenceProfile.profile(JSArguments.getUserArgumentCount(frame.getArguments()) == 0)) {
                    returnResult = executeReturnMethod.executeCall(JSArguments.create(syncIterator, method));
                } else {
                    returnResult = executeReturnMethod.executeCall(JSArguments.create(syncIterator, method, value));
                }
            } catch (GraalJSException e) {
                promiseCapabilityReject(promiseCapability, e);
                return promiseCapability.getPromise();
            }
            if (!JSDynamicObject.isJSDynamicObject(returnResult)) {
                promiseCapabilityReject(promiseCapability, Errors.createTypeErrorNotAnObject(returnResult));
                return promiseCapability.getPromise();
            }
            return asyncFromSyncIteratorContinuation(returnResult, promiseCapability);
        }
    }

    public abstract static class AsyncFromSyncReturn extends AsyncFromSyncMethod {

        @Child private GetMethodNode getReturn;
        @Child private CreateIterResultObjectNode createIterResult;

        public AsyncFromSyncReturn(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getReturn = GetMethodNode.create(context, "return");
            this.createIterResult = CreateIterResultObjectNode.create(getContext());
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
            this.getThrow = GetMethodNode.create(context, "throw");
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

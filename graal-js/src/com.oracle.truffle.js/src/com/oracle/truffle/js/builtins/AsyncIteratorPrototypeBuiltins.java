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
package com.oracle.truffle.js.builtins;

import java.util.function.Function;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorDirectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerOrInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.AsyncHandlerRootNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Contains builtins for {@linkplain JSArray}.prototype.
 */
public final class AsyncIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncIteratorPrototypeBuiltins.AsyncIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncIteratorPrototypeBuiltins();

    public static final HiddenKey PROMISE_ID = new HiddenKey("promise");

    AsyncIteratorPrototypeBuiltins() {
        super(JSAsyncIterator.PROTOTYPE_NAME, AsyncIteratorPrototype.class);
    }

    public enum AsyncIteratorPrototype implements BuiltinEnum<AsyncIteratorPrototype> {
        map(1),
        filter(1),
        take(1),
        drop(1),
        indexed(0),
        flatMap(1),

        reduce(1),
        toArray(0),
        forEach(1),
        some(1),
        every(1),
        find(1);

        private final int length;

        AsyncIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case map:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorMapNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case filter:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorFilterNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case take:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorTakeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case drop:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorDropNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case indexed:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorIndexedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case flatMap:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorFlatMapNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reduce:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorReduceNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case toArray:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorToArrayNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case forEach:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case some:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorSomeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case every:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorEveryNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case find:
                return AsyncIteratorPrototypeBuiltinsFactory.AsyncIteratorFindNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    protected static class AsyncIteratorAwaitNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends JavaScriptBaseNode {
        private static final HiddenKey ARGS_ID = new HiddenKey("args");

        public abstract static class AsyncIteratorRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends JavaScriptRootNode implements AsyncHandlerRootNode {
            @Child protected JavaScriptNode valueNode;
            @Child private PropertyGetNode getArgsNode;
            @Child protected JSFunctionCallNode callNode;

            AsyncIteratorRootNode(JSContext context) {
                valueNode = AccessIndexedArgumentNode.create(0);
                getArgsNode = PropertyGetNode.createGetHidden(ARGS_ID, context);
                callNode = JSFunctionCallNode.createCall();
            }

            protected T getArgs(VirtualFrame frame) {
                JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                //noinspection unchecked
                return (T) getArgsNode.getValue(functionObject);
            }

            @Override
            public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
                assert JSFunction.isJSFunction(handlerFunction) && ((RootCallTarget) JSFunction.getFunctionData(handlerFunction).getCallTarget()).getRootNode() == this;
                JSDynamicObject promise = (JSDynamicObject) JSObjectUtil.getHiddenProperty(handlerFunction, PROMISE_ID);
                return new AsyncStackTraceInfo(promise, null);
            }
        }

        public static class AsyncIteratorArgs {
        }

        @Child private PropertySetNode setArgs;
        @Child protected JSFunctionCallNode callNode;
        @Child protected PropertyGetNode getConstructorNode;
        @Child protected NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child protected PerformPromiseThenNode performPromiseThenNode;
        private final JSContext.BuiltinFunctionKey key;
        private final JSContext context;
        private final Function<JSContext, JSFunctionData> create;

        public AsyncIteratorAwaitNode(JSContext context, JSContext.BuiltinFunctionKey key, Function<JSContext, JSFunctionData> create) {
            this.key = key;
            this.context = context;
            this.create = create;

            setArgs = PropertySetNode.createSetHidden(ARGS_ID, context);
            getConstructorNode = PropertyGetNode.create(JSObject.CONSTRUCTOR, context);
            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            performPromiseThenNode = PerformPromiseThenNode.create(context);
            callNode = JSFunctionCallNode.createCall();
        }

        public JSDynamicObject execute(Object promiseOrValue, T args) {
            JSDynamicObject promise;
            if (!JSPromise.isJSPromise(promiseOrValue) || getConstructorNode.getValueOrDefault(promiseOrValue, Undefined.instance) != getRealm().getPromiseConstructor()) {
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getResolve(), promiseOrValue));
                promise = promiseCapability.getPromise();
            } else {
                promise = (JSDynamicObject) promiseOrValue;
            }

            return performPromiseThenNode.execute(promise, createFunction(args), Undefined.instance, newPromiseCapabilityNode.executeDefault());
        }

        public JSFunctionObject createFunction(T args) {
            JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(this.key, create);
            JSFunctionObject function = JSFunction.create(getRealm(), functionData);
            setArgs.setValue(function, args);
            return function;
        }

        public static <T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> AsyncIteratorAwaitNode<T> create(JSContext context, JSContext.BuiltinFunctionKey key,
                        Function<JSContext, JSFunctionData> create) {
            return new AsyncIteratorAwaitNode<>(context, key, create);
        }
    }

    protected abstract static class AsyncIteratorNextUtilsRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<T> {
        @Child private IteratorCompleteNode iteratorCompleteNode;
        @Child protected IsObjectNode isObjectNode;
        @Child protected CreateIterResultObjectNode createIterResultObjectNode;
        @Child protected NewPromiseCapabilityNode newPromiseCapabilityNode;

        public AsyncIteratorNextUtilsRootNode(JSContext context) {
            super(context);

            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            isObjectNode = IsObjectNode.create();
            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            iteratorCompleteNode = IteratorCompleteNode.create(context);
        }

        protected JSDynamicObject checkNext(Object value) {
            if (isObjectNode.executeBoolean(value)) {
                return null;
            }

            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), Errors.createTypeErrorIterResultNotAnObject(value, this)));
            return promiseCapability.getPromise();
        }

        protected JSDynamicObject checkComplete(VirtualFrame frame, Object value) {
            if (!iteratorCompleteNode.execute(value)) {
                return null;
            }

            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), createIterResultObjectNode.execute(frame, Undefined.instance, true)));
            return promiseCapability.getPromise();
        }
    }

    protected static class AsyncIteratorCreateResultNode extends JavaScriptBaseNode {
        @Child AsyncIteratorAwaitNode<AsyncIteratorAwaitNode.AsyncIteratorArgs> awaitNode;

        public AsyncIteratorCreateResultNode(JSContext context) {
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorCreateResult, AsyncIteratorCreateResultNode::createCreateResultFunctionImpl);
        }

        public Object execute(Object value) {
            return awaitNode.execute(value, new AsyncIteratorAwaitNode.AsyncIteratorArgs());
        }

        protected static class AsyncIteratorCreateResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorAwaitNode.AsyncIteratorArgs> {
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            public AsyncIteratorCreateResultRootNode(JSContext context) {
                super(context);

                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return createIterResultObjectNode.execute(frame, valueNode.execute(frame), false);
            }
        }

        private static JSFunctionData createCreateResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorCreateResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        public static AsyncIteratorCreateResultNode create(JSContext context) {
            return new AsyncIteratorCreateResultNode(context);
        }
    }

    protected abstract static class AsyncIteratorMapNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorMapArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;

        public AsyncIteratorMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorMap, AsyncIteratorMapNode::createMapFunctionImpl);
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject map(Object thisObj, Object mapper) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorMapArgs(record, mapper)));
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(Object thisObj, Object mapper) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static class AsyncIteratorMapArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object mapper;

            public AsyncIteratorMapArgs(IteratorRecord iterated, Object mapper) {
                this.iterated = iterated;
                this.mapper = mapper;
            }
        }

        protected static class AsyncIteratorMapRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorMapArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorMapArgs> awaitNode;

            public AsyncIteratorMapRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorMapWithValue, AsyncIteratorMapNode::createMapWithValueFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object value = iteratorNextNode.execute(getArgs(frame).iterated);
                return awaitNode.execute(value, getArgs(frame));
            }
        }

        protected static class AsyncIteratorMapWithValueRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorMapArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorCreateResultNode createResultNode;

            public AsyncIteratorMapWithValueRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                createResultNode = AsyncIteratorCreateResultNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorMapArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                Object value = iteratorValueNode.execute(next);
                Object result = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.mapper, value));
                return createResultNode.execute(result);
            }
        }

        private static JSFunctionData createMapFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorMapRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createMapWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorMapWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorFilterNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;

        public AsyncIteratorFilterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilter, AsyncIteratorFilterNode::createFilterFunctionImpl);
        }

        @Specialization(guards = "isCallable(filterer)")
        public JSDynamicObject filter(Object thisObj, Object filterer) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorFilterArgs(record, filterer)));
        }

        @Specialization(guards = "!isCallable(filterer)")
        public Object unsupported(Object thisObj, Object filterer) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static class AsyncIteratorFilterArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object filterer;

            public AsyncIteratorFilterArgs(IteratorRecord iterated, Object filterer) {
                this.iterated = iterated;
                this.filterer = filterer;
            }
        }

        protected static class AsyncIteratorFilterWithResultArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final AsyncIteratorFilterArgs args;
            public final Object value;

            public AsyncIteratorFilterWithResultArgs(AsyncIteratorFilterArgs args, Object value) {
                this.args = args;
                this.value = value;
            }
        }

        protected static class AsyncIteratorFilterRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorFilterArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterArgs> awaitNode;

            public AsyncIteratorFilterRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilterWithValue, AsyncIteratorFilterNode::createFilterWithValueFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object value = iteratorNextNode.execute(getArgs(frame).iterated);
                return awaitNode.execute(value, getArgs(frame));
            }
        }

        protected static class AsyncIteratorFilterWithValueRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorFilterArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterWithResultArgs> awaitNode;

            public AsyncIteratorFilterWithValueRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilterWithResult, AsyncIteratorFilterNode::createFilterWithResultFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorFilterArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                Object value = iteratorValueNode.execute(next);
                Object result = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.filterer, value));
                return awaitNode.execute(result, new AsyncIteratorFilterWithResultArgs(getArgs(frame), value));
            }
        }

        protected static class AsyncIteratorFilterWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorFilterWithResultArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterArgs> awaitNode;
            @Child private JSToBooleanNode toBooleanNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            public AsyncIteratorFilterWithResultRootNode(JSContext context) {
                super(context);

                toBooleanNode = JSToBooleanNode.create();
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilter, AsyncIteratorFilterNode::createFilterFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                if (toBooleanNode.executeBoolean(valueNode.execute(frame))) {
                    return createIterResultObjectNode.execute(frame, getArgs(frame).value, false);
                } else {
                    return awaitNode.execute(Undefined.instance, getArgs(frame).args);
                }
            }
        }

        private static JSFunctionData createFilterFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFilterRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createFilterWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFilterWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createFilterWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFilterWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorTakeNode extends JSBuiltinNode {
        private static final HiddenKey REMAINING_ID = new HiddenKey("remaining");

        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorTakeArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;
        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;
        @Child private PropertySetNode setRemainingNode;

        public AsyncIteratorTakeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorTake, AsyncIteratorTakeNode::createTakeFunctionImpl);
            toNumberNode = JSToNumberNode.create();
            toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
            setRemainingNode = PropertySetNode.createSetHidden(REMAINING_ID, context);
        }

        @Specialization
        public JSDynamicObject take(Object thisObj, Object limit) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);

            Number numLimit = toNumberNode.executeNumber(limit);
            if (Double.isNaN(numLimit.doubleValue())) {
                throw Errors.createRangeError("NAN not allowed (TODO: error message)", this);
            }

            Number integerLimit = toIntegerOrInfinityNode.executeNumber(limit);
            if (integerLimit.doubleValue() < 0) {
                throw Errors.createRangeErrorIndexNegative(this);
            }

            JSFunctionObject functionObject = awaitNode.createFunction(new AsyncIteratorTakeArgs(record, integerLimit));
            setRemainingNode.setValue(functionObject, integerLimit);
            return createAsyncIteratorHelperNode.execute(record, functionObject);
        }

        protected static class AsyncIteratorTakeArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Number limit;

            public AsyncIteratorTakeArgs(IteratorRecord iterated, Number limit) {
                this.iterated = iterated;
                this.limit = limit;
            }
        }

        protected static class AsyncIteratorTakeRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorTakeArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorTakeArgs> awaitNode;
            @Child private PropertyGetNode getRemainingNode;
            @Child private PropertySetNode setRemainingNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            public AsyncIteratorTakeRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorTakeWithValue, AsyncIteratorTakeNode::createTakeWithValueFunctionImpl);
                getRemainingNode = PropertyGetNode.createGetHidden(REMAINING_ID, context);
                setRemainingNode = PropertySetNode.createSetHidden(REMAINING_ID, context);
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                double remaining = ((Number) getRemainingNode.getValue(functionObject)).doubleValue();
                if (remaining == 0) {
                    return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                }
                if (remaining != Double.POSITIVE_INFINITY) {
                    remaining--;
                    setRemainingNode.setValue(functionObject, remaining);
                }

                Object value = iteratorNextNode.execute(getArgs(frame).iterated);
                return awaitNode.execute(value, getArgs(frame));
            }
        }

        protected static class AsyncIteratorTakeWithValueRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorTakeArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorCreateResultNode createResultNode;

            public AsyncIteratorTakeWithValueRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                createResultNode = AsyncIteratorCreateResultNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                return createResultNode.execute(iteratorValueNode.execute(next));
            }
        }

        private static JSFunctionData createTakeFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorTakeRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createTakeWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorTakeWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorDropNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorDropArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;
        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;

        public AsyncIteratorDropNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorDrop, AsyncIteratorDropNode::createDropFunctionImpl);
            toNumberNode = JSToNumberNode.create();
            toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
        }

        @Specialization
        public JSDynamicObject drop(Object thisObj, Object limit) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);

            Number numLimit = toNumberNode.executeNumber(limit);
            if (Double.isNaN(numLimit.doubleValue())) {
                throw Errors.createRangeError("NAN not allowed (TODO: error message)", this);
            }

            Number integerLimit = toIntegerOrInfinityNode.executeNumber(limit);
            if (integerLimit.doubleValue() < 0) {
                throw Errors.createRangeErrorIndexNegative(this);
            }

            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorDropArgs(record, integerLimit)));
        }

        protected static class AsyncIteratorDropArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Number limit;

            public AsyncIteratorDropArgs(IteratorRecord iterated, Number limit) {
                this.iterated = iterated;
                this.limit = limit;
            }
        }

        protected static class AsyncIteratorDropRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorDropArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorDropArgs> awaitNode;

            public AsyncIteratorDropRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorDropWithValue, AsyncIteratorDropNode::createDropWithValueFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object value = iteratorNextNode.execute(getArgs(frame).iterated);
                return awaitNode.execute(value, getArgs(frame));
            }
        }

        protected static class AsyncIteratorDropWithValueRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorDropArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorCreateResultNode createResultNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorDropArgs> awaitNode;

            public AsyncIteratorDropWithValueRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                createResultNode = AsyncIteratorCreateResultNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorDrop, AsyncIteratorDropNode::createDropFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                AsyncIteratorDropArgs args = getArgs(frame);
                double remaining = args.limit.doubleValue();
                if (remaining > 0) {
                    AsyncIteratorDropArgs newArgs;
                    if (remaining != Double.POSITIVE_INFINITY) {
                        newArgs = new AsyncIteratorDropArgs(args.iterated, args.limit.doubleValue() - 1);
                    } else {
                        newArgs = args;
                    }

                    return awaitNode.execute(Undefined.instance, newArgs);
                }

                return createResultNode.execute(iteratorValueNode.execute(next));
            }
        }

        private static JSFunctionData createDropFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorDropRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createDropWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorDropWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorIndexedNode extends JSBuiltinNode {
        private static final HiddenKey INDEX_ID = new HiddenKey("index");
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorIndexedArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;

        public AsyncIteratorIndexedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorIndexed, AsyncIteratorIndexedNode::createIndexedFunctionImpl);
        }

        @Specialization
        public JSDynamicObject indexed(Object thisObj) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorIndexedArgs(record, 0)));
        }

        protected static class AsyncIteratorIndexedArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final long index;

            public AsyncIteratorIndexedArgs(IteratorRecord iterated, long index) {
                this.iterated = iterated;
                this.index = index;
            }
        }

        protected static class AsyncIteratorIndexedRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorIndexedArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorIndexedArgs> awaitNode;
            @Child private PropertyGetNode getIndexNode;
            @Child private PropertySetNode setIndexNode;

            public AsyncIteratorIndexedRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                getIndexNode = PropertyGetNode.createGetHidden(INDEX_ID, context);
                setIndexNode = PropertySetNode.createSetHidden(INDEX_ID, context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorIndexedWithValue, AsyncIteratorIndexedNode::createIndexedWithValueFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                long index = (long) getIndexNode.getValueOrDefault(functionObject, 0L);
                setIndexNode.setValue(functionObject, index + 1);

                Object value = iteratorNextNode.execute(getArgs(frame).iterated);
                return awaitNode.execute(value, new AsyncIteratorIndexedArgs(getArgs(frame).iterated, index));
            }
        }

        protected static class AsyncIteratorIndexedWithValueRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorIndexedArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;
            private final JSContext context;

            public AsyncIteratorIndexedWithValueRootNode(JSContext context) {
                super(context);
                this.context = context;

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                Object value = iteratorValueNode.execute(next);
                JSArrayObject pair = JSArray.createConstantObjectArray(context, getRealm(), new Object[]{getArgs(frame).index, value});
                return createIterResultObjectNode.execute(frame, pair, false);
            }
        }

        private static JSFunctionData createIndexedFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorIndexedRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createIndexedWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorIndexedWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorFlatMapNode extends JSBuiltinNode {
        private static HiddenKey CURRENT_ID = new HiddenKey("current");

        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;

        public AsyncIteratorFlatMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMap, AsyncIteratorFlatMapNode::createFlatMapFunctionImpl);
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject flatMap(Object thisObj, Object mapper) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorFlatMapArgs(record, mapper)));
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(Object thisObj, Object mapper) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static class AsyncIteratorFlatMapArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object mapper;

            public AsyncIteratorFlatMapArgs(IteratorRecord iterated, Object mapper) {
                this.iterated = iterated;
                this.mapper = mapper;
            }
        }

        protected static class AsyncIteratorFlatMapRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorFlatMapArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitInnerNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitOuterNode;

            @Child private PropertyGetNode getInnerNode;

            public AsyncIteratorFlatMapRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                getInnerNode = PropertyGetNode.createGetHidden(CURRENT_ID, context);
                awaitOuterNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapWithValue, AsyncIteratorFlatMapNode::createFlatMapWithValueFunctionImpl);
                awaitInnerNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapInnerWithValue,
                                AsyncIteratorFlatMapNode::createFlatMapInnerWithValueFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                IteratorRecord iterated = getArgs(frame).iterated;

                IteratorRecord inner = (IteratorRecord) getInnerNode.getValueOrDefault(iterated.getIterator(), null);
                if (inner != null) {
                    Object value = iteratorNextNode.execute(inner);
                    return awaitInnerNode.execute(value, getArgs(frame));
                }

                Object value = iteratorNextNode.execute(iterated);
                return awaitOuterNode.execute(value, getArgs(frame));
            }
        }

        protected static class AsyncIteratorFlatMapWithValueRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorFlatMapArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorFlatMapWithValueRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapWithResult, AsyncIteratorFlatMapNode::createFlatMapWithResultFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorFlatMapArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                Object value = iteratorValueNode.execute(next);
                Object mapped = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.mapper, value));
                return awaitNode.execute(mapped, args);
            }
        }

        protected static class AsyncIteratorFlatMapWithResultRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorFlatMapArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private GetIteratorNode getIteratorNode;
            @Child private PropertySetNode setCurrentNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitInnerNode;

            public AsyncIteratorFlatMapWithResultRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                getIteratorNode = GetIteratorNode.createAsync(context, null);
                setCurrentNode = PropertySetNode.createSetHidden(CURRENT_ID, context);
                awaitInnerNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapInnerWithValue,
                                AsyncIteratorFlatMapNode::createFlatMapInnerWithValueFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorFlatMapArgs args = getArgs(frame);

                Object mapped = valueNode.execute(frame);
                IteratorRecord inner = getIteratorNode.execute(mapped);
                setCurrentNode.setValue(args.iterated.getIterator(), inner);

                Object innerNext = iteratorNextNode.execute(inner);
                return awaitInnerNode.execute(innerNext, args);
            }
        }

        protected static class AsyncIteratorFlatMapInnerWithValueRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorFlatMapArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;
            @Child private PropertySetNode setCurrentNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitNode;

            public AsyncIteratorFlatMapInnerWithValueRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
                setCurrentNode = PropertySetNode.createSetHidden(CURRENT_ID, context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMap, AsyncIteratorFlatMapNode::createFlatMapFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorFlatMapArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    setCurrentNode.setValue(args.iterated.getIterator(), null);
                    return awaitNode.execute(Undefined.instance, getArgs(frame));
                }

                Object value = iteratorValueNode.execute(next);
                return createIterResultObjectNode.execute(frame, value, false);
            }
        }

        private static JSFunctionData createFlatMapFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFlatMapRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createFlatMapWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFlatMapWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createFlatMapWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFlatMapWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createFlatMapInnerWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFlatMapInnerWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorReduceNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> initialAwaitNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;

        public AsyncIteratorReduceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduce, AsyncIteratorReduceNode::createReduceFunctionImpl);
            initialAwaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduceInitial, AsyncIteratorReduceNode::createReduceInitialFunctionImpl);
        }

        @Specialization(guards = "isCallable(reducer)")
        public JSDynamicObject reduce(Object thisObj, Object reducer, Object[] args) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            Object value = iteratorNextNode.execute(record);

            Object initialValue = JSRuntime.getArgOrUndefined(args, 0);
            if (initialValue == Undefined.instance) {
                return initialAwaitNode.execute(value, new AsyncIteratorReduceArgs(record, reducer, Undefined.instance));
            } else {
                return awaitNode.execute(value, new AsyncIteratorReduceArgs(record, reducer, initialValue));
            }
        }

        @Specialization(guards = "!isCallable(reducer)")
        public Object unsupported(Object thisObj, Object reducer, Object[] args) {
            throw Errors.createTypeErrorCallableExpected();
        }

        private static class AsyncIteratorReduceArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object reducer;
            public final Object accumulator;

            AsyncIteratorReduceArgs(IteratorRecord iterated, Object reducer, Object accumulator) {
                this.iterated = iterated;
                this.reducer = reducer;
                this.accumulator = accumulator;
            }
        }

        protected static class AsyncIteratorReduceRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorReduceArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;
            @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorReduceRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduceWithResult, AsyncIteratorReduceNode::createReduceWithResultFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorReduceArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    PromiseCapabilityRecord capabilityRecord = newPromiseCapabilityNode.executeDefault();
                    callNode.executeCall(JSArguments.createOneArg(capabilityRecord.getPromise(), capabilityRecord.getResolve(), args.accumulator));
                    return capabilityRecord.getPromise();
                }

                Object value = iteratorValueNode.execute(next);
                Object result = callNode.executeCall(JSArguments.create(Undefined.instance, args.reducer, args.accumulator, value));
                return awaitNode.execute(result, args);
            }
        }

        protected static class AsyncIteratorReduceInitialRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorReduceArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;
            @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorReduceInitialRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduce, AsyncIteratorReduceNode::createReduceFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorReduceArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    PromiseCapabilityRecord capabilityRecord = newPromiseCapabilityNode.executeDefault();
                    callNode.executeCall(
                                    JSArguments.createOneArg(capabilityRecord.getPromise(), capabilityRecord.getReject(), Errors.createTypeError("Reduce of empty iterator with no initial value")));
                    return capabilityRecord.getPromise();
                }

                Object value = iteratorValueNode.execute(next);
                Object nextNext = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(nextNext, new AsyncIteratorReduceArgs(args.iterated, args.reducer, value));
            }
        }

        protected static class AsyncIteratorReduceWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorReduceArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;

            public AsyncIteratorReduceWithResultRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduce, AsyncIteratorReduceNode::createReduceFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorReduceArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(value, new AsyncIteratorReduceArgs(args.iterated, args.reducer, result));
            }
        }

        private static JSFunctionData createReduceFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorReduceRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createReduceInitialFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorReduceInitialRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createReduceWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorReduceWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorToArrayNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorToArrayArgs> awaitNode;

        public AsyncIteratorToArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorToArray, AsyncIteratorToArrayNode::createToArrayFunctionImpl);
        }

        @Specialization
        public JSDynamicObject toArray(Object thisObj) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.execute(value, new AsyncIteratorToArrayArgs(record, new SimpleArrayList<>()));
        }

        private static class AsyncIteratorToArrayArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final SimpleArrayList<Object> result;

            AsyncIteratorToArrayArgs(IteratorRecord iterated, SimpleArrayList<Object> result) {
                this.iterated = iterated;
                this.result = result;
            }
        }

        protected static class AsyncIteratorToArrayRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorToArrayArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorToArrayArgs> awaitNode;
            @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
            @Child private JSFunctionCallNode callNode;

            private final JSContext context;
            private final BranchProfile growProfile = BranchProfile.create();

            public AsyncIteratorToArrayRootNode(JSContext context) {
                super(context);
                this.context = context;

                iteratorNextNode = IteratorNextNode.create();
                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorToArray, AsyncIteratorToArrayNode::createToArrayFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorToArrayArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    PromiseCapabilityRecord capabilityRecord = newPromiseCapabilityNode.executeDefault();
                    callNode.executeCall(JSArguments.createOneArg(capabilityRecord.getPromise(), capabilityRecord.getResolve(), JSArray.createConstant(context, getRealm(), args.result.toArray())));
                    return capabilityRecord.getPromise();
                }

                Object value = iteratorValueNode.execute(next);
                args.result.add(value, growProfile);

                return awaitNode.execute(iteratorNextNode.execute(args.iterated), args);
            }
        }

        private static JSFunctionData createToArrayFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorToArrayRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorForEachNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorForEachArgs> awaitNode;

        public AsyncIteratorForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorForEach, AsyncIteratorForEachNode::createForEachFunctionImpl);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject forEach(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.execute(value, new AsyncIteratorForEachArgs(record, fn));
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(Object thisObj, Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        private static class AsyncIteratorForEachArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object fn;

            AsyncIteratorForEachArgs(IteratorRecord iterated, Object fn) {
                this.iterated = iterated;
                this.fn = fn;
            }
        }

        protected static class AsyncIteratorForEachRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorForEachArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorForEachArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorForEachRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorForEachWithResult, AsyncIteratorForEachNode::createForEachWithResultFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorForEachArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                Object value = iteratorValueNode.execute(next);
                Object result = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.fn, value));
                return awaitNode.execute(result, args);
            }
        }

        protected static class AsyncIteratorForEachWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorForEachArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorForEachArgs> awaitNode;

            public AsyncIteratorForEachWithResultRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorForEach, AsyncIteratorForEachNode::createForEachFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorForEachArgs args = getArgs(frame);
                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(value, args);
            }
        }

        private static JSFunctionData createForEachFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorForEachRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createForEachWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorForEachWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorSomeNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorSomeArgs> awaitNode;

        public AsyncIteratorSomeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorSome, AsyncIteratorSomeNode::createSomeFunctionImpl);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject some(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.execute(value, new AsyncIteratorSomeArgs(record, fn));
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(Object thisObj, Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        private static class AsyncIteratorSomeArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object fn;

            AsyncIteratorSomeArgs(IteratorRecord iterated, Object fn) {
                this.iterated = iterated;
                this.fn = fn;
            }
        }

        protected static class AsyncIteratorSomeRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorSomeArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorSomeArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorSomeRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorSomeWithResult, AsyncIteratorSomeNode::createSomeWithResultFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorSomeArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return createIterResultObjectNode.execute(frame, false, true);
                }

                Object value = iteratorValueNode.execute(next);
                Object result = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.fn, value));
                return awaitNode.execute(result, args);
            }
        }

        protected static class AsyncIteratorSomeWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorSomeArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorSomeArgs> awaitNode;
            @Child private JSToBooleanNode toBooleanNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            public AsyncIteratorSomeWithResultRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                toBooleanNode = JSToBooleanNode.create();
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorSome, AsyncIteratorSomeNode::createSomeFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorSomeArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                if (toBooleanNode.executeBoolean(result)) {
                    return createIterResultObjectNode.execute(frame, true, true);
                }

                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(value, args);
            }
        }

        private static JSFunctionData createSomeFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorSomeRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createSomeWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorSomeWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorEveryNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorEveryArgs> awaitNode;

        public AsyncIteratorEveryNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorEvery, AsyncIteratorEveryNode::createEveryFunctionImpl);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject every(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.execute(value, new AsyncIteratorEveryArgs(record, fn));
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(Object thisObj, Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        private static class AsyncIteratorEveryArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object fn;

            AsyncIteratorEveryArgs(IteratorRecord iterated, Object fn) {
                this.iterated = iterated;
                this.fn = fn;
            }
        }

        protected static class AsyncIteratorEveryRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorEveryArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorEveryArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorEveryRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorEveryWithResult, AsyncIteratorEveryNode::createEveryWithResultFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorEveryArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return createIterResultObjectNode.execute(frame, true, true);
                }

                Object value = iteratorValueNode.execute(next);
                Object result = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.fn, value));
                return awaitNode.execute(result, args);
            }
        }

        protected static class AsyncIteratorEveryWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorEveryArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorEveryArgs> awaitNode;
            @Child private JSToBooleanNode toBooleanNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            public AsyncIteratorEveryWithResultRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                toBooleanNode = JSToBooleanNode.create();
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorEvery, AsyncIteratorEveryNode::createEveryFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorEveryArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                if (!toBooleanNode.executeBoolean(result)) {
                    return createIterResultObjectNode.execute(frame, false, true);
                }

                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(value, args);
            }
        }

        private static JSFunctionData createEveryFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorEveryRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createEveryWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorEveryWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorFindNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorFindArgs> awaitNode;

        public AsyncIteratorFindNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFind, AsyncIteratorFindNode::createFindFunctionImpl);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject find(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirectNode.execute(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.execute(value, new AsyncIteratorFindArgs(record, fn));
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(Object thisObj, Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        private static class AsyncIteratorFindArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final IteratorRecord iterated;
            public final Object fn;

            AsyncIteratorFindArgs(IteratorRecord iterated, Object fn) {
                this.iterated = iterated;
                this.fn = fn;
            }
        }

        private static class AsyncIteratorFindWithResultArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final AsyncIteratorFindArgs args;
            public final Object value;

            AsyncIteratorFindWithResultArgs(AsyncIteratorFindArgs args, Object value) {
                this.args = args;
                this.value = value;
            }
        }

        protected static class AsyncIteratorFindRootNode extends AsyncIteratorNextUtilsRootNode<AsyncIteratorFindArgs> {
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFindWithResultArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorFindRootNode(JSContext context) {
                super(context);

                iteratorValueNode = IteratorValueNode.create(context);
                callNode = JSFunctionCallNode.createCall();
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFindWithResult, AsyncIteratorFindNode::createFindWithResultFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorFindArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                JSDynamicObject promiseResult = checkNext(next);
                if (promiseResult != null) {
                    return promiseResult;
                }

                JSDynamicObject promiseCompleted = checkComplete(frame, next);
                if (promiseCompleted != null) {
                    return promiseCompleted;
                }

                Object value = iteratorValueNode.execute(next);
                Object result = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.fn, value));
                return awaitNode.execute(result, new AsyncIteratorFindWithResultArgs(args, value));
            }
        }

        protected static class AsyncIteratorFindWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<AsyncIteratorFindWithResultArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFindArgs> awaitNode;
            @Child private JSToBooleanNode toBooleanNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            public AsyncIteratorFindWithResultRootNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                callNode = JSFunctionCallNode.createCall();
                toBooleanNode = JSToBooleanNode.create();
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFind, AsyncIteratorFindNode::createFindFunctionImpl);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorFindWithResultArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                if (toBooleanNode.executeBoolean(result)) {
                    return createIterResultObjectNode.execute(frame, args.value, true);
                }

                Object value = iteratorNextNode.execute(args.args.iterated);
                return awaitNode.execute(value, args.args);
            }
        }

        private static JSFunctionData createFindFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFindRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createFindWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFindWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }
}

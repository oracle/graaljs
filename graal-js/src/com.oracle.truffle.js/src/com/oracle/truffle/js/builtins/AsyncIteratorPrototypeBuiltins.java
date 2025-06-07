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
package com.oracle.truffle.js.builtins;

import java.util.ArrayDeque;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.AsyncIteratorPrototypeBuiltins.AsyncIteratorAwaitNode.AsyncIteratorArgs;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltins.IteratorMethodNode;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltins.IteratorMethodWithCallableNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.AsyncIteratorCloseNode;
import com.oracle.truffle.js.nodes.access.GetIteratorFlattenableNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
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
import com.oracle.truffle.js.nodes.cast.LongToIntOrDoubleNode;
import com.oracle.truffle.js.nodes.control.AsyncGeneratorDrainQueueNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
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
import com.oracle.truffle.js.runtime.builtins.JSAsyncGeneratorObject;
import com.oracle.truffle.js.runtime.builtins.JSAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Contains builtins for AsyncIterator.prototype.
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

    public static class AsyncIteratorAwaitNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends JavaScriptBaseNode {
        public static final HiddenKey THIS_ID = new HiddenKey("awaitThis");
        public static final HiddenKey ARGS_ID = new HiddenKey("awaitArgs");

        public abstract static class AsyncIteratorRootNode<T> extends JavaScriptRootNode implements AsyncHandlerRootNode {
            @Child protected JavaScriptNode valueNode;
            @Child private PropertyGetNode getArgsNode;
            @Child private PropertyGetNode getThisNode;
            @Child private JSFunctionCallNode callNode;
            @Child private LongToIntOrDoubleNode indexToNumber = LongToIntOrDoubleNode.create();

            protected final JSContext context;

            protected AsyncIteratorRootNode(JSContext context) {
                super(context.getLanguage(), null, null);
                this.context = context;

                this.valueNode = AccessIndexedArgumentNode.create(0);
                this.getArgsNode = PropertyGetNode.createGetHidden(ARGS_ID, context);
                this.callNode = JSFunctionCallNode.createCall();
            }

            @SuppressWarnings("unchecked")
            protected final T getArgs(VirtualFrame frame) {
                JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                return (T) getArgsNode.getValue(functionObject);
            }

            protected final JSAsyncGeneratorObject getThis(VirtualFrame frame) {
                if (getThisNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getThisNode = insert(PropertyGetNode.createGetHidden(THIS_ID, context));
                }
                return (JSAsyncGeneratorObject) getThisNode.getValue(JSFrameUtil.getFunctionObject(frame));
            }

            @Override
            public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
                assert JSFunction.isJSFunction(handlerFunction) && ((RootCallTarget) JSFunction.getFunctionData(handlerFunction).getCallTarget()).getRootNode() == this;
                if (JSObjectUtil.getHiddenProperty(handlerFunction, THIS_ID) instanceof JSDynamicObject thisObj) {
                    JSDynamicObject promise = (JSDynamicObject) JSObjectUtil.getHiddenProperty(thisObj, PROMISE_ID);
                    return new AsyncStackTraceInfo(promise, null);
                }
                return new AsyncStackTraceInfo();
            }

            protected final Object indexToJS(long index) {
                return indexToNumber.fromIndex(null, index);
            }

            protected final void callResolve(PromiseCapabilityRecord promiseCapability, Object resultValue) {
                callResolveOrReject(promiseCapability.getPromise(), promiseCapability.getResolve(), resultValue);
            }

            protected final void callReject(PromiseCapabilityRecord promiseCapability, Object resultValue) {
                callResolveOrReject(promiseCapability.getPromise(), promiseCapability.getReject(), resultValue);
            }

            private void callResolveOrReject(JSDynamicObject promise, Object resolveOrReject, Object resultValue) {
                callNode.executeCall(JSArguments.createOneArg(promise, resolveOrReject, resultValue));
            }
        }

        private static class AsyncIteratorIfAbruptCloseNode extends AsyncIteratorRootNode<AsyncIteratorArgs> {
            @Child private AsyncIteratorCloseNode closeNode;

            AsyncIteratorIfAbruptCloseNode(JSContext context) {
                super(context);
                closeNode = AsyncIteratorCloseNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                AsyncIteratorArgs args = getArgs(frame);
                return closeNode.executeAbrupt(args.iterated.getIterator(), valueNode.execute(frame));
            }
        }

        private static class AsyncIteratorIfAbruptReturnNode extends AsyncIteratorRootNode<AsyncIteratorArgs> {
            @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;

            AsyncIteratorIfAbruptReturnNode(JSContext context) {
                super(context);
                this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                PromiseCapabilityRecord capabilityRecord = newPromiseCapabilityNode.executeDefault();
                Object rejection = valueNode.execute(frame);
                callReject(capabilityRecord, rejection);
                return capabilityRecord.getPromise();
            }
        }

        private static class AsyncIteratorGeneratorIfAbruptCloseNode extends AbstractAsyncIteratorGeneratorResumptionRootNode<AsyncIteratorArgs> {
            @Child private AsyncIteratorCloseNode closeNode;

            AsyncIteratorGeneratorIfAbruptCloseNode(JSContext context) {
                super(context);

                closeNode = AsyncIteratorCloseNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object rejection = valueNode.execute(frame);
                AsyncIteratorArgs args = getArgs(frame);
                closeNode.executeAbrupt(args.iterated.getIterator(), rejection);
                return asyncGeneratorComplete(frame, Completion.Type.Throw, rejection);
            }
        }

        private static class AsyncIteratorGeneratorIfAbruptReturnNode extends AbstractAsyncIteratorGeneratorResumptionRootNode<AsyncIteratorArgs> {

            AsyncIteratorGeneratorIfAbruptReturnNode(JSContext context) {
                super(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object rejection = valueNode.execute(frame);
                return asyncGeneratorComplete(frame, Completion.Type.Throw, rejection);
            }
        }

        private static class AsyncIteratorGeneratorReturnNode extends AbstractAsyncIteratorGeneratorResumptionRootNode<AsyncIteratorArgs> {

            AsyncIteratorGeneratorReturnNode(JSContext context) {
                super(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object value = valueNode.execute(frame);
                return asyncGeneratorComplete(frame, Completion.Type.Normal, value);
            }
        }

        /**
         * Resumption entry point for non-generator-based async iterators.
         */
        public abstract static class AsyncIteratorNonGeneratorResumptionRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends AsyncIteratorRootNode<T> {
            @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
            @Child private IsObjectNode isObjectNode;
            @Child private IteratorCompleteNode iteratorCompleteNode;
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

            AsyncIteratorNonGeneratorResumptionRootNode(JSContext context) {
                super(context);
                this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                this.isObjectNode = IsObjectNode.create();
                this.iteratorCompleteNode = IteratorCompleteNode.create();
            }

            public abstract Object executeBody(VirtualFrame frame);

            @Override
            public Object execute(VirtualFrame frame) {
                PromiseCapabilityRecord promiseCapability;
                Object resultValue;
                try {
                    resultValue = executeBody(frame);
                    if (resultValue instanceof JSPromiseObject) {
                        // Await
                        return resultValue;
                    }
                    promiseCapability = newPromiseCapabilityNode.executeDefault();
                    callResolve(promiseCapability, resultValue);
                } catch (AbstractTruffleException e) {
                    resultValue = getErrorObject(e);
                    promiseCapability = newPromiseCapabilityNode.executeDefault();
                    callReject(promiseCapability, resultValue);
                }
                return promiseCapability.getPromise();
            }

            protected final Object getErrorObject(AbstractTruffleException ex) {
                if (getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                }
                return getErrorObjectNode.execute(ex);
            }

            protected final Object checkNext(Object value) {
                if (!isObjectNode.executeBoolean(value)) {
                    throw Errors.createTypeErrorIterResultNotAnObject(value, this);
                }
                return value;
            }

            protected final boolean iteratorComplete(Object value) {
                return iteratorCompleteNode.execute(value);
            }
        }

        public abstract static class AsyncIteratorNonGeneratorResumptionWithCloseRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends AsyncIteratorNonGeneratorResumptionRootNode<T> {
            @Child protected IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorCloseNode closeNode;

            AsyncIteratorNonGeneratorResumptionWithCloseRootNode(JSContext context) {
                super(context);
                this.iteratorValueNode = IteratorValueNode.create();
                this.closeNode = AsyncIteratorCloseNode.create(context);
            }

            protected final Object asyncIteratorCloseAbrupt(IteratorRecord iterated, AbstractTruffleException exception) {
                Object rejection = getErrorObject(exception);
                closeNode.executeAbrupt(iterated.getIterator(), rejection);
                throw exception;
            }
        }

        public abstract static class AbstractAsyncIteratorGeneratorResumptionRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends AsyncIteratorRootNode<T> {

            @Child private AsyncGeneratorDrainQueueNode asyncGeneratorOpNode;

            protected AbstractAsyncIteratorGeneratorResumptionRootNode(JSContext context) {
                super(context);

                this.asyncGeneratorOpNode = AsyncGeneratorDrainQueueNode.create(context);
            }

            protected final Object getErrorObject(AbstractTruffleException ex) {
                return asyncGeneratorOpNode.getErrorObject(ex);
            }

            protected final Object asyncGeneratorComplete(VirtualFrame frame, Completion.Type resultType, Object resultValue) {
                var generator = getThis(frame);
                asyncGeneratorOpNode.asyncGeneratorCompleteStepAndDrainQueue(generator, resultType, resultValue);
                return Undefined.instance;
            }

            protected final void asyncGeneratorCompleteStep(Completion.Type completionType, Object completionValue, boolean done, ArrayDeque<AsyncGeneratorRequest> queue) {
                asyncGeneratorOpNode.asyncGeneratorCompleteStep(completionType, completionValue, done, queue);
            }
        }

        /**
         * Await resumption entry point for generator-based async iterators.
         */
        public abstract static class AsyncIteratorGeneratorAwaitResumptionRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends AbstractAsyncIteratorGeneratorResumptionRootNode<T> {

            AsyncIteratorGeneratorAwaitResumptionRootNode(JSContext context) {
                super(context);
            }

            public abstract Object executeBody(VirtualFrame frame);

            @Override
            public final Object execute(VirtualFrame frame) {
                assertResumingAwait(frame);
                Completion.Type resultType;
                Object resultValue;
                try {
                    resultValue = executeBody(frame);
                    if (resultValue instanceof JSPromiseObject) {
                        // Await
                        return resultValue;
                    }
                    // normal or return
                    resultType = Completion.Type.Normal;
                } catch (AbstractTruffleException e) {
                    resultType = Completion.Type.Throw;
                    resultValue = getErrorObject(e);
                }

                return asyncGeneratorComplete(frame, resultType, resultValue);
            }

            protected final void assertResumingAwait(VirtualFrame frame) {
                var generator = getThis(frame);
                assert generator.getAsyncGeneratorState() == JSFunction.AsyncGeneratorState.Executing : generator.getAsyncGeneratorState();
            }
        }

        public abstract static class AsyncIteratorGeneratorAwaitResumptionWithNextRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs>
                        extends AsyncIteratorGeneratorAwaitResumptionRootNode<T> {
            @Child private IsObjectNode isObjectNode;
            @Child private IteratorCompleteNode iteratorCompleteNode;
            @Child protected IteratorValueNode iteratorValueNode;

            AsyncIteratorGeneratorAwaitResumptionWithNextRootNode(JSContext context) {
                super(context);
                this.isObjectNode = IsObjectNode.create();
                this.iteratorCompleteNode = IteratorCompleteNode.create();
                this.iteratorValueNode = IteratorValueNode.create();
            }

            protected final void checkNext(Object value) {
                if (!isObjectNode.executeBoolean(value)) {
                    throw Errors.createTypeErrorIterResultNotAnObject(value, this);
                }
            }

            protected final boolean iteratorComplete(Object value) {
                return iteratorCompleteNode.execute(value);
            }
        }

        public abstract static class AsyncIteratorGeneratorAwaitResumptionWithCloseRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs>
                        extends AsyncIteratorGeneratorAwaitResumptionWithNextRootNode<T> {
            @Child private AsyncIteratorCloseNode closeNode;

            AsyncIteratorGeneratorAwaitResumptionWithCloseRootNode(JSContext context) {
                super(context);
            }

            protected final Object asyncIteratorCloseAbrupt(IteratorRecord iterated, AbstractTruffleException exception) {
                Object rejection = getErrorObject(exception);
                if (closeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    closeNode = insert(AsyncIteratorCloseNode.create(context));
                }
                closeNode.executeAbrupt(iterated.getIterator(), rejection);
                throw exception;
            }
        }

        /**
         * Suspended start/yield entry point for generator-based async iterators.
         */
        public abstract static class AsyncIteratorGeneratorYieldResumptionRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> extends AbstractAsyncIteratorGeneratorResumptionRootNode<T> {

            @Child private AsyncIteratorAwaitNode<AsyncIteratorArgs> awaitYieldResumptionNode;

            AsyncIteratorGeneratorYieldResumptionRootNode(JSContext context) {
                super(context);
                if (this instanceof AsyncIteratorFlatMapNode.AsyncIteratorFlatMapRootNode) {
                    this.awaitYieldResumptionNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorUnwrapYieldResumptionCloseInnerIterator,
                                    AsyncIteratorFlatMapUnwrapYieldResumptionRootNode::createCreateUnwrapYieldResumptionCloseImpl, true);
                } else {
                    this.awaitYieldResumptionNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorUnwrapYieldResumptionClose,
                                    AsyncIteratorUnwrapYieldResumptionRootNode::createCreateUnwrapYieldResumptionCloseImpl, true);
                }
            }

            public abstract Object executeBody(VirtualFrame frame);

            @Override
            public final Object execute(VirtualFrame frame) {
                var generator = getThis(frame);
                JSFunction.AsyncGeneratorState suspendedState = generator.getAsyncGeneratorState();
                // State can be Executing for cases where we loop back to the start from an Await.
                assert suspendedState == JSFunction.AsyncGeneratorState.SuspendedStart || suspendedState == JSFunction.AsyncGeneratorState.SuspendedYield ||
                                suspendedState == JSFunction.AsyncGeneratorState.Executing : suspendedState;
                generator.setAsyncGeneratorState(JSFunction.AsyncGeneratorState.Executing);

                Completion.Type resultType = Completion.Type.Normal;
                Object resultValue = Undefined.instance;
                if (suspendedState == JSFunction.AsyncGeneratorState.SuspendedYield) {
                    ArrayDeque<AsyncGeneratorRequest> queue = generator.getAsyncGeneratorQueue();
                    assert !queue.isEmpty();
                    AsyncGeneratorRequest toYield = queue.peekFirst();
                    // AsyncGeneratorUnwrapYieldResumption
                    if (toYield.isReturn()) {
                        return awaitYieldResumptionNode.executeThis(toYield.getCompletionValue(), getArgs(frame), generator);
                    } else if (toYield.isThrow()) {
                        resultType = Completion.Type.Throw;
                        resultValue = toYield.getCompletionValue();
                    }
                }
                if (resultType == Completion.Type.Normal) {
                    try {
                        resultValue = executeBody(frame);
                        if (resultValue instanceof JSPromiseObject) {
                            // Await
                            return resultValue;
                        }
                        // normal or return completion
                    } catch (AbstractTruffleException e) {
                        resultType = Completion.Type.Throw;
                        resultValue = getErrorObject(e);
                    }
                }
                assert resultType == Completion.Type.Normal || resultType == Completion.Type.Throw;
                return asyncGeneratorComplete(frame, resultType, resultValue);
            }
        }

        public static class AsyncIteratorArgs {
            public final IteratorRecord iterated;

            public AsyncIteratorArgs(IteratorRecord iterated) {
                this.iterated = iterated;
            }
        }

        protected static class AsyncIteratorWithCounterArgs extends AsyncIteratorArgs {
            public long counter;

            protected AsyncIteratorWithCounterArgs(IteratorRecord iterated) {
                super(iterated);
            }
        }

        @Child private PropertySetNode setArgs;
        @Child private PropertySetNode setThisNode;
        @Child private PropertyGetNode getThisNode;
        @Child protected JSFunctionCallNode callNode;
        @Child protected PropertyGetNode getConstructorNode;
        @Child protected NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child protected PerformPromiseThenNode performPromiseThenNode;
        private final JSContext.BuiltinFunctionKey thenKey;
        private final Function<JSContext, JSFunctionData> thenCreate;
        private final JSContext.BuiltinFunctionKey catchKey;
        private final Function<JSContext, JSFunctionData> catchCreate;
        private final JSContext context;

        public AsyncIteratorAwaitNode(JSContext context, JSContext.BuiltinFunctionKey thenKey, Function<JSContext, JSFunctionData> thenCreate,
                        JSContext.BuiltinFunctionKey catchKey, Function<JSContext, JSFunctionData> catchCreate) {
            this.context = context;
            this.thenKey = thenKey;
            this.thenCreate = thenCreate;
            this.catchKey = catchKey;
            this.catchCreate = catchCreate;

            this.setArgs = PropertySetNode.createSetHidden(ARGS_ID, context);
            this.setThisNode = PropertySetNode.createSetHidden(THIS_ID, context);
            this.getThisNode = PropertyGetNode.createGetHidden(THIS_ID, context);
            this.getConstructorNode = PropertyGetNode.create(JSObject.CONSTRUCTOR, context);
            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.performPromiseThenNode = PerformPromiseThenNode.create(context);
            this.callNode = JSFunctionCallNode.createCall();
        }

        public final JSDynamicObject execute(VirtualFrame frame, Object promiseOrValue, T args) {
            return executeThis(promiseOrValue, args, getThisNode.getValue(JSFrameUtil.getFunctionObject(frame)));
        }

        private JSPromiseObject promiseResolve(Object promiseOrValue) {
            if (JSPromise.isJSPromise(promiseOrValue) && getConstructorNode.getValueOrDefault(promiseOrValue, Undefined.instance) == getRealm().getPromiseConstructor()) {
                return (JSPromiseObject) promiseOrValue;
            } else {
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getResolve(), promiseOrValue));
                return (JSPromiseObject) promiseCapability.getPromise();
            }
        }

        public final JSDynamicObject executeThis(Object promiseOrValue, T args, Object thisObj) {
            JSPromiseObject promise = promiseResolve(promiseOrValue);

            JSFunctionObject then = createFunction(args);
            JSFunctionObject catchObj = createFunctionWithArgs(args, context.getOrCreateBuiltinFunctionData(catchKey, catchCreate));

            setThisNode.setValue(then, thisObj);
            setThisNode.setValue(catchObj, thisObj);

            return performPromiseThenNode.execute(promise, then, catchObj, newPromiseCapabilityNode.executeDefault());
        }

        protected final JSFunctionObject createFunctionWithArgs(AsyncIteratorArgs args, JSFunctionData functionData) {
            JSFunctionObject function = JSFunction.create(getRealm(), functionData);
            setArgs.setValue(function, args);
            return function;
        }

        public JSFunctionObject createFunction(T args) {
            return createFunctionWithArgs(args, context.getOrCreateBuiltinFunctionData(thenKey, thenCreate));
        }

        public static <T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> AsyncIteratorAwaitNode<T> create(JSContext context,
                        JSContext.BuiltinFunctionKey key, Function<JSContext, JSFunctionData> create, boolean closeOnAbrupt) {
            JSContext.BuiltinFunctionKey catchKey;
            Function<JSContext, JSFunctionData> catchCreate;
            if (closeOnAbrupt) {
                catchKey = JSContext.BuiltinFunctionKey.AsyncIteratorIfAbruptClose;
                catchCreate = AsyncIteratorAwaitNode::createIfAbruptCloseFunctionImpl;
            } else {
                catchKey = JSContext.BuiltinFunctionKey.AsyncIteratorIfAbruptReturn;
                catchCreate = AsyncIteratorAwaitNode::createIfAbruptReturnFunctionImpl;
            }
            return create(context, key, create, catchKey, catchCreate);
        }

        public static <T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> AsyncIteratorAwaitNode<T> createGen(JSContext context,
                        JSContext.BuiltinFunctionKey key, Function<JSContext, JSFunctionData> create, boolean closeOnAbrupt) {
            JSContext.BuiltinFunctionKey catchKey;
            Function<JSContext, JSFunctionData> catchCreate;
            if (closeOnAbrupt) {
                catchKey = JSContext.BuiltinFunctionKey.AsyncIteratorGeneratorIfAbruptClose;
                catchCreate = AsyncIteratorAwaitNode::createGeneratorIfAbruptCloseFunctionImpl;
            } else {
                catchKey = JSContext.BuiltinFunctionKey.AsyncIteratorGeneratorIfAbruptReturn;
                catchCreate = AsyncIteratorAwaitNode::createGeneratorIfAbruptReturnFunctionImpl;
            }
            return create(context, key, create, catchKey, catchCreate);
        }

        public static <T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> AsyncIteratorAwaitNode<T> create(JSContext context,
                        JSContext.BuiltinFunctionKey thenKey, Function<JSContext, JSFunctionData> thenCreate,
                        JSContext.BuiltinFunctionKey catchKey, Function<JSContext, JSFunctionData> catchCreate) {
            return new AsyncIteratorAwaitNode<>(context, thenKey, thenCreate, catchKey, catchCreate);
        }

        public static <T extends AsyncIteratorAwaitNode.AsyncIteratorArgs> AsyncIteratorAwaitNode<T> createGeneratorYield(JSContext context) {
            // First part of AsyncGeneratorYield: Await(value).
            // Always followed by IfAbruptCloseAsyncIterator.
            return AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorYield, AsyncIteratorYieldResultRootNode::createYieldResultFunctionImpl, true);
        }

        private static JSFunctionData createIfAbruptCloseFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorIfAbruptCloseNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createIfAbruptReturnFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorIfAbruptReturnNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createGeneratorIfAbruptCloseFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorGeneratorIfAbruptCloseNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createGeneratorIfAbruptReturnFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorGeneratorIfAbruptReturnNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createGeneratorReturnFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorGeneratorReturnNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected static class AsyncIteratorYieldResultRootNode extends AsyncIteratorAwaitNode.AbstractAsyncIteratorGeneratorResumptionRootNode<AsyncIteratorArgs> {
        @Child private JSFunctionCallNode callNode;
        @Child private PropertyGetNode getContinuation;

        protected AsyncIteratorYieldResultRootNode(JSContext context) {
            super(context);
            this.callNode = JSFunctionCallNode.createCall();
            this.getContinuation = PropertyGetNode.createGetHidden(AsyncIteratorHelperPrototypeBuiltins.IMPL_ID, context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object value = valueNode.execute(frame); // awaited value
            var generator = getThis(frame);

            ArrayDeque<AsyncGeneratorRequest> queue = generator.getAsyncGeneratorQueue();
            assert !queue.isEmpty();
            // Perform AsyncGeneratorCompleteStep
            asyncGeneratorCompleteStep(Completion.Type.Normal, value, false, queue);

            if (!queue.isEmpty()) {
                // NOTE: Execution continues without suspending the generator.
                return asyncGeneratorUnwrapYieldResumption(generator);
            } else {
                generator.setAsyncGeneratorState(JSFunction.AsyncGeneratorState.SuspendedYield);
                return Undefined.instance;
            }
        }

        private Object asyncGeneratorUnwrapYieldResumption(JSAsyncGeneratorObject generator) {
            generator.setAsyncGeneratorState(JSFunction.AsyncGeneratorState.SuspendedYield);
            Object continuation = getContinuation.getValue(generator);
            return callNode.executeCall(JSArguments.createZeroArg(generator, continuation));
        }

        protected static JSFunctionData createYieldResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorYieldResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected static class AsyncIteratorUnwrapYieldResumptionRootNode<T extends AsyncIteratorAwaitNode.AsyncIteratorArgs>
                    extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionRootNode<T> {

        @Child protected GetMethodNode getReturnNode;
        @Child protected AsyncIteratorAwaitNode<AsyncIteratorArgs> awaitReturnResult;
        @Child protected IsJSObjectNode isObjectNode;
        @Child private JSFunctionCallNode callReturnNode;

        protected AsyncIteratorUnwrapYieldResumptionRootNode(JSContext context, boolean closeResumption) {
            super(context);
            this.getReturnNode = GetMethodNode.create(context, Strings.RETURN);
            this.awaitReturnResult = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorUnwrapYieldResumptionCloseResumption,
                            AsyncIteratorUnwrapYieldResumptionRootNode::createCreateUnwrapYieldResumptionCloseResumptionImpl, false);
            this.isObjectNode = closeResumption ? IsJSObjectNode.create() : null;
        }

        @Override
        public Object executeBody(VirtualFrame frame) {
            Object awaitedValue = valueNode.execute(frame);
            var generator = getThis(frame);

            ArrayDeque<AsyncGeneratorRequest> queue = generator.getAsyncGeneratorQueue();
            assert !queue.isEmpty();
            AsyncGeneratorRequest result = queue.peekFirst();
            if (result.isReturn()) {
                // AsyncIteratorClose
                AsyncIteratorArgs args = getArgs(frame);
                if (isObjectNode == null) {
                    Object iterator = args.iterated.getIterator();
                    Object returnMethod = getReturnNode.executeWithTarget(iterator);
                    if (returnMethod != Undefined.instance) {
                        Object returnResult = callReturn(iterator, returnMethod);
                        return awaitReturnResult.executeThis(returnResult, args, generator);
                    }
                } else {
                    if (!isObjectNode.executeBoolean(awaitedValue)) {
                        throw Errors.createTypeErrorIterResultNotAnObject(awaitedValue, this);
                    }
                }
            }
            return Undefined.instance;
        }

        protected final Object callReturn(Object iterator, Object returnMethod) {
            if (callReturnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callReturnNode = insert(JSFunctionCallNode.createCall());
            }
            return callReturnNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
        }

        /**
         * Resumption of UnwrapYieldResumption: Await(resumptionValue.[[Value]]). Continues with
         * AsyncIteratorClose(iterated, awaitedValue) and either awaits or returns;
         */
        protected static JSFunctionData createCreateUnwrapYieldResumptionCloseImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorUnwrapYieldResumptionRootNode<>(context, false).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        /**
         * Resumption of AsyncIteratorClose: Await(innerResult.[[Value]]) with normal completion.
         * Checks that innerResult is an object and returns from the generator.
         */
        protected static JSFunctionData createCreateUnwrapYieldResumptionCloseResumptionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorUnwrapYieldResumptionRootNode<>(context, true).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected static class AsyncIteratorFlatMapUnwrapYieldResumptionRootNode
                    extends AsyncIteratorUnwrapYieldResumptionRootNode<AsyncIteratorFlatMapNode.AsyncIteratorFlatMapArgs> {

        @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapNode.AsyncIteratorFlatMapArgs> awaitInnerIteratorReturnResult;

        protected AsyncIteratorFlatMapUnwrapYieldResumptionRootNode(JSContext context, boolean closeResumption) {
            super(context, closeResumption);
            this.awaitInnerIteratorReturnResult = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorUnwrapYieldResumptionCloseInnerIteratorResumption,
                            AsyncIteratorFlatMapUnwrapYieldResumptionRootNode::createCreateUnwrapYieldResumptionCloseResumptionImpl, true);
        }

        @Override
        public Object executeBody(VirtualFrame frame) {
            Object awaitedValue = valueNode.execute(frame);
            var generator = getThis(frame);

            ArrayDeque<AsyncGeneratorRequest> queue = generator.getAsyncGeneratorQueue();
            assert !queue.isEmpty();
            AsyncGeneratorRequest result = queue.peekFirst();
            if (result.isReturn()) {
                // 1. Let backupCompletion be Completion(IteratorClose(innerIterator, completion)).
                // 2. IfAbruptCloseIterator(backupCompletion, iterated).
                // 3. Return ? IteratorClose(completion, iterated).
                AsyncIteratorFlatMapNode.AsyncIteratorFlatMapArgs args = getArgs(frame);
                if (isObjectNode == null) {
                    assert args.innerIterator != null;
                    Object iterator = args.innerIterator.getIterator();
                    Object returnMethod = getReturnNode.executeWithTarget(iterator);
                    if (returnMethod != Undefined.instance) {
                        Object returnResult = callReturn(iterator, returnMethod);
                        return awaitInnerIteratorReturnResult.executeThis(returnResult, args, generator);
                    } else {
                        args.innerIterator = null;
                    }
                    iterator = args.iterated.getIterator();
                    returnMethod = getReturnNode.executeWithTarget(iterator);
                    if (returnMethod != Undefined.instance) {
                        Object returnResult = callReturn(iterator, returnMethod);
                        return awaitReturnResult.executeThis(returnResult, args, generator);
                    }
                } else {
                    if (!isObjectNode.executeBoolean(awaitedValue)) {
                        throw Errors.createTypeErrorIterResultNotAnObject(awaitedValue, this);
                    }
                    assert args.innerIterator != null;
                    args.innerIterator = null;
                    Object iterator = args.iterated.getIterator();
                    Object returnMethod = getReturnNode.executeWithTarget(iterator);
                    if (returnMethod != Undefined.instance) {
                        Object returnResult = callReturn(iterator, returnMethod);
                        return awaitReturnResult.executeThis(returnResult, args, generator);
                    }
                }
            }
            return Undefined.instance;
        }

        /**
         * Resumption of UnwrapYieldResumption: Await(resumptionValue.[[Value]]). Continues with
         * AsyncIteratorClose(iterated, awaitedValue) and either awaits or returns;
         */
        protected static JSFunctionData createCreateUnwrapYieldResumptionCloseImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFlatMapUnwrapYieldResumptionRootNode(context, false).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        /**
         * Resumption of AsyncIteratorClose: Await(innerResult.[[Value]]) with normal completion.
         * Checks that innerResult is an object and returns from the generator.
         */
        protected static JSFunctionData createCreateUnwrapYieldResumptionCloseResumptionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorFlatMapUnwrapYieldResumptionRootNode(context, true).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorMapNode extends IteratorMethodWithCallableNode {
        @Child private AsyncIteratorAwaitNode<AsyncIteratorMapArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;

        public AsyncIteratorMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorMap, AsyncIteratorMapNode::createMapFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject map(Object thisObj, Object mapper) {
            IteratorRecord record = getIteratorDirect(thisObj);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorMapArgs(record, mapper)));
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object mapper) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorMapArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object mapper;

            public AsyncIteratorMapArgs(IteratorRecord iterated, Object mapper) {
                super(iterated);
                this.mapper = mapper;
            }
        }

        protected static class AsyncIteratorMapRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorYieldResumptionRootNode<AsyncIteratorMapArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorMapArgs> awaitNode;

            public AsyncIteratorMapRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorMapWithValue, AsyncIteratorMapNode::createMapWithValueFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorMapArgs args = getArgs(frame);
                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(frame, value, args);
            }
        }

        protected static class AsyncIteratorMapWithValueRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionWithCloseRootNode<AsyncIteratorMapArgs> {
            @Child private JSFunctionCallNode callNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorArgs> yieldNode;

            public AsyncIteratorMapWithValueRootNode(JSContext context) {
                super(context);

                this.callNode = JSFunctionCallNode.createCall();
                this.yieldNode = AsyncIteratorAwaitNode.createGeneratorYield(context);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorMapArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                Object value = iteratorValueNode.execute(next);
                Object result;
                try {
                    result = callNode.executeCall(JSArguments.create(Undefined.instance, args.mapper, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.counter++;
                return yieldNode.execute(frame, result, args);
            }
        }

        private static JSFunctionData createMapFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorMapRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createMapWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorMapWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorFilterNode extends IteratorMethodWithCallableNode {
        @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;

        public AsyncIteratorFilterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilter, AsyncIteratorFilterNode::createFilterFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(filterer)")
        public JSDynamicObject filter(Object thisObj, Object filterer) {
            IteratorRecord record = getIteratorDirect(thisObj);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorFilterArgs(record, filterer)));
        }

        @Specialization(guards = "!isCallable(filterer)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object filterer) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorFilterArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object filterer;
            public Object value;

            public AsyncIteratorFilterArgs(IteratorRecord iterated, Object filterer) {
                super(iterated);
                this.filterer = filterer;
            }
        }

        protected static class AsyncIteratorFilterRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorYieldResumptionRootNode<AsyncIteratorFilterArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterArgs> awaitNode;

            public AsyncIteratorFilterRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilterWithValue, AsyncIteratorFilterNode::createFilterWithValueFunctionImpl,
                                false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFilterArgs args = getArgs(frame);
                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(frame, value, args);
            }
        }

        protected static class AsyncIteratorFilterWithValueRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionWithCloseRootNode<AsyncIteratorFilterArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterArgs> awaitNode;
            @Child private JSFunctionCallNode callFiltererNode;

            public AsyncIteratorFilterWithValueRootNode(JSContext context) {
                super(context);

                this.callFiltererNode = JSFunctionCallNode.createCall();
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilterWithResult,
                                AsyncIteratorFilterNode::createFilterWithResultFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFilterArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                Object value = iteratorValueNode.execute(next);
                Object result;
                try {
                    result = callFiltererNode.executeCall(JSArguments.create(Undefined.instance, args.filterer, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.value = value;
                args.counter++;
                return awaitNode.execute(frame, result, args);
            }
        }

        protected static class AsyncIteratorFilterWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionRootNode<AsyncIteratorFilterArgs> {
            @Child private JSToBooleanNode toBooleanNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFilterArgs> awaitNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorArgs> yieldNode;

            public AsyncIteratorFilterWithResultRootNode(JSContext context) {
                super(context);

                this.toBooleanNode = JSToBooleanNode.create();
                this.yieldNode = AsyncIteratorAwaitNode.createGeneratorYield(context);
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFilter, AsyncIteratorFilterNode::createFilterFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFilterArgs args = getArgs(frame);
                Object value = args.value;
                assert value != null;
                args.value = null;
                if (toBooleanNode.executeBoolean(valueNode.execute(frame))) {
                    return yieldNode.execute(frame, value, args);
                } else {
                    return awaitNode.execute(frame, Undefined.instance, args);
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

    protected abstract static class AsyncIteratorTakeNode extends IteratorMethodNode {

        @Child private AsyncIteratorAwaitNode<AsyncIteratorTakeArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;
        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;

        public AsyncIteratorTakeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorTake, AsyncIteratorTakeNode::createTakeFunctionImpl, false);
            this.toNumberNode = JSToNumberNode.create();
            this.toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
        }

        @Specialization
        public JSDynamicObject take(Object thisObj, Object limit,
                        @Cached IsObjectNode isObjectNode,
                        @Cached InlinedBranchProfile errorBranch) {

            if (!isObjectNode.executeBoolean(thisObj)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorNotAnObject(thisObj, this);
            }

            Number numLimit = toNumberNode.executeNumber(limit);
            if (JSRuntime.isNaN(numLimit)) {
                errorBranch.enter(this);
                throw Errors.createRangeError("NaN is not allowed", this);
            }

            double integerLimit = JSRuntime.doubleValue(toIntegerOrInfinityNode.executeNumber(numLimit));
            if (integerLimit < 0) {
                errorBranch.enter(this);
                throw Errors.createRangeErrorIndexNegative(this);
            }

            IteratorRecord record = getIteratorDirect(thisObj);
            AsyncIteratorTakeArgs args = new AsyncIteratorTakeArgs(record, integerLimit);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(args));
        }

        protected static class AsyncIteratorTakeArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public double remaining;

            public AsyncIteratorTakeArgs(IteratorRecord iterated, double limit) {
                super(iterated);
                this.remaining = limit;
            }
        }

        protected static class AsyncIteratorTakeRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorYieldResumptionRootNode<AsyncIteratorTakeArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorTakeArgs> awaitNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorTakeArgs> awaitInnerResultNode;
            @Child private AsyncIteratorCloseNode asyncIteratorCloseNode;

            public AsyncIteratorTakeRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorTakeWithValue, AsyncIteratorTakeNode::createTakeWithValueFunctionImpl, false);
                this.awaitInnerResultNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorGeneratorReturn,
                                AsyncIteratorAwaitNode::createGeneratorReturnFunctionImpl, false);
                this.asyncIteratorCloseNode = AsyncIteratorCloseNode.create(context);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorTakeArgs args = getArgs(frame);
                double remaining = args.remaining;
                if (remaining == 0) {
                    Object returnValue = asyncIteratorCloseNode.execute(args.iterated.getIterator(), Undefined.instance);
                    if (returnValue == Undefined.instance) {
                        return returnValue;
                    }
                    return awaitInnerResultNode.execute(frame, returnValue, args);
                }
                if (remaining != Double.POSITIVE_INFINITY) {
                    args.remaining--;
                }

                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(frame, value, args);
            }
        }

        protected static class AsyncIteratorTakeWithValueRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionWithNextRootNode<AsyncIteratorTakeArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorArgs> yieldNode;

            public AsyncIteratorTakeWithValueRootNode(JSContext context) {
                super(context);

                this.yieldNode = AsyncIteratorAwaitNode.createGeneratorYield(context);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                AsyncIteratorTakeArgs args = getArgs(frame);
                Object value = iteratorValueNode.execute(next);
                return yieldNode.execute(frame, value, args);
            }
        }

        private static JSFunctionData createTakeFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorTakeRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createTakeWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorTakeWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorDropNode extends IteratorMethodNode {

        @Child private AsyncIteratorAwaitNode<AsyncIteratorDropArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;
        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;

        public AsyncIteratorDropNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorDrop, AsyncIteratorDropNode::createDropFunctionImpl, false);
            this.toNumberNode = JSToNumberNode.create();
            this.toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
        }

        @Specialization
        public JSDynamicObject drop(Object thisObj, Object limit,
                        @Cached IsObjectNode isObjectNode,
                        @Cached InlinedBranchProfile errorBranch) {

            if (!isObjectNode.executeBoolean(thisObj)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorNotAnObject(thisObj, this);
            }

            Number numLimit = toNumberNode.executeNumber(limit);
            if (JSRuntime.isNaN(numLimit)) {
                errorBranch.enter(this);
                throw Errors.createRangeError("NaN is not allowed", this);
            }

            double integerLimit = JSRuntime.doubleValue(toIntegerOrInfinityNode.executeNumber(numLimit));
            if (integerLimit < 0) {
                errorBranch.enter(this);
                throw Errors.createRangeErrorIndexNegative(this);
            }

            IteratorRecord record = getIteratorDirect(thisObj);
            AsyncIteratorDropArgs args = new AsyncIteratorDropArgs(record, integerLimit);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(args));
        }

        protected static class AsyncIteratorDropArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public double remaining;

            public AsyncIteratorDropArgs(IteratorRecord iterated, double limit) {
                super(iterated);
                this.remaining = limit;
            }
        }

        protected static class AsyncIteratorDropRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorYieldResumptionRootNode<AsyncIteratorDropArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorDropArgs> awaitNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorDropArgs> awaitLoopNode;

            public AsyncIteratorDropRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorDropWithValue, AsyncIteratorDropNode::createDropWithValueFunctionImpl, false);
                this.awaitLoopNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorDropWithValueLoop, AsyncIteratorDropNode::createDropWithValueLoopFunctionImpl,
                                false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorDropArgs args = getArgs(frame);
                Object value = iteratorNextNode.execute(args.iterated);

                double remaining = args.remaining;
                if (remaining > 0) {
                    return awaitLoopNode.execute(frame, value, args);
                }

                return awaitNode.execute(frame, value, args);
            }
        }

        protected static class AsyncIteratorDropWithValueLoopRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionWithNextRootNode<AsyncIteratorDropArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorArgs> yieldNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorDropArgs> awaitNode;

            public AsyncIteratorDropWithValueLoopRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.yieldNode = AsyncIteratorAwaitNode.createGeneratorYield(context);
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorDropWithValueLoop, AsyncIteratorDropNode::createDropWithValueLoopFunctionImpl,
                                false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorDropArgs args = getArgs(frame);
                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                double remaining = args.remaining;
                if (remaining > 0) {
                    if (remaining != Double.POSITIVE_INFINITY) {
                        args.remaining--;
                    }
                    Object value = iteratorNextNode.execute(args.iterated);
                    return awaitNode.execute(frame, value, args);
                }
                Object nextValue = iteratorValueNode.execute(next);
                return yieldNode.execute(frame, nextValue, args);
            }
        }

        protected static class AsyncIteratorDropWithValueRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionWithNextRootNode<AsyncIteratorDropArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorArgs> yieldNode;

            public AsyncIteratorDropWithValueRootNode(JSContext context) {
                super(context);

                this.yieldNode = AsyncIteratorAwaitNode.createGeneratorYield(context);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                AsyncIteratorDropArgs args = getArgs(frame);
                Object nextValue = iteratorValueNode.execute(next);
                return yieldNode.execute(frame, nextValue, args);
            }
        }

        private static JSFunctionData createDropFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorDropRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createDropWithValueLoopFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorDropWithValueLoopRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createDropWithValueFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorDropWithValueRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorFlatMapNode extends IteratorMethodWithCallableNode {

        @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitNode;
        @Child private AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode createAsyncIteratorHelperNode;

        public AsyncIteratorFlatMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.createAsyncIteratorHelperNode = AsyncIteratorHelperPrototypeBuiltins.CreateAsyncIteratorHelperNode.create(context);
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMap, AsyncIteratorFlatMapNode::createFlatMapFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject flatMap(Object thisObj, Object mapper) {
            IteratorRecord record = getIteratorDirect(thisObj);
            return createAsyncIteratorHelperNode.execute(record, awaitNode.createFunction(new AsyncIteratorFlatMapArgs(record, mapper)));
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object mapper) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorFlatMapArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object mapper;

            protected IteratorRecord innerIterator;

            public AsyncIteratorFlatMapArgs(IteratorRecord iterated, Object mapper) {
                super(iterated);
                this.mapper = mapper;
            }
        }

        protected static class AsyncIteratorFlatMapRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorYieldResumptionRootNode<AsyncIteratorFlatMapArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitInnerNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitOuterNode;

            public AsyncIteratorFlatMapRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.awaitOuterNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapWithValue,
                                AsyncIteratorFlatMapNode::createFlatMapWithValueFunctionImpl, false);
                this.awaitInnerNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapInnerWithValue,
                                AsyncIteratorFlatMapNode::createFlatMapInnerWithValueFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFlatMapArgs args = getArgs(frame);
                IteratorRecord iterated = args.iterated;

                IteratorRecord inner = args.innerIterator;
                if (inner != null) {
                    Object value = iteratorNextNode.execute(inner);
                    return awaitInnerNode.execute(frame, value, args);
                }

                Object value = iteratorNextNode.execute(iterated);
                return awaitOuterNode.execute(frame, value, args);
            }
        }

        protected static class AsyncIteratorFlatMapWithValueRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionWithCloseRootNode<AsyncIteratorFlatMapArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorFlatMapWithValueRootNode(JSContext context) {
                super(context);

                this.callNode = JSFunctionCallNode.createCall();
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapWithResult,
                                AsyncIteratorFlatMapNode::createFlatMapWithResultFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFlatMapArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                Object value = iteratorValueNode.execute(next);
                Object mapped;
                try {
                    mapped = callNode.executeCall(JSArguments.create(Undefined.instance, args.mapper, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.counter++;
                return awaitNode.execute(frame, mapped, args);
            }
        }

        protected static class AsyncIteratorFlatMapWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionRootNode<AsyncIteratorFlatMapArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private GetIteratorFlattenableNode getIteratorFlattenableNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitInnerNode;

            public AsyncIteratorFlatMapWithResultRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.getIteratorFlattenableNode = GetIteratorFlattenableNode.create(true, true, context);
                this.awaitInnerNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMapInnerWithValue,
                                AsyncIteratorFlatMapNode::createFlatMapInnerWithValueFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFlatMapArgs args = getArgs(frame);

                Object mapped = valueNode.execute(frame);
                IteratorRecord inner = getIteratorFlattenableNode.execute(mapped);
                args.innerIterator = inner;

                Object innerNext = iteratorNextNode.execute(inner);
                return awaitInnerNode.execute(frame, innerNext, args);
            }
        }

        protected static class AsyncIteratorFlatMapInnerWithValueRootNode extends AsyncIteratorAwaitNode.AsyncIteratorGeneratorAwaitResumptionWithNextRootNode<AsyncIteratorFlatMapArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorArgs> yieldNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFlatMapArgs> awaitNode;

            public AsyncIteratorFlatMapInnerWithValueRootNode(JSContext context) {
                super(context);

                this.yieldNode = AsyncIteratorAwaitNode.createGeneratorYield(context);
                this.awaitNode = AsyncIteratorAwaitNode.createGen(context, JSContext.BuiltinFunctionKey.AsyncIteratorFlatMap, AsyncIteratorFlatMapNode::createFlatMapFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFlatMapArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    args.innerIterator = null;
                    return awaitNode.execute(frame, Undefined.instance, args);
                }

                Object value = iteratorValueNode.execute(next);
                return yieldNode.execute(frame, value, args);
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

    protected abstract static class AsyncIteratorReduceNode extends IteratorMethodWithCallableNode {
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> initialAwaitNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;

        public AsyncIteratorReduceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.iteratorNextNode = IteratorNextNode.create();
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduce, AsyncIteratorReduceNode::createReduceFunctionImpl, false);
            this.initialAwaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduceInitial, AsyncIteratorReduceNode::createReduceInitialFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(reducer)")
        public JSDynamicObject reduce(Object thisObj, Object reducer, Object[] args) {
            IteratorRecord record = getIteratorDirect(thisObj);
            Object value = iteratorNextNode.execute(record);

            if (args.length == 0) {
                return initialAwaitNode.executeThis(value, new AsyncIteratorReduceArgs(record, reducer, Undefined.instance), thisObj);
            } else {
                Object initialValue = args[0];
                return awaitNode.executeThis(value, new AsyncIteratorReduceArgs(record, reducer, initialValue), thisObj);
            }
        }

        @Specialization(guards = "!isCallable(reducer)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object reducer, @SuppressWarnings("unused") Object[] args) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorReduceArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object reducer;
            public Object accumulator;

            AsyncIteratorReduceArgs(IteratorRecord iterated, Object reducer, Object accumulator) {
                super(iterated);
                this.reducer = reducer;
                this.accumulator = accumulator;
            }
        }

        protected static class AsyncIteratorReduceRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionWithCloseRootNode<AsyncIteratorReduceArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorReduceRootNode(JSContext context) {
                super(context);

                this.callNode = JSFunctionCallNode.createCall();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduceWithResult,
                                AsyncIteratorReduceNode::createReduceWithResultFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorReduceArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return args.accumulator;
                }

                Object value = iteratorValueNode.execute(next);
                Object result;
                try {
                    result = callNode.executeCall(JSArguments.create(Undefined.instance, args.reducer, args.accumulator, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.accumulator = result;
                args.counter++;
                return awaitNode.execute(frame, result, args);
            }
        }

        protected static class AsyncIteratorReduceInitialRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionRootNode<AsyncIteratorReduceArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;

            public AsyncIteratorReduceInitialRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.iteratorValueNode = IteratorValueNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduce, AsyncIteratorReduceNode::createReduceFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorReduceArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    throw Errors.createTypeError("Reduce of empty iterator with no initial value");
                }

                Object value = iteratorValueNode.execute(next);
                Object nextNext = iteratorNextNode.execute(args.iterated);
                args.accumulator = value;
                args.counter = 1;
                return awaitNode.execute(frame, nextNext, args);
            }
        }

        protected static class AsyncIteratorReduceWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionRootNode<AsyncIteratorReduceArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorReduceArgs> awaitNode;

            public AsyncIteratorReduceWithResultRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorReduce, AsyncIteratorReduceNode::createReduceFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorReduceArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                Object next = iteratorNextNode.execute(args.iterated);
                args.accumulator = result;
                return awaitNode.execute(frame, next, args);
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

    protected abstract static class AsyncIteratorToArrayNode extends IteratorMethodNode {
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorToArrayArgs> awaitNode;

        public AsyncIteratorToArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.iteratorNextNode = IteratorNextNode.create();
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorToArray, AsyncIteratorToArrayNode::createToArrayFunctionImpl, false);
        }

        @Specialization
        public JSDynamicObject toArray(Object thisObj) {
            IteratorRecord record = getIteratorDirect(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.executeThis(value, new AsyncIteratorToArrayArgs(record, new SimpleArrayList<>()), thisObj);
        }

        protected static final class AsyncIteratorToArrayArgs extends AsyncIteratorAwaitNode.AsyncIteratorArgs {
            public final SimpleArrayList<Object> result;

            AsyncIteratorToArrayArgs(IteratorRecord iterated, SimpleArrayList<Object> result) {
                super(iterated);
                this.result = result;
            }
        }

        protected static class AsyncIteratorToArrayRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionRootNode<AsyncIteratorToArrayArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorToArrayArgs> awaitNode;
            private final JSContext context;

            public AsyncIteratorToArrayRootNode(JSContext context) {
                super(context);
                this.context = context;

                this.iteratorNextNode = IteratorNextNode.create();
                this.iteratorValueNode = IteratorValueNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorToArray, AsyncIteratorToArrayNode::createToArrayFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorToArrayArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return JSArray.createConstant(context, getRealm(), args.result.toArray());
                }

                Object value = iteratorValueNode.execute(next);
                args.result.add(value, null, InlinedBranchProfile.getUncached());

                return awaitNode.execute(frame, iteratorNextNode.execute(args.iterated), args);
            }
        }

        private static JSFunctionData createToArrayFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorToArrayRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorForEachNode extends IteratorMethodWithCallableNode {
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorForEachArgs> awaitNode;

        public AsyncIteratorForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.iteratorNextNode = IteratorNextNode.create();
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorForEach, AsyncIteratorForEachNode::createForEachFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject forEach(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirect(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.executeThis(value, new AsyncIteratorForEachArgs(record, fn), thisObj);
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorForEachArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object fn;

            AsyncIteratorForEachArgs(IteratorRecord iterated, Object fn) {
                super(iterated);
                this.fn = fn;
            }
        }

        protected static class AsyncIteratorForEachRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionWithCloseRootNode<AsyncIteratorForEachArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorForEachArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorForEachRootNode(JSContext context) {
                super(context);

                this.callNode = JSFunctionCallNode.createCall();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorForEachWithResult,
                                AsyncIteratorForEachNode::createForEachWithResultFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorForEachArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                Object value = iteratorValueNode.execute(next);
                Object result;
                try {
                    result = callNode.executeCall(JSArguments.create(Undefined.instance, args.fn, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.counter++;
                return awaitNode.execute(frame, result, args);
            }
        }

        protected static class AsyncIteratorForEachWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionRootNode<AsyncIteratorForEachArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorForEachArgs> awaitNode;

            public AsyncIteratorForEachWithResultRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorForEach, AsyncIteratorForEachNode::createForEachFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorForEachArgs args = getArgs(frame);
                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(frame, value, args);
            }
        }

        private static JSFunctionData createForEachFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorForEachRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createForEachWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorForEachWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorSomeNode extends IteratorMethodWithCallableNode {
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorSomeArgs> awaitNode;

        public AsyncIteratorSomeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.iteratorNextNode = IteratorNextNode.create();
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorSome, AsyncIteratorSomeNode::createSomeFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject some(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirect(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.executeThis(value, new AsyncIteratorSomeArgs(record, fn), thisObj);
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorSomeArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object fn;

            AsyncIteratorSomeArgs(IteratorRecord iterated, Object fn) {
                super(iterated);
                this.fn = fn;
            }
        }

        protected static class AsyncIteratorSomeRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionWithCloseRootNode<AsyncIteratorSomeArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorSomeArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorSomeRootNode(JSContext context) {
                super(context);

                callNode = JSFunctionCallNode.createCall();
                awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorSomeWithResult, AsyncIteratorSomeNode::createSomeWithResultFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorSomeArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return false;
                }

                Object value = iteratorValueNode.execute(next);
                Object result;
                try {
                    result = callNode.executeCall(JSArguments.create(Undefined.instance, args.fn, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.counter++;
                return awaitNode.execute(frame, result, args);
            }
        }

        protected static class AsyncIteratorSomeWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionRootNode<AsyncIteratorSomeArgs> {

            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorSomeArgs> awaitNode;
            @Child private JSToBooleanNode toBooleanNode;
            @Child private AsyncIteratorCloseNode asyncIteratorCloseNode;

            public AsyncIteratorSomeWithResultRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.toBooleanNode = JSToBooleanNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorSome, AsyncIteratorSomeNode::createSomeFunctionImpl, false);
                this.asyncIteratorCloseNode = AsyncIteratorCloseNode.create(context);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorSomeArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                if (toBooleanNode.executeBoolean(result)) {
                    return asyncIteratorCloseNode.execute(args.iterated.getIterator(), true);
                }

                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(frame, value, args);
            }
        }

        private static JSFunctionData createSomeFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorSomeRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createSomeWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorSomeWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorEveryNode extends IteratorMethodWithCallableNode {
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorEveryArgs> awaitNode;

        public AsyncIteratorEveryNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.iteratorNextNode = IteratorNextNode.create();
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorEvery, AsyncIteratorEveryNode::createEveryFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject every(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirect(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.executeThis(value, new AsyncIteratorEveryArgs(record, fn), thisObj);
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorEveryArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object fn;

            AsyncIteratorEveryArgs(IteratorRecord iterated, Object fn) {
                super(iterated);
                this.fn = fn;
            }
        }

        protected static class AsyncIteratorEveryRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionWithCloseRootNode<AsyncIteratorEveryArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorEveryArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorEveryRootNode(JSContext context) {
                super(context);

                this.callNode = JSFunctionCallNode.createCall();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorEveryWithResult, AsyncIteratorEveryNode::createEveryWithResultFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorEveryArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return true;
                }

                Object value = iteratorValueNode.execute(next);
                Object result;
                try {
                    result = callNode.executeCall(JSArguments.create(Undefined.instance, args.fn, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.counter++;
                return awaitNode.execute(frame, result, args);
            }
        }

        protected static class AsyncIteratorEveryWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionRootNode<AsyncIteratorEveryArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorEveryArgs> awaitNode;
            @Child private JSToBooleanNode toBooleanNode;
            @Child private AsyncIteratorCloseNode asyncIteratorCloseNode;

            public AsyncIteratorEveryWithResultRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.toBooleanNode = JSToBooleanNode.create();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorEvery, AsyncIteratorEveryNode::createEveryFunctionImpl, false);
                this.asyncIteratorCloseNode = AsyncIteratorCloseNode.create(context);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorEveryArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                if (!toBooleanNode.executeBoolean(result)) {
                    return asyncIteratorCloseNode.execute(args.iterated.getIterator(), false);
                }

                Object value = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(frame, value, args);
            }
        }

        private static JSFunctionData createEveryFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorEveryRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        private static JSFunctionData createEveryWithResultFunctionImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorEveryWithResultRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }
    }

    protected abstract static class AsyncIteratorFindNode extends IteratorMethodWithCallableNode {
        @Child private IteratorNextNode iteratorNextNode;
        @Child private AsyncIteratorAwaitNode<AsyncIteratorFindArgs> awaitNode;

        public AsyncIteratorFindNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.iteratorNextNode = IteratorNextNode.create();
            this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFind, AsyncIteratorFindNode::createFindFunctionImpl, false);
        }

        @Specialization(guards = "isCallable(fn)")
        public JSDynamicObject find(Object thisObj, Object fn) {
            IteratorRecord record = getIteratorDirect(thisObj);
            Object value = iteratorNextNode.execute(record);

            return awaitNode.executeThis(value, new AsyncIteratorFindArgs(record, fn), thisObj);
        }

        @Specialization(guards = "!isCallable(fn)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object fn) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected static final class AsyncIteratorFindArgs extends AsyncIteratorAwaitNode.AsyncIteratorWithCounterArgs {
            public final Object fn;
            public Object value;

            AsyncIteratorFindArgs(IteratorRecord iterated, Object fn) {
                super(iterated);
                this.fn = fn;
            }
        }

        protected static class AsyncIteratorFindRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionWithCloseRootNode<AsyncIteratorFindArgs> {
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFindArgs> awaitNode;
            @Child private JSFunctionCallNode callNode;

            public AsyncIteratorFindRootNode(JSContext context) {
                super(context);

                this.callNode = JSFunctionCallNode.createCall();
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFindWithResult, AsyncIteratorFindNode::createFindWithResultFunctionImpl, true);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFindArgs args = getArgs(frame);

                Object next = valueNode.execute(frame);
                checkNext(next);

                if (iteratorComplete(next)) {
                    return Undefined.instance;
                }

                Object value = iteratorValueNode.execute(next);
                Object result;
                try {
                    result = callNode.executeCall(JSArguments.create(Undefined.instance, args.fn, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException ex) {
                    return asyncIteratorCloseAbrupt(args.iterated, ex);
                }
                args.value = value;
                args.counter++;
                return awaitNode.execute(frame, result, args);
            }
        }

        protected static class AsyncIteratorFindWithResultRootNode extends AsyncIteratorAwaitNode.AsyncIteratorNonGeneratorResumptionRootNode<AsyncIteratorFindArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private AsyncIteratorAwaitNode<AsyncIteratorFindArgs> awaitNode;
            @Child private JSToBooleanNode toBooleanNode;
            @Child private AsyncIteratorCloseNode asyncIteratorCloseNode;

            public AsyncIteratorFindWithResultRootNode(JSContext context) {
                super(context);

                this.iteratorNextNode = IteratorNextNode.create();
                this.toBooleanNode = JSToBooleanNode.create();
                this.asyncIteratorCloseNode = AsyncIteratorCloseNode.create(context);
                this.awaitNode = AsyncIteratorAwaitNode.create(context, JSContext.BuiltinFunctionKey.AsyncIteratorFind, AsyncIteratorFindNode::createFindFunctionImpl, false);
            }

            @Override
            public Object executeBody(VirtualFrame frame) {
                AsyncIteratorFindArgs args = getArgs(frame);
                Object result = valueNode.execute(frame);
                Object value = args.value;
                assert value != null;
                args.value = null;
                if (toBooleanNode.executeBoolean(result)) {
                    return asyncIteratorCloseNode.execute(args.iterated.getIterator(), value);
                }

                Object next = iteratorNextNode.execute(args.iterated);
                return awaitNode.execute(frame, next, args);
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

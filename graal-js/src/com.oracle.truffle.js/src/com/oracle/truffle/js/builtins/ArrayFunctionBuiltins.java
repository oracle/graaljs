/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltinsFactory.JSArrayFromAsyncNodeGen;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltinsFactory.JSArrayFromNodeGen;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltinsFactory.JSArrayOfNodeGen;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltinsFactory.JSIsArrayNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperation;
import com.oracle.truffle.js.builtins.AsyncIteratorPrototypeBuiltins.AsyncIteratorAwaitNode;
import com.oracle.truffle.js.builtins.AsyncIteratorPrototypeBuiltins.AsyncIteratorAwaitNode.AsyncIteratorRootNode;
import com.oracle.truffle.js.nodes.access.AsyncIteratorCloseNode;
import com.oracle.truffle.js.nodes.access.CreateAsyncFromSyncIteratorNode;
import com.oracle.truffle.js.nodes.access.GetIteratorFromMethodNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.array.JSSetLengthNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.nodes.function.InternalCallNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSArray} function (constructor).
 */
public final class ArrayFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<ArrayFunctionBuiltins.ArrayFunction> {

    public static final JSBuiltinsContainer BUILTINS = new ArrayFunctionBuiltins();

    protected ArrayFunctionBuiltins() {
        super(JSArray.CLASS_NAME, ArrayFunction.class);
    }

    public enum ArrayFunction implements BuiltinEnum<ArrayFunction> {
        isArray(1),

        // ES6
        of(0),
        from(1),

        fromAsync(1);

        private final int length;

        ArrayFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case of, from -> JSConfig.ECMAScript2015;
                case fromAsync -> JSConfig.StagingECMAScriptVersion;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ArrayFunction builtinEnum) {
        switch (builtinEnum) {
            case isArray:
                return JSIsArrayNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case of:
                return JSArrayOfNodeGen.create(context, builtin, false, args().withThis().varArgs().createArgumentNodes(context));
            case from:
                return JSArrayFromNodeGen.create(context, builtin, false, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case fromAsync:
                return JSArrayFromAsyncNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSIsArrayNode extends JSBuiltinNode {
        @Child private com.oracle.truffle.js.nodes.unary.JSIsArrayNode isArrayNode = com.oracle.truffle.js.nodes.unary.JSIsArrayNode.createIsArrayLike();

        public JSIsArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isArray(Object object) {
            return isArrayNode.execute(object);
        }
    }

    public abstract static class JSArrayFunctionOperation extends JSArrayOperation {
        @Child protected IsConstructorNode isConstructor = IsConstructorNode.create();

        public JSArrayFunctionOperation(JSContext context, JSBuiltin builtin, boolean isTypedArray) {
            super(context, builtin, isTypedArray);
        }

        protected Object constructOrArray(Object thisObj, long len, boolean provideLengthArg) {
            if (isTypedArrayImplementation) {
                return getArraySpeciesConstructorNode().typedArrayCreate(thisObj, JSRuntime.longToIntOrDouble(len));
            } else {
                if (isConstructor.executeBoolean(thisObj)) {
                    if (provideLengthArg) {
                        return getArraySpeciesConstructorNode().construct(thisObj, JSRuntime.longToIntOrDouble(len));
                    } else {
                        return getArraySpeciesConstructorNode().construct(thisObj);
                    }
                } else {
                    return arrayCreate(len);
                }
            }
        }
    }

    public abstract static class JSArrayOfNode extends JSArrayFunctionOperation {

        public JSArrayOfNode(JSContext context, JSBuiltin builtin, boolean isTypedArray) {
            super(context, builtin, isTypedArray);
        }

        @Specialization
        protected Object arrayOf(Object thisObj, Object[] args) {
            int len = args.length;
            Object obj = constructOrArray(thisObj, len, true);

            int pos = 0;
            for (Object arg : args) {
                Object value = JSRuntime.nullToUndefined(arg);
                writeOwn(obj, pos, value);
                pos++;
            }
            setLength(obj, len);
            return obj;
        }
    }

    public abstract static class JSArrayFromNode extends JSArrayFunctionOperation {
        @Child private JSFunctionCallNode callMapFnNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private IteratorValueNode getIteratorValueNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private GetMethodNode getIteratorMethodNode;
        @Child private JSGetLengthNode getSourceLengthNode;
        private final ConditionProfile isIterable = ConditionProfile.create();

        public JSArrayFromNode(JSContext context, JSBuiltin builtin, boolean isTypedArray) {
            super(context, builtin, isTypedArray);
            this.getIteratorMethodNode = GetMethodNode.create(context, Symbol.SYMBOL_ITERATOR);
        }

        protected void iteratorCloseAbrupt(Object iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        protected Object getIteratorValue(Object iteratorResult) {
            if (getIteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorValueNode = insert(IteratorValueNode.create());
            }
            return getIteratorValueNode.execute(iteratorResult);
        }

        protected Object iteratorStep(IteratorRecord iteratorRecord) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create());
            }
            return iteratorStepNode.execute(iteratorRecord);
        }

        protected final Object callMapFn(Object target, Object function, Object... userArguments) {
            if (callMapFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callMapFnNode = insert(JSFunctionCallNode.createCall());
            }
            return callMapFnNode.executeCall(JSArguments.create(target, function, userArguments));
        }

        protected long getSourceLength(Object thisObject) {
            if (getSourceLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSourceLengthNode = insert(JSGetLengthNode.create(getContext()));
            }
            return getSourceLengthNode.executeLong(thisObject);
        }

        @Specialization
        protected Object arrayFrom(Object thisObj, Object items, Object mapFn, Object thisArg,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethod,
                        @Cached InlinedBranchProfile growProfile) {
            return arrayFromCommon(thisObj, items, mapFn, thisArg, true, getIteratorFromMethod, growProfile);
        }

        protected Object arrayFromCommon(Object thisObj, Object items, Object mapFn, Object thisArg, boolean setLength,
                        GetIteratorFromMethodNode getIteratorFromMethod, InlinedBranchProfile growProfile) {
            boolean mapping;
            if (mapFn == Undefined.instance) {
                mapping = false;
            } else {
                checkCallbackIsFunction(mapFn);
                mapping = true;
            }
            Object usingIterator = getIteratorMethodNode.executeWithTarget(items);
            if (isIterable.profile(usingIterator != Undefined.instance)) {
                return arrayFromIterable(thisObj, items, usingIterator, mapFn, thisArg, mapping, getIteratorFromMethod, growProfile);
            } else {
                // NOTE: source is not an Iterable so assume it is already an array-like object.
                Object itemsObject = toObject(items);
                return arrayFromArrayLike(thisObj, itemsObject, mapFn, thisArg, mapping, setLength);
            }
        }

        protected Object arrayFromIterable(Object thisObj, Object items, Object usingIterator, Object mapFn, Object thisArg, boolean mapping,
                        GetIteratorFromMethodNode getIteratorFromMethod, @SuppressWarnings("unused") InlinedBranchProfile growProfile) {
            Object obj = constructOrArray(thisObj, 0, false);

            IteratorRecord iteratorRecord = getIteratorFromMethod.execute(this, items, usingIterator);
            return arrayFromIteratorRecord(obj, iteratorRecord, mapFn, thisArg, mapping);
        }

        private Object arrayFromIteratorRecord(Object obj, IteratorRecord iteratorRecord, Object mapFn, Object thisArg, boolean mapping) {
            long k = 0;
            try {
                while (true) {
                    Object next = iteratorStep(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        setLength(obj, k);
                        return obj;
                    }
                    Object mapped = getIteratorValue(next);
                    if (mapping) {
                        mapped = callMapFn(thisArg, mapFn, mapped, JSRuntime.positiveLongToIntOrDouble(k));
                    }
                    writeOwn(obj, k, mapped);
                    k++;
                }
            } catch (AbstractTruffleException ex) {
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex; // should be executed by iteratorClose
            }
        }

        protected Object arrayFromArrayLike(Object thisObj, Object items, Object mapFn, Object thisArg, boolean mapping, boolean setLength) {
            long len = getSourceLength(items);

            Object obj = constructOrArray(thisObj, len, true);

            long k = 0;
            while (k < len) {
                Object value = read(items, k);
                Object mapped = value;
                if (mapping) {
                    mapped = callMapFn(thisArg, mapFn, mapped, JSRuntime.positiveLongToIntOrDouble(k));
                }
                writeOwn(obj, k, mapped);
                k++;
            }
            if (setLength) {
                setLength(obj, len);
            }
            return obj;
        }
    }

    @ImportStatic(Symbol.class)
    public abstract static class JSArrayFromAsyncNode extends JSArrayFunctionOperation {
        @Child private PropertySetNode setArgs;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        public JSArrayFromAsyncNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
            this.setArgs = PropertySetNode.createSetHidden(AsyncIteratorAwaitNode.ARGS_ID, context);
        }

        private JSFunctionObject createFunctionWithArgs(ArrayFromAsyncArgs args, JSFunctionData functionData) {
            JSFunctionObject function = JSFunction.create(getRealm(), functionData);
            setArgs.setValue(function, args);
            return function;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected final Object arrayFromAsync(Object thisObj, Object asyncItems, Object mapFn, Object thisArg,
                        @Bind Node node,
                        @Cached("create(getContext())") NewPromiseCapabilityNode newPromiseCapability,
                        @Cached("create(getContext(), SYMBOL_ASYNC_ITERATOR)") GetMethodNode getAsyncIteratorMethodNode,
                        @Cached("create(getContext(), SYMBOL_ITERATOR)") GetMethodNode getIteratorMethodNode,
                        @Cached("create(getContext())") JSGetLengthNode getLengthNode,
                        @Cached CreateAsyncFromSyncIteratorNode createAsyncFromSyncIterator,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode,
                        @Cached InternalCallNode internalCallNode,
                        @Cached("createCall()") JSFunctionCallNode callRejectNode,
                        @Cached InlinedConditionProfile isAsyncIterator) {
            var promiseCapability = newPromiseCapability.executeDefault();
            try {
                final boolean mapping;
                if (mapFn == Undefined.instance) {
                    mapping = false;
                } else {
                    checkCallbackIsFunction(mapFn);
                    mapping = true;
                }
                IteratorRecord asyncIteratorRecord;
                Object usingAsyncIterator = getAsyncIteratorMethodNode.executeWithTarget(asyncItems);
                if (isAsyncIterator.profile(node, usingAsyncIterator != Undefined.instance)) {
                    asyncIteratorRecord = getIteratorFromMethodNode.execute(node, asyncItems, usingAsyncIterator);
                } else {
                    Object usingSyncIterator = getIteratorMethodNode.executeWithTarget(asyncItems);
                    if (usingSyncIterator != Undefined.instance) {
                        IteratorRecord syncIteratorRecord = getIteratorFromMethodNode.execute(node, asyncItems, usingSyncIterator);
                        asyncIteratorRecord = createAsyncFromSyncIterator.execute(node, syncIteratorRecord);
                    } else {
                        asyncIteratorRecord = null;
                    }
                }

                Object result;
                JSFunctionObject closure;
                if (asyncIteratorRecord != null) {
                    result = constructOrArray(thisObj, 0, false);
                    var args = new ArrayFromAsyncIteratorArgs(promiseCapability, asyncIteratorRecord, result, mapping, mapFn, thisArg);
                    closure = createFunctionWithArgs(args, getContext().getOrCreateBuiltinFunctionData(
                                    BuiltinFunctionKey.ArrayFromAsyncIteratorResumption, ArrayFromAsyncIteratorResumptionRootNode::createFunctionImpl));
                } else {
                    /*
                     * Note: asyncItems is neither an AsyncIterable nor an Iterable so assume it is
                     * an array-like object.
                     */
                    Object arrayLike = toObject(asyncItems);
                    long len = getLengthNode.executeLong(arrayLike);
                    result = constructOrArray(thisObj, len, true);
                    var args = new ArrayFromAsyncArrayLikeArgs(promiseCapability, len, arrayLike, result, mapping, mapFn, thisArg);
                    closure = createFunctionWithArgs(args, getContext().getOrCreateBuiltinFunctionData(
                                    BuiltinFunctionKey.ArrayFromAsyncArrayLikeResumption, ArrayFromAsyncArrayLikeResumptionRootNode::createFunctionImpl));
                }

                internalCallNode.execute(JSFunction.getCallTarget(closure), JSArguments.createOneArg(Undefined.instance, closure, Undefined.instance));
            } catch (AbstractTruffleException ex) {
                Object error = getErrorObject(ex);
                callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
            }
            return promiseCapability.getPromise();
        }

        private Object getErrorObject(AbstractTruffleException ex) {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
            }
            return getErrorObjectNode.execute(ex);
        }

        abstract static sealed class ArrayFromAsyncArgs {
            /** Array.fromAsync result promise. */
            final PromiseCapabilityRecord promiseCapability;
            /** Result array or array-like. */
            final Object result;

            final boolean mapping;
            final Object mapFn;
            final Object thisArg;

            /** Resumption point. */
            int state;
            /** Current result array index. */
            long resultIndex;

            ArrayFromAsyncArgs(PromiseCapabilityRecord promiseCapability, Object result, boolean mapping, Object mapFn, Object thisArg) {
                this.promiseCapability = promiseCapability;
                this.result = result;
                this.mapping = mapping;
                this.mapFn = mapFn;
                this.thisArg = thisArg;
            }
        }

        static final class ArrayFromAsyncIteratorArgs extends ArrayFromAsyncArgs {
            final IteratorRecord iterator;

            ArrayFromAsyncIteratorArgs(PromiseCapabilityRecord promiseCapability, IteratorRecord iterator, Object result, boolean mapping, Object mapFn, Object thisArg) {
                super(promiseCapability, result, mapping, mapFn, thisArg);
                this.iterator = iterator;
            }
        }

        static final class ArrayFromAsyncArrayLikeArgs extends ArrayFromAsyncArgs {
            final long len;
            final Object arrayLike;

            ArrayFromAsyncArrayLikeArgs(PromiseCapabilityRecord promiseCapability, long len, Object arrayLike, Object result, boolean mapping, Object mapFn, Object thisArg) {
                super(promiseCapability, result, mapping, mapFn, thisArg);
                this.len = len;
                this.arrayLike = arrayLike;
            }
        }

        protected abstract static class ArrayFromAsyncResumptionRootNode<T extends ArrayFromAsyncArgs> extends AsyncIteratorAwaitNode.AsyncIteratorRootNode<T> {
            @Child private PromiseResolveNode promiseResolveNode;
            @Child private PerformPromiseThenNode performPromiseThenNode;
            @Child private PropertySetNode setArgs;
            @Child private JSSetLengthNode setLengthNode;
            @Child private WriteElementNode writeOwnElementNode;
            @Child private JSFunctionCallNode callMapFnNode;
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

            protected static final int STATE_START = 0;
            protected static final int STATE_AWAIT_NEXT_RESULT = 1;
            protected static final int STATE_AWAIT_MAPPED_VALUE = 2;

            public ArrayFromAsyncResumptionRootNode(JSContext context) {
                super(context);

                this.promiseResolveNode = PromiseResolveNode.create(context);
                this.performPromiseThenNode = PerformPromiseThenNode.create(context);
                this.setArgs = PropertySetNode.createSetHidden(AsyncIteratorAwaitNode.ARGS_ID, context);
                this.setLengthNode = JSSetLengthNode.create(context, THROW_ERROR);
                this.writeOwnElementNode = WriteElementNode.create(context, THROW_ERROR, true);
            }

            protected final JSFunctionObject createFunctionWithArgs(T args, JSFunctionData functionData) {
                JSFunctionObject function = JSFunction.create(getRealm(), functionData);
                setArgs.setValue(function, args);
                return function;
            }

            protected final Object suspendAwait(VirtualFrame frame, T args, Object promiseOrValue, int nextState, long k) {
                var promise = promiseResolveNode.executeDefault(promiseOrValue);

                // Save state and suspend built-in async function.
                assert getArgs(frame) == args;
                args.state = nextState;
                args.resultIndex = k;

                /*
                 * Once the awaited promise is fulfilled (f.) or rejected (r.), either (f.) resume
                 * at the suspended Await point, or (r.) run the abrupt completion handler closing
                 * the async iterator (if any) and rejecting the promise returned by Array.fromAsync
                 * with the Await error, respectively.
                 */
                var resumeAwait = JSFrameUtil.getFunctionObject(frame);
                var rejectAwait = createIfAbruptHandler(args);

                performPromiseThenNode.execute(promise, resumeAwait, rejectAwait);
                throw YieldException.AWAIT_NULL; // value is ignored
            }

            protected abstract JSFunctionObject createIfAbruptHandler(T args);

            protected final Object resumeAwait(VirtualFrame frame, T args, int expectedState) {
                // We have been restored at this point. Argument 0 is the awaited value.
                assert getArgs(frame) == args && args.state == expectedState;
                Object awaitedValue = JSArguments.getUserArgument(frame.getArguments(), 0);
                args.state = STATE_START;
                return awaitedValue;
            }

            protected final void setLength(Object thisObject, long length) {
                setLengthNode.execute(thisObject, indexToJS(length));
            }

            protected final void createDataPropertyOrThrow(Object result, long k, Object mappedValue) {
                writeOwnElementNode.executeWithTargetAndIndexAndValue(result, k, mappedValue);
            }

            protected final Object callMapFn(Object mapFn, Object thisArg, Object kValue, long k) {
                if (callMapFnNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    callMapFnNode = insert(JSFunctionCallNode.createCall());
                }
                return callMapFnNode.executeCall(JSArguments.create(thisArg, mapFn, kValue, indexToJS(k)));
            }

            protected final Object getErrorObject(AbstractTruffleException ex) {
                if (getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                }
                return getErrorObjectNode.execute(ex);
            }
        }

        protected static final class ArrayFromAsyncIteratorResumptionRootNode extends ArrayFromAsyncResumptionRootNode<ArrayFromAsyncIteratorArgs> {

            @Child private JSFunctionCallNode callNextMethodNode;
            @Child private IteratorValueNode iteratorValueNode;
            @Child private AsyncIteratorCloseNode asyncIteratorCloseNode;
            @Child private IsObjectNode isObjectNode;
            @Child private IteratorCompleteNode iteratorCompleteNode;

            ArrayFromAsyncIteratorResumptionRootNode(JSContext context) {
                super(context);
                this.callNextMethodNode = JSFunctionCallNode.createCall();
                this.iteratorValueNode = IteratorValueNode.create();
                this.asyncIteratorCloseNode = AsyncIteratorCloseNode.create(context);
                this.isObjectNode = IsObjectNode.create();
                this.iteratorCompleteNode = IteratorCompleteNode.create();
            }

            private Object checkIterResult(Object value) {
                if (!isObjectNode.executeBoolean(value)) {
                    throw Errors.createTypeErrorIterResultNotAnObject(value, this);
                }
                return value;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                var args = getArgs(frame);

                var promiseCapability = args.promiseCapability;
                IteratorRecord iteratorRecord = args.iterator;
                Object result = args.result;
                boolean mapping = args.mapping;
                long k = args.resultIndex;
                returnNow: try {
                    for (int state = args.state; k < JSRuntime.MAX_SAFE_INTEGER_LONG; ++k, state = STATE_START) {
                        Object mappedValue;
                        if (state < STATE_AWAIT_MAPPED_VALUE) {
                            Object nextResult;
                            if (state == STATE_START) {
                                nextResult = callNextMethodNode.executeCall(JSArguments.createZeroArg(iteratorRecord.getIterator(), iteratorRecord.getNextMethod()));
                                nextResult = suspendAwait(frame, args, nextResult, STATE_AWAIT_NEXT_RESULT, k);
                            } else {
                                nextResult = resumeAwait(frame, args, STATE_AWAIT_NEXT_RESULT);
                                checkIterResult(nextResult);
                                if (iteratorCompleteNode.execute(nextResult)) {
                                    setLength(result, k);
                                    callResolve(promiseCapability, result);
                                    break returnNow;
                                }
                            }
                            Object nextValue = iteratorValueNode.execute(nextResult);
                            if (mapping) {
                                mappedValue = callMapFn(args.mapFn, args.thisArg, nextValue, k);
                                mappedValue = suspendAwait(frame, args, mappedValue, STATE_AWAIT_MAPPED_VALUE, k);
                            } else {
                                mappedValue = nextValue;
                            }
                        } else {
                            mappedValue = resumeAwait(frame, args, STATE_AWAIT_MAPPED_VALUE);
                        }
                        createDataPropertyOrThrow(result, k, mappedValue);
                    }
                    assert k >= JSRuntime.MAX_SAFE_INTEGER_LONG : k;
                    throw Errors.createTypeErrorIndexTooLarge();
                } catch (YieldException e) {
                    assert e.isAwait() && args.state != STATE_START;
                } catch (AbstractTruffleException e) {
                    Object error = getErrorObject(e);
                    asyncIteratorCloseNode.executeAbruptReject(iteratorRecord.getIterator(), error, promiseCapability);
                }
                return promiseCapability.getPromise();
            }

            @Override
            protected JSFunctionObject createIfAbruptHandler(ArrayFromAsyncIteratorArgs args) {
                return createFunctionWithArgs(args, context.getOrCreateBuiltinFunctionData(
                                BuiltinFunctionKey.ArrayFromAsyncAwaitIfAbruptClose, ArrayFromAsyncIteratorResumptionRootNode::createIfAbruptCloseImpl));
            }

            static JSFunctionData createFunctionImpl(JSContext context) {
                return JSFunctionData.createCallOnly(context, new ArrayFromAsyncIteratorResumptionRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
            }

            static JSFunctionData createIfAbruptCloseImpl(JSContext context) {
                return JSFunctionData.createCallOnly(context, new IfAbruptCloseNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
            }

            private static class IfAbruptCloseNode extends AsyncIteratorRootNode<ArrayFromAsyncIteratorArgs> {
                @Child private AsyncIteratorCloseNode closeNode;

                IfAbruptCloseNode(JSContext context) {
                    super(context);
                    this.closeNode = AsyncIteratorCloseNode.create(context);
                }

                @Override
                public Object execute(VirtualFrame frame) {
                    var args = getArgs(frame);
                    var promiseCapability = args.promiseCapability;
                    Object error = valueNode.execute(frame);
                    closeNode.executeAbruptReject(args.iterator.getIterator(), error, promiseCapability);
                    return promiseCapability.getPromise();
                }
            }
        }

        protected static final class ArrayFromAsyncArrayLikeResumptionRootNode extends ArrayFromAsyncResumptionRootNode<ArrayFromAsyncArrayLikeArgs> {

            @Child private ReadElementNode getNode;

            ArrayFromAsyncArrayLikeResumptionRootNode(JSContext context) {
                super(context);
                this.getNode = ReadElementNode.create(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                var args = getArgs(frame);

                var promiseCapability = args.promiseCapability;
                Object result = args.result;
                boolean mapping = args.mapping;
                long len = args.len;
                long k = args.resultIndex;
                try {
                    for (int state = args.state; k < len; ++k, state = STATE_START) {
                        Object mappedValue;
                        if (state < STATE_AWAIT_MAPPED_VALUE) {
                            Object kValue;
                            if (state == STATE_START) {
                                kValue = getNode.executeWithTargetAndIndex(args.arrayLike, k);
                                kValue = suspendAwait(frame, args, kValue, STATE_AWAIT_NEXT_RESULT, k);
                            } else {
                                assert state == STATE_AWAIT_NEXT_RESULT;
                                kValue = resumeAwait(frame, args, STATE_AWAIT_NEXT_RESULT);
                            }
                            if (mapping) {
                                mappedValue = callMapFn(args.mapFn, args.thisArg, kValue, k);
                                mappedValue = suspendAwait(frame, args, mappedValue, STATE_AWAIT_MAPPED_VALUE, k);
                            } else {
                                mappedValue = kValue;
                            }
                        } else {
                            assert state == STATE_AWAIT_MAPPED_VALUE;
                            mappedValue = resumeAwait(frame, args, STATE_AWAIT_MAPPED_VALUE);
                        }
                        createDataPropertyOrThrow(result, k, mappedValue);
                    }
                    setLength(result, len);
                    callResolve(promiseCapability, result);
                } catch (YieldException e) {
                    assert e.isAwait() && args.state != STATE_START;
                } catch (AbstractTruffleException e) {
                    Object error = getErrorObject(e);
                    callReject(promiseCapability, error);
                }
                return promiseCapability.getPromise();
            }

            @Override
            protected JSFunctionObject createIfAbruptHandler(ArrayFromAsyncArrayLikeArgs args) {
                return createFunctionWithArgs(args, context.getOrCreateBuiltinFunctionData(
                                BuiltinFunctionKey.ArrayFromAsyncAwaitIfAbruptReturn, ArrayFromAsyncArrayLikeResumptionRootNode::createIfAbruptReturnImpl));
            }

            static JSFunctionData createFunctionImpl(JSContext context) {
                return JSFunctionData.createCallOnly(context, new ArrayFromAsyncArrayLikeResumptionRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
            }

            static JSFunctionData createIfAbruptReturnImpl(JSContext context) {
                return JSFunctionData.createCallOnly(context, new IfAbruptReturnNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
            }

            private static class IfAbruptReturnNode extends AsyncIteratorRootNode<ArrayFromAsyncArrayLikeArgs> {

                IfAbruptReturnNode(JSContext context) {
                    super(context);
                }

                @Override
                public Object execute(VirtualFrame frame) {
                    var args = getArgs(frame);
                    var promiseCapability = args.promiseCapability;
                    Object error = valueNode.execute(frame);
                    callReject(promiseCapability, error);
                    return promiseCapability.getPromise();
                }
            }
        }
    }
}

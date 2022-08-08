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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorDirectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorGetNextValueNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerOrInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSWrapForAsyncIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSArray}.prototype.
 */
public final class IteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorPrototypeBuiltins.IteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorPrototypeBuiltins();

    public static final HiddenKey FLATMAP_ALIVE_ID = new HiddenKey("innerAlive");
    public static final HiddenKey FLATMAP_INNER_ID = new HiddenKey("innerIterator");

    private IteratorPrototypeBuiltins() {
        super(JSArray.PROTOTYPE_NAME, IteratorPrototype.class);
    }

    public enum IteratorPrototype implements BuiltinEnum<IteratorPrototype> {
        toAsync(0),
        toArray(0),
        forEach(1),

        some(1),
        every(1),
        find(1),

        reduce(1),

        map(1),
        filter(1),
        take(1),
        drop(1),
        indexed(0),
        flatMap(1);

        private final int length;

        IteratorPrototype(int length) {
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
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case toAsync:
                return IteratorPrototypeBuiltinsFactory.IteratorToAsyncNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case toArray:
                return IteratorPrototypeBuiltinsFactory.IteratorToArrayNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case forEach:
                return IteratorPrototypeBuiltinsFactory.IteratorForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case some:
                return IteratorPrototypeBuiltinsFactory.IteratorSomeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case every:
                return IteratorPrototypeBuiltinsFactory.IteratorEveryNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case find:
                return IteratorPrototypeBuiltinsFactory.IteratorFindNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reduce:
                return IteratorPrototypeBuiltinsFactory.IteratorReduceNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case map:
                return IteratorPrototypeBuiltinsFactory.IteratorMapNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case filter:
                return IteratorPrototypeBuiltinsFactory.IteratorFilterNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case take:
                return IteratorPrototypeBuiltinsFactory.IteratorTakeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case drop:
                return IteratorPrototypeBuiltinsFactory.IteratorDropNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case indexed:
                return IteratorPrototypeBuiltinsFactory.IteratorIndexedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case flatMap:
                return IteratorPrototypeBuiltinsFactory.IteratorFlatMapNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public static class IteratorArgs {
        public final IteratorRecord target;

        public IteratorArgs(IteratorRecord target) {
            this.target = target;
        }
    }

    private abstract static class IteratorBaseNode<T extends IteratorArgs> extends JSBuiltinNode {

        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setArgsNode;
        @Child private PropertySetNode setNextNode;
        private final JSContext.BuiltinFunctionKey key;

        IteratorBaseNode(JSContext context, JSBuiltin builtin, JSContext.BuiltinFunctionKey key) {
            super(context, builtin);

            this.key = key;

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            setArgsNode = PropertySetNode.createSetHidden(IteratorHelperPrototypeBuiltins.ARGS_ID, context);
            setNextNode = PropertySetNode.createSetHidden(IteratorHelperPrototypeBuiltins.NEXT_ID, context);
        }

        protected abstract static class IteratorImplNode<T extends IteratorArgs> extends JavaScriptBaseNode {
            @Child private PropertyGetNode getArgsNode;
            @Child private HasHiddenKeyCacheNode hasArgsNode;
            @Child protected IteratorGetNextValueNode getNextValueNode;
            private final BranchProfile hasNoArgsProfile = BranchProfile.create();
            private final JSContext context;

            public IteratorImplNode(JSContext context) {
                this.context = context;

                getArgsNode = PropertyGetNode.createGetHidden(IteratorHelperPrototypeBuiltins.ARGS_ID, context);
                hasArgsNode = HasHiddenKeyCacheNode.create(IteratorHelperPrototypeBuiltins.ARGS_ID);
                getNextValueNode = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), true);
            }

            protected abstract Object execute(VirtualFrame frame, Object thisObj);

            protected T getArgs(Object thisObj) {
                if (!hasArgsNode.executeHasHiddenKey(thisObj)) {
                    hasNoArgsProfile.enter();
                    throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
                }
                //noinspection unchecked
                return (T) getArgsNode.getValue(thisObj);
            }

            public JSContext getContext() {
                return context;
            }
        }

        private static class IteratorRootNode extends JavaScriptRootNode {
            @Child IteratorImplNode<?> implNode;

            IteratorRootNode(IteratorImplNode<?> implNode) {
                this.implNode = implNode;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return implNode.execute(frame, JSFrameUtil.getThisObj(frame));
            }
        }

        protected IteratorRecord getIteratorDirect(Object thisObj) {
            return getIteratorDirectNode.execute(thisObj);
        }

        protected JSDynamicObject createIterator(T args) {
            JSDynamicObject iterator = createObjectNode.execute(JSRealm.get(this).getIteratorHelperPrototype());
            setArgsNode.setValue(iterator, args);
            setNextNode.setValue(iterator, getNextCallTarget());
            return iterator;
        }

        private CallTarget getNextCallTarget() {
            return getContext().getOrCreateBuiltinFunctionData(key,
                    c -> JSFunctionData.createCallOnly(c, new IteratorRootNode(this.getImplementation(c)).getCallTarget(), 1, Strings.EMPTY_STRING)
            ).getCallTarget();
        }

        protected abstract IteratorImplNode<T> getImplementation(JSContext context);
    }

    protected abstract static class IteratorMapNode extends IteratorBaseNode<IteratorMapNode.IteratorMapArgs> {
        protected IteratorMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorMap);
        }

        protected static class IteratorMapArgs extends IteratorArgs {
            public final Object mapper;

            public IteratorMapArgs(IteratorRecord target, Object mapper) {
                super(target);
                this.mapper = mapper;
            }
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject map(Object thisObj, Object mapper) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            return createIterator(new IteratorMapArgs(iterated, mapper));
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(Object thisObj, Object mapper) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected abstract static class IteratorMapNextNode extends IteratorImplNode<IteratorMapArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;

            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            @Child private JSFunctionCallNode callNode;

            protected IteratorMapNextNode(JSContext context) {
                super(context);

                iteratorCloseNode = IteratorCloseNode.create(context);

                createIterResultObjectNode = CreateIterResultObjectNode.create(context);

                callNode = JSFunctionCallNode.createCall();
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorMapArgs args = getArgs(thisObj);

                Object value = getNextValueNode.execute(frame, args.target);
                if (value == null) {
                    return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                }

                Object mapped;
                try {
                    mapped = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.mapper, value));
                } catch (AbstractTruffleException e) {
                    iteratorCloseNode.executeAbrupt(args.target.getIterator());
                    throw e;
                }
                return createIterResultObjectNode.execute(frame, mapped, false);
            }
        }
        @Override
        protected IteratorImplNode<IteratorMapArgs> getImplementation(JSContext context) {
            return IteratorPrototypeBuiltinsFactory.IteratorMapNodeGen.IteratorMapNextNodeGen.create(context);
        }
    }

    protected abstract static class IteratorFilterNode extends IteratorBaseNode<IteratorFilterNode.IteratorFilterArgs> {
        protected IteratorFilterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorFilter);
        }

        protected static class IteratorFilterArgs extends IteratorArgs {
            public final Object filterer;

            public IteratorFilterArgs(IteratorRecord target, Object filterer) {
                super(target);
                this.filterer = filterer;
            }
        }

        @Specialization(guards = "isCallable(filterer)")
        public JSDynamicObject filter(Object thisObj, Object filterer) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            return createIterator(new IteratorFilterArgs(iterated, filterer));
        }

        @Specialization(guards = "!isCallable(filterer)")
        public Object unsupported(Object thisObj, Object filterer) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected abstract static class IteratorFilterNextNode extends IteratorImplNode<IteratorFilterArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;
            @Child private JSFunctionCallNode callNode;
            @Child private JSToBooleanNode toBooleanNode;

            protected IteratorFilterNextNode(JSContext context) {
                super(context);

                iteratorCloseNode = IteratorCloseNode.create(context);

                createIterResultObjectNode = CreateIterResultObjectNode.create(context);

                callNode = JSFunctionCallNode.createCall();

                toBooleanNode = JSToBooleanNode.create();
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorFilterArgs args = getArgs(thisObj);

                while (true) {
                    Object value = getNextValueNode.execute(frame, args.target);
                    if (value == null) {
                        break;
                    }

                    Object selected;
                    try {
                        selected = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.filterer, value));
                    } catch (AbstractTruffleException e) {
                        iteratorCloseNode.executeAbrupt(args.target.getIterator());
                        throw e;
                    }
                    if (toBooleanNode.executeBoolean(selected)) {
                        return createIterResultObjectNode.execute(frame, value, false);
                    }
                }

                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }
        }
        @Override
        protected IteratorImplNode<IteratorFilterArgs> getImplementation(JSContext context) {
            return IteratorPrototypeBuiltinsFactory.IteratorFilterNodeGen.IteratorFilterNextNodeGen.create(context);
        }
    }

    protected abstract static class IteratorIndexedNode extends IteratorBaseNode<IteratorArgs> {
        private static final HiddenKey INDEX_ID = new HiddenKey("index");

        @Child private PropertySetNode setIndexNode;

        protected IteratorIndexedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorIndexed);

            setIndexNode = PropertySetNode.createSetHidden(INDEX_ID, context);
        }

        @Specialization
        public JSDynamicObject indexed(Object thisObj) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            JSDynamicObject result = createIterator(new IteratorArgs(iterated));
            setIndexNode.setValueInt(result, 0);
            return result;
        }

        protected abstract static class IteratorIndexedNextNode extends IteratorBaseNode.IteratorImplNode<IteratorArgs> {
            @Child private CreateIterResultObjectNode createIterResultObjectNode;
            @Child private PropertyGetNode getIndexNode;
            @Child private PropertySetNode setIndexNode;

            protected IteratorIndexedNextNode(JSContext context) {
                super(context);

                createIterResultObjectNode = CreateIterResultObjectNode.create(context);

                setIndexNode = PropertySetNode.createSetHidden(INDEX_ID, context);
                getIndexNode = PropertyGetNode.createGetHidden(INDEX_ID, context);
            }

            @Specialization(rewriteOn = RuntimeException.class)
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorArgs args = getArgs(thisObj);

                int index;
                try {
                    index = getIndexNode.getValueInt(thisObj);
                } catch (UnexpectedResultException e) {
                    throw new RuntimeException(e);
                }

                Object value = getNextValueNode.execute(frame, args.target);
                if (value == null) {
                    return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                }

                JSArrayObject pair = JSArray.createConstant(getContext(), getRealm(), new Object[]{index, value});
                setIndexNode.setValueInt(thisObj, index + 1);

                return createIterResultObjectNode.execute(frame, pair, false);
            }

            @Specialization
            public Object unreachable(Object thisObj) {
                throw Errors.createError("unreachable unexepected result");
            }
        }
        @Override
        protected IteratorBaseNode.IteratorImplNode<IteratorArgs> getImplementation(JSContext context) {
            return IteratorPrototypeBuiltinsFactory.IteratorIndexedNodeGen.IteratorIndexedNextNodeGen.create(context);
        }
    }

    protected abstract static class IteratorTakeNode extends IteratorBaseNode<IteratorTakeNode.IteratorTakeArgs> {
        private static final HiddenKey LIMIT_ID = new HiddenKey("limit");

        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;
        @Child private PropertySetNode setLimitNode;

        protected IteratorTakeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorTake);

            toNumberNode = JSToNumberNode.create();
            toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
            setLimitNode = PropertySetNode.createSetHidden(LIMIT_ID, context);
        }

        protected static class IteratorTakeArgs extends IteratorArgs {
            public final boolean finite;

            public IteratorTakeArgs(IteratorRecord target, boolean finite) {
                super(target);
                this.finite = finite;
            }
        }

        @Specialization
        public JSDynamicObject take(Object thisObj, Object limit) {
            IteratorRecord iterated = getIteratorDirect(thisObj);

            Number numLimit = toNumberNode.executeNumber(limit);
            if (Double.isNaN(numLimit.doubleValue())) {
                throw Errors.createRangeError("NAN not allowed (TODO: error message)", this);
            }

            Number integerLimit = toIntegerOrInfinityNode.executeNumber(limit);
            if (integerLimit.doubleValue() < 0) {
                throw Errors.createRangeErrorIndexNegative(this);
            }

            JSDynamicObject result = createIterator(new IteratorTakeArgs(iterated, !Double.isInfinite(integerLimit.doubleValue())));
            setLimitNode.setValue(result, Double.isInfinite(integerLimit.doubleValue()) ? Long.MAX_VALUE : integerLimit.longValue());
            return result;
        }

        protected abstract static class IteratorTakeNextNode extends IteratorBaseNode.IteratorImplNode<IteratorTakeArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;

            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            @Child private PropertyGetNode getLimitNode;
            @Child private PropertySetNode setLimitNode;

            private final ConditionProfile finiteProfile = ConditionProfile.createBinaryProfile();


            protected IteratorTakeNextNode(JSContext context) {
                super(context);

                iteratorCloseNode = IteratorCloseNode.create(context);

                createIterResultObjectNode = CreateIterResultObjectNode.create(context);

                getLimitNode = PropertyGetNode.createGetHidden(LIMIT_ID, context);
                setLimitNode = PropertySetNode.createSetHidden(LIMIT_ID, context);
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorTakeArgs args = getArgs(thisObj);

                if (finiteProfile.profile(args.finite)) {
                    long remaining;
                    try {
                        remaining = getLimitNode.getValueLong(thisObj);
                    } catch (UnexpectedResultException e) {
                        assert false : "Unreachable";
                        throw new RuntimeException(e); //Unreachable
                    }

                    if (remaining == 0) {
                        iteratorCloseNode.executeVoid(args.target.getIterator());
                        return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                    }

                    setLimitNode.setValue(thisObj, remaining - 1);
                }

                Object value = getNextValueNode.execute(frame, args.target);
                if (value == null) {
                    return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                }
                return createIterResultObjectNode.execute(frame, value, false);
            }
        }
        @Override
        protected IteratorBaseNode.IteratorImplNode<IteratorTakeArgs> getImplementation(JSContext context) {
            return IteratorPrototypeBuiltinsFactory.IteratorTakeNodeGen.IteratorTakeNextNodeGen.create(context);
        }
    }

    protected abstract static class IteratorDropNode extends IteratorBaseNode<IteratorDropNode.IteratorDropArgs> {
        private static final HiddenKey LIMIT_ID = new HiddenKey("limit");

        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;
        @Child private PropertySetNode setLimitNode;

        protected IteratorDropNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorDrop);

            toNumberNode = JSToNumberNode.create();
            toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
            setLimitNode = PropertySetNode.createSetHidden(LIMIT_ID, context);
        }

        protected static class IteratorDropArgs extends IteratorArgs {
            public final boolean finite;

            public IteratorDropArgs(IteratorRecord target, boolean finite) {
                super(target);
                this.finite = finite;
            }
        }

        @Specialization
        public JSDynamicObject drop(Object thisObj, Object limit) {
            IteratorRecord iterated = getIteratorDirect(thisObj);

            Number numLimit = toNumberNode.executeNumber(limit);
            if (Double.isNaN(numLimit.doubleValue())) {
                throw Errors.createRangeError("NAN not allowed (TODO: error message)", this);
            }

            Number integerLimit = toIntegerOrInfinityNode.executeNumber(limit);
            if (integerLimit.doubleValue() < 0) {
                throw Errors.createRangeErrorIndexNegative(this);
            }

            JSDynamicObject result = createIterator(new IteratorDropArgs(iterated, !Double.isInfinite(integerLimit.doubleValue())));
            setLimitNode.setValue(result, Double.isInfinite(integerLimit.doubleValue()) ? Long.MAX_VALUE : integerLimit.longValue());
            return result;
        }

        protected abstract static class IteratorDropNextNode extends IteratorBaseNode.IteratorImplNode<IteratorDropArgs> {
            @Child private IteratorNextNode iteratorNextNode;
            @Child private IteratorCompleteNode iteratorCompleteNode;

            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            @Child private PropertyGetNode getLimitNode;
            @Child private PropertySetNode setLimitNode;

            private final ConditionProfile finiteProfile = ConditionProfile.createBinaryProfile();


            protected IteratorDropNextNode(JSContext context) {
                super(context);

                iteratorNextNode = IteratorNextNode.create();
                iteratorCompleteNode = IteratorCompleteNode.create(context);

                createIterResultObjectNode = CreateIterResultObjectNode.create(context);

                getLimitNode = PropertyGetNode.createGetHidden(LIMIT_ID, context);
                setLimitNode = PropertySetNode.createSetHidden(LIMIT_ID, context);
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorDropArgs args = getArgs(thisObj);

                if (finiteProfile.profile(args.finite)) {
                    long remaining;
                    try {
                        remaining = getLimitNode.getValueLong(thisObj);
                    } catch (UnexpectedResultException e) {
                        assert false : "Unreachable";
                        throw new RuntimeException(e); //Unreachable
                    }

                    while (remaining > 0) {
                        Object value = getNextValueNode.execute(frame, args.target);
                        if (value == null) {
                            setLimitNode.setValue(thisObj, remaining);
                            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                        }

                        remaining--;
                    }

                    setLimitNode.setValue(thisObj, 0L);

                    Object value = getNextValueNode.execute(frame, args.target);
                    if (value == null) {
                        return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                    }
                    return createIterResultObjectNode.execute(frame, value, false);
                } else {
                    boolean done;
                    do {
                        Object next = iteratorNextNode.execute(args.target);
                        done = iteratorCompleteNode.execute(next);
                    } while (!done);
                    return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                }
            }
        }

        @Override
        protected IteratorBaseNode.IteratorImplNode<IteratorDropArgs> getImplementation(JSContext context) {
            return IteratorPrototypeBuiltinsFactory.IteratorDropNodeGen.IteratorDropNextNodeGen.create(context);
        }
    }

    protected abstract static class IteratorFlatMapNode extends IteratorBaseNode<IteratorFlatMapNode.IteratorFlatMapArgs> {
        @Child private PropertySetNode setAliveNode;

        protected IteratorFlatMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorFlatMap);

            setAliveNode = PropertySetNode.createSetHidden(FLATMAP_ALIVE_ID, context);
        }

        protected static class IteratorFlatMapArgs extends IteratorArgs {
            public final Object mapper;

            public IteratorFlatMapArgs(IteratorRecord target, Object mapper) {
                super(target);
                this.mapper = mapper;
            }
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject flatMap(Object thisObj, Object mapper) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            JSDynamicObject result = createIterator(new IteratorFlatMapArgs(iterated, mapper));
            setAliveNode.setValueBoolean(result, false);
            return result;
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(Object thisObj, Object mapper) {
            throw Errors.createTypeErrorCallableExpected();
        }

        protected abstract static class IteratorFlatMapNextNode extends IteratorBaseNode.IteratorImplNode<IteratorFlatMapArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;

            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            @Child private JSFunctionCallNode callNode;
            @Child private GetIteratorNode getIteratorNode;

            @Child private PropertyGetNode getAliveNode;
            @Child private PropertySetNode setAliveNode;

            @Child private PropertyGetNode getInnerNode;
            @Child private PropertySetNode setInnerNode;

            protected IteratorFlatMapNextNode(JSContext context) {
                super(context);

                iteratorCloseNode = IteratorCloseNode.create(context);

                createIterResultObjectNode = CreateIterResultObjectNode.create(context);

                callNode = JSFunctionCallNode.createCall();
                getIteratorNode = GetIteratorNode.create(context);

                setAliveNode = PropertySetNode.createSetHidden(FLATMAP_ALIVE_ID, context);
                getAliveNode = PropertyGetNode.createGetHidden(FLATMAP_ALIVE_ID, context);

                setInnerNode = PropertySetNode.createSetHidden(FLATMAP_INNER_ID, context);
                getInnerNode = PropertyGetNode.createGetHidden(FLATMAP_INNER_ID, context);
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorFlatMapArgs args = getArgs(thisObj);

                boolean innerAlive;
                try {
                    innerAlive = getAliveNode.getValueBoolean(thisObj);
                } catch (UnexpectedResultException e) {
                    assert false : "Unreachable";
                    throw new RuntimeException(e); //Unreachable
                }

                while (true) {
                    if (innerAlive) {
                        IteratorRecord iterated = (IteratorRecord) getInnerNode.getValue(thisObj);

                        Object value;
                        try {
                            value = getNextValueNode.execute(frame, iterated);
                        } catch (AbstractTruffleException e) {
                            iteratorCloseNode.executeAbrupt(args.target.getIterator());
                            throw e;
                        }
                        if (value == null) {
                            innerAlive = false;
                            continue;
                        }
                        return createIterResultObjectNode.execute(frame, value, false);
                    } else {
                        Object value = getNextValueNode.execute(frame, args.target);
                        if (value == null) {
                            setAliveNode.setValueBoolean(thisObj, false);
                            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                        }

                        IteratorRecord innerIterator;
                        try {
                            Object mapped = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.mapper, value));
                            innerIterator = getIteratorNode.execute(mapped);
                        } catch (AbstractTruffleException e) {
                            iteratorCloseNode.executeAbrupt(args.target.getIterator());
                            throw e;
                        }
                        setInnerNode.setValue(thisObj, innerIterator);
                        innerAlive = true;
                        setAliveNode.setValueBoolean(thisObj, true);
                    }
                }
            }
        }
        @Override
        protected IteratorBaseNode.IteratorImplNode<IteratorFlatMapArgs> getImplementation(JSContext context) {
            return IteratorPrototypeBuiltinsFactory.IteratorFlatMapNodeGen.IteratorFlatMapNextNodeGen.create(context);
        }
    }

    protected abstract static class IteratorWithCallableNode extends JSBuiltinNode {
        @Child protected IsCallableNode isCallableNode;
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorGetNextValueNode getNextValueNode;
        @Child private JSFunctionCallNode callNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        protected static final Object CONTINUE = new Object();

        protected IteratorWithCallableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            isCallableNode = IsCallableNode.create();
            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            getNextValueNode = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), true);
            callNode = JSFunctionCallNode.createCall();
        }

        protected void prepare() {

        }

        protected Object end() {
            return Undefined.instance;
        }

        protected Object step(IteratorRecord iterated, Object fn, Object value) {
            return CONTINUE;
        }

        protected Object callMapper(IteratorRecord iterated, Object fn, Object value) {
            try {
                return callNode.executeCall(JSArguments.createOneArg(Undefined.instance, fn, value));
            } catch (AbstractTruffleException ex) {
                if (iteratorCloseNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
                }
                iteratorCloseNode.executeAbrupt(iterated.getIterator());
                throw ex; // should be executed by iteratorClose
            }
        }

        @Specialization(guards = "isCallableNode.executeBoolean(fn)")
        protected Object compatible(VirtualFrame frame, Object thisObj, Object fn) {
            IteratorRecord iterated = getIteratorDirectNode.execute(thisObj);
            prepare();
            while (true) {
                Object value = getNextValueNode.execute(frame, iterated);
                if (value == null) {
                    return end();
                }

                Object result = step(iterated, fn, value);
                if (result != CONTINUE) {
                    if (iteratorCloseNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
                    }
                    return iteratorCloseNode.execute(iterated.getIterator(), result);
                }
            }
        }

        @Specialization(guards = "!isCallableNode.executeBoolean(fn)")
        protected void incompatible(Object thisObj, Object fn) {
            getIteratorDirectNode.execute(thisObj);
            throw Errors.createTypeErrorNotAFunction(fn);
        }
    }

    protected abstract static class IteratorToArrayNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private com.oracle.truffle.js.nodes.access.IteratorToArrayNode toArrayNode;

        protected IteratorToArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            toArrayNode = com.oracle.truffle.js.nodes.access.IteratorToArrayNode.create(context, null);
        }

        @Specialization
        protected Object toArray(VirtualFrame frame, Object thisObj) {
            IteratorRecord iterated = getIteratorDirectNode.execute(thisObj);
            return toArrayNode.execute(frame, iterated);
        }
    }

    public abstract static class IteratorToAsyncNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;

        protected IteratorToAsyncNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
        }

        @Specialization
        protected JSDynamicObject toAsync(Object thisObj) {
            IteratorRecord iterated = getIteratorDirectNode.execute(thisObj);
            return JSWrapForAsyncIterator.create(getContext(), getRealm(), iterated);
        }
    }

    public abstract static class IteratorForEachNode extends IteratorWithCallableNode {
        protected IteratorForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value) {
            callMapper(iterated, fn, value);
            return CONTINUE;
        }
    }

    public abstract static class IteratorSomeNode extends IteratorWithCallableNode {
        @Child private JSToBooleanNode toBooleanNode;

        protected IteratorSomeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            toBooleanNode = JSToBooleanNode.create();
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value) {
            if (toBooleanNode.executeBoolean(callMapper(iterated, fn, value))) {
                return true;
            }
            return CONTINUE;
        }

        @Override
        protected Object end() {
            return false;
        }
    }

    public abstract static class IteratorEveryNode extends IteratorWithCallableNode {
        @Child private JSToBooleanNode toBooleanNode;

        protected IteratorEveryNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            toBooleanNode = JSToBooleanNode.create();
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value) {
            if (!toBooleanNode.executeBoolean(callMapper(iterated, fn, value))) {
                return false;
            }
            return CONTINUE;
        }

        @Override
        protected Object end() {
            return true;
        }
    }

    public abstract static class IteratorFindNode extends IteratorWithCallableNode {
        @Child private JSToBooleanNode toBooleanNode;

        protected IteratorFindNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            toBooleanNode = JSToBooleanNode.create();
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value) {
            if (toBooleanNode.executeBoolean(callMapper(iterated, fn, value))) {
                return value;
            }
            return CONTINUE;
        }
    }

    public abstract static class IteratorReduceNode extends JSBuiltinNode {
        @Child protected IsCallableNode isCallableNode;
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private JSFunctionCallNode callNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        protected IteratorReduceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = GetIteratorDirectNode.create(context);
            isCallableNode = IsCallableNode.create();
            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
            callNode = JSFunctionCallNode.createCall();
        }

        private Object callReducer(IteratorRecord iterated, Object reducer, Object accumulator, Object value) {
            try {
                return callNode.executeCall(JSArguments.create(Undefined.instance, reducer, accumulator, value));
            } catch (AbstractTruffleException ex) {
                if (iteratorCloseNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
                }
                iteratorCloseNode.executeAbrupt(iterated.getIterator());
                throw ex; // should be executed by iteratorClose
            }
        }

        @Specialization(guards = "isCallableNode.executeBoolean(reducer)")
        protected Object reduce(Object thisObj, Object reducer, Object[] args) {
            IteratorRecord iterated = getIteratorDirectNode.execute(thisObj);

            Object initialValue = JSRuntime.getArgOrUndefined(args, 0);

            Object accumulator;
            if (initialValue == Undefined.instance) {
                Object next = iteratorStepNode.execute(iterated);
                if (next == (Boolean) false) {
                    throw Errors.createTypeError("Reduce of empty iterator with no initial value");
                }

                accumulator = iteratorValueNode.execute(next);
            } else {
                accumulator = initialValue;
            }

            while (true) {
                Object next = iteratorStepNode.execute(iterated);
                if (next == (Boolean) false) {
                    return accumulator;
                }
                Object value = iteratorValueNode.execute(next);
                accumulator = callReducer(iterated, reducer, accumulator, value);
            }
        }

        @Specialization(guards = "!isCallableNode.executeBoolean(reducer)")
        protected void incompatible(Object thisObj, Object reducer, Object[] args) {
            getIteratorDirectNode.execute(thisObj);
            throw Errors.createTypeErrorNotAFunction(reducer);
        }
    }
}

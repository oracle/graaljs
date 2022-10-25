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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorDirectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorFlattenableNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
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
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.SuppressFBWarnings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidAsyncIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSIterator}.prototype.
 */
public final class IteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorPrototypeBuiltins.IteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorPrototypeBuiltins();

    private IteratorPrototypeBuiltins() {
        super(JSIterator.PROTOTYPE_NAME, IteratorPrototype.class);
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
        flatMap(1);

        private final int length;

        IteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
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
            case flatMap:
                return IteratorPrototypeBuiltinsFactory.IteratorFlatMapNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public static class IteratorArgs {
        public final IteratorRecord iterated;

        public IteratorArgs(IteratorRecord iterated) {
            this.iterated = iterated;
        }
    }

    protected static class IteratorWithCounterArgs extends IteratorArgs {
        public long counter;

        protected IteratorWithCounterArgs(IteratorRecord iterated) {
            super(iterated);
        }
    }

    protected abstract static class IteratorMethodNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;

        protected IteratorMethodNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorDirectNode = GetIteratorDirectNode.create(context);
        }

        protected final IteratorRecord getIteratorDirect(Object thisObj) {
            return getIteratorDirectNode.execute(thisObj);
        }
    }

    protected abstract static class IteratorMethodWithCallableNode extends IteratorMethodNode {

        @Child private IsCallableNode isCallableNode;

        protected IteratorMethodWithCallableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isCallableNode = IsCallableNode.create();
        }

        public final boolean isCallable(Object fn) {
            return isCallableNode.executeBoolean(fn);
        }

        protected final void requireCallable(Object fn) {
            if (!isCallableNode.executeBoolean(fn)) {
                throw Errors.createTypeErrorNotAFunction(fn);
            }
        }
    }

    private abstract static class IteratorFromGeneratorNode<T extends IteratorArgs> extends IteratorMethodWithCallableNode {
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setArgsNode;
        @Child private PropertySetNode setNextNode;
        @Child private PropertySetNode setGeneratorStateNode;

        private final BuiltinFunctionKey nextKey;
        private final Function<JSContext, JSFunctionData> nextFactory;

        IteratorFromGeneratorNode(JSContext context, JSBuiltin builtin, BuiltinFunctionKey nextKey, Function<JSContext, JSFunctionData> nextFactory) {
            super(context, builtin);

            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.setArgsNode = PropertySetNode.createSetHidden(IteratorHelperPrototypeBuiltins.ARGS_ID, context);
            this.setNextNode = PropertySetNode.createSetHidden(IteratorHelperPrototypeBuiltins.NEXT_ID, context);
            this.setGeneratorStateNode = PropertySetNode.createSetHidden(JSFunction.GENERATOR_STATE_ID, context);
            this.nextKey = nextKey;
            this.nextFactory = nextFactory;
        }

        protected abstract static class IteratorFromGeneratorImplNode<T extends IteratorArgs> extends JavaScriptBaseNode {
            @Child private PropertyGetNode getArgsNode;
            @Child private HasHiddenKeyCacheNode hasArgsNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            @Child private IteratorStepNode iteratorStepNode;
            @Child private IteratorValueNode iteratorValueNode;
            @Child private PropertySetNode setGeneratorStateNode;

            private final BranchProfile hasNoArgsProfile = BranchProfile.create();
            private final BranchProfile doubleIndexBranch = BranchProfile.create();
            protected final JSContext context;

            public IteratorFromGeneratorImplNode(JSContext context) {
                this.context = context;

                this.setGeneratorStateNode = PropertySetNode.createSetHidden(JSFunction.GENERATOR_STATE_ID, context);
                this.getArgsNode = PropertyGetNode.createGetHidden(IteratorHelperPrototypeBuiltins.ARGS_ID, context);
                this.hasArgsNode = HasHiddenKeyCacheNode.create(IteratorHelperPrototypeBuiltins.ARGS_ID);
                this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
                this.iteratorValueNode = IteratorValueNode.create();
                this.iteratorStepNode = IteratorStepNode.create();
            }

            protected abstract Object execute(VirtualFrame frame, Object thisObj);

            protected final Object iteratorStep(IteratorRecord iterated) {
                return iteratorStepNode.execute(iterated);
            }

            protected final Object iteratorValue(Object next) {
                return iteratorValueNode.execute(next);
            }

            protected final Object getNextValue(VirtualFrame frame, Object thisObj, IteratorRecord iterated) {
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    return createResultDone(frame, thisObj);
                }
                return createResultContinue(frame, thisObj, iteratorValue(next));
            }

            protected final Object createResultContinue(VirtualFrame frame, Object thisObj, Object value) {
                setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.SuspendedYield);
                return createIterResultObjectNode.execute(frame, value, false);
            }

            protected final Object createResultDone(VirtualFrame frame, Object thisObj) {
                setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Completed);
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            @SuppressWarnings("unchecked")
            protected final T getArgs(Object thisObj) {
                if (!hasArgsNode.executeHasHiddenKey(thisObj)) {
                    hasNoArgsProfile.enter();
                    throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
                }
                return (T) getArgsNode.getValue(thisObj);
            }

            public JSContext getContext() {
                return context;
            }

            public abstract IteratorFromGeneratorImplNode<T> copyUninitialized();

            protected final Object indexToJS(long index) {
                return JSRuntime.longToIntOrDouble(index, doubleIndexBranch);
            }
        }

        private static class IteratorRootNode extends JavaScriptRootNode {
            @Child private IteratorFromGeneratorImplNode<?> implNode;

            IteratorRootNode(IteratorFromGeneratorImplNode<?> implNode) {
                this.implNode = implNode;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return implNode.execute(frame, JSFrameUtil.getThisObj(frame));
            }

            public static IteratorRootNode create(IteratorFromGeneratorImplNode<?> implNode) {
                return new IteratorRootNode(implNode);
            }

            @Override
            public boolean isCloningAllowed() {
                return true;
            }

            @Override
            protected boolean isCloneUninitializedSupported() {
                return true;
            }

            @Override
            protected RootNode cloneUninitialized() {
                return create(implNode.copyUninitialized());
            }

            @Override
            public String toString() {
                return implNode.toString();
            }
        }

        protected final JSDynamicObject createIterator(@SuppressWarnings("unused") Object thisObj, T args) {
            JSDynamicObject iterator = createObjectNode.execute(getIteratorHelperPrototype());
            setArgsNode.setValue(iterator, args);
            setNextNode.setValue(iterator, JSFunction.create(getRealm(), getContext().getOrCreateBuiltinFunctionData(nextKey, nextFactory)));
            setGeneratorStateNode.setValue(iterator, JSFunction.GeneratorState.SuspendedStart);
            return iterator;
        }

        protected static JSFunctionData createIteratorFromGeneratorFunctionImpl(JSContext context, IteratorFromGeneratorImplNode<?> implNode) {
            return JSFunctionData.createCallOnly(context, IteratorRootNode.create(implNode).getCallTarget(), 0, Strings.EMPTY);
        }

        private JSDynamicObject getIteratorHelperPrototype() {
            return getRealm().getIteratorHelperPrototype();
        }
    }

    protected abstract static class IteratorMapNode extends IteratorFromGeneratorNode<IteratorMapNode.IteratorMapArgs> {
        protected IteratorMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, BuiltinFunctionKey.IteratorMap, c -> createIteratorFromGeneratorFunctionImpl(c, IteratorMapNextNode.create(c)));
        }

        protected static class IteratorMapArgs extends IteratorWithCounterArgs {
            public final Object mapper;

            public IteratorMapArgs(IteratorRecord target, Object mapper) {
                super(target);
                this.mapper = mapper;
            }
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject map(Object thisObj, Object mapper) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            return createIterator(thisObj, new IteratorMapArgs(iterated, mapper));
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object mapper) {
            getIteratorDirect(thisObj);
            throw Errors.createTypeErrorCallableExpected();
        }

        protected abstract static class IteratorMapNextNode extends IteratorFromGeneratorImplNode<IteratorMapArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;
            @Child private JSFunctionCallNode callNode;

            protected IteratorMapNextNode(JSContext context) {
                super(context);

                this.iteratorCloseNode = IteratorCloseNode.create(context);
                this.callNode = JSFunctionCallNode.createCall();
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorMapArgs args = getArgs(thisObj);
                Object next = iteratorStep(args.iterated);
                if (next == Boolean.FALSE) {
                    return createResultDone(frame, thisObj);
                }

                Object value = iteratorValue(next);
                Object mapped;
                try {
                    mapped = callNode.executeCall(JSArguments.create(Undefined.instance, args.mapper, value, indexToJS(args.counter)));
                } catch (AbstractTruffleException e) {
                    iteratorCloseNode.executeAbrupt(args.iterated.getIterator());
                    throw e;
                }
                args.counter++;
                return createResultContinue(frame, thisObj, mapped);
            }

            @Override
            public IteratorFromGeneratorImplNode<IteratorMapArgs> copyUninitialized() {
                return create(context);
            }

            public static IteratorMapNextNode create(JSContext context) {
                return IteratorPrototypeBuiltinsFactory.IteratorMapNodeGen.IteratorMapNextNodeGen.create(context);
            }
        }

    }

    protected abstract static class IteratorFilterNode extends IteratorFromGeneratorNode<IteratorFilterNode.IteratorFilterArgs> {
        protected IteratorFilterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, BuiltinFunctionKey.IteratorFilter, c -> createIteratorFromGeneratorFunctionImpl(c, IteratorFilterNextNode.create(c)));
        }

        protected static class IteratorFilterArgs extends IteratorWithCounterArgs {
            public final Object filterer;

            public IteratorFilterArgs(IteratorRecord target, Object filterer) {
                super(target);
                this.filterer = filterer;
            }
        }

        @Specialization(guards = "isCallable(filterer)")
        public JSDynamicObject filter(Object thisObj, Object filterer) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            return createIterator(thisObj, new IteratorFilterArgs(iterated, filterer));
        }

        @Specialization(guards = "!isCallable(filterer)")
        public Object unsupported(Object thisObj, @SuppressWarnings("unused") Object filterer) {
            getIteratorDirect(thisObj);
            throw Errors.createTypeErrorCallableExpected();
        }

        protected abstract static class IteratorFilterNextNode extends IteratorFromGeneratorImplNode<IteratorFilterArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;
            @Child private JSFunctionCallNode callNode;
            @Child private JSToBooleanNode toBooleanNode;

            protected IteratorFilterNextNode(JSContext context) {
                super(context);

                this.iteratorCloseNode = IteratorCloseNode.create(context);
                this.callNode = JSFunctionCallNode.createCall();
                this.toBooleanNode = JSToBooleanNode.create();
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorFilterArgs args = getArgs(thisObj);
                while (true) {
                    Object next = iteratorStep(args.iterated);
                    if (next == Boolean.FALSE) {
                        return createResultDone(frame, thisObj);
                    }

                    Object value = iteratorValue(next);

                    Object selected;
                    try {
                        selected = callNode.executeCall(JSArguments.create(Undefined.instance, args.filterer, value, indexToJS(args.counter)));
                    } catch (AbstractTruffleException e) {
                        iteratorCloseNode.executeAbrupt(args.iterated.getIterator());
                        throw e;
                    }
                    args.counter++;
                    if (toBooleanNode.executeBoolean(selected)) {
                        return createResultContinue(frame, thisObj, value);
                    }
                }
            }

            @Override
            public IteratorFromGeneratorImplNode<IteratorFilterArgs> copyUninitialized() {
                return create(context);
            }

            public static IteratorFilterNextNode create(JSContext context) {
                return IteratorPrototypeBuiltinsFactory.IteratorFilterNodeGen.IteratorFilterNextNodeGen.create(context);
            }
        }

    }

    protected abstract static class IteratorTakeNode extends IteratorFromGeneratorNode<IteratorTakeNode.IteratorTakeArgs> {

        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;

        private BranchProfile errorProfile = BranchProfile.create();

        protected IteratorTakeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, BuiltinFunctionKey.IteratorTake, c -> createIteratorFromGeneratorFunctionImpl(c, IteratorTakeNextNode.create(c)));

            this.toNumberNode = JSToNumberNode.create();
            this.toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
        }

        protected static class IteratorTakeArgs extends IteratorArgs {
            public double remaining;

            public IteratorTakeArgs(IteratorRecord target, double limit) {
                super(target);
                this.remaining = limit;
            }
        }

        @Specialization
        public JSDynamicObject take(Object thisObj, Object limit) {
            IteratorRecord iterated = getIteratorDirect(thisObj);

            Number numLimit = toNumberNode.executeNumber(limit);
            if (Double.isNaN(numLimit.doubleValue())) {
                errorProfile.enter();
                throw Errors.createRangeError("NaN is not allowed", this);
            }

            double integerLimit = toIntegerOrInfinityNode.executeNumber(limit).doubleValue();
            if (integerLimit < 0) {
                errorProfile.enter();
                throw Errors.createRangeErrorIndexNegative(this);
            }

            return createIterator(thisObj, new IteratorTakeArgs(iterated, integerLimit));
        }

        protected abstract static class IteratorTakeNextNode extends IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<IteratorTakeArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;

            private final ConditionProfile finiteProfile = ConditionProfile.createBinaryProfile();

            protected IteratorTakeNextNode(JSContext context) {
                super(context);

                this.iteratorCloseNode = IteratorCloseNode.create(context);
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorTakeArgs args = getArgs(thisObj);
                double remaining = args.remaining;

                if (remaining == 0) {
                    iteratorCloseNode.executeVoid(args.iterated.getIterator());
                    return createResultDone(frame, thisObj);
                } else if (finiteProfile.profile(!Double.isInfinite(remaining))) {
                    args.remaining = remaining - 1;
                }

                return getNextValue(frame, thisObj, args.iterated);
            }

            @Override
            public IteratorFromGeneratorImplNode<IteratorTakeArgs> copyUninitialized() {
                return create(context);
            }

            public static IteratorTakeNextNode create(JSContext context) {
                return IteratorPrototypeBuiltinsFactory.IteratorTakeNodeGen.IteratorTakeNextNodeGen.create(context);
            }
        }

    }

    protected abstract static class IteratorDropNode extends IteratorFromGeneratorNode<IteratorDropNode.IteratorDropArgs> {

        @Child private JSToNumberNode toNumberNode;
        @Child private JSToIntegerOrInfinityNode toIntegerOrInfinityNode;

        private final BranchProfile errorProfile = BranchProfile.create();

        protected IteratorDropNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, BuiltinFunctionKey.IteratorDrop, c -> createIteratorFromGeneratorFunctionImpl(c, IteratorDropNextNode.create(c)));

            this.toNumberNode = JSToNumberNode.create();
            this.toIntegerOrInfinityNode = JSToIntegerOrInfinityNode.create();
        }

        protected static class IteratorDropArgs extends IteratorArgs {
            public double remaining;

            public IteratorDropArgs(IteratorRecord target, double limit) {
                super(target);
                this.remaining = limit;
            }
        }

        @Specialization
        public JSDynamicObject drop(Object thisObj, Object limit) {
            IteratorRecord iterated = getIteratorDirect(thisObj);

            Number numLimit = toNumberNode.executeNumber(limit);
            if (Double.isNaN(numLimit.doubleValue())) {
                errorProfile.enter();
                throw Errors.createRangeError("NaN is not allowed", this);
            }

            double integerLimit = toIntegerOrInfinityNode.executeNumber(limit).doubleValue();
            if (integerLimit < 0) {
                errorProfile.enter();
                throw Errors.createRangeErrorIndexNegative(this);
            }

            return createIterator(thisObj, new IteratorDropArgs(iterated, integerLimit));
        }

        protected abstract static class IteratorDropNextNode extends IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<IteratorDropArgs> {

            private final ConditionProfile finiteProfile = ConditionProfile.createBinaryProfile();

            protected IteratorDropNextNode(JSContext context) {
                super(context);
            }

            @SuppressFBWarnings(value = "FL_FLOATS_AS_LOOP_COUNTERS", justification = "intentional use of floating-point variable as loop counter")
            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorDropArgs args = getArgs(thisObj);
                double remaining = args.remaining;
                while (remaining > 0) {
                    if (finiteProfile.profile(!Double.isInfinite(remaining))) {
                        args.remaining = remaining -= 1;
                    }
                    Object next = iteratorStep(args.iterated);
                    if (next == Boolean.FALSE) {
                        return createResultDone(frame, thisObj);
                    }
                }

                return getNextValue(frame, thisObj, args.iterated);
            }

            @Override
            public IteratorFromGeneratorImplNode<IteratorDropArgs> copyUninitialized() {
                return create(context);
            }

            public static IteratorDropNextNode create(JSContext context) {
                return IteratorPrototypeBuiltinsFactory.IteratorDropNodeGen.IteratorDropNextNodeGen.create(context);
            }
        }

    }

    protected abstract static class IteratorFlatMapNode extends IteratorFromGeneratorNode<IteratorFlatMapNode.IteratorFlatMapArgs> {

        protected IteratorFlatMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, BuiltinFunctionKey.IteratorFlatMap, c -> createIteratorFromGeneratorFunctionImpl(c, IteratorFlatMapNextNode.create(c)));
        }

        protected static class IteratorFlatMapArgs extends IteratorWithCounterArgs {
            public final Object mapper;
            public boolean innerAlive;
            public IteratorRecord innerIterator;

            public IteratorFlatMapArgs(IteratorRecord target, Object mapper) {
                super(target);
                this.mapper = mapper;
            }
        }

        @Specialization(guards = "isCallable(mapper)")
        public JSDynamicObject flatMap(Object thisObj, Object mapper) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            return createIterator(thisObj, new IteratorFlatMapArgs(iterated, mapper));
        }

        @Specialization(guards = "!isCallable(mapper)")
        public Object unsupported(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object mapper) {
            getIteratorDirect(thisObj);
            throw Errors.createTypeErrorCallableExpected();
        }

        protected abstract static class IteratorFlatMapNextNode extends IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<IteratorFlatMapArgs> {
            @Child private IteratorCloseNode iteratorCloseNode;

            @Child private JSFunctionCallNode callNode;
            @Child private GetIteratorFlattenableNode getIteratorFlattenableNode;
            @Child private IteratorGetNextValueNode getNextValueNode;

            protected IteratorFlatMapNextNode(JSContext context) {
                super(context);

                this.iteratorCloseNode = IteratorCloseNode.create(context);

                this.callNode = JSFunctionCallNode.createCall();
                this.getIteratorFlattenableNode = GetIteratorFlattenableNode.create(false, context);

                this.getNextValueNode = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), true);
            }

            @Specialization
            public Object next(VirtualFrame frame, Object thisObj) {
                IteratorFlatMapArgs args = getArgs(thisObj);
                boolean innerAlive = args.innerAlive;
                while (true) {
                    if (innerAlive) {
                        Object innerValue;
                        try {
                            innerValue = getNextValueNode.execute(frame, args.innerIterator);
                        } catch (AbstractTruffleException e) {
                            iteratorCloseNode.executeAbrupt(args.iterated.getIterator());
                            throw e;
                        }
                        if (innerValue == null) {
                            innerAlive = false;
                            args.innerAlive = false;
                            args.innerIterator = null;
                            continue;
                        }
                        return createResultContinue(frame, thisObj, innerValue);
                    } else {
                        Object value = getNextValueNode.execute(frame, args.iterated);
                        if (value == null) {
                            return createResultDone(frame, thisObj);
                        }

                        try {
                            Object mapped = callNode.executeCall(JSArguments.create(Undefined.instance, args.mapper, value, indexToJS(args.counter)));
                            args.innerIterator = getIteratorFlattenableNode.execute(mapped);
                        } catch (AbstractTruffleException e) {
                            iteratorCloseNode.executeAbrupt(args.iterated.getIterator());
                            throw e;
                        }
                        args.counter++;
                        innerAlive = true;
                        args.innerAlive = true;
                    }
                }
            }

            @Override
            public IteratorFromGeneratorImplNode<IteratorFlatMapArgs> copyUninitialized() {
                return create(context);
            }

            public static IteratorFlatMapNextNode create(JSContext context) {
                return IteratorPrototypeBuiltinsFactory.IteratorFlatMapNodeGen.IteratorFlatMapNextNodeGen.create(context);
            }
        }

    }

    protected abstract static class IteratorConsumerWithCallableNode extends IteratorMethodWithCallableNode {
        @Child private JSFunctionCallNode callNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private PropertyGetNode getDoneNode;
        @Child private IsJSObjectNode isObjectNode;
        @Child private JSToBooleanNode toBooleanNode;
        @Child private IteratorValueNode iteratorValueNode;

        protected final BranchProfile errorProfile = BranchProfile.create();
        private final BranchProfile doubleIndexBranch = BranchProfile.create();

        protected static final Object CONTINUE = new Object();

        protected IteratorConsumerWithCallableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.callNode = JSFunctionCallNode.createCall();

            this.iteratorNextNode = IteratorNextNode.create();
            this.iteratorValueNode = IteratorValueNode.create();
            this.getDoneNode = PropertyGetNode.create(Strings.DONE, false, context);
            this.isObjectNode = IsJSObjectNode.create();
            this.toBooleanNode = JSToBooleanNode.create();
        }

        protected Object end() {
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        protected Object step(IteratorRecord iterated, Object fn, Object value, long counter) {
            return CONTINUE;
        }

        protected final Object callMapper(IteratorRecord iterated, Object fn, Object value, long counter) {
            try {
                return callNode.executeCall(JSArguments.create(Undefined.instance, fn, value, indexToJS(counter)));
            } catch (AbstractTruffleException ex) {
                if (iteratorCloseNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
                }
                iteratorCloseNode.executeAbrupt(iterated.getIterator());
                throw ex; // should be executed by iteratorClose
            }
        }

        @Specialization(guards = "isCallable(fn)")
        protected Object compatible(Object thisObj, Object fn) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            long counter = 0;
            while (true) {
                Object next = iteratorNextNode.execute(iterated);
                if (!isObjectNode.executeBoolean(next)) {
                    errorProfile.enter();
                    throw Errors.createTypeErrorIterResultNotAnObject(next, this);
                }
                if (toBooleanNode.executeBoolean(getDoneNode.getValue(next))) {
                    return end();
                }

                Object result = step(iterated, fn, iteratorValueNode.execute(next), counter++);
                if (result != CONTINUE) {
                    if (iteratorCloseNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
                    }
                    return iteratorCloseNode.execute(iterated.getIterator(), result);
                }
            }
        }

        @Specialization(guards = "!isCallable(fn)")
        protected void incompatible(Object thisObj, Object fn) {
            getIteratorDirect(thisObj);
            throw Errors.createTypeErrorNotAFunction(fn);
        }

        protected final Object indexToJS(long index) {
            return JSRuntime.longToIntOrDouble(index, doubleIndexBranch);
        }
    }

    protected abstract static class IteratorToArrayNode extends IteratorMethodNode {
        @Child private com.oracle.truffle.js.nodes.access.IteratorToArrayNode toArrayNode;

        protected IteratorToArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.toArrayNode = com.oracle.truffle.js.nodes.access.IteratorToArrayNode.create(context, null);
        }

        @Specialization
        protected Object toArray(VirtualFrame frame, Object thisObj) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            return toArrayNode.execute(frame, iterated);
        }
    }

    public abstract static class IteratorToAsyncNode extends IteratorMethodNode {

        protected IteratorToAsyncNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject toAsync(Object thisObj) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            return JSWrapForValidAsyncIterator.create(getContext(), getRealm(), iterated);
        }
    }

    public abstract static class IteratorForEachNode extends IteratorConsumerWithCallableNode {
        protected IteratorForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value, long counter) {
            callMapper(iterated, fn, value, counter);
            return CONTINUE;
        }
    }

    public abstract static class IteratorSomeNode extends IteratorConsumerWithCallableNode {
        @Child private JSToBooleanNode toBooleanNode;

        protected IteratorSomeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.toBooleanNode = JSToBooleanNode.create();
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value, long counter) {
            if (toBooleanNode.executeBoolean(callMapper(iterated, fn, value, counter))) {
                return true;
            }
            return CONTINUE;
        }

        @Override
        protected Object end() {
            return false;
        }
    }

    public abstract static class IteratorEveryNode extends IteratorConsumerWithCallableNode {
        @Child private JSToBooleanNode toBooleanNode;

        protected IteratorEveryNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.toBooleanNode = JSToBooleanNode.create();
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value, long counter) {
            if (!toBooleanNode.executeBoolean(callMapper(iterated, fn, value, counter))) {
                return false;
            }
            return CONTINUE;
        }

        @Override
        protected Object end() {
            return true;
        }
    }

    public abstract static class IteratorFindNode extends IteratorConsumerWithCallableNode {
        @Child private JSToBooleanNode toBooleanNode;

        protected IteratorFindNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.toBooleanNode = JSToBooleanNode.create();
        }

        @Override
        protected Object step(IteratorRecord iterated, Object fn, Object value, long counter) {
            if (toBooleanNode.executeBoolean(callMapper(iterated, fn, value, counter))) {
                return value;
            }
            return CONTINUE;
        }
    }

    public abstract static class IteratorReduceNode extends IteratorMethodWithCallableNode {
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private JSFunctionCallNode callNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        private final BranchProfile doubleIndexBranch = BranchProfile.create();

        protected IteratorReduceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.iteratorStepNode = IteratorStepNode.create();
            this.iteratorValueNode = IteratorValueNode.create();
            this.callNode = JSFunctionCallNode.createCall();
        }

        private Object callReducer(IteratorRecord iterated, Object reducer, Object accumulator, Object value, long counter) {
            try {
                return callNode.executeCall(JSArguments.create(Undefined.instance, reducer, accumulator, value, JSRuntime.longToIntOrDouble(counter, doubleIndexBranch)));
            } catch (AbstractTruffleException ex) {
                if (iteratorCloseNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
                }
                iteratorCloseNode.executeAbrupt(iterated.getIterator());
                throw ex; // should be executed by iteratorClose
            }
        }

        @Specialization(guards = "isCallable(reducer)")
        protected Object reduce(Object thisObj, Object reducer, Object[] args) {
            IteratorRecord iterated = getIteratorDirect(thisObj);

            Object initialValue = JSRuntime.getArgOrUndefined(args, 0);

            Object accumulator;
            long counter;
            if (initialValue == Undefined.instance) {
                Object next = iteratorStepNode.execute(iterated);
                if (next == Boolean.FALSE) {
                    throw Errors.createTypeError("Reduce of empty iterator with no initial value");
                }

                accumulator = iteratorValueNode.execute(next);
                counter = 1;
            } else {
                accumulator = initialValue;
                counter = 0;
            }

            while (true) {
                Object next = iteratorStepNode.execute(iterated);
                if (next == Boolean.FALSE) {
                    return accumulator;
                }
                Object value = iteratorValueNode.execute(next);
                accumulator = callReducer(iterated, reducer, accumulator, value, counter);
                counter++;
            }
        }

        @Specialization(guards = "!isCallable(reducer)")
        protected void incompatible(Object thisObj, Object reducer, @SuppressWarnings("unused") Object[] args) {
            getIteratorDirect(thisObj);
            throw Errors.createTypeErrorNotAFunction(reducer);
        }
    }
}

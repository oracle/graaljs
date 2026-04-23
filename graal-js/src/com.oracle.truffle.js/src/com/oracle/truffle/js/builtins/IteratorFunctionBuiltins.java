/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltins.IteratorArgs;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltins.IteratorFromGeneratorNode;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.builtins.helper.ListSizeNode;
import com.oracle.truffle.js.nodes.access.GetIteratorFlattenableNode;
import com.oracle.truffle.js.nodes.access.GetIteratorFromMethodNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNode.OrdinaryHasInstanceNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSIteratorHelperObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Contains builtins for {@linkplain JSIterator} function (constructor).
 */
public final class IteratorFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorFunctionBuiltins.IteratorFunction> {

    static final byte ZIP_MODE_SHORTEST = 0;
    static final byte ZIP_MODE_LONGEST = 1;
    static final byte ZIP_MODE_STRICT = 2;

    public static final JSBuiltinsContainer BUILTINS = new IteratorFunctionBuiltins();

    IteratorFunctionBuiltins() {
        super(JSIterator.CLASS_NAME, IteratorFunction.class);
    }

    public enum IteratorFunction implements BuiltinEnum<IteratorFunction> {
        from(1),

        concat(0),
        zip(1),
        zipKeyed(1);

        private final int length;

        IteratorFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (this == concat) {
                return JSConfig.ECMAScript2026;
            } else if (this == zip || this == zipKeyed) {
                return JSConfig.StagingECMAScriptVersion;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return IteratorFunctionBuiltinsFactory.JSIteratorFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case concat:
                return IteratorFunctionBuiltinsFactory.IteratorConcatNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case zip:
                return IteratorFunctionBuiltinsFactory.IteratorZipNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case zipKeyed:
                return IteratorFunctionBuiltinsFactory.IteratorZipKeyedNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }

        return null;
    }

    public abstract static class JSIteratorFromNode extends JSBuiltinNode {
        @Child private GetIteratorFlattenableNode getIteratorFlattenableNode;

        @Child private OrdinaryHasInstanceNode ordinaryHasInstanceNode;

        protected JSIteratorFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorFlattenableNode = GetIteratorFlattenableNode.create(false, false, context);
            this.ordinaryHasInstanceNode = OrdinaryHasInstanceNode.create(context);
        }

        @Specialization
        protected Object iteratorFrom(Object arg) {
            IteratorRecord iteratorRecord = getIteratorFlattenableNode.execute(arg);

            JSRealm realm = getRealm();
            boolean hasInstance = ordinaryHasInstanceNode.executeBoolean(iteratorRecord.getIterator(), realm.getIteratorConstructor());
            if (hasInstance) {
                return iteratorRecord.getIterator();
            }

            return JSWrapForValidIterator.create(getContext(), realm, iteratorRecord);
        }

    }

    private record Iterable(Object openMethod, Object iterable) {
    }

    static final class ConcatArgs extends IteratorArgs {

        private final Iterable[] iterables;
        private int iterableIndex;
        boolean innerAlive;
        IteratorRecord innerIterator;

        ConcatArgs(Iterable[] iterables) {
            super(null);
            this.iterables = iterables;
        }
    }

    @ImportStatic({Symbol.class})
    public abstract static class IteratorConcatNode extends IteratorFromGeneratorNode<ConcatArgs> {

        protected IteratorConcatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorConcat,
                            c -> createIteratorFromGeneratorFunctionImpl(c, IteratorConcatNextNode.create(c)));
        }

        @Specialization
        protected final Object iteratorConcat(Object[] items,
                        @Cached IsObjectNode isObjectNode,
                        @Cached(parameters = {"getContext()", "SYMBOL_ITERATOR"}) GetMethodNode getIteratorMethodNode,
                        @Cached InlinedBranchProfile errorBranch) {
            Iterable[] iterables = new Iterable[items.length];
            for (int i = 0; i < items.length; i++) {
                Object item = items[i];
                if (!isObjectNode.executeBoolean(item)) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorNotAnObject(item);
                }
                Object method = getIteratorMethodNode.executeWithTarget(item);
                if (method == Undefined.instance) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorNotIterable(item, this);
                }
                iterables[i] = new Iterable(method, item);
            }
            return createIteratorHelperObject(new ConcatArgs(iterables));
        }
    }

    protected abstract static class IteratorConcatNextNode extends IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<ConcatArgs> {

        protected IteratorConcatNextNode(JSContext context) {
            super(context);
        }

        @Specialization
        protected final Object next(JSIteratorHelperObject thisObj,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode,
                        @Cached IteratorStepNode iteratorStepNode,
                        @Cached IteratorValueNode iteratorValueNode,
                        @Cached("create(context)") IteratorCloseNode iteratorCloseNode,
                        @Cached InlinedBranchProfile errorBranch) {
            ConcatArgs args = getArgs(thisObj);
            var iterables = args.iterables;
            int iterableIndex = args.iterableIndex;

            while (iterableIndex < iterables.length) {
                IteratorRecord iterator = args.innerIterator;
                if (!args.innerAlive) {
                    var iterable = iterables[iterableIndex];
                    args.innerIterator = iterator = getIteratorFromMethodNode.execute(this, iterable.iterable, iterable.openMethod);
                    args.innerAlive = true;
                }

                try {
                    assert args.innerAlive && iterator != null;
                    Object result = iteratorStepNode.execute(iterator);
                    if (result == Boolean.FALSE) {
                        args.innerAlive = false;
                        args.innerIterator = null;
                        args.iterableIndex = ++iterableIndex;
                    } else {
                        Object innerValue = iteratorValueNode.execute(result);
                        return createResultContinue(thisObj, innerValue);
                        // Note: Abrupt completion is handled by IteratorHelperReturnNode.
                    }
                } catch (AbstractTruffleException ex) {
                    errorBranch.enter(this);
                    iteratorCloseNode.executeAbrupt(iterator);
                    throw ex;
                }
            }
            return createResultDone(thisObj);
        }

        @Override
        public IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<ConcatArgs> copyUninitialized() {
            return create(context);
        }

        static IteratorConcatNextNode create(JSContext context) {
            return IteratorFunctionBuiltinsFactory.IteratorConcatNextNodeGen.create(context);
        }
    }

    record ZipOptions(byte mode, Object paddingOption) {
    }

    static final class IteratorZipArgs extends IteratorArgs {
        final IteratorRecord[] iterators;
        final IteratorRecord[] openIterators;
        int openIteratorsCount;
        final byte mode;
        final Object[] padding;
        final Object[] keys;

        IteratorZipArgs(SimpleArrayList<IteratorRecord> iterators, byte mode, Object[] padding, Object[] keys) {
            super(null);
            this.iterators = iterators.toArray(new IteratorRecord[iterators.size()]);
            this.openIterators = iterators.toArray(new IteratorRecord[iterators.size()]);
            this.openIteratorsCount = iterators.size();
            this.mode = mode;
            this.padding = padding;
            this.keys = keys;
        }

        boolean keyed() {
            return keys != null;
        }

        void removeOpenIterator(int index, IteratorRecord openIterator) {
            assert openIterators[index] == openIterator;
            openIterators[index] = null;
            openIteratorsCount--;
        }
    }

    protected abstract static class IteratorZipMethodNode extends IteratorFromGeneratorNode<IteratorZipArgs> {

        protected IteratorZipMethodNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorZip,
                            c -> createIteratorFromGeneratorFunctionImpl(c, IteratorZipNextNode.create(c)));
        }
    }

    @ImportStatic(Strings.class)
    protected abstract static class IteratorZipNode extends IteratorZipMethodNode {

        protected IteratorZipNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected final JSIteratorHelperObject iteratorZip(Object iterables, Object options,
                        @Bind Node node,
                        @Cached IsObjectNode isObjectNode,
                        @Cached("create(getContext())") GetOptionsObjectNode getOptionsObjectNode,
                        @Cached("create(MODE, getContext())") PropertyGetNode getModeNode,
                        @Cached("create(PADDING, getContext())") PropertyGetNode getPaddingNode,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached("create(true, false, getContext())") GetIteratorFlattenableNode getIteratorFlattenableNode,
                        @Cached IteratorStepNode iteratorStepNode,
                        @Cached IteratorValueNode iteratorValueNode,
                        @Cached("create(getContext())") IteratorCloseNode iteratorCloseNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedBranchProfile growBranch) {
            if (!isObjectNode.executeBoolean(iterables)) {
                errorBranch.enter(node);
                throw Errors.createTypeErrorNotAnObject(iterables, this);
            }

            ZipOptions zipOptions = getZipOptions(options, isObjectNode, getOptionsObjectNode, getModeNode, getPaddingNode, equalNode, node, errorBranch);

            SimpleArrayList<IteratorRecord> iterators = new SimpleArrayList<>();
            IteratorRecord inputIter = getIteratorNode.execute(node, iterables);
            while (true) {
                Object next;
                try {
                    next = iteratorStepValue(inputIter, iteratorStepNode, iteratorValueNode);
                } catch (AbstractTruffleException ex) {
                    errorBranch.enter(node);
                    iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
                    throw ex;
                }
                if (next == null) {
                    break;
                }
                try {
                    iterators.add(getIteratorFlattenableNode.execute(next), node, growBranch);
                } catch (AbstractTruffleException ex) {
                    errorBranch.enter(node);
                    iteratorCloseAllAbrupt(iteratorCloseNode, prependIterator(inputIter, iterators));
                    throw ex;
                }
            }

            Object[] padding = createIterablePadding(node, zipOptions.paddingOption, iterators.size(), iterators,
                            getIteratorNode, iteratorStepNode, iteratorValueNode, iteratorCloseNode, errorBranch);
            return createIteratorHelperObject(new IteratorZipArgs(iterators, zipOptions.mode(), padding, null));
        }
    }

    @ImportStatic(Strings.class)
    protected abstract static class IteratorZipKeyedNode extends IteratorZipMethodNode {

        protected IteratorZipKeyedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected final JSIteratorHelperObject iteratorZipKeyed(Object iterables, Object options,
                        @Bind Node node,
                        @Cached IsObjectNode isObjectNode,
                        @Cached("create(getContext())") GetOptionsObjectNode getOptionsObjectNode,
                        @Cached("create(MODE, getContext())") PropertyGetNode getModeNode,
                        @Cached("create(PADDING, getContext())") PropertyGetNode getPaddingNode,
                        @Cached("create(getContext())") ReadElementNode readElementNode,
                        @Cached ListSizeNode listSize,
                        @Cached ListGetNode listGet,
                        @Cached("create(true, false, getContext())") GetIteratorFlattenableNode getIteratorFlattenableNode,
                        @Cached("create(getContext())") IteratorCloseNode iteratorCloseNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!isObjectNode.executeBoolean(iterables)) {
                errorBranch.enter(node);
                throw Errors.createTypeErrorNotAnObject(iterables, this);
            }
            if (!(iterables instanceof JSDynamicObject iterablesObject)) {
                errorBranch.enter(node);
                throw Errors.createTypeError("Iterator.zipKeyed requires a JavaScript object");
            }
            ZipOptions zipOptions = getZipOptions(options, isObjectNode, getOptionsObjectNode, getModeNode, getPaddingNode, equalNode, node, errorBranch);

            List<Object> ownKeys = JSObject.ownPropertyKeys(iterablesObject);
            int ownKeysSize = listSize.execute(ownKeys);
            SimpleArrayList<Object> keys = new SimpleArrayList<>(ownKeysSize);
            SimpleArrayList<IteratorRecord> iterators = new SimpleArrayList<>(ownKeysSize);
            for (int i = 0; i < ownKeysSize; i++) {
                Object key = listGet.execute(ownKeys, i);
                PropertyDescriptor desc;
                try {
                    desc = JSObject.getOwnProperty(iterablesObject, key);
                } catch (AbstractTruffleException ex) {
                    errorBranch.enter(node);
                    iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
                    throw ex;
                }
                if (desc != null && desc.getEnumerable()) {
                    Object value;
                    try {
                        value = readElementNode.executeWithTargetAndIndex(iterables, key);
                    } catch (AbstractTruffleException ex) {
                        errorBranch.enter(node);
                        iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
                        throw ex;
                    }
                    if (value != Undefined.instance) {
                        keys.addUnchecked(key);
                        try {
                            iterators.addUnchecked(getIteratorFlattenableNode.execute(value));
                        } catch (AbstractTruffleException ex) {
                            errorBranch.enter(node);
                            iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
                            throw ex;
                        }
                    }
                }
            }

            Object[] padding = createKeyedPadding(node, zipOptions.paddingOption, keys, iterators, readElementNode, iteratorCloseNode, errorBranch);
            return createIteratorHelperObject(new IteratorZipArgs(iterators, zipOptions.mode(), padding, keys.toArray()));
        }
    }

    protected abstract static class IteratorZipNextNode extends IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<IteratorZipArgs> {

        protected IteratorZipNextNode(JSContext context) {
            super(context);
        }

        @Specialization
        protected final Object next(JSIteratorHelperObject thisObj,
                        @Cached IteratorStepNode iteratorStepNode,
                        @Cached IteratorValueNode iteratorValueNode,
                        @Cached("create(context)") IteratorCloseNode iteratorCloseNode,
                        @Cached InlinedBranchProfile errorBranch) {
            IteratorZipArgs args = getArgs(thisObj);
            IteratorRecord[] iterators = args.iterators;
            int iterCount = iterators.length;
            if (iterCount == 0) {
                return createResultDone(thisObj);
            }

            Object[] results = new Object[iterCount];
            for (int i = 0; i < iterCount; i++) {
                IteratorRecord iterator = iterators[i];
                Object result;
                if (iterator == null) {
                    assert args.mode == ZIP_MODE_LONGEST;
                    result = args.padding[i];
                } else {
                    Object next;
                    try {
                        next = iteratorStepValue(iterator, iteratorStepNode, iteratorValueNode);
                    } catch (AbstractTruffleException ex) {
                        args.removeOpenIterator(i, iterator);
                        iteratorCloseAllAbrupt(iteratorCloseNode, args.openIterators);
                        throw ex;
                    }
                    if (next == null) {
                        args.removeOpenIterator(i, iterator);
                        if (args.mode == ZIP_MODE_SHORTEST) {
                            iteratorCloseAll(iteratorCloseNode, args.openIterators);
                            return createResultDone(thisObj);
                        } else if (args.mode == ZIP_MODE_STRICT) {
                            if (i != 0) {
                                errorBranch.enter(this);
                                iteratorCloseAllAbrupt(iteratorCloseNode, args.openIterators);
                                throw Errors.createTypeError("Iterators completed at different times");
                            }
                            for (int k = 1; k < iterCount; k++) {
                                IteratorRecord openIterator = iterators[k];
                                assert openIterator != null;
                                Object open;
                                try {
                                    open = iteratorStepNode.execute(openIterator);
                                } catch (AbstractTruffleException ex) {
                                    args.removeOpenIterator(k, openIterator);
                                    iteratorCloseAllAbrupt(iteratorCloseNode, args.openIterators);
                                    throw ex;
                                }
                                if (open == Boolean.FALSE) {
                                    args.removeOpenIterator(k, openIterator);
                                } else {
                                    errorBranch.enter(this);
                                    iteratorCloseAllAbrupt(iteratorCloseNode, args.openIterators);
                                    throw Errors.createTypeError("Iterators completed at different times");
                                }
                            }
                            return createResultDone(thisObj);
                        } else {
                            assert args.mode == ZIP_MODE_LONGEST;
                            if (args.openIteratorsCount == 0) {
                                return createResultDone(thisObj);
                            }
                            iterators[i] = null;
                            result = args.padding[i];
                        }
                    } else {
                        result = next;
                    }
                }
                results[i] = result;
            }

            return createResultContinue(thisObj, finishResults(args, results));
        }

        private Object finishResults(IteratorZipArgs args, Object[] results) {
            if (!args.keyed()) {
                return JSArray.createConstant(context, getRealm(), results);
            }
            JSDynamicObject object = JSOrdinary.createWithNullPrototype(context);
            for (int i = 0; i < args.keys.length; i++) {
                JSRuntime.createDataPropertyOrThrow(object, args.keys[i], results[i]);
            }
            return object;
        }

        @Override
        public IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<IteratorZipArgs> copyUninitialized() {
            return create(context);
        }

        static IteratorZipNextNode create(JSContext context) {
            return IteratorFunctionBuiltinsFactory.IteratorZipNextNodeGen.create(context);
        }
    }

    static void iteratorCloseAll(IteratorCloseNode iteratorCloseNode, IteratorRecord[] iteratorRecords) {
        AbstractTruffleException closeError = null;
        for (int i = iteratorRecords.length - 1; i >= 0; i--) {
            IteratorRecord iteratorRecord = iteratorRecords[i];
            if (iteratorRecord == null) {
                continue;
            }
            if (closeError == null) {
                try {
                    iteratorCloseNode.executeVoid(iteratorRecord);
                } catch (AbstractTruffleException ex) {
                    closeError = ex;
                }
            } else {
                iteratorCloseNode.executeAbrupt(iteratorRecord);
            }
        }
        if (closeError != null) {
            throw closeError;
        }
    }

    static void iteratorCloseAllAbrupt(IteratorCloseNode iteratorCloseNode, SimpleArrayList<IteratorRecord> iteratorRecords) {
        for (int i = iteratorRecords.size() - 1; i >= 0; i--) {
            iteratorCloseNode.executeAbrupt(iteratorRecords.get(i));
        }
    }

    static void iteratorCloseAllAbrupt(IteratorCloseNode iteratorCloseNode, IteratorRecord[] iteratorRecords) {
        for (int i = iteratorRecords.length - 1; i >= 0; i--) {
            IteratorRecord iteratorRecord = iteratorRecords[i];
            if (iteratorRecord != null) {
                iteratorCloseNode.executeAbrupt(iteratorRecord);
            }
        }
    }

    private static ZipOptions getZipOptions(Object options, IsObjectNode isObjectNode,
                    GetOptionsObjectNode getOptionsObjectNode, PropertyGetNode getModeNode, PropertyGetNode getPaddingNode,
                    TruffleString.EqualNode equalNode, Node node, InlinedBranchProfile errorBranch) {
        Object optionsObject = getOptionsObjectNode.execute(options);
        byte mode = toZipMode(getModeNode.getValue(optionsObject), equalNode, node, errorBranch);
        Object paddingOption = Undefined.instance;
        if (mode == ZIP_MODE_LONGEST) {
            paddingOption = getPaddingNode.getValue(optionsObject);
            if (paddingOption != Undefined.instance && !isObjectNode.executeBoolean(paddingOption)) {
                errorBranch.enter(node);
                throw Errors.createTypeErrorNotAnObject(paddingOption);
            }
        }
        return new ZipOptions(mode, paddingOption);
    }

    private static byte toZipMode(Object mode, TruffleString.EqualNode equalNode, Node node, InlinedBranchProfile errorBranch) {
        if (mode == Undefined.instance) {
            return ZIP_MODE_SHORTEST;
        } else if (mode instanceof TruffleString modeString) {
            if (Strings.equals(equalNode, modeString, Strings.SHORTEST)) {
                return ZIP_MODE_SHORTEST;
            } else if (Strings.equals(equalNode, modeString, Strings.LONGEST)) {
                return ZIP_MODE_LONGEST;
            } else if (Strings.equals(equalNode, modeString, Strings.STRICT)) {
                return ZIP_MODE_STRICT;
            }
        }
        errorBranch.enter(node);
        throw Errors.createTypeError("Invalid Iterator.zip mode");
    }

    private static Object iteratorStepValue(IteratorRecord iterator, IteratorStepNode iteratorStepNode, IteratorValueNode iteratorValueNode) {
        Object next = iteratorStepNode.execute(iterator);
        if (next == Boolean.FALSE) {
            return null;
        }
        return iteratorValueNode.execute(next);
    }

    private static Object[] createIterablePadding(Node node, Object paddingOption, int iterCount, SimpleArrayList<IteratorRecord> iterators,
                    GetIteratorNode getIteratorNode, IteratorStepNode iteratorStepNode, IteratorValueNode iteratorValueNode,
                    IteratorCloseNode iteratorCloseNode, InlinedBranchProfile errorBranch) {
        Object[] padding = new Object[iterCount];
        if (paddingOption == Undefined.instance) {
            Arrays.fill(padding, Undefined.instance);
            return padding;
        }

        IteratorRecord paddingIter;
        try {
            paddingIter = getIteratorNode.execute(node, paddingOption);
        } catch (AbstractTruffleException ex) {
            errorBranch.enter(node);
            iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
            throw ex;
        }

        boolean usingIterator = true;
        for (int i = 0; i < iterCount; i++) {
            if (usingIterator) {
                Object next;
                try {
                    next = iteratorStepValue(paddingIter, iteratorStepNode, iteratorValueNode);
                } catch (AbstractTruffleException ex) {
                    errorBranch.enter(node);
                    iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
                    throw ex;
                }
                if (next == null) {
                    usingIterator = false;
                } else {
                    padding[i] = next;
                    continue;
                }
            }
            padding[i] = Undefined.instance;
        }

        if (usingIterator) {
            try {
                iteratorCloseNode.executeVoid(paddingIter);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter(node);
                iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
                throw ex;
            }
        }
        return padding;
    }

    private static Object[] createKeyedPadding(Node node, Object paddingOption, SimpleArrayList<Object> keys, SimpleArrayList<IteratorRecord> iterators,
                    ReadElementNode readElementNode, IteratorCloseNode iteratorCloseNode, InlinedBranchProfile errorBranch) {
        Object[] padding = new Object[keys.size()];
        if (paddingOption == Undefined.instance) {
            Arrays.fill(padding, Undefined.instance);
            return padding;
        }

        for (int i = 0; i < keys.size(); i++) {
            try {
                padding[i] = readElementNode.executeWithTargetAndIndex(paddingOption, keys.get(i));
            } catch (AbstractTruffleException ex) {
                errorBranch.enter(node);
                iteratorCloseAllAbrupt(iteratorCloseNode, iterators);
                throw ex;
            }
        }
        return padding;
    }

    private static SimpleArrayList<IteratorRecord> prependIterator(IteratorRecord first, SimpleArrayList<IteratorRecord> rest) {
        SimpleArrayList<IteratorRecord> iterators = new SimpleArrayList<>(rest.size() + 1);
        iterators.addUnchecked(first);
        for (int i = 0; i < rest.size(); i++) {
            iterators.addUnchecked(rest.get(i));
        }
        return iterators;
    }
}

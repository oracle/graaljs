/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.tuples.JSIsTupleNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTuple;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Contains builtins for Tuple function.
 */
public final class TupleFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TupleFunctionBuiltins.TupleFunction> {

    public static final JSBuiltinsContainer BUILTINS = new TupleFunctionBuiltins();

    protected TupleFunctionBuiltins() {
        super(JSTuple.CLASS_NAME, TupleFunction.class);
    }

    public enum TupleFunction implements BuiltinEnum<TupleFunction> {
        isTuple(1),
        from(1),
        of(1);

        private final int length;

        TupleFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TupleFunction builtinEnum) {
        switch (builtinEnum) {
            case isTuple:
                return TupleFunctionBuiltinsFactory.TupleIsTupleNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case from:
                return TupleFunctionBuiltinsFactory.TupleFromNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case of:
                return TupleFunctionBuiltinsFactory.TupleOfNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class TupleIsTupleNode extends JSBuiltinNode {

        @Child private JSIsTupleNode isTupleNode = JSIsTupleNode.create();

        public TupleIsTupleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isTuple(Object object) {
            return isTupleNode.execute(object);
        }
    }

    public abstract static class TupleFromNode extends JSBuiltinNode {

        private final ConditionProfile usingIterableProfile = ConditionProfile.create();
        private final BranchProfile growProfile = BranchProfile.create();
        private final BranchProfile isCallableErrorBranch = BranchProfile.create();
        private final BranchProfile isObjectErrorBranch = BranchProfile.create();
        private final BranchProfile iteratorErrorBranch = BranchProfile.create();

        @Child private IsCallableNode isCallableNode;
        @Child private GetMethodNode getIteratorMethodNode;
        @Child private GetIteratorNode getIteratorNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private IsObjectNode isObjectNode;
        @Child private JSToObjectNode toObjectNode;
        @Child private JSGetLengthNode getLengthNode;
        @Child private ReadElementNode readElementNode;
        @Child private JSFunctionCallNode functionCallNode;

        public TupleFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Tuple from(Object items, Object mapFn, Object thisArg) {
            boolean mapping;
            if (mapFn == Undefined.instance) {
                mapping = false;
            } else {
                if (!isCallable(mapFn)) {
                    isCallableErrorBranch.enter();
                    throw Errors.createTypeError("The mapping function must be either a function or undefined");
                }
                mapping = true;
            }
            SimpleArrayList<Object> list = new SimpleArrayList<>();
            long k = 0;

            Object usingIterator = getIteratorMethod(items);
            if (usingIterableProfile.profile(usingIterator != Undefined.instance)) {
                // TODO: re-evaluate, check proposal for changes
                // NOTE: Proposal spec would not work as intended...
                // For this reason I replaced the AddEntriesFromIterable(...) call
                // with the corresponding code of https://tc39.es/ecma262/#sec-array.from.
                IteratorRecord iteratorRecord = getIterator(items);
                try {
                    while (true) {
                        Object next = iteratorStep(iteratorRecord);
                        if (next == Boolean.FALSE) {
                            return Tuple.create(list.toArray());
                        }
                        Object value = iteratorValue((DynamicObject) next);
                        if (mapping) {
                            value = call(mapFn, thisArg, value, k);
                        }
                        if (isObject(value)) {
                            isObjectErrorBranch.enter();
                            throw Errors.createTypeError("Tuples cannot contain non-primitive values");
                        }
                        list.add(value, growProfile);
                        k++;
                    }
                } catch (Exception ex) {
                    iteratorErrorBranch.enter();
                    iteratorCloseAbrupt(iteratorRecord.getIterator());
                    throw ex;
                }
            }

            // NOTE: items is not an Iterable so assume it is an array-like object.
            Object arrayLike = toObject(items);
            long len = getLengthOfArrayLike(items);
            while (k < len) {
                Object value = get(arrayLike, k);
                if (mapping) {
                    value = call(mapFn, thisArg, value, k);
                }
                if (isObject(value)) {
                    isObjectErrorBranch.enter();
                    throw Errors.createTypeError("Tuples cannot contain non-primitive values");
                }
                list.add(value, growProfile);
                k++;
            }
            return Tuple.create(list.toArray());
        }

        private boolean isCallable(Object obj) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(obj);
        }

        private Object getIteratorMethod(Object obj) {
            if (getIteratorMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorMethodNode = insert(GetMethodNode.create(getContext(), null, Symbol.SYMBOL_ITERATOR));
            }
            return getIteratorMethodNode.executeWithTarget(obj);
        }

        private IteratorRecord getIterator(Object obj) {
            if (getIteratorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorNode = insert(GetIteratorNode.create(getContext()));
            }
            return getIteratorNode.execute(obj);
        }

        private Object iteratorStep(IteratorRecord iterator) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create(getContext()));
            }
            return iteratorStepNode.execute(iterator);
        }

        private Object iteratorValue(DynamicObject obj) {
            if (iteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorValueNode = insert(IteratorValueNode.create(getContext()));
            }
            return iteratorValueNode.execute( obj);
        }

        protected void iteratorCloseAbrupt(DynamicObject iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        private boolean isObject(Object obj) {
            if (isObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isObjectNode = insert(IsObjectNode.create());
            }
            return isObjectNode.executeBoolean(obj);
        }

        private Object toObject(Object obj) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return toObjectNode.execute(obj);
        }

        private long getLengthOfArrayLike(Object obj) {
            if (getLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLengthNode = insert(JSGetLengthNode.create(getContext()));
            }
            return getLengthNode.executeLong(obj);
        }

        private Object get(Object obj, long idx) {
            if (readElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readElementNode = insert(ReadElementNode.create(getContext()));
            }
            return readElementNode.executeWithTargetAndIndex(obj, idx);
        }

        private Object call(Object function, Object target, Object... arguments) {
            if (functionCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionCallNode = insert(JSFunctionCallNode.createCall());
            }
            return functionCallNode.executeCall(JSArguments.create(target, function, arguments));
        }
    }

    public abstract static class TupleOfNode extends JSBuiltinNode {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Child private IsObjectNode isObjectNode = IsObjectNode.create();

        public TupleOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Tuple of(Object[] items) {
            for (Object item : items) {
                if (isObjectNode.executeBoolean(item)) {
                    errorProfile.enter();
                    throw Errors.createTypeError("Tuples cannot contain non-primitive values");
                }
            }
            return Tuple.create(items);
        }
    }
}

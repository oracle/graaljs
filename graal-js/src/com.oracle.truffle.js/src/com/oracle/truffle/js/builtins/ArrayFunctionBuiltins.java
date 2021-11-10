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

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltinsFactory.JSArrayFromNodeGen;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltinsFactory.JSArrayOfNodeGen;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltinsFactory.JSIsArrayNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperation;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.array.ArrayCreateNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
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
        from(1);

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
            if (EnumSet.of(of, from).contains(this)) {
                return 6;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
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
                return JSArrayFromNodeGen.create(context, builtin, false, args().withThis().varArgs().createArgumentNodes(context));
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
        @Child private ArrayCreateNode arrayCreateNode;
        private final ConditionProfile isConstructor = ConditionProfile.createBinaryProfile();

        public JSArrayFunctionOperation(JSContext context, JSBuiltin builtin, boolean isTypedArray) {
            super(context, builtin, isTypedArray);
        }

        protected DynamicObject constructOrArray(Object thisObj, long len, boolean provideLengthArg) {
            if (isTypedArrayImplementation) {
                return getArraySpeciesConstructorNode().typedArrayCreate((DynamicObject) thisObj, JSRuntime.longToIntOrDouble(len));
            } else {
                if (isConstructor.profile(JSFunction.isConstructor(thisObj))) {
                    if (provideLengthArg) {
                        return (DynamicObject) getArraySpeciesConstructorNode().construct((DynamicObject) thisObj, JSRuntime.longToIntOrDouble(len));
                    } else {
                        return (DynamicObject) getArraySpeciesConstructorNode().construct((DynamicObject) thisObj);
                    }
                } else {
                    if (arrayCreateNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        arrayCreateNode = insert(ArrayCreateNode.create(getContext()));
                    }
                    return arrayCreateNode.execute(len);
                }
            }
        }

        protected boolean isTypedArrayConstructor(Object thisObj) {
            return JSFunction.isConstructor(thisObj) && thisObj != getRealm().getArrayConstructor();
        }
    }

    public abstract static class JSArrayOfNode extends JSArrayFunctionOperation {

        public JSArrayOfNode(JSContext context, JSBuiltin builtin, boolean isTypedArray) {
            super(context, builtin, isTypedArray);
        }

        @Specialization
        protected DynamicObject arrayOf(Object thisObj, Object[] args) {
            int len = args.length;
            DynamicObject obj = constructOrArray(thisObj, len, true);

            int pos = 0;
            for (Object arg : args) {
                Object value = JSRuntime.nullToUndefined(arg);
                JSRuntime.createDataPropertyOrThrow(obj, Boundaries.stringValueOf(pos), value);
                pos++;
            }
            JSObject.set(obj, JSAbstractArray.LENGTH, len, true, this);
            return obj;
        }
    }

    public abstract static class JSArrayFromNode extends JSArrayFunctionOperation {
        @Child private JSFunctionCallNode callMapFnNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private JSFunctionCallNode callIteratorMethodNode;
        @Child private IteratorValueNode getIteratorValueNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private GetMethodNode getIteratorMethodNode;
        @Child private GetIteratorNode getIteratorNode;
        @Child private IsJSObjectNode isObjectNode;
        @Child private PropertyGetNode getNextMethodNode;
        @Child private JSGetLengthNode getSourceLengthNode;
        @Child private IsArrayNode isFastArrayNode;
        private final ConditionProfile isIterable = ConditionProfile.createBinaryProfile();

        public JSArrayFromNode(JSContext context, JSBuiltin builtin, boolean isTypedArray) {
            super(context, builtin, isTypedArray);
            this.getIteratorMethodNode = GetMethodNode.create(context, Symbol.SYMBOL_ITERATOR);
            this.isFastArrayNode = isTypedArrayImplementation ? null : IsArrayNode.createIsFastArray();
        }

        protected void iteratorCloseAbrupt(DynamicObject iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        protected IteratorRecord getIterator(Object object, Object usingIterator) {
            if (callIteratorMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callIteratorMethodNode = insert(JSFunctionCallNode.createCall());
            }
            if (isObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isObjectNode = insert(IsJSObjectNode.create());
            }
            if (getNextMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNextMethodNode = insert(PropertyGetNode.create(JSRuntime.NEXT, getContext()));
            }

            return GetIteratorNode.getIterator(object, usingIterator, callIteratorMethodNode, isObjectNode, getNextMethodNode, this);
        }

        protected Object getIteratorValue(DynamicObject iteratorResult) {
            if (getIteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorValueNode = insert(IteratorValueNode.create(getContext()));
            }
            return getIteratorValueNode.execute(iteratorResult);
        }

        protected Object iteratorStep(IteratorRecord iteratorRecord) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create(getContext()));
            }
            return iteratorStepNode.execute(iteratorRecord);
        }

        protected final Object callMapFn(Object target, DynamicObject function, Object... userArguments) {
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
        protected DynamicObject arrayFrom(Object thisObj, Object[] args) {
            Object items = JSRuntime.getArgOrUndefined(args, 0);
            Object mapFn = JSRuntime.getArgOrUndefined(args, 1);
            Object thisArg = JSRuntime.getArgOrUndefined(args, 2);

            return arrayFromIntl(thisObj, items, mapFn, thisArg, true);
        }

        protected DynamicObject arrayFromIntl(Object thisObj, Object items, Object mapFn, Object thisArg, boolean setLength) {
            boolean mapping;
            if (mapFn == Undefined.instance) {
                mapping = false;
            } else {
                checkCallbackIsFunction(mapFn);
                mapping = true;
            }
            Object usingIterator = getIteratorMethodNode.executeWithTarget(items);
            if (isIterable.profile(usingIterator != Undefined.instance)) {
                return arrayFromIterable(thisObj, items, usingIterator, mapFn, thisArg, mapping);
            } else {
                // NOTE: source is not an Iterable so assume it is already an array-like object.
                Object itemsObject = toObject(items);
                return arrayFromArrayLike(thisObj, itemsObject, mapFn, thisArg, mapping, setLength);
            }
        }

        protected DynamicObject arrayFromIterable(Object thisObj, Object items, Object mapFn, Object thisArg, boolean mapping) {
            if (getIteratorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorNode = insert(GetIteratorNode.create(getContext()));
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(items);
            DynamicObject obj = constructOrArray(thisObj, 0, false);
            return arrayFromIteratorRecord(obj, iteratorRecord, mapFn, thisArg, mapping);
        }

        protected DynamicObject arrayFromIterable(Object thisObj, Object items, Object usingIterator, Object mapFn, Object thisArg, boolean mapping) {
            DynamicObject obj = constructOrArray(thisObj, 0, false);

            IteratorRecord iteratorRecord = getIterator(items, usingIterator);
            return arrayFromIteratorRecord(obj, iteratorRecord, mapFn, thisArg, mapping);
        }

        private DynamicObject arrayFromIteratorRecord(DynamicObject obj, IteratorRecord iteratorRecord, Object mapFn, Object thisArg, boolean mapping) {
            long k = 0;
            try {
                while (true) {
                    Object next = iteratorStep(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        setLength(obj, k);
                        return obj;
                    }
                    Object mapped = getIteratorValue((DynamicObject) next);
                    if (mapping) {
                        mapped = callMapFn(thisArg, (DynamicObject) mapFn, mapped, JSRuntime.positiveLongToIntOrDouble(k));
                    }
                    if (isTypedArrayImplementation || isFastArrayNode.execute(obj)) {
                        writeOwn(obj, k, mapped);
                    } else {
                        JSRuntime.createDataPropertyOrThrow(obj, Boundaries.stringValueOf(k), mapped);
                    }
                    k++;
                }
            } catch (Exception ex) {
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex; // should be executed by iteratorClose
            }
        }

        protected DynamicObject arrayFromArrayLike(Object thisObj, Object items, Object mapFn, Object thisArg, boolean mapping, boolean setLength) {
            long len = getSourceLength(items);

            DynamicObject obj = constructOrArray(thisObj, len, true);

            long k = 0;
            while (k < len) {
                Object value = read(items, k);
                Object mapped = value;
                if (mapping) {
                    mapped = callMapFn(thisArg, (DynamicObject) mapFn, mapped, JSRuntime.positiveLongToIntOrDouble(k));
                }
                if (isTypedArrayImplementation || isFastArrayNode.execute(obj)) {
                    writeOwn(obj, k, mapped);
                } else {
                    JSRuntime.createDataPropertyOrThrow(obj, Boundaries.stringValueOf(k), mapped);
                }
                k++;
            }
            if (setLength) {
                setLength(obj, len);
            }
            return obj;
        }
    }
}

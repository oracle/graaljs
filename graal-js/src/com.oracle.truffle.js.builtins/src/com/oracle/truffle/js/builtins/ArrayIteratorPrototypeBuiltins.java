/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ArrayIteratorPrototypeBuiltinsFactory.ArrayIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.JSGetLengthNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %ArrayIteratorPrototype% object.
 */
public final class ArrayIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ArrayIteratorPrototypeBuiltins.ArrayIteratorPrototype> {
    protected ArrayIteratorPrototypeBuiltins() {
        super(JSArray.ITERATOR_PROTOTYPE_NAME, ArrayIteratorPrototype.class);
    }

    public enum ArrayIteratorPrototype implements BuiltinEnum<ArrayIteratorPrototype> {
        next(0);

        private final int length;

        ArrayIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ArrayIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return ArrayIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ArrayIteratorNextNode extends JSBuiltinNode {
        @Child private HasHiddenKeyCacheNode isArrayIteratorNode;
        @Child private PropertyGetNode getIteratedObjectNode;
        @Child private PropertyGetNode getNextIndexNode;
        @Child private PropertyGetNode getIterationKindNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private JSGetLengthNode getLengthNode;
        @Child private ReadElementNode readElementNode;
        private final ConditionProfile intIndexProfile;
        private final BranchProfile errorBranch;

        public ArrayIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isArrayIteratorNode = HasHiddenKeyCacheNode.create(JSArray.ARRAY_ITERATION_KIND_ID);
            this.getIteratedObjectNode = PropertyGetNode.createGetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.getNextIndexNode = PropertyGetNode.createGetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.getIterationKindNode = PropertyGetNode.createGetHidden(JSArray.ARRAY_ITERATION_KIND_ID, context);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            this.intIndexProfile = ConditionProfile.createBinaryProfile();
            this.errorBranch = BranchProfile.create();
        }

        @Specialization(guards = "isArrayIterator(iterator)")
        protected DynamicObject doArrayIterator(VirtualFrame frame, DynamicObject iterator) {
            Object array = getIteratedObjectNode.getValue(iterator);
            if (array == Undefined.instance) {
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            long index = getNextIndex(iterator);
            int itemKind = getIterationKind(iterator);
            long length;
            if (JSArrayBufferView.isJSArrayBufferView(array)) {
                DynamicObject typedArray = (DynamicObject) array;
                if (JSArrayBufferView.hasDetachedBuffer(typedArray, getContext())) {
                    errorBranch.enter();
                    throw Errors.createTypeError("Cannot perform Array Iterator.prototype.next on a detached ArrayBuffer");
                }
                length = JSArrayBufferView.typedArrayGetLength(typedArray);
            } else {
                length = getLength().executeLong((TruffleObject) array);
            }

            if (index >= length) {
                setIteratedObjectNode.setValue(iterator, Undefined.instance);
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            setNextIndexNode.setValue(iterator, index + 1);
            if (itemKind == JSRuntime.ITERATION_KIND_KEY) {
                return createIterResultObjectNode.execute(frame, indexToJS(index), false);
            }

            Object elementValue = readElement().executeWithTargetAndIndex(array, index);
            Object result;
            if (itemKind == JSRuntime.ITERATION_KIND_VALUE) {
                result = elementValue;
            } else {
                assert itemKind == JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE;
                result = JSArray.createConstantObjectArray(getContext(), new Object[]{indexToJS(index), elementValue});
            }
            return createIterResultObjectNode.execute(frame, result, false);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected DynamicObject doIncompatibleReceiver(Object iterator) {
            throw Errors.createTypeError("not an Array Iterator");
        }

        protected final boolean isArrayIterator(Object thisObj) {
            // If the [[ArrayIterationKind]] internal slot is present, the others must be as well.
            return isArrayIteratorNode.executeHasHiddenKey(thisObj);
        }

        private long getNextIndex(DynamicObject iterator) {
            try {
                return getNextIndexNode.getValueLong(iterator);
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }
        }

        private int getIterationKind(DynamicObject iterator) {
            try {
                return getIterationKindNode.getValueInt(iterator);
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }
        }

        private Object indexToJS(long index) {
            if (intIndexProfile.profile(JSRuntime.longIsRepresentableAsInt(index))) {
                return (int) index;
            } else {
                return (double) index;
            }
        }

        private ReadElementNode readElement() {
            if (readElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readElementNode = insert(ReadElementNode.create(getContext()));
            }
            return readElementNode;
        }

        private JSGetLengthNode getLength() {
            if (getLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLengthNode = insert(JSGetLengthNode.create(getContext()));
            }
            return getLengthNode;
        }
    }
}

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

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedIntValueProfile;
import com.oracle.truffle.js.builtins.ArrayIteratorPrototypeBuiltinsFactory.ArrayIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.array.TypedArrayLengthNode;
import com.oracle.truffle.js.nodes.cast.LongToIntOrDoubleNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayIterator;
import com.oracle.truffle.js.runtime.builtins.JSArrayIteratorObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %ArrayIteratorPrototype% object.
 */
public final class ArrayIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ArrayIteratorPrototypeBuiltins.ArrayIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new ArrayIteratorPrototypeBuiltins();

    protected ArrayIteratorPrototypeBuiltins() {
        super(JSArrayIterator.PROTOTYPE_NAME, ArrayIteratorPrototype.class);
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

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected ArrayIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        protected JSObject createIteratorResultObject(Object value, boolean done) {
            return createIterResultObjectNode.execute(value, done);
        }

        @Specialization(guards = {"!isUndefined(array)"})
        JSObject doArrayIterator(JSArrayIteratorObject iterator,
                        @Bind("iterator.getIteratedObject()") Object array,
                        @Cached("create(getContext())") ReadElementNode readElementNode,
                        @Cached ArrayIteratorGetLengthNode getLengthNode,
                        @Cached(inline = true) LongToIntOrDoubleNode toJSIndex,
                        @Cached InlinedIntValueProfile iterationKindProfile,
                        @Cached InlinedConditionProfile skipLengthCheckBranch) {
            long index = iterator.getNextIndex();
            assert index >= 0;
            if (skipLengthCheckBranch.profile(this, iterator.isSkipGetLength())) {
                // Already checked in the caller, no need to get the length again.
                iterator.setSkipGetLength(false);
            } else {
                long length = getLengthNode.execute(this, array, getContext());
                if (index >= length) {
                    iterator.setIteratedObject(Undefined.instance);
                    return createIteratorResultObject(Undefined.instance, true);
                }
            }

            int itemKind = iterationKindProfile.profile(this, iterator.getIterationKind());
            iterator.setNextIndex(index + 1);
            Object indexNumber = null;
            if ((itemKind & JSRuntime.ITERATION_KIND_KEY) != 0) {
                indexNumber = toJSIndex.execute(this, index);
            }
            Object result;
            if (itemKind == JSRuntime.ITERATION_KIND_KEY) {
                result = indexNumber;
            } else {
                Object elementValue = readElementNode.executeWithTargetAndIndex(array, index);
                if (itemKind == JSRuntime.ITERATION_KIND_VALUE) {
                    result = elementValue;
                } else {
                    assert itemKind == JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE && indexNumber != null;
                    result = JSArray.createConstantObjectArray(getContext(), getRealm(), new Object[]{indexNumber, elementValue});
                }
            }
            return createIteratorResultObject(result, false);
        }

        @Specialization(guards = {"isUndefined(iterator.getIteratedObject())"})
        final JSObject doArrayIteratorDetached(@SuppressWarnings("unused") JSArrayIteratorObject iterator) {
            return createIteratorResultObject(Undefined.instance, true);
        }

        @SuppressWarnings("unused")
        @Fallback
        static JSObject doIncompatibleReceiver(Object iterator) {
            throw Errors.createTypeError("not an Array Iterator");
        }
    }

    @GenerateInline
    @GenerateCached(false)
    public abstract static class ArrayIteratorGetLengthNode extends JavaScriptBaseNode {

        public abstract long execute(Node node, Object array, JSContext context);

        @Specialization
        static long doArray(JSArrayObject array, @SuppressWarnings("unused") JSContext context) {
            return JSArray.arrayGetLength(array);
        }

        @Specialization
        static long doTypedArray(Node node, JSTypedArrayObject typedArray, JSContext context,
                        @Cached TypedArrayLengthNode typedArrayLengthNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (JSArrayBufferView.isOutOfBounds(typedArray, context)) {
                errorBranch.enter(node);
                throw Errors.createTypeError("Cannot perform Array Iterator.prototype.next on a detached ArrayBuffer");
            }
            return typedArrayLengthNode.execute(node, typedArray, context);
        }

        @Fallback
        static long doArrayLike(Object array, @SuppressWarnings("unused") JSContext context,
                        @Cached(value = "create(context)", inline = false) JSGetLengthNode getLengthNode) {
            return getLengthNode.executeLong(array);
        }
    }

    /**
     * Like {@link ArrayIteratorGetLengthNode}, but designed to avoid observable side effects. Only
     * handles JS arrays, typed arrays, and foreign arrays (assumes getArraySize to well-behaved).
     * Returns {@value JSRuntime#INVALID_INTEGER_INDEX} for other object types or if getting the
     * length would throw an error.
     */
    @ImportStatic(JSConfig.class)
    @GenerateInline
    @GenerateCached(false)
    public abstract static class ArrayIteratorGetLengthSafeNode extends JavaScriptBaseNode {

        public abstract long execute(Node node, Object array);

        @Specialization
        static long doArray(JSArrayObject array) {
            return JSArray.arrayGetLength(array);
        }

        @Specialization
        static long doTypedArray(Node node, JSTypedArrayObject typedArray,
                        @Cached TypedArrayLengthNode typedArrayLengthNode,
                        @Cached InlinedBranchProfile errorBranch) {
            JSContext context = JSContext.get(node);
            if (JSArrayBufferView.isOutOfBounds(typedArray, context)) {
                errorBranch.enter(node);
                // Must call into the next method to ensure correct stack trace for the error.
                return JSRuntime.INVALID_INTEGER_INDEX;
            }
            return typedArrayLengthNode.execute(node, typedArray, context);
        }

        @Specialization(guards = "isUndefined(array)")
        static long doUndefined(@SuppressWarnings("unused") Object array) {
            // Iterator is already done and detached.
            return 0L;
        }

        @Specialization(guards = {"interop.hasArrayElements(array)", "isForeignObject(array)"}, limit = "InteropLibraryLimit")
        static long doForeignArray(Object array,
                        @CachedLibrary("array") InteropLibrary interop) {
            try {
                return interop.getArraySize(array);
            } catch (UnsupportedMessageException e) {
                return JSRuntime.INVALID_INTEGER_INDEX;
            }
        }

        @Fallback
        static long doArrayLike(@SuppressWarnings("unused") Object array) {
            // Must call into the next method to ensure correct stack trace for length getters.
            return JSRuntime.INVALID_INTEGER_INDEX;
        }
    }
}

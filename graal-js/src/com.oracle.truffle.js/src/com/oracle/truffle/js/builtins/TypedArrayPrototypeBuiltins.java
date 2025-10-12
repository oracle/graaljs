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

import static com.oracle.truffle.js.runtime.array.TypedArray.BUFFER_TYPE_ARRAY;
import static com.oracle.truffle.js.runtime.array.TypedArray.BUFFER_TYPE_DIRECT;
import static com.oracle.truffle.js.runtime.array.TypedArray.BUFFER_TYPE_INTEROP;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetArrayType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferOperation;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.AbstractArraySortNode;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArrayForEachIndexCallOperation;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperation;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperationWithToInt;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayAtNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayCopyWithinNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayEveryNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFillNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFilterNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFindIndexNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFindNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayIncludesNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayIndexOfNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayJoinNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayMapNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayReduceNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySomeNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltins.DataViewGetNode.GetBufferElementNode;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltins.DataViewSetNode.SetBufferElementNode;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.GetTypedArrayBufferOrNameNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.GetTypedArrayLengthOrOffsetNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewFillNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewForEachNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewIteratorNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewReverseNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewSetNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewSubarrayNodeGen;
import com.oracle.truffle.js.builtins.sort.SortComparator;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResult;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResultNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.array.JSTypedArraySortNode;
import com.oracle.truffle.js.nodes.array.TypedArrayLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayIterator;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains %TypedArrayPrototype% methods.
 */
public final class TypedArrayPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TypedArrayPrototypeBuiltins.TypedArrayPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TypedArrayPrototypeBuiltins();

    protected TypedArrayPrototypeBuiltins() {
        super(JSArrayBufferView.PROTOTYPE_NAME, TypedArrayPrototype.class);
    }

    public enum TypedArrayPrototype implements BuiltinEnum<TypedArrayPrototype> {
        subarray(2),
        set(1),
        forEach(1),
        find(1),
        findIndex(1),
        fill(1),
        reduce(1),
        reduceRight(1),
        sort(1),
        slice(2),
        every(1),
        copyWithin(2),
        indexOf(1),
        lastIndexOf(1),
        filter(1),
        some(1),
        map(1),
        toLocaleString(0),
        join(1),
        reverse(0),
        keys(0),
        values(0),
        entries(0),

        // getters
        length(0),
        buffer(0),
        byteLength(0),
        byteOffset(0),
        _toStringTag(0) {
            @Override
            public Object getKey() {
                return Symbol.SYMBOL_TO_STRING_TAG;
            }
        },

        // ES2016
        includes(1),

        // ES2022
        at(1),

        // ES2023
        findLast(1),
        findLastIndex(1),
        toReversed(0),
        toSorted(1),
        with(2);

        private final int functionLength;

        TypedArrayPrototype(int length) {
            this.functionLength = length;
        }

        @Override
        public int getLength() {
            return functionLength;
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case includes -> JSConfig.ECMAScript2016;
                case at -> JSConfig.ECMAScript2022;
                case findLast, findLastIndex, toReversed, toSorted, with -> JSConfig.ECMAScript2023;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }

        @Override
        public boolean isGetter() {
            return EnumSet.range(length, _toStringTag).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TypedArrayPrototype builtinEnum) {
        switch (builtinEnum) {
            case subarray:
                return JSArrayBufferViewSubarrayNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case set:
                return JSArrayBufferViewSetNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case forEach:
                return JSArrayBufferViewForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case find:
                return JSArrayFindNodeGen.create(context, builtin, true, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findIndex:
                return JSArrayFindIndexNodeGen.create(context, builtin, true, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findLast:
                return JSArrayFindNodeGen.create(context, builtin, true, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findLastIndex:
                return JSArrayFindIndexNodeGen.create(context, builtin, true, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case fill:
                return context.getEcmaScriptVersion() >= JSConfig.ECMAScript2018
                                ? JSArrayBufferViewFillNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context))
                                : JSArrayFillNodeGen.create(context, builtin, true, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case reduce:
                return JSArrayReduceNodeGen.create(context, builtin, true, true, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case reduceRight:
                return JSArrayReduceNodeGen.create(context, builtin, true, false, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case slice:
                return TypedArrayPrototypeBuiltinsFactory.TypedArraySliceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case every:
                return JSArrayEveryNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case copyWithin:
                return JSArrayCopyWithinNodeGen.create(context, builtin, true, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case indexOf:
                return JSArrayIndexOfNodeGen.create(context, builtin, true, true, args().withThis().varArgs().createArgumentNodes(context));
            case lastIndexOf:
                return JSArrayIndexOfNodeGen.create(context, builtin, true, false, args().withThis().varArgs().createArgumentNodes(context));
            case filter:
                return JSArrayFilterNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case some:
                return JSArraySomeNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case map:
                return JSArrayMapNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case toLocaleString:
                return JSArrayToLocaleStringNodeGen.create(context, builtin, true, args().withThis().createArgumentNodes(context));
            case join:
                return JSArrayJoinNodeGen.create(context, builtin, true, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reverse:
                return JSArrayBufferViewReverseNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case keys:
                return JSArrayBufferViewIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_KEY, args().withThis().createArgumentNodes(context));
            case values:
                return JSArrayBufferViewIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_VALUE, args().withThis().createArgumentNodes(context));
            case entries:
                return JSArrayBufferViewIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE, args().withThis().createArgumentNodes(context));
            case includes:
                return JSArrayIncludesNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case at:
                return JSArrayAtNodeGen.create(context, builtin, true, args().withThis().fixedArgs(1).createArgumentNodes(context));

            case length:
            case byteLength:
            case byteOffset:
                return GetTypedArrayLengthOrOffsetNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
            case buffer:
            case _toStringTag:
                return GetTypedArrayBufferOrNameNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case toReversed:
                return ArrayPrototypeBuiltinsFactory.JSArrayToReversedNodeGen.create(context, builtin, true, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case sort:
            case toSorted:
                return TypedArrayPrototypeBuiltinsFactory.TypedArraySortMethodNodeGen.create(context, builtin, builtinEnum == TypedArrayPrototype.toSorted,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
            case with:
                return ArrayPrototypeBuiltinsFactory.JSArrayWithNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSArrayBufferViewSubarrayNode extends JSArrayBufferOperation {

        @Child private ArraySpeciesConstructorNode arraySpeciesCreateNode;

        public JSArrayBufferViewSubarrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        /**
         * TypedArray subarray(long begin, optional long end).
         *
         * Returns a new TypedArray view of the ArrayBuffer store for this TypedArray, referencing
         * the elements at begin, inclusive, up to end, exclusive. If either begin or end is
         * negative, it refers to an index from the end of the array, as opposed to from the
         * beginning.
         *
         * If end is unspecified, the subarray contains all elements from begin to the end of the
         * TypedArray.
         *
         * The range specified by the begin and end values is clamped to the valid index range for
         * the current array. If the computed length of the new TypedArray would be negative, it is
         * clamped to zero.
         *
         * The returned TypedArray will be of the same type as the array on which this method is
         * invoked.
         */
        @Specialization
        protected JSTypedArrayObject subarray(JSTypedArrayObject thisObj, Object start, Object end,
                        @Cached TypedArrayLengthNode typedArrayLength,
                        @Cached InlinedConditionProfile negativeBegin,
                        @Cached InlinedConditionProfile negativeEnd,
                        @Cached InlinedConditionProfile smallerEnd) {
            TypedArray array = typedArrayGetArrayType(thisObj);
            long srcLength = typedArrayLength.execute(this, thisObj, getContext());
            long relativeStart = toInteger(start);
            long startIndex = negativeBegin.profile(this, relativeStart < 0) ? Math.max(srcLength + relativeStart, 0) : Math.min(relativeStart, srcLength);
            int srcByteOffset = thisObj.getByteOffset();
            long beginByteOffset = srcByteOffset + startIndex * array.bytesPerElement();
            Object newLength;
            if (thisObj.hasAutoLength() && end == Undefined.instance) {
                newLength = Undefined.instance;
            } else {
                long relativeEnd = end == Undefined.instance ? srcLength : toInteger(end);
                long endIndex = negativeEnd.profile(this, relativeEnd < 0) ? Math.max(srcLength + relativeEnd, 0) : Math.min(relativeEnd, srcLength);
                if (smallerEnd.profile(this, endIndex < startIndex)) {
                    newLength = 0;
                } else {
                    newLength = (int) (endIndex - startIndex);
                }
            }
            return subarrayImpl(thisObj, (int) beginByteOffset, newLength);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected JSTypedArrayObject subarrayGeneric(Object thisObj, Object begin0, Object end0) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }

        protected JSTypedArrayObject subarrayImpl(JSTypedArrayObject thisObj, int beginByteOffset, Object newLength) {
            JSArrayBufferObject arrayBuffer = JSArrayBufferView.getArrayBuffer(thisObj);
            return getArraySpeciesConstructorNode().typedArraySpeciesCreate(thisObj, arrayBuffer, beginByteOffset, newLength);
        }

        protected ArraySpeciesConstructorNode getArraySpeciesConstructorNode() {
            if (arraySpeciesCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arraySpeciesCreateNode = insert(ArraySpeciesConstructorNode.create(true));
            }
            return arraySpeciesCreateNode;
        }
    }

    public abstract static class JSArrayBufferViewSetNode extends JSArrayBufferOperation {

        public JSArrayBufferViewSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        /**
         * void set(TypedArray array, optional unsigned long offset).
         *
         * void set(type[] array, optional unsigned long offset).
         *
         * Set multiple values, reading input values from the array.
         *
         * The optional offset value indicates the index in the current array where values are
         * written. If omitted, it is assumed to be 0.
         *
         * If the input array is a TypedArray, the two arrays may use the same underlying
         * ArrayBuffer. In this situation, setting the values takes place as if all the data is
         * first copied into a temporary buffer that does not overlap either of the arrays, and then
         * the data from the temporary buffer is copied into the current array.
         *
         * If the offset plus the length of the given array is out of range for the current
         * TypedArray, an exception is raised.
         *
         *
         * @param targetObj destination TypedArray
         * @param array source object
         * @param offset destination array offset
         * @return void
         */
        @Specialization
        protected Object set(JSTypedArrayObject targetObj, Object array, Object offset,
                        @Cached SetTypedArrayNode setTypedArrayNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (targetObj.getArrayBuffer().isImmutable()) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorImmutableBuffer();
            }
            long targetOffsetLong = toInteger(offset);
            if (targetOffsetLong < 0 || targetOffsetLong > Integer.MAX_VALUE) {
                errorBranch.enter(this);
                throw Errors.createRangeError("out of bounds");
            }
            int targetOffset = (int) targetOffsetLong;
            SetTypedArrayNode.checkOutOfBounds(targetObj, getContext(), errorBranch, this);
            setTypedArrayNode.execute(targetObj, array, targetOffset, getContext());
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected Object set(Object thisObj, Object array, Object offset) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }
    }

    abstract static class SetTypedArrayNode extends JavaScriptBaseNode {

        @Child private InteropLibrary interopLibrary;
        @Child private InteropLibrary getByteBufferInterop;
        @Child private JSToNumberNode toNumberNode;
        @Child private JSToBigIntNode toBigIntNode;

        abstract void execute(JSTypedArrayObject targetObj, Object array, int targetOffset, JSContext context);

        @Specialization
        void setTypedArrayFromTypedArray(JSTypedArrayObject targetObj, JSTypedArrayObject array, int targetOffset, JSContext context,
                        @Cached CopyTypedArrayElementsNode copyTypedArrayElementsNode,
                        @Shared @Cached TypedArrayLengthNode typedArrayLengthNode,
                        @Shared @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile sameBufferProf,
                        @Cached InlinedConditionProfile isDirectProf) {
            checkOutOfBounds(array, context, errorBranch, this);
            TypedArray sourceArray = array.getArrayType();
            TypedArray targetArray = targetObj.getArrayType();
            long sourceLength = typedArrayLengthNode.execute(this, array, context);
            long targetLength = typedArrayLengthNode.execute(this, targetObj, context);

            rangeCheck(0, sourceLength, targetOffset, targetLength, errorBranch, this);

            int sourceLen = (int) sourceLength;
            JSArrayBufferObject sourceBuffer = JSArrayBufferView.getArrayBuffer(array);
            JSArrayBufferObject targetBuffer = JSArrayBufferView.getArrayBuffer(targetObj);
            int srcByteOffset = array.getByteOffset();
            int targetByteOffset = targetObj.getByteOffset();

            int srcByteIndex;
            if (sameBufferProf.profile(this, sourceBuffer == targetBuffer)) {
                int srcByteLength = sourceLen * sourceArray.bytesPerElement();
                int targetByteIndex = targetByteOffset + targetOffset * targetArray.bytesPerElement();

                boolean cloneNotNeeded = srcByteOffset + srcByteLength <= targetByteIndex || targetByteIndex + srcByteLength <= srcByteOffset;
                if (cloneNotNeeded) {
                    srcByteIndex = srcByteOffset;
                } else {
                    sourceBuffer = cloneArrayBuffer(sourceBuffer, sourceArray, srcByteLength, srcByteOffset, context, isDirectProf, this);
                    if (sourceArray.isInterop()) {
                        getInterop(); // implicit branch profile
                        // cloned buffer is not an interop buffer anymore
                        sourceArray = sourceArray.getFactory().createArrayType(context.isOptionDirectByteBuffer() ? BUFFER_TYPE_DIRECT : BUFFER_TYPE_ARRAY, false, true);
                    }
                    srcByteIndex = 0;
                }
            } else {
                srcByteIndex = srcByteOffset;
            }
            copyTypedArrayElementsDistinctBuffers(targetBuffer, sourceBuffer, targetArray, sourceArray, targetOffset, targetByteOffset, sourceLen, srcByteIndex,
                            copyTypedArrayElementsNode, this);
        }

        @Specialization(guards = {"isJSFastArray(array)"})
        void setTypedArrayFromFastArray(JSTypedArrayObject targetObj, JSArrayObject array, int targetOffset, JSContext context,
                        @Shared @Cached TypedArrayLengthNode typedArrayLengthNode,
                        @Shared @Cached(parameters = "context") ReadElementNode readElementNode,
                        @Shared @Cached SetBufferElementTypeDispatchNode setBufferElementNode,
                        @Shared @Cached InlinedBranchProfile errorBranch) {
            TypedArray targetArray = targetObj.getArrayType();
            long sourceLen = JSArray.arrayGetLength(array);
            long targetLength = typedArrayLengthNode.execute(this, targetObj, context);
            rangeCheck(0, sourceLen, targetOffset, targetLength, errorBranch, this);
            int targetElementSize = targetArray.bytesPerElement();
            int targetBufferByteOffset = targetArray.getOffset(targetObj);
            JSArrayBufferObject targetBuffer = targetObj.getArrayBuffer();

            TypedArrayFactory targetArrayFactory = targetArray.getFactory();
            boolean isBigInt = targetArrayFactory.isBigInt();
            boolean littleEndian = ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder();
            for (long i = 0; i < sourceLen; i++) {
                Object value = readElementNode.executeWithTargetAndIndex(array, i);
                // IntegerIndexedElementSet
                Object numValue = isBigInt ? toBigInt(value) : toNumber(value);
                if (i < typedArrayLengthNode.execute(this, targetObj, context)) {
                    int targetIndex = (int) (targetBufferByteOffset + (targetOffset + i) * targetElementSize);
                    setBufferElementNode.execute(this, targetBuffer, targetIndex, littleEndian, numValue, targetArrayFactory, null);
                }
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(this, sourceLen);
        }

        @Fallback
        void setTypedArrayFromArrayLike(JSTypedArrayObject targetObj, Object array, int targetOffset, JSContext context,
                        @Shared @Cached TypedArrayLengthNode typedArrayLengthNode,
                        @Shared @Cached(parameters = {"context"}) ReadElementNode readElementNode,
                        @Shared @Cached SetBufferElementTypeDispatchNode setBufferElementNode,
                        @Shared @Cached InlinedBranchProfile errorBranch,
                        @Cached JSToObjectNode toObjectNode,
                        @Cached(parameters = {"context"}) JSGetLengthNode getLengthNode) {
            assert !(array instanceof JSTypedArrayObject) && !JSArray.isJSFastArray(array);
            TypedArray targetArray = targetObj.getArrayType();
            long targetLength = typedArrayLengthNode.execute(this, targetObj, context);
            Object src = toObjectNode.execute(array);
            long srcLength = getLengthNode.executeLong(src);
            rangeCheck(0, srcLength, targetOffset, targetLength, errorBranch, this);

            int targetElementSize = targetArray.bytesPerElement();
            int targetBufferByteOffset = targetArray.getOffset(targetObj);
            JSArrayBufferObject targetBuffer = targetObj.getArrayBuffer();

            TypedArrayFactory targetArrayFactory = targetArray.getFactory();
            boolean isBigInt = JSArrayBufferView.isBigIntArrayBufferView(targetObj);
            boolean littleEndian = ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder();
            for (int i = 0; i < srcLength; i++) {
                Object value = readElementNode.executeWithTargetAndIndex(array, i);
                // IntegerIndexedElementSet
                Object numValue = isBigInt ? toBigInt(value) : toNumber(value);
                if (i < typedArrayLengthNode.execute(this, targetObj, context)) {
                    int targetIndex = targetBufferByteOffset + (targetOffset + i) * targetElementSize;
                    setBufferElementNode.execute(this, targetBuffer, targetIndex, littleEndian, numValue, targetArrayFactory, null);
                }
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(this, srcLength);
        }

        protected Object toNumber(Object value) {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode.execute(value);
        }

        protected Object toBigInt(Object value) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            return toBigIntNode.execute(value);
        }

        private void copyTypedArrayElementsDistinctBuffers(JSArrayBufferObject targetBuffer, JSArrayBufferObject sourceBuffer, TypedArray targetType, TypedArray sourceType,
                        int targetOffset, int targetByteOffset, int sourceLength, int sourceByteIndex,
                        CopyTypedArrayElementsNode copyTypedArrayElementsNode, Node node) {
            int targetElementSize = targetType.bytesPerElement();
            int sourceElementSize = sourceType.bytesPerElement();
            int targetByteIndex = targetByteOffset + targetOffset * targetElementSize;

            /*
             * If we are able to get a ByteBuffer from the source and/or target interop buffer, then
             * use it directly instead of going through interop for each element.
             */
            ByteBuffer sourceInteropByteBuffer = null;
            if (sourceType.isInterop()) {
                sourceInteropByteBuffer = getByteBufferFromInteropBuffer(sourceBuffer);
            }
            ByteBuffer targetInteropByteBuffer = null;
            if (targetType.isInterop()) {
                targetInteropByteBuffer = getByteBufferFromInteropBuffer(targetBuffer);
            }

            copyTypedArrayElementsNode.execute(node, targetBuffer, sourceBuffer,
                            targetByteIndex, sourceByteIndex, sourceLength,
                            targetElementSize, sourceElementSize, targetType, sourceType,
                            sourceInteropByteBuffer, targetInteropByteBuffer, true);
            reportLoopCount(node, sourceLength);
        }

        private ByteBuffer getByteBufferFromInteropBuffer(JSArrayBufferObject interopBuffer) {
            if (getByteBufferInterop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getByteBufferInterop = insert(InteropLibrary.getFactory().createDispatched(1));
            }
            return JSInteropUtil.jsInteropBufferAsByteBuffer(interopBuffer, getByteBufferInterop, getRealm());
        }

        private JSArrayBufferObject cloneArrayBuffer(JSArrayBufferObject sourceBuffer, TypedArray sourceArray, int srcByteLength, int srcByteOffset, JSContext context,
                        InlinedConditionProfile isDirectProfile, Node node) {
            JSArrayBufferObject clonedArrayBuffer;
            if (sourceArray.isInterop()) {
                InteropLibrary interop = getInterop();
                clonedArrayBuffer = cloneInteropArrayBuffer(sourceBuffer, srcByteLength, srcByteOffset, context, interop);
            } else if (isDirectProfile.profile(node, sourceArray.isDirect())) {
                clonedArrayBuffer = JSArrayBuffer.createDirectArrayBuffer(context, getRealm(), srcByteLength);
                ByteBuffer clonedBackingBuffer = JSArrayBuffer.getDirectByteBuffer(clonedArrayBuffer);
                ByteBuffer sourceBackingBuffer = JSArrayBuffer.getDirectByteBuffer(sourceBuffer);
                Boundaries.byteBufferPutSlice(clonedBackingBuffer, 0, sourceBackingBuffer, srcByteOffset, srcByteOffset + srcByteLength);
            } else {
                clonedArrayBuffer = JSArrayBuffer.createArrayBuffer(context, getRealm(), srcByteLength);
                byte[] clonedBackingBuffer = JSArrayBuffer.getByteArray(clonedArrayBuffer);
                byte[] sourceBackingBuffer = JSArrayBuffer.getByteArray(sourceBuffer);
                System.arraycopy(sourceBackingBuffer, srcByteOffset, clonedBackingBuffer, 0, srcByteLength);
            }
            return clonedArrayBuffer;
        }

        private JSArrayBufferObject cloneInteropArrayBuffer(JSArrayBufferObject sourceBuffer, int srcByteLength, int srcByteOffset, JSContext context, InteropLibrary interop) {
            assert JSArrayBuffer.isJSInteropArrayBuffer(sourceBuffer);
            boolean direct = context.isOptionDirectByteBuffer();
            var sourceType = (TypedArray.TypedIntArray) TypedArrayFactory.Int8Array.createArrayType(BUFFER_TYPE_INTEROP, false, true);
            var clonedType = (TypedArray.TypedIntArray) TypedArrayFactory.Int8Array.createArrayType(direct ? BUFFER_TYPE_DIRECT : BUFFER_TYPE_ARRAY, false, true);
            JSArrayBufferObject clonedArrayBuffer = direct
                            ? JSArrayBuffer.createDirectArrayBuffer(context, getRealm(), srcByteLength)
                            : JSArrayBuffer.createArrayBuffer(context, getRealm(), srcByteLength);
            for (int i = 0; i < srcByteLength; i++) {
                int value = sourceType.getIntImpl(sourceBuffer, srcByteOffset, i, interop);
                clonedType.setIntImpl(clonedArrayBuffer, 0, i, value, interop);
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(this, srcByteLength);
            return clonedArrayBuffer;
        }

        private static void rangeCheck(long sourceStart, long sourceLength, long targetStart, long targetLength,
                        InlinedBranchProfile errorBranch, Node node) {
            if (!(sourceStart >= 0 && targetStart >= 0 && sourceStart <= sourceLength && targetStart <= targetLength && sourceLength - sourceStart <= targetLength - targetStart)) {
                errorBranch.enter(node);
                throw Errors.createRangeError("out of bounds");
            }
        }

        static void checkOutOfBounds(JSTypedArrayObject view, JSContext context,
                        InlinedBranchProfile errorBranch, Node node) {
            if (JSArrayBufferView.isOutOfBounds(view, context)) {
                errorBranch.enter(node);
                throw Errors.createTypeErrorOutOfBoundsTypedArray();
            }
        }

        private InteropLibrary getInterop() {
            InteropLibrary lib = interopLibrary;
            if (lib == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interopLibrary = lib = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            return lib;
        }
    }

    @GenerateCached(false)
    @GenerateInline
    abstract static class CopyTypedArrayElementsNode extends JavaScriptBaseNode {

        abstract void execute(Node node, JSArrayBufferObject targetBuffer, JSArrayBufferObject sourceBuffer,
                        int targetByteIndex, int sourceByteIndex, int sourceLength,
                        int targetElementSize, int sourceElementSize,
                        TypedArray targetType, TypedArray sourceType,
                        ByteBuffer sourceByteBuffer,
                        ByteBuffer targetByteBuffer,
                        boolean distinctBuffers);

        /**
         * If source element type == target element type, the copy must be performed in a manner
         * that preserves the bit-level encoding of the source data. This is not guaranteed for
         * float16 and float32 when copying elementwise due to the round-trip conversion to and from
         * double being lossy for NaN values.
         */
        @Specialization(guards = {"targetType.getFactory() == sourceType.getFactory()"})
        static void copyBytewise(Node node, JSArrayBufferObject targetBuffer, JSArrayBufferObject sourceBuffer,
                        int targetByteIndex, int sourceByteIndex, int sourceLength,
                        @SuppressWarnings("unused") int targetElementSize, int sourceElementSize,
                        TypedArray targetType, TypedArray sourceType,
                        ByteBuffer sourceInteropByteBuffer,
                        ByteBuffer targetInteropByteBuffer,
                        boolean distinctBuffers,
                        @Cached @Shared GetBufferElementTypeDispatchNode getBufferElementNode,
                        @Cached @Shared SetBufferElementTypeDispatchNode setBufferElementNode,
                        @Cached InlinedConditionProfile bothArraysBranch,
                        @Cached @Exclusive InlinedBranchProfile sameArrayBranch) {
            int sourceByteLength = sourceLength * sourceElementSize;
            if (bothArraysBranch.profile(node, targetType.isArray() && sourceType.isArray())) {
                byte[] sourceByteArray = JSArrayBuffer.getByteArray(sourceBuffer);
                byte[] targetByteArray = JSArrayBuffer.getByteArray(targetBuffer);
                if (distinctBuffers || sourceByteArray != targetByteArray) {
                    System.arraycopy(sourceByteArray, sourceByteIndex, targetByteArray, targetByteIndex, sourceByteLength);
                    return;
                } else {
                    sameArrayBranch.enter(node);
                }
            }

            ByteBuffer sourceByteBuffer;
            ByteBuffer targetByteBuffer;
            if (sourceType.isDirect()) {
                sourceByteBuffer = JSArrayBuffer.getDirectByteBuffer(sourceBuffer);
            } else if (sourceType.isInterop()) {
                sourceByteBuffer = sourceInteropByteBuffer;
            } else {
                assert sourceType.isArray();
                sourceByteBuffer = Boundaries.byteBufferWrap(JSArrayBuffer.getByteArray(sourceBuffer));
            }
            if (targetType.isDirect()) {
                targetByteBuffer = JSArrayBuffer.getDirectByteBuffer(targetBuffer);
            } else if (targetType.isInterop()) {
                targetByteBuffer = targetInteropByteBuffer;
            } else {
                assert targetType.isArray();
                targetByteBuffer = Boundaries.byteBufferWrap(JSArrayBuffer.getByteArray(targetBuffer));
            }
            // if buffers are distinct, try to perform a bulk copy
            if (distinctBuffers && (sourceByteBuffer != null && targetByteBuffer != null)) {
                Boundaries.byteBufferPutSlice(
                                targetByteBuffer, targetByteIndex,
                                sourceByteBuffer, sourceByteIndex,
                                sourceByteIndex + sourceByteLength);
                return;
            }

            // if we could not do a bulk copy, perform a bytewise copy
            boolean littleEndian = ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder();
            TypedArrayFactory sourceFactory = TypedArrayFactory.Uint8Array;
            TypedArrayFactory targetFactory = TypedArrayFactory.Uint8Array;
            for (int i = 0; i < sourceByteLength; i++) {
                Object value = getBufferElementNode.execute(node, sourceBuffer, sourceByteIndex + i, littleEndian, sourceFactory, sourceByteBuffer);
                setBufferElementNode.execute(node, targetBuffer, targetByteIndex + i, littleEndian, value, targetFactory, targetByteBuffer);
                TruffleSafepoint.poll(node);
            }
        }

        /**
         * If source element type != target element type, must do an elementwise copy.
         */
        @Specialization(guards = {"targetType.getFactory() != sourceType.getFactory()"})
        static void copyElementwise(Node node, JSArrayBufferObject targetBuffer, JSArrayBufferObject sourceBuffer,
                        int targetByteIndex, int sourceByteIndex, int sourceLength,
                        int targetElementSize, int sourceElementSize,
                        TypedArray targetType, TypedArray sourceType,
                        ByteBuffer sourceByteBuffer,
                        ByteBuffer targetByteBuffer,
                        @SuppressWarnings("unused") boolean distinctBuffers,
                        @Cached @Shared GetBufferElementTypeDispatchNode getBufferElementNode,
                        @Cached @Shared SetBufferElementTypeDispatchNode setBufferElementNode,
                        @Cached @Exclusive InlinedBranchProfile errorBranch) {
            if ((sourceType instanceof TypedArray.TypedBigIntArray) != (targetType instanceof TypedArray.TypedBigIntArray)) {
                errorBranch.enter(node);
                throw Errors.createTypeErrorCannotMixBigIntWithOtherTypes(node);
            }

            boolean littleEndian = ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder();
            TypedArrayFactory sourceFactory = sourceType.getFactory();
            TypedArrayFactory targetFactory = targetType.getFactory();
            for (int i = 0; i < sourceLength; i++) {
                Object value = getBufferElementNode.execute(node, sourceBuffer, sourceByteIndex + i * sourceElementSize, littleEndian, sourceFactory, sourceByteBuffer);
                setBufferElementNode.execute(node, targetBuffer, targetByteIndex + i * targetElementSize, littleEndian, value, targetFactory, targetByteBuffer);
                TruffleSafepoint.poll(node);
            }
        }
    }

    /**
     * Wrapper around {@link GetBufferElementNode}, dispatching on the typed array element type.
     */
    @GenerateInline
    @GenerateCached(false)
    @ImportStatic({TypedArrayFactory.class})
    public abstract static class GetBufferElementTypeDispatchNode extends JavaScriptBaseNode {
        public abstract Object execute(Node node, JSArrayBufferObject buffer, int bufferIndex, boolean littleEndian, TypedArrayFactory factory, ByteBuffer sourceByteBuffer);

        @Specialization(guards = {"factory == cachedFactory"}, limit = "NUMBER_OF_ELEMENT_TYPES")
        static Object doCached(Node node, JSArrayBufferObject buffer, int bufferIndex, boolean littleEndian,
                        @SuppressWarnings("unused") TypedArrayFactory factory, ByteBuffer byteBuffer,
                        @Cached("factory") TypedArrayFactory cachedFactory,
                        @Cached GetBufferElementNode getBufferElementNode) {
            return getBufferElementNode.execute(node, buffer, bufferIndex, littleEndian, cachedFactory, byteBuffer);
        }
    }

    /**
     * Wrapper around {@link SetBufferElementNode}, dispatching on the typed array element type.
     */
    @GenerateInline
    @GenerateCached(false)
    @ImportStatic({TypedArrayFactory.class})
    public abstract static class SetBufferElementTypeDispatchNode extends JavaScriptBaseNode {
        public abstract void execute(Node node, JSArrayBufferObject buffer, int bufferIndex, boolean littleEndian, Object value, TypedArrayFactory factory, ByteBuffer targetByteBuffer);

        @Specialization(guards = {"factory == cachedFactory"}, limit = "NUMBER_OF_ELEMENT_TYPES")
        static void doCached(Node node, JSArrayBufferObject buffer, int bufferIndex, boolean littleEndian, Object value,
                        @SuppressWarnings("unused") TypedArrayFactory factory, ByteBuffer byteBuffer,
                        @Cached("factory") TypedArrayFactory cachedFactory,
                        @Cached SetBufferElementNode setBufferElementNode) {
            setBufferElementNode.execute(node, buffer, bufferIndex, littleEndian, value, cachedFactory, byteBuffer);
        }
    }

    public abstract static class TypedArraySliceNode extends JSArrayOperation {

        @Child private InteropLibrary getByteBufferInterop;

        public TypedArraySliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization
        protected Object slice(Object thisObj, Object begin, Object end,
                        @Cached JSToIntegerAsLongNode toIntegerAsLong,
                        @Cached CopyTypedArrayElementsNode copyTypedArrayElements,
                        @Cached InlinedConditionProfile sizeIsZero,
                        @Cached InlinedConditionProfile offsetProfile1,
                        @Cached InlinedConditionProfile offsetProfile2) {
            JSTypedArrayObject sourceTypedArray = validateTypedArray(thisObj, false);
            long len = getLength(sourceTypedArray);
            long startPos = begin != Undefined.instance ? JSRuntime.getOffset(toIntegerAsLong.executeLong(begin), len, this, offsetProfile1) : 0;

            long endPos;
            if (end == Undefined.instance) {
                endPos = len;
            } else {
                endPos = JSRuntime.getOffset(toIntegerAsLong.executeLong(end), len, this, offsetProfile2);
            }

            long size = startPos <= endPos ? endPos - startPos : 0;
            JSTypedArrayObject resultTypedArray = getArraySpeciesConstructorNode().typedArraySpeciesCreateInWriteMode(sourceTypedArray, JSRuntime.longToIntOrDouble(size));
            if (sizeIsZero.profile(this, size > 0)) {
                checkOutOfBounds(sourceTypedArray);
                endPos = Math.min(endPos, getLength(sourceTypedArray));
                size = startPos <= endPos ? endPos - startPos : 0;

                TypedArray sourceType = sourceTypedArray.getArrayType();
                TypedArray targetType = resultTypedArray.getArrayType();
                JSArrayBufferObject sourceBuffer = sourceTypedArray.getArrayBuffer();
                JSArrayBufferObject targetBuffer = resultTypedArray.getArrayBuffer();
                int sourceElementSize = sourceType.bytesPerElement();
                int targetElementSize = targetType.bytesPerElement();
                int sourceByteOffset = sourceType.getOffset(sourceTypedArray);
                int targetByteOffset = targetType.getOffset(resultTypedArray);
                long sourceByteIndex = startPos * sourceElementSize + sourceByteOffset;
                assert JSRuntime.longIsRepresentableAsInt(size) && JSRuntime.longIsRepresentableAsInt(sourceByteIndex);

                /*
                 * If we are able to get a ByteBuffer from the source and/or target interop buffer,
                 * then use it directly instead of going through interop for each element.
                 */
                ByteBuffer sourceInteropByteBuffer = null;
                if (sourceType.isInterop()) {
                    sourceInteropByteBuffer = getByteBufferFromInteropBuffer(sourceBuffer);
                }
                ByteBuffer targetInteropByteBuffer = null;
                if (targetType.isInterop()) {
                    targetInteropByteBuffer = getByteBufferFromInteropBuffer(targetBuffer);
                }

                copyTypedArrayElements.execute(this, targetBuffer, sourceBuffer,
                                targetByteOffset, (int) sourceByteIndex, (int) size,
                                targetElementSize, sourceElementSize, targetType, sourceType,
                                sourceInteropByteBuffer, targetInteropByteBuffer, false);
                reportLoopCount(this, size);
            }
            return resultTypedArray;
        }

        private ByteBuffer getByteBufferFromInteropBuffer(JSArrayBufferObject interopBuffer) {
            if (getByteBufferInterop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getByteBufferInterop = insert(InteropLibrary.getFactory().createDispatched(1));
            }
            return JSInteropUtil.jsInteropBufferAsByteBuffer(interopBuffer, getByteBufferInterop, getRealm());
        }
    }

    public abstract static class JSArrayBufferViewForEachNode extends ArrayForEachIndexCallOperation {
        public JSArrayBufferViewForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object forEach(JSTypedArrayObject thisJSObj, Object callback, Object thisArg) {
            checkOutOfBounds(thisJSObj);
            long length = JSArrayBufferView.typedArrayGetArrayType(thisJSObj).length(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);
            return forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, Undefined.instance);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisJSObj)")
        protected Object forEachNonTypedArray(Object thisJSObj, Object callback, Object thisArg) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    return MaybeResult.continueResult(currentResult);
                }
            };
        }

        @Override
        protected boolean shouldCheckHasProperty() {
            return false;
        }

    }

    public abstract static class JSArrayBufferViewReverseNode extends JSArrayOperation {
        public JSArrayBufferViewReverseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization
        protected JSDynamicObject reverse(JSTypedArrayObject thisObj,
                        @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode) {
            if (thisObj.getArrayBuffer().isImmutable()) {
                errorBranch.enter();
                throw Errors.createTypeErrorImmutableBuffer();
            }
            checkOutOfBounds(thisObj);
            long len = getLength(thisObj);
            long middle = len / 2L;
            long lower = 0;

            while (lower != middle) {
                long upper = len - lower - 1;
                Object lowerValue = null;
                Object upperValue = null;
                boolean lowerExists = hasProperty(thisObj, lower);
                if (lowerExists) {
                    lowerValue = read(thisObj, lower);
                }
                boolean upperExists = hasProperty(thisObj, upper);
                if (upperExists) {
                    upperValue = read(thisObj, upper);
                }

                if (lowerExists && upperExists) {
                    write(thisObj, lower, upperValue);
                    write(thisObj, upper, lowerValue);
                } else if (upperExists) {
                    write(thisObj, lower, upperValue);
                    deletePropertyNode.executeEvaluated(thisObj, upper);
                } else if (lowerExists) {
                    deletePropertyNode.executeEvaluated(thisObj, lower);
                    write(thisObj, upper, lowerValue);
                }

                long nextLower = nextElementIndex(thisObj, lower, len);
                long nextUpper = previousElementIndex(thisObj, upper);
                if ((len - nextLower - 1) >= nextUpper) {
                    lower = nextLower;
                } else {
                    lower = len - nextUpper - 1;
                }
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(this, middle);
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisJSObj)")
        protected Object reverse(Object thisJSObj) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }
    }

    public abstract static class JSArrayBufferViewFillNode extends JSArrayOperationWithToInt {

        public JSArrayBufferViewFillNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization
        protected JSDynamicObject fill(Object thisObj, Object value, Object start, Object end,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached JSToBigIntNode toBigIntNode,
                        @Cached InlinedConditionProfile offsetProfile1,
                        @Cached InlinedConditionProfile offsetProfile2) {
            JSTypedArrayObject thisJSObj = validateTypedArray(thisObj, true);
            long len = getLength(thisJSObj);
            Object convValue = JSArrayBufferView.isBigIntArrayBufferView(thisJSObj)
                            ? toBigIntNode.execute(value)
                            : toNumberNode.execute(value);
            long lStart = JSRuntime.getOffset(toIntegerAsLong(start), len, this, offsetProfile1);
            long lEnd = end == Undefined.instance ? len : JSRuntime.getOffset(toIntegerAsLong(end), len, this, offsetProfile2);
            checkOutOfBounds(thisJSObj);
            for (long idx = lStart; idx < lEnd; idx++) {
                write(thisJSObj, idx, convValue);
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(this, lEnd - lStart);
            return thisJSObj;
        }
    }

    public abstract static class TypedArraySortMethodNode extends AbstractArraySortNode {

        private final boolean toSorted;

        protected TypedArraySortMethodNode(JSContext context, JSBuiltin builtin, boolean toSorted) {
            super(context, builtin, true);
            this.toSorted = toSorted;
        }

        @Specialization
        protected final JSTypedArrayObject sortTypedArray(Object thisObj, Object compare,
                        @Cached JSTypedArraySortNode typedArraySortNode,
                        @Cached TypedArrayLengthNode typedArrayLengthNode,
                        @Cached InlinedConditionProfile isCompareUndefined) {
            checkCompareCallableOrUndefined(compare);
            JSTypedArrayObject thisArray = validateTypedArray(thisObj, true);
            int len = typedArrayLengthNode.execute(this, thisArray, getContext());

            JSTypedArrayObject resultArray;
            if (toSorted) {
                resultArray = typedArrayCreateSameType(thisArray, len);
                if (len == 0) {
                    return resultArray;
                }
            } else {
                resultArray = thisArray;
                if (len <= 1) {
                    // nothing to do
                    return resultArray;
                }
            }

            Comparator<Object> comparator = isCompareUndefined.profile(this, compare == Undefined.instance) ? null : new SortComparator(compare);
            typedArraySortNode.execute(thisArray, resultArray, len, comparator);
            return resultArray;
        }
    }

    public abstract static class JSArrayBufferViewIteratorNode extends JSBuiltinNode {
        private final int iterationKind;

        public JSArrayBufferViewIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.iterationKind = iterationKind;
        }

        private JSObject createArrayIterator(Object thisObj) {
            return JSArrayIterator.create(getContext(), getRealm(), thisObj, 0L, iterationKind);
        }

        @Specialization
        protected final JSObject doObject(JSTypedArrayObject thisObj,
                        @Cached InlinedBranchProfile errorBranch) {
            if (JSArrayBufferView.isOutOfBounds(thisObj, getContext())) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorOutOfBoundsTypedArray();
            }
            return createArrayIterator(thisObj);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected static JSObject doNotObject(Object thisObj) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }
    }

    @ImportStatic({JSArrayBufferView.class})
    public abstract static class GetTypedArrayLengthOrOffsetNode extends JSBuiltinNode {

        private final TypedArrayPrototype getter;

        protected GetTypedArrayLengthOrOffsetNode(JSContext context, JSBuiltin builtin, TypedArrayPrototype getter) {
            super(context, builtin);
            this.getter = getter;
        }

        @Specialization(guards = {"!isOutOfBounds(typedArray, getContext())"})
        protected final int doTypedArray(JSTypedArrayObject typedArray,
                        @Cached TypedArrayLengthNode typedArrayLengthNode) {
            switch (getter) {
                case length:
                    return typedArrayLengthNode.execute(this, typedArray, getContext());
                case byteLength:
                    return typedArrayLengthNode.execute(this, typedArray, getContext()) << typedArray.getArrayType().bytesPerElementShift();
                case byteOffset:
                    return typedArray.getByteOffset();
                default:
                    throw Errors.shouldNotReachHere();
            }
        }

        @Specialization(guards = {"isOutOfBounds(typedArray, getContext())"})
        protected static int doTypedArrayOutOfBounds(@SuppressWarnings("unused") JSTypedArrayObject typedArray) {
            return 0;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected int doIncompatibleReceiver(Object thisObj) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }
    }

    public abstract static class GetTypedArrayBufferOrNameNode extends JSBuiltinNode {

        private final TypedArrayPrototype getter;

        protected GetTypedArrayBufferOrNameNode(JSContext context, JSBuiltin builtin, TypedArrayPrototype getter) {
            super(context, builtin);
            this.getter = getter;
        }

        @Specialization
        protected final Object doTypedArray(JSTypedArrayObject typedArray) {
            switch (getter) {
                case buffer:
                    return typedArray.getArrayBuffer();
                case _toStringTag:
                    return JSArrayBufferView.typedArrayGetName(typedArray);
                default:
                    throw Errors.shouldNotReachHere();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected Object doIncompatibleReceiver(Object thisObj) {
            if (getter == TypedArrayPrototype._toStringTag) {
                return Undefined.instance;
            }
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }
    }
}

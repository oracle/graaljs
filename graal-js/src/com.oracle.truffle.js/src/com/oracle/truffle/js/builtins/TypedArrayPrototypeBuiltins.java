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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetArrayType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferOperation;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferSliceNode;
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
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySliceNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySomeNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.GetTypedArrayBufferOrNameNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.GetTypedArrayLengthOrOffsetNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewFillNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewForEachNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewIteratorNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewReverseNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewSetNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewSubarrayNodeGen;
import com.oracle.truffle.js.builtins.sort.SortComparator;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResult;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResultNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.array.JSTypedArraySortNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
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
import com.oracle.truffle.js.runtime.array.ByteBufferAccess;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.ElementType;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayIterator;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

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
            if (this == includes) {
                return JSConfig.ECMAScript2016;
            } else if (this == at) {
                return JSConfig.ECMAScript2022;
            } else if (EnumSet.of(findLast, findLastIndex, toReversed, toSorted, with).contains(this)) {
                return JSConfig.ECMAScript2023;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
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
                return JSArraySliceNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
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
         *
         * @param thisObj TypedArray
         * @param begin begin index
         * @param end end index
         * @return subarray TypedArray
         */
        @Specialization
        protected JSTypedArrayObject subarray(JSTypedArrayObject thisObj, int begin, int end) {
            TypedArray array = typedArrayGetArrayType(thisObj);
            int length = (int) array.length(thisObj);
            int clampedBegin = JSArrayBufferSliceNode.clampIndex(begin, 0, length);
            int clampedEnd = JSArrayBufferSliceNode.clampIndex(end, clampedBegin, length);
            return subarrayImpl(thisObj, array, clampedBegin, clampedEnd);
        }

        @Specialization
        protected JSTypedArrayObject subarray(JSTypedArrayObject thisObj, Object begin0, Object end0,
                        @Cached InlinedConditionProfile negativeBegin,
                        @Cached InlinedConditionProfile negativeEnd,
                        @Cached InlinedConditionProfile smallerEnd) {
            TypedArray array = typedArrayGetArrayType(thisObj);
            long len = array.length(thisObj);
            long relativeBegin = toInteger(begin0);
            long beginIndex = negativeBegin.profile(this, relativeBegin < 0) ? Math.max(len + relativeBegin, 0) : Math.min(relativeBegin, len);
            long relativeEnd = end0 == Undefined.instance ? len : toInteger(end0);
            long endIndex = negativeEnd.profile(this, relativeEnd < 0) ? Math.max(len + relativeEnd, 0) : Math.min(relativeEnd, len);
            if (smallerEnd.profile(this, endIndex < beginIndex)) {
                endIndex = beginIndex;
            }
            return subarrayImpl(thisObj, array, (int) beginIndex, (int) endIndex);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected JSTypedArrayObject subarrayGeneric(Object thisObj, Object begin0, Object end0) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }

        protected JSTypedArrayObject subarrayImpl(JSTypedArrayObject thisObj, TypedArray arrayType, int begin, int end) {
            assert arrayType == JSArrayBufferView.typedArrayGetArrayType(thisObj);
            int offset = JSArrayBufferView.typedArrayGetOffset(thisObj);
            JSArrayBufferObject arrayBuffer = JSArrayBufferView.getArrayBuffer(thisObj);
            return getArraySpeciesConstructorNode().typedArraySpeciesCreate(thisObj, arrayBuffer, offset + begin * arrayType.bytesPerElement(), end - begin);
        }

        protected ArraySpeciesConstructorNode getArraySpeciesConstructorNode() {
            if (arraySpeciesCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arraySpeciesCreateNode = insert(ArraySpeciesConstructorNode.create(getContext(), true));
            }
            return arraySpeciesCreateNode;
        }
    }

    public abstract static class JSArrayBufferViewSetNode extends JSArrayBufferOperation {

        public JSArrayBufferViewSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile needErrorBranch = BranchProfile.create();
        private final ConditionProfile sameBufferProf = ConditionProfile.create();
        private final ValueProfile sourceArrayProf = ValueProfile.createIdentityProfile();
        private final ValueProfile targetArrayProf = ValueProfile.createIdentityProfile();
        private final JSClassProfile sourceArrayClassProfile = JSClassProfile.create();

        private final ConditionProfile srcIsJSObject = ConditionProfile.create();
        private final ConditionProfile arrayIsFastArray = ConditionProfile.create();
        private final ConditionProfile arrayIsArrayBufferView = ConditionProfile.create();
        private final ConditionProfile isDirectProfile = ConditionProfile.create();
        private final ConditionProfile sourceInteropBufferProfile = ConditionProfile.create();
        private final ConditionProfile targetInteropBufferProfile = ConditionProfile.create();
        private final BranchProfile intToIntBranch = BranchProfile.create();
        private final BranchProfile floatToFloatBranch = BranchProfile.create();
        private final BranchProfile bigIntToBigIntBranch = BranchProfile.create();
        private final BranchProfile objectToObjectBranch = BranchProfile.create();
        private final ValueProfile sourceTypeProfile = ValueProfile.createClassProfile();
        private final ValueProfile targetTypeProfile = ValueProfile.createClassProfile();
        private final ValueProfile sourceElemTypeProfile = ValueProfile.createIdentityProfile();
        private final ValueProfile targetElemTypeProfile = ValueProfile.createIdentityProfile();

        @Child private JSToObjectNode toObjectNode;
        @Child private JSGetLengthNode getLengthNode;
        @Child private InteropLibrary interopLibrary;
        @Child private InteropLibrary getByteBufferInterop;
        @Child private JSToNumberNode toNumberNode;
        @Child private JSToBigIntNode toBigIntNode;

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
        @Specialization(guards = "isJSArrayBufferView(targetObj)")
        protected Object set(JSDynamicObject targetObj, Object array, Object offset) {
            long targetOffsetLong = toInteger(offset);
            if (targetOffsetLong < 0 || targetOffsetLong > Integer.MAX_VALUE) {
                needErrorBranch.enter();
                throw Errors.createRangeError("out of bounds");
            }
            checkHasDetachedBuffer(targetObj);
            int targetOffset = (int) targetOffsetLong;
            if (arrayIsArrayBufferView.profile(JSArrayBufferView.isJSArrayBufferView(array))) {
                setArrayBufferView(targetObj, (JSDynamicObject) array, targetOffset);
            } else if (arrayIsFastArray.profile(JSArray.isJSFastArray(array))) {
                setFastArray(targetObj, (JSDynamicObject) array, targetOffset);
            } else {
                setOther(targetObj, array, targetOffset);
            }
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected Object set(Object thisObj, Object array, Object offset) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }

        private void setFastArray(JSDynamicObject thisObj, JSDynamicObject array, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(thisObj);
            assert JSArray.isJSFastArray(array);
            ScriptArray sourceArray = sourceArrayProf.profile(arrayGetArrayType(array));
            TypedArray targetArray = targetArrayProf.profile(JSArrayBufferView.typedArrayGetArrayType(thisObj));
            long sourceLen = sourceArray.length(array);
            rangeCheck(0, sourceLen, offset, targetArray.length(thisObj));

            boolean isBigInt = JSArrayBufferView.isBigIntArrayBufferView(thisObj);
            for (int i = 0, j = offset; i < sourceLen; i++, j++) {
                Object value = sourceArray.getElement(array, i);
                // IntegerIndexedElementSet
                Object numValue = isBigInt ? toBigInt(value) : toNumber(value);
                if (!JSArrayBufferView.hasDetachedBuffer(thisObj, getContext())) {
                    targetArray.setElement(thisObj, j, numValue, false);
                }
                TruffleSafepoint.poll(this);
            }
        }

        private void setOther(JSDynamicObject thisObj, Object array, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(thisObj);
            assert !JSArray.isJSFastArray(array);
            Object src = toObject(array);
            long srcLength = objectGetLength(src);
            TypedArray targetArray = targetArrayProf.profile(JSArrayBufferView.typedArrayGetArrayType(thisObj));

            rangeCheck(0, srcLength, offset, targetArray.length(thisObj));

            boolean isJSObject = JSDynamicObject.isJSDynamicObject(src);
            boolean isBigInt = JSArrayBufferView.isBigIntArrayBufferView(thisObj);
            for (int i = 0, j = offset; i < srcLength; i++, j++) {
                Object value;
                if (srcIsJSObject.profile(isJSObject)) {
                    value = JSObject.get((JSDynamicObject) src, i, sourceArrayClassProfile);
                } else {
                    value = JSInteropUtil.readArrayElementOrDefault(src, i, Undefined.instance);
                }
                // IntegerIndexedElementSet
                Object numValue = isBigInt ? toBigInt(value) : toNumber(value);
                if (!JSArrayBufferView.hasDetachedBuffer(thisObj, getContext())) {
                    targetArray.setElement(thisObj, j, numValue, false);
                }
                TruffleSafepoint.poll(this);
            }
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

        private void setArrayBufferView(JSDynamicObject targetView, JSDynamicObject sourceView, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(targetView);
            assert JSArrayBufferView.isJSArrayBufferView(sourceView);
            checkHasDetachedBuffer(sourceView);
            TypedArray sourceArray = sourceArrayProf.profile(typedArrayGetArrayType(sourceView));
            TypedArray targetArray = targetArrayProf.profile(typedArrayGetArrayType(targetView));
            long sourceLength = sourceArray.length(sourceView);

            rangeCheck(0, sourceLength, offset, targetArray.length(targetView));

            int sourceLen = (int) sourceLength;
            JSArrayBufferObject sourceBuffer = JSArrayBufferView.getArrayBuffer(sourceView);
            JSArrayBufferObject targetBuffer = JSArrayBufferView.getArrayBuffer(targetView);
            int srcByteOffset = JSArrayBufferView.typedArrayGetOffset(sourceView);
            int targetByteOffset = JSArrayBufferView.typedArrayGetOffset(targetView);

            int srcByteIndex;
            if (sameBufferProf.profile(sourceBuffer == targetBuffer)) {
                int srcByteLength = sourceLen * sourceArray.bytesPerElement();
                int targetByteIndex = targetByteOffset + offset * targetArray.bytesPerElement();

                boolean cloneNotNeeded = srcByteOffset + srcByteLength <= targetByteIndex || targetByteIndex + srcByteLength <= srcByteOffset;
                if (cloneNotNeeded) {
                    srcByteIndex = srcByteOffset;
                } else {
                    sourceBuffer = cloneArrayBuffer(sourceBuffer, sourceArray, srcByteLength, srcByteOffset);
                    if (sourceArray.isInterop()) {
                        // cloned buffer is not an interop buffer anymore
                        sourceArray = sourceArray.getFactory().createArrayType(getContext().isOptionDirectByteBuffer(), false);
                    }
                    srcByteIndex = 0;
                }
            } else {
                srcByteIndex = srcByteOffset;
            }
            copyTypedArrayElementsDistinctBuffers(targetBuffer, sourceBuffer, targetArray, sourceArray, offset, targetByteOffset, sourceLen, srcByteIndex);
        }

        @SuppressWarnings("unchecked")
        private void copyTypedArrayElementsDistinctBuffers(JSArrayBufferObject targetBuffer, JSArrayBufferObject sourceBuffer, TypedArray targetType, TypedArray sourceType,
                        int targetOffset, int targetByteOffset, int sourceLength, int sourceByteIndex) {
            int targetElementSize = targetType.bytesPerElement();
            int sourceElementSize = sourceType.bytesPerElement();
            int targetByteIndex = targetByteOffset + targetOffset * targetElementSize;
            ElementType sourceElemType = sourceType.getElementType();
            ElementType targetElemType = targetType.getElementType();

            ByteBuffer sourceInteropByteBuffer = null;
            if (sourceType.isInterop()) {
                sourceInteropByteBuffer = getByteBufferFromInteropBuffer(sourceBuffer);
            }
            ByteBuffer targetInteropByteBuffer = null;
            if (targetType.isInterop()) {
                targetInteropByteBuffer = getByteBufferFromInteropBuffer(targetBuffer);
            }

            if (sourceElemType == targetElemType) {
                // same element type => bulk copy (if possible)
                ByteBuffer sourceByteBuffer = null;
                ByteBuffer targetByteBuffer = null;
                byte[] sourceByteArray = null;
                byte[] targetByteArray = null;
                if (sourceType.isDirect()) {
                    sourceByteBuffer = JSArrayBuffer.getDirectByteBuffer(sourceBuffer);
                } else if (sourceType.isInterop()) {
                    sourceByteBuffer = sourceInteropByteBuffer;
                } else {
                    sourceByteArray = JSArrayBuffer.getByteArray(sourceBuffer);
                }
                if (targetType.isDirect()) {
                    targetByteBuffer = JSArrayBuffer.getDirectByteBuffer(targetBuffer);
                } else if (targetType.isInterop()) {
                    targetByteBuffer = targetInteropByteBuffer;
                } else {
                    targetByteArray = JSArrayBuffer.getByteArray(targetBuffer);
                }
                int sourceByteLength = sourceLength * sourceElementSize;
                if (sourceByteBuffer != null && targetByteBuffer != null) {
                    Boundaries.byteBufferPutSlice(
                                    targetByteBuffer, targetByteIndex,
                                    sourceByteBuffer, sourceByteIndex,
                                    sourceByteIndex + sourceByteLength);
                    return;
                } else if (sourceByteArray != null && targetByteArray != null) {
                    System.arraycopy(sourceByteArray, sourceByteIndex, targetByteArray, targetByteIndex, sourceByteLength);
                    return;
                }
            }

            if ((sourceType instanceof TypedArray.TypedBigIntArray) != (targetType instanceof TypedArray.TypedBigIntArray)) {
                needErrorBranch.enter();
                throw Errors.createTypeErrorCannotMixBigIntWithOtherTypes(this);
            }

            InteropLibrary interop = (sourceType.isInterop() || targetType.isInterop()) ? getInterop() : null;

            // If we are able to get ByteBuffer from an interop source/target buffer then
            // use it directly instead of going through interop for each element
            boolean hasSourceInteropByteBuffer = sourceInteropBufferProfile.profile(sourceInteropByteBuffer != null);
            boolean hasTargetInteropByteBuffer = targetInteropBufferProfile.profile(targetInteropByteBuffer != null);
            if (hasSourceInteropByteBuffer || hasTargetInteropByteBuffer) {
                boolean littleEndian = ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder();
                ByteBufferAccess bufferAccess = ByteBufferAccess.forOrder(littleEndian);
                for (int i = 0; i < sourceLength; i++) {
                    Object value;
                    int sourceIndex = sourceByteIndex + i * sourceElementSize;
                    if (hasSourceInteropByteBuffer) {
                        value = JSRuntime.getBufferElementDirect(bufferAccess, sourceInteropByteBuffer, sourceElemTypeProfile.profile(sourceElemType), sourceIndex);
                    } else {
                        value = sourceTypeProfile.profile(sourceType).getBufferElement(sourceBuffer, sourceIndex, littleEndian, interop);
                    }
                    int targetIndex = targetByteIndex + i * targetElementSize;
                    if (hasTargetInteropByteBuffer) {
                        JSRuntime.setBufferElementDirect(bufferAccess, targetInteropByteBuffer, targetElemTypeProfile.profile(targetElemType), targetIndex, value);
                    } else {
                        targetTypeProfile.profile(targetType).setBufferElement(targetBuffer, targetIndex, littleEndian, value, interop);
                    }
                    TruffleSafepoint.poll(this);
                }
                return;
            }

            // getIntImpl of Uint32 returns negative int for large values (which breaks clamping)
            if (sourceType instanceof TypedArray.TypedIntArray && targetType instanceof TypedArray.TypedIntArray &&
                            (sourceElemType != ElementType.Uint32 || targetElemType != ElementType.Uint8Clamped)) {
                intToIntBranch.enter();
                for (int i = 0; i < sourceLength; i++) {
                    int value = ((TypedArray.TypedIntArray) sourceType).getIntImpl(sourceBuffer, sourceByteIndex, i, interop);
                    ((TypedArray.TypedIntArray) targetType).setIntImpl(targetBuffer, targetByteOffset, i + targetOffset, value, interop);
                    TruffleSafepoint.poll(this);
                }
            } else if (sourceType instanceof TypedArray.TypedFloatArray && targetType instanceof TypedArray.TypedFloatArray) {
                floatToFloatBranch.enter();
                for (int i = 0; i < sourceLength; i++) {
                    double value = ((TypedArray.TypedFloatArray) sourceType).getDoubleImpl(sourceBuffer, sourceByteIndex, i, interop);
                    ((TypedArray.TypedFloatArray) targetType).setDoubleImpl(targetBuffer, targetByteOffset, i + targetOffset, value, interop);
                    TruffleSafepoint.poll(this);
                }
            } else if (sourceType instanceof TypedArray.TypedBigIntArray && targetType instanceof TypedArray.TypedBigIntArray) {
                bigIntToBigIntBranch.enter();
                for (int i = 0; i < sourceLength; i++) {
                    long value = ((TypedArray.TypedBigIntArray) sourceType).getLongImpl(sourceBuffer, sourceByteIndex, i, interop);
                    ((TypedArray.TypedBigIntArray) targetType).setLongImpl(targetBuffer, targetByteOffset, i + targetOffset, value, interop);
                    TruffleSafepoint.poll(this);
                }
            } else {
                objectToObjectBranch.enter();
                boolean littleEndian = ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder();
                for (int i = 0; i < sourceLength; i++) {
                    Object value = sourceType.getBufferElement(sourceBuffer, sourceByteIndex + i * sourceElementSize, littleEndian, interop);
                    targetType.setBufferElement(targetBuffer, targetByteIndex + i * targetElementSize, littleEndian, value, interop);
                    TruffleSafepoint.poll(this);
                }
            }
        }

        private ByteBuffer getByteBufferFromInteropBuffer(JSArrayBufferObject interopBuffer) {
            if (getByteBufferInterop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getByteBufferInterop = insert(InteropLibrary.getFactory().createDispatched(1));
            }
            return JSInteropUtil.wasmMemoryAsByteBuffer(interopBuffer, getByteBufferInterop, getRealm());
        }

        private JSArrayBufferObject cloneArrayBuffer(JSArrayBufferObject sourceBuffer, TypedArray sourceArray, int srcByteLength, int srcByteOffset) {
            JSArrayBufferObject clonedArrayBuffer;
            if (sourceArray.isInterop()) {
                InteropLibrary interop = getInterop();
                clonedArrayBuffer = cloneInteropArrayBuffer(sourceBuffer, srcByteLength, srcByteOffset, interop);
            } else if (isDirectProfile.profile(sourceArray.isDirect())) {
                clonedArrayBuffer = JSArrayBuffer.createDirectArrayBuffer(getContext(), getRealm(), srcByteLength);
                ByteBuffer clonedBackingBuffer = JSArrayBuffer.getDirectByteBuffer(clonedArrayBuffer);
                ByteBuffer sourceBackingBuffer = JSArrayBuffer.getDirectByteBuffer(sourceBuffer);
                Boundaries.byteBufferPutSlice(clonedBackingBuffer, 0, sourceBackingBuffer, srcByteOffset, srcByteOffset + srcByteLength);
            } else {
                clonedArrayBuffer = JSArrayBuffer.createArrayBuffer(getContext(), getRealm(), srcByteLength);
                byte[] clonedBackingBuffer = JSArrayBuffer.getByteArray(clonedArrayBuffer);
                byte[] sourceBackingBuffer = JSArrayBuffer.getByteArray(sourceBuffer);
                System.arraycopy(sourceBackingBuffer, srcByteOffset, clonedBackingBuffer, 0, srcByteLength);
            }
            return clonedArrayBuffer;
        }

        private JSArrayBufferObject cloneInteropArrayBuffer(JSArrayBufferObject sourceBuffer, int srcByteLength, int srcByteOffset, InteropLibrary interop) {
            assert JSArrayBuffer.isJSInteropArrayBuffer(sourceBuffer);
            boolean direct = getContext().isOptionDirectByteBuffer();
            TypedArray sourceType = TypedArrayFactory.Int8Array.createArrayType(false, false, true);
            TypedArray clonedType = TypedArrayFactory.Int8Array.createArrayType(direct, false);
            JSArrayBufferObject clonedArrayBuffer = direct
                            ? JSArrayBuffer.createDirectArrayBuffer(getContext(), getRealm(), srcByteLength)
                            : JSArrayBuffer.createArrayBuffer(getContext(), getRealm(), srcByteLength);
            for (int i = 0; i < srcByteLength; i++) {
                int value = ((TypedArray.TypedIntArray) sourceType).getIntImpl(sourceBuffer, srcByteOffset, i, interop);
                ((TypedArray.TypedIntArray) clonedType).setIntImpl(clonedArrayBuffer, 0, i, value, interop);
                TruffleSafepoint.poll(this);
            }
            return clonedArrayBuffer;
        }

        private void rangeCheck(long sourceStart, long sourceLength, long targetStart, long targetLength) {
            if (!(sourceStart >= 0 && targetStart >= 0 && sourceStart <= sourceLength && targetStart <= targetLength && sourceLength - sourceStart <= targetLength - targetStart)) {
                needErrorBranch.enter();
                throw Errors.createRangeError("out of bounds");
            }
        }

        private Object toObject(Object array) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.create());
            }
            return toObjectNode.execute(array);
        }

        private long objectGetLength(Object thisObject) {
            if (getLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLengthNode = insert(JSGetLengthNode.create(getContext()));
            }
            return getLengthNode.executeLong(thisObject);
        }

        private void checkHasDetachedBuffer(JSDynamicObject view) {
            if (JSArrayBufferView.hasDetachedBuffer(view, getContext())) {
                needErrorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
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

    public abstract static class JSArrayBufferViewForEachNode extends ArrayForEachIndexCallOperation {
        public JSArrayBufferViewForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSArrayBufferView(thisJSObj)")
        protected Object forEach(JSDynamicObject thisJSObj, Object callback, Object thisArg) {
            checkHasDetachedBuffer(thisJSObj);
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

        @Specialization(guards = "isJSArrayBufferView(thisObj)")
        protected JSDynamicObject reverse(JSDynamicObject thisObj,
                        @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deletePropertyNode) {
            checkHasDetachedBuffer(thisObj);
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
            validateTypedArray(thisObj);
            JSDynamicObject thisJSObj = (JSDynamicObject) thisObj;
            long len = getLength(thisJSObj);
            Object convValue = JSArrayBufferView.isBigIntArrayBufferView(thisJSObj)
                            ? toBigIntNode.execute(value)
                            : toNumberNode.execute(value);
            long lStart = JSRuntime.getOffset(toIntegerAsLong(start), len, this, offsetProfile1);
            long lEnd = end == Undefined.instance ? len : JSRuntime.getOffset(toIntegerAsLong(end), len, this, offsetProfile2);
            checkHasDetachedBuffer(thisJSObj);
            for (long idx = lStart; idx < lEnd; idx++) {
                write(thisJSObj, idx, convValue);
                TruffleSafepoint.poll(this);
            }
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
                        @Cached InlinedConditionProfile isCompareUndefined) {
            checkCompareCallableOrUndefined(compare);
            JSTypedArrayObject thisArray = validateTypedArray(thisObj);
            long len = getLength(thisArray);

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
            if (JSArrayBufferView.hasDetachedBuffer(thisObj, getContext())) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorDetachedBuffer();
            }
            return createArrayIterator(thisObj);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected static JSObject doNotObject(Object thisObj) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }
    }

    public abstract static class GetTypedArrayLengthOrOffsetNode extends JSBuiltinNode {

        private final TypedArrayPrototype getter;

        protected GetTypedArrayLengthOrOffsetNode(JSContext context, JSBuiltin builtin, TypedArrayPrototype getter) {
            super(context, builtin);
            this.getter = getter;
        }

        @Specialization
        protected final int doTypedArray(JSTypedArrayObject typedArray,
                        @Cached InlinedBranchProfile detachedBranch) {
            if (JSArrayBufferView.hasDetachedBuffer(typedArray, getContext())) {
                detachedBranch.enter(this);
                return 0;
            }
            switch (getter) {
                case length:
                    return JSArrayBufferView.typedArrayGetLength(typedArray);
                case byteLength:
                    return JSArrayBufferView.getByteLength(typedArray, getContext());
                case byteOffset:
                    return JSArrayBufferView.getByteOffset(typedArray, getContext());
                default:
                    throw Errors.shouldNotReachHere();
            }
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
                    return JSArrayBufferView.getArrayBuffer(typedArray);
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

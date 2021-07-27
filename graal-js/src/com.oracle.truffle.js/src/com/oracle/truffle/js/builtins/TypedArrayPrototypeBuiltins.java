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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetArrayType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferOperation;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferSliceNode;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArrayForEachIndexCallOperation;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.CreateArrayIteratorNode;
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
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySortNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewFillNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewForEachNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewIteratorNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewReverseNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewSetNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltinsFactory.JSArrayBufferViewSubarrayNodeGen;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResult;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResultNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
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
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
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

        // ES2016
        includes(1),

        // ES2022
        at(1);

        private final int length;

        TypedArrayPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (this == includes) {
                return JSConfig.ECMAScript2016;
            } else if (this == at) {
                return JSConfig.ECMAScript2022;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
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
                return JSArrayFindNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findIndex:
                return JSArrayFindIndexNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case fill:
                return context.getEcmaScriptVersion() >= 9 ? JSArrayBufferViewFillNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context))
                                : JSArrayFillNodeGen.create(context, builtin, true, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case reduce:
                return JSArrayReduceNodeGen.create(context, builtin, true, true, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case reduceRight:
                return JSArrayReduceNodeGen.create(context, builtin, true, false, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case sort:
                return JSArraySortNodeGen.create(context, builtin, true, args().withThis().fixedArgs(1).createArgumentNodes(context));
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
        @Specialization(guards = "isJSArrayBufferView(thisObj)")
        protected DynamicObject subarray(DynamicObject thisObj, int begin, int end,
                        @Cached("createIdentityProfile()") ValueProfile arrayTypeProfile) {
            TypedArray array = arrayTypeProfile.profile(typedArrayGetArrayType(thisObj));
            int length = (int) array.length(thisObj);
            int clampedBegin = JSArrayBufferSliceNode.clampIndex(begin, 0, length);
            int clampedEnd = JSArrayBufferSliceNode.clampIndex(end, clampedBegin, length);
            return subarrayImpl(thisObj, array, clampedBegin, clampedEnd);
        }

        @Specialization(guards = "isJSArrayBufferView(thisObj)")
        protected DynamicObject subarray(DynamicObject thisObj, Object begin0, Object end0,
                        @Cached("createIdentityProfile()") ValueProfile arrayTypeProfile,
                        @Cached("createBinaryProfile()") ConditionProfile negativeBegin,
                        @Cached("createBinaryProfile()") ConditionProfile negativeEnd,
                        @Cached("createBinaryProfile()") ConditionProfile smallerEnd) {
            TypedArray array = arrayTypeProfile.profile(typedArrayGetArrayType(thisObj));
            long len = array.length(thisObj);
            long relativeBegin = toInteger(begin0);
            long beginIndex = negativeBegin.profile(relativeBegin < 0) ? Math.max(len + relativeBegin, 0) : Math.min(relativeBegin, len);
            long relativeEnd = end0 == Undefined.instance ? len : toInteger(end0);
            long endIndex = negativeEnd.profile(relativeEnd < 0) ? Math.max(len + relativeEnd, 0) : Math.min(relativeEnd, len);
            if (smallerEnd.profile(endIndex < beginIndex)) {
                endIndex = beginIndex;
            }
            return subarrayImpl(thisObj, array, (int) beginIndex, (int) endIndex);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected DynamicObject subarrayGeneric(Object thisObj, Object begin0, Object end0) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }

        protected DynamicObject subarrayImpl(DynamicObject thisObj, TypedArray arrayType, int begin, int end) {
            assert arrayType == JSArrayBufferView.typedArrayGetArrayType(thisObj);
            int offset = JSArrayBufferView.typedArrayGetOffset(thisObj);
            DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(thisObj);
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
        private final ConditionProfile sameBufferProf = ConditionProfile.createBinaryProfile();
        private final ValueProfile sourceArrayProf = ValueProfile.createIdentityProfile();
        private final ValueProfile targetArrayProf = ValueProfile.createIdentityProfile();
        private final JSClassProfile sourceArrayClassProfile = JSClassProfile.create();

        private final ConditionProfile srcIsJSObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayIsFastArray = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayIsArrayBufferView = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isDirectProf = ConditionProfile.createBinaryProfile();
        private final BranchProfile intToIntBranch = BranchProfile.create();
        private final BranchProfile floatToFloatBranch = BranchProfile.create();
        private final BranchProfile bigIntToBigIntBranch = BranchProfile.create();
        private final BranchProfile objectToObjectBranch = BranchProfile.create();

        @Child private JSToObjectNode toObjectNode;
        @Child private JSGetLengthNode getLengthNode;
        @Child private InteropLibrary interopLibrary;

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
        protected Object set(DynamicObject targetObj, Object array, Object offset) {
            long targetOffsetLong = toInteger(offset);
            if (targetOffsetLong < 0 || targetOffsetLong > Integer.MAX_VALUE) {
                needErrorBranch.enter();
                throw Errors.createRangeError("out of bounds");
            }
            checkHasDetachedBuffer(targetObj);
            int targetOffset = (int) targetOffsetLong;
            if (arrayIsArrayBufferView.profile(JSArrayBufferView.isJSArrayBufferView(array))) {
                setArrayBufferView(targetObj, (DynamicObject) array, targetOffset);
            } else if (arrayIsFastArray.profile(JSArray.isJSFastArray(array))) {
                setFastArray(targetObj, (DynamicObject) array, targetOffset);
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

        private void setFastArray(DynamicObject thisObj, DynamicObject array, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(thisObj);
            assert JSArray.isJSFastArray(array);
            ScriptArray sourceArray = sourceArrayProf.profile(arrayGetArrayType(array));
            TypedArray targetArray = targetArrayProf.profile(JSArrayBufferView.typedArrayGetArrayType(thisObj));
            long sourceLen = sourceArray.length(array);
            rangeCheck(0, sourceLen, offset, targetArray.length(thisObj));

            for (int i = 0, j = offset; i < sourceLen; i++, j++) {
                targetArray.setElement(thisObj, j, sourceArray.getElement(array, i), false);
                TruffleSafepoint.poll(this);
            }
        }

        private void setOther(DynamicObject thisObj, Object array, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(thisObj);
            assert !JSArray.isJSFastArray(array);
            if (getContext().isOptionV8CompatibilityMode() && JSRuntime.isNumber(array)) {
                throw Errors.createTypeError("invalid_argument");
            }
            Object src = toObject(array);
            long srcLength = objectGetLength(src);
            TypedArray targetArray = targetArrayProf.profile(JSArrayBufferView.typedArrayGetArrayType(thisObj));

            rangeCheck(0, srcLength, offset, targetArray.length(thisObj));

            boolean isJSObject = JSDynamicObject.isJSDynamicObject(src);
            for (int i = 0, j = offset; i < srcLength; i++, j++) {
                Object value;
                if (srcIsJSObject.profile(isJSObject)) {
                    value = JSObject.get((DynamicObject) src, i, sourceArrayClassProfile);
                } else {
                    value = JSInteropUtil.readArrayElementOrDefault(src, i, Undefined.instance);
                }
                checkHasDetachedBuffer(thisObj);
                targetArray.setElement(thisObj, j, value, false);
                TruffleSafepoint.poll(this);
            }
        }

        private void setArrayBufferView(DynamicObject targetView, DynamicObject sourceView, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(targetView);
            assert JSArrayBufferView.isJSArrayBufferView(sourceView);
            checkHasDetachedBuffer(sourceView);
            TypedArray sourceArray = sourceArrayProf.profile(typedArrayGetArrayType(sourceView));
            TypedArray targetArray = targetArrayProf.profile(typedArrayGetArrayType(targetView));
            long sourceLength = sourceArray.length(sourceView);

            rangeCheck(0, sourceLength, offset, targetArray.length(targetView));

            int sourceLen = (int) sourceLength;
            DynamicObject sourceBuffer = JSArrayBufferView.getArrayBuffer(sourceView);
            DynamicObject targetBuffer = JSArrayBufferView.getArrayBuffer(targetView);
            int srcByteOffset = JSArrayBufferView.typedArrayGetOffset(sourceView);
            int targetByteOffset = JSArrayBufferView.typedArrayGetOffset(targetView);

            int srcByteIndex;
            if (sameBufferProf.profile(sourceBuffer == targetBuffer)) {
                int srcByteLength = sourceLen * sourceArray.bytesPerElement();
                sourceBuffer = cloneArrayBuffer(sourceBuffer, sourceArray, srcByteLength, srcByteOffset);
                srcByteIndex = 0;
            } else {
                srcByteIndex = srcByteOffset;
            }
            copyTypedArrayElementsDistinctBuffers(targetBuffer, sourceBuffer, targetArray, sourceArray, offset, targetByteOffset, sourceLen, srcByteIndex);
        }

        @SuppressWarnings("unchecked")
        private void copyTypedArrayElementsDistinctBuffers(DynamicObject targetBuffer, DynamicObject sourceBuffer, TypedArray targetType, TypedArray sourceType,
                        int targetOffset, int targetByteOffset, int sourceLength, int sourceByteIndex) {
            int targetElementSize = targetType.bytesPerElement();
            int sourceElementSize = sourceType.bytesPerElement();
            int targetByteIndex = targetByteOffset + targetOffset * targetElementSize;
            InteropLibrary interop = (sourceType.isInterop() || targetType.isInterop()) ? getInterop() : null;
            if (sourceType == targetType && !sourceType.isInterop()) {
                // same element type => bulk copy
                int sourceByteLength = sourceLength * sourceElementSize;
                if (isDirectProf.profile(targetType.isDirect())) {
                    Boundaries.byteBufferPutSlice(
                                    JSArrayBuffer.getDirectByteBuffer(targetBuffer), targetByteIndex,
                                    JSArrayBuffer.getDirectByteBuffer(sourceBuffer), sourceByteIndex,
                                    sourceByteIndex + sourceByteLength);
                } else {
                    System.arraycopy(JSArrayBuffer.getByteArray(sourceBuffer), sourceByteIndex, JSArrayBuffer.getByteArray(targetBuffer), targetByteIndex, sourceByteLength);
                }
            } else if (sourceType instanceof TypedArray.TypedIntArray && targetType instanceof TypedArray.TypedIntArray) {
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
            } else if ((sourceType instanceof TypedArray.TypedBigIntArray) != (targetType instanceof TypedArray.TypedBigIntArray)) {
                needErrorBranch.enter();
                throw Errors.createTypeErrorCannotMixBigIntWithOtherTypes(this);
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

        private DynamicObject cloneArrayBuffer(DynamicObject sourceBuffer, TypedArray sourceArray, int srcByteLength, int srcByteOffset) {
            DynamicObject clonedArrayBuffer;
            if (sourceArray.isInterop()) {
                InteropLibrary interop = getInterop();
                clonedArrayBuffer = cloneInteropArrayBuffer(sourceBuffer, srcByteLength, srcByteOffset, interop);
            } else if (isDirectProf.profile(sourceArray.isDirect())) {
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

        private DynamicObject cloneInteropArrayBuffer(DynamicObject sourceBuffer, int srcByteLength, int srcByteOffset, InteropLibrary interop) {
            assert JSArrayBuffer.isJSInteropArrayBuffer(sourceBuffer);
            boolean direct = getContext().isOptionDirectByteBuffer();
            TypedArray sourceType = TypedArrayFactory.Int8Array.createArrayType(false, false, true);
            TypedArray clonedType = TypedArrayFactory.Int8Array.createArrayType(direct, false);
            DynamicObject clonedArrayBuffer = direct
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
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
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

        private void checkHasDetachedBuffer(DynamicObject view) {
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
        protected Object forEach(DynamicObject thisJSObj, Object callback, Object thisArg) {
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
        protected DynamicObject reverse(DynamicObject thisObj,
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
        private final ConditionProfile offsetProfile1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile2 = ConditionProfile.createBinaryProfile();
        @Child private JSToNumberNode toNumberNode;
        @Child private JSToBigIntNode toBigIntNode;

        public JSArrayBufferViewFillNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization
        protected DynamicObject fill(Object thisObj, Object value, Object start, Object end) {
            validateTypedArray(thisObj);
            DynamicObject thisJSObj = (DynamicObject) thisObj;
            long len = getLength(thisJSObj);
            Object convValue = JSArrayBufferView.isBigIntArrayBufferView(thisJSObj) ? toBigInt(value) : toNumber(value);
            long lStart = JSRuntime.getOffset(toIntegerAsLong(start), len, offsetProfile1);
            long lEnd = end == Undefined.instance ? len : JSRuntime.getOffset(toIntegerAsLong(end), len, offsetProfile2);
            checkHasDetachedBuffer(thisJSObj);
            for (long idx = lStart; idx < lEnd; idx++) {
                write(thisJSObj, idx, convValue);
                TruffleSafepoint.poll(this);
            }
            return thisJSObj;
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
    }

    public abstract static class JSArrayBufferViewIteratorNode extends JSBuiltinNode {
        @Child private CreateArrayIteratorNode createArrayIteratorNode;
        private final BranchProfile errorBranch = BranchProfile.create();

        public JSArrayBufferViewIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.createArrayIteratorNode = CreateArrayIteratorNode.create(context, iterationKind);
        }

        @Specialization(guards = "isJSArrayBufferView(thisObj)")
        protected DynamicObject doObject(VirtualFrame frame, DynamicObject thisObj) {
            checkHasDetachedBuffer(thisObj);
            return createArrayIteratorNode.execute(frame, thisObj);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected DynamicObject doNotObject(Object thisObj) {
            throw Errors.createTypeErrorArrayBufferViewExpected();
        }

        protected final void checkHasDetachedBuffer(DynamicObject view) {
            if (JSArrayBufferView.hasDetachedBuffer(view, getContext())) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }
    }
}

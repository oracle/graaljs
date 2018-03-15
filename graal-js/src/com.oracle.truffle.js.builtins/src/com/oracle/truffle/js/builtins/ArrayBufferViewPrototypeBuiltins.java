/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetArrayType;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferOperation;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferSliceNode;
import com.oracle.truffle.js.builtins.ArrayBufferViewPrototypeBuiltinsFactory.JSArrayBufferViewFillNodeGen;
import com.oracle.truffle.js.builtins.ArrayBufferViewPrototypeBuiltinsFactory.JSArrayBufferViewForEachNodeGen;
import com.oracle.truffle.js.builtins.ArrayBufferViewPrototypeBuiltinsFactory.JSArrayBufferViewReverseNodeGen;
import com.oracle.truffle.js.builtins.ArrayBufferViewPrototypeBuiltinsFactory.JSArrayBufferViewSetNodeGen;
import com.oracle.truffle.js.builtins.ArrayBufferViewPrototypeBuiltinsFactory.JSArrayBufferViewSubarrayNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArrayForEachIndexCallOperation;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperation;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperationWithToInt;
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
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToStringNodeGen;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResult;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResultNode;
import com.oracle.truffle.js.nodes.access.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * Contains builtins for {@linkplain JSArrayBuffer}.prototype.
 */
public final class ArrayBufferViewPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ArrayBufferViewPrototypeBuiltins.ArrayBufferViewPrototype> {
    protected ArrayBufferViewPrototypeBuiltins() {
        super(JSArrayBufferView.PROTOTYPE_NAME, ArrayBufferViewPrototype.class);
    }

    public enum ArrayBufferViewPrototype implements BuiltinEnum<ArrayBufferViewPrototype> {
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
        toString(0),
        toLocaleString(0),
        join(1),
        reverse(0),

        // ES7
        includes(1);

        private final int length;

        ArrayBufferViewPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (this == includes) {
                return 7;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ArrayBufferViewPrototype builtinEnum) {
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
                return JSArraySliceNodeGen.create(context, builtin, true, args().withThis().varArgs().createArgumentNodes(context));
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
            case toString:
                return JSArrayToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toLocaleString:
                return JSArrayToLocaleStringNodeGen.create(context, builtin, true, args().withThis().createArgumentNodes(context));
            case join:
                return JSArrayJoinNodeGen.create(context, builtin, true, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reverse:
                return JSArrayBufferViewReverseNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case includes:
                return JSArrayIncludesNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
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
            boolean condition = JSArrayBufferView.isJSArrayBufferView(thisObj);
            TypedArray array = arrayTypeProfile.profile(typedArrayGetArrayType(thisObj, condition));
            int length = (int) array.length(thisObj, condition);
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
            boolean condition = JSArrayBufferView.isJSArrayBufferView(thisObj);
            TypedArray array = arrayTypeProfile.profile(typedArrayGetArrayType(thisObj, condition));
            long len = array.length(thisObj, condition);
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
            DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(thisObj, JSArrayBufferView.isJSArrayBufferView(thisObj));
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
        private final ConditionProfile sinkEqualsSource = ConditionProfile.createBinaryProfile();
        private final ValueProfile sourceArrayProf = ValueProfile.createIdentityProfile();
        private final ValueProfile sinkArrayProf = ValueProfile.createIdentityProfile();
        private final JSClassProfile sourceArrayClassProfile = JSClassProfile.create();

        private final ConditionProfile arrayIsObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayIsArray = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayIsArrayBufferView = ConditionProfile.createBinaryProfile();
        private final BranchProfile intToIntBranch = BranchProfile.create();
        private final BranchProfile floatToFloatBranch = BranchProfile.create();
        private final BranchProfile objectToObjectBranch = BranchProfile.create();

        @Child private JSGetLengthNode getLengthNode;

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
         * @param source source TypedArray or type[]
         * @param offset destination array offset
         * @return void
         */
        @Specialization(guards = "isJSArrayBufferView(targetObj)")
        protected Object set(DynamicObject targetObj, Object source, Object offset) {
            if (arrayIsObject.profile(JSObject.isDynamicObject(source))) {
                DynamicObject sourceObj = (DynamicObject) source;
                long targetOffsetLong = toInteger(offset);
                if (targetOffsetLong < 0 || targetOffsetLong > Integer.MAX_VALUE) {
                    throw Errors.createRangeError("out of bounds");
                }
                checkHasDetachedBuffer(targetObj);
                int targetOffset = (int) targetOffsetLong;
                if (arrayIsArrayBufferView.profile(JSArrayBufferView.isJSArrayBufferView(sourceObj))) {
                    setArrayBufferView(targetObj, sourceObj, targetOffset);
                } else if (arrayIsArray.profile(JSArray.isJSArray(sourceObj))) {
                    setArray(targetObj, sourceObj, targetOffset);
                } else {
                    setObject(targetObj, sourceObj, targetOffset);
                }
                return Undefined.instance;
            }
            throw Errors.createTypeError("array expected");
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSArrayBufferView(thisObj)")
        protected Object set(Object thisObj, Object array, Object offset) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }

        private void setArray(DynamicObject thisObj, DynamicObject array, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(thisObj);
            assert JSArray.isJSArray(array);
            boolean sourceCondition = JSArray.isJSArray(array);
            boolean sinkCondition = JSArrayBufferView.isJSArrayBufferView(thisObj);
            ScriptArray sourceArray = arrayGetArrayType(array, sourceCondition);
            TypedArray sinkArray = JSArrayBufferView.typedArrayGetArrayType(thisObj, sinkCondition);
            long sourceLen = sourceArray.length(array, sourceCondition);
            rangeCheck(0, sourceLen, offset, sinkArray.length(thisObj, sinkCondition));

            for (int i = 0, j = offset; i < sourceLen; i++, j++) {
                sinkArray.setElement(thisObj, j, sourceArray.getElement(array, i), false);
            }
        }

        private void setObject(DynamicObject thisObj, DynamicObject array, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(thisObj);
            assert !JSArray.isJSArray(array);
            boolean sinkCondition = JSArrayBufferView.isJSArrayBufferView(thisObj);
            long len = objectGetLength(array);
            TypedArray sinkArray = sinkArrayProf.profile(JSArrayBufferView.typedArrayGetArrayType(thisObj, sinkCondition));

            rangeCheck(0, len, offset, sinkArray.length(thisObj, sinkCondition));

            for (int i = 0, j = offset; i < len; i++, j++) {
                Object value = JSObject.get(array, i, sourceArrayClassProfile);
                checkHasDetachedBuffer(thisObj);
                sinkArray.setElement(thisObj, j, value, false);
            }
        }

        private void setArrayBufferView(DynamicObject sinkView, DynamicObject sourceView, int offset) {
            assert JSArrayBufferView.isJSArrayBufferView(sinkView);
            assert JSArrayBufferView.isJSArrayBufferView(sourceView);
            checkHasDetachedBuffer(sourceView);
            boolean sourceCondition = JSArrayBufferView.isJSArrayBufferView(sourceView);
            TypedArray sourceArray = sourceArrayProf.profile(typedArrayGetArrayType(sourceView, sourceCondition));
            boolean sinkCondition = JSArrayBufferView.isJSArrayBufferView(sinkView);
            TypedArray sinkArray = sinkArrayProf.profile(typedArrayGetArrayType(sinkView, sinkCondition));
            rangeCheck(0, sourceArray.length(sourceView, sourceCondition), offset, sinkArray.length(sinkView, sinkCondition));

            if (sinkEqualsSource.profile(JSArrayBufferView.getArrayBuffer(sourceView, sourceCondition) == JSArrayBufferView.getArrayBuffer(sinkView, sinkCondition))) {
                setArrayBufferViewSameBuffer(sinkView, sourceView, offset, sinkArray, sourceArray);
            } else {
                setArrayBufferViewDistinctBuffers(sinkView, sourceView, offset, sinkArray, sourceArray);
            }
        }

        private void setArrayBufferViewDistinctBuffers(DynamicObject sinkView, DynamicObject sourceView, int offset, TypedArray sinkArray, TypedArray sourceArray) {
            long sourceLen = sourceArray.length(sourceView);
            if (sourceArray instanceof TypedArray.TypedIntArray && sinkArray instanceof TypedArray.TypedIntArray) {
                intToIntBranch.enter();
                for (int i = 0; i < sourceLen; i++) {
                    int value = ((TypedArray.TypedIntArray) sourceArray).getInt(sourceView, i, false);
                    checkHasDetachedBuffer(sinkView);
                    ((TypedArray.TypedIntArray) sinkArray).setInt(sinkView, i + offset, value, false);
                }
            } else if (sourceArray instanceof TypedArray.TypedFloatArray && sinkArray instanceof TypedArray.TypedFloatArray) {
                floatToFloatBranch.enter();
                for (int i = 0; i < sourceLen; i++) {
                    double value = ((TypedArray.TypedFloatArray) sourceArray).getDouble(sourceView, i, false);
                    checkHasDetachedBuffer(sinkView);
                    ((TypedArray.TypedFloatArray) sinkArray).setDouble(sinkView, i + offset, value, false);
                }
            } else {
                objectToObjectBranch.enter();
                for (int i = 0; i < sourceLen; i++) {
                    Object value = sourceArray.getElement(sourceView, i, false);
                    checkHasDetachedBuffer(sinkView);
                    sinkArray.setElement(sinkView, i + offset, value, false);
                }
            }
        }

        private void setArrayBufferViewSameBuffer(DynamicObject sinkView, DynamicObject sourceView, int offset, TypedArray sinkArray, TypedArray sourceArray) {
            DynamicObject clonedSourceView = cloneArrayBufferView(sourceView, sourceArray);
            setArrayBufferViewDistinctBuffers(sinkView, clonedSourceView, offset, sinkArray, sourceArray);
        }

        private DynamicObject cloneArrayBufferView(DynamicObject sourceView, TypedArray sourceArray) {
            int length = (int) sourceArray.length(sourceView);
            int byteLength = length * sourceArray.bytesPerElement();
            TypedArray sinkArray = sourceArray.getFactory().createArrayType(sourceArray.isDirect(), false);
            DynamicObject clonedArrayBuffer = sinkArray.isDirect() ? JSArrayBuffer.createDirectArrayBuffer(getContext(), byteLength) : JSArrayBuffer.createArrayBuffer(getContext(), byteLength);
            DynamicObject sinkView = JSArrayBufferView.createArrayBufferView(getContext(), clonedArrayBuffer, sinkArray, 0, length);

            setArrayBufferViewDistinctBuffers(sinkView, sourceView, 0, sinkArray, sourceArray);

            return sinkView;
        }

        private void rangeCheck(long sourceStart, long sourceLength, long sinkStart, long sinkLength) {
            if (!(sourceStart >= 0 && sinkStart >= 0 && sourceStart <= sourceLength && sinkStart <= sinkLength && sourceLength - sourceStart <= sinkLength - sinkStart)) {
                needErrorBranch.enter();
                throw Errors.createRangeError("out of bounds");
            }
        }

        private long objectGetLength(DynamicObject thisObject) {
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
    }

    public abstract static class JSArrayBufferViewForEachNode extends ArrayForEachIndexCallOperation {
        public JSArrayBufferViewForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSArrayBufferView(thisJSObj)")
        protected Object forEach(DynamicObject thisJSObj, Object callback, Object thisArg) {
            checkHasDetachedBuffer(thisJSObj);
            long length = JSArrayBufferView.typedArrayGetArrayType(thisJSObj, JSArrayBufferView.isJSArrayBufferView(thisJSObj)).length(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);
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
    }

    public abstract static class JSArrayBufferViewReverseNode extends JSArrayOperation {
        public JSArrayBufferViewReverseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization(guards = "isJSArrayBufferView(thisObj)")
        protected DynamicObject reverse(DynamicObject thisObj,
                        @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode) {
            checkHasDetachedBuffer(thisObj);
            long len = getLength(thisObj);
            long middle = (long) JSRuntime.mathFloor(len / 2);
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
                upper = len - lower - 1;
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

        public JSArrayBufferViewFillNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization
        protected TruffleObject fill(Object thisObj, Object value, Object start, Object end) {
            validateTypedArray(thisObj);
            DynamicObject thisJSObj = (DynamicObject) thisObj;
            long len = getLength(thisJSObj);
            Object convValue = toNumber(value);
            long lStart = JSRuntime.getOffset(toIntegerSpecial(start), len, offsetProfile1);
            long lEnd = end == Undefined.instance ? len : JSRuntime.getOffset(toIntegerSpecial(end), len, offsetProfile2);
            checkHasDetachedBuffer(thisJSObj);
            for (long idx = lStart; idx < lEnd; idx++) {
                write(thisJSObj, idx, convValue);
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
    }
}

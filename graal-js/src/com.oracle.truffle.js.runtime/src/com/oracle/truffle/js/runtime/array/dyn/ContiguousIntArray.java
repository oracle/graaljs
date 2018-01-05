/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ContiguousIntArray extends AbstractContiguousIntArray {

    private static final ContiguousIntArray CONTIGUOUS_INT_ARRAY = new ContiguousIntArray(INTEGRITY_LEVEL_NONE, createCache());

    public static ContiguousIntArray makeContiguousIntArray(DynamicObject object, long length, int[] array, long indexOffset, int arrayOffset, int usedLength, int integrityLevel) {
        ContiguousIntArray arrayType = createContiguousIntArray().setIntegrityLevel(object, integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        return arrayType;
    }

    private static ContiguousIntArray createContiguousIntArray() {
        return CONTIGUOUS_INT_ARRAY;
    }

    private ContiguousIntArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    protected int prepareInBounds(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareInBoundsContiguous(object, index, condition, profile);
    }

    @Override
    protected int prepareSupported(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareSupportedContiguous(object, index, condition, profile);
    }

    @Override
    public boolean isSupported(DynamicObject object, long index, boolean condition) {
        return isSupportedContiguous(object, index, condition);
    }

    @Override
    public ContiguousDoubleArray toDouble(DynamicObject object, long index, double value, boolean condition) {
        int[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        double[] doubleCopy = ArrayCopy.intToDouble(array, arrayOffset, usedLength);
        ContiguousDoubleArray newArray = ContiguousDoubleArray.makeContiguousDoubleArray(object, length, doubleCopy, indexOffset, arrayOffset, usedLength, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ContiguousObjectArray toObject(DynamicObject object, long index, Object value, boolean condition) {
        int[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        Object[] doubleCopy = ArrayCopy.intToObject(array, arrayOffset, usedLength);
        ContiguousObjectArray newArray = ContiguousObjectArray.makeContiguousObjectArray(object, length, doubleCopy, indexOffset, arrayOffset, usedLength, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray toHoles(DynamicObject object, long index, Object value, boolean condition) {
        int[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        AbstractWritableArray newArray;
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, containsHoleValue(object, condition))) {
            newArray = toObjectHoles(object, condition);
        } else {
            newArray = HolesIntArray.makeHolesIntArray(object, length, array, indexOffset, arrayOffset, usedLength, 0, integrityLevel);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    protected HolesObjectArray toObjectHoles(DynamicObject object, boolean condition) {
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        return HolesObjectArray.makeHolesObjectArray(object, length, convertToObject(object, condition), indexOffset, arrayOffset, usedLength, 0, integrityLevel);
    }

    @Override
    public ZeroBasedIntArray toNonContiguous(DynamicObject object, int index, Object value, boolean condition, ProfileHolder profile) {
        setSupported(object, index, (int) value, condition, profile);

        int[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        ZeroBasedIntArray newArray = ZeroBasedIntArray.makeZeroBasedIntArray(object, length, usedLength, array, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        return removeRangeContiguous(object, start, end);
    }

    @Override
    protected ContiguousIntArray withIntegrityLevel(int newIntegrityLevel) {
        return new ContiguousIntArray(newIntegrityLevel, cache);
    }
}

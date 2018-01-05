/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ContiguousJSObjectArray extends AbstractContiguousJSObjectArray {

    private static final ContiguousJSObjectArray CONTIGUOUS_JSOBJECT_ARRAY = new ContiguousJSObjectArray(INTEGRITY_LEVEL_NONE, createCache());

    public static ContiguousJSObjectArray makeContiguousJSObjectArray(DynamicObject object, long length, DynamicObject[] array, long indexOffset, int arrayOffset, int usedLength, int integrityLevel) {
        ContiguousJSObjectArray arrayType = createContiguousJSObjectArray().setIntegrityLevel(object, integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        return arrayType;
    }

    private static ContiguousJSObjectArray createContiguousJSObjectArray() {
        return CONTIGUOUS_JSOBJECT_ARRAY;
    }

    private ContiguousJSObjectArray(int integrityLevel, DynamicArrayCache cache) {
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
    public HolesJSObjectArray toHoles(DynamicObject object, long index, Object value, boolean condition) {
        DynamicObject[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        HolesJSObjectArray newArray = HolesJSObjectArray.makeHolesJSObjectArray(object, length, array, indexOffset, arrayOffset, usedLength, 0, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ZeroBasedJSObjectArray toNonContiguous(DynamicObject object, int index, Object value, boolean condition, ProfileHolder profile) {
        setSupported(object, index, (DynamicObject) value, condition, profile);

        DynamicObject[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        ZeroBasedJSObjectArray newArray = ZeroBasedJSObjectArray.makeZeroBasedJSObjectArray(object, length, usedLength, array, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ContiguousObjectArray toObject(DynamicObject object, long index, Object value, boolean condition) {
        DynamicObject[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        Object[] doubleCopy = ArrayCopy.jsobjectToObject(array, arrayOffset, usedLength);
        ContiguousObjectArray newArray = ContiguousObjectArray.makeContiguousObjectArray(object, length, doubleCopy, indexOffset, arrayOffset, usedLength, integrityLevel);
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
    protected ContiguousJSObjectArray withIntegrityLevel(int newIntegrityLevel) {
        return new ContiguousJSObjectArray(newIntegrityLevel, cache);
    }
}

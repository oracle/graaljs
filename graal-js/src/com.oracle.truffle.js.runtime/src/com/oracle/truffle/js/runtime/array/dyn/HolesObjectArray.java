/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetHoleCount;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetHoleCount;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class HolesObjectArray extends AbstractContiguousObjectArray {

    private static final HolesObjectArray HOLES_OBJECT_ARRAY = new HolesObjectArray(INTEGRITY_LEVEL_NONE, createCache());

    private HolesObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    public static HolesObjectArray makeHolesObjectArray(DynamicObject object, int length, Object[] array, long indexOffset, int arrayOffset, int usedLength, int holeCount, int integrityLevel) {
        HolesObjectArray arrayType = createHolesObjectArray().setIntegrityLevel(object, integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        assert holeCount == arrayType.countHoles(object) : String.format("holeCount, %d, differs from the actual count, %d", holeCount, arrayType.countHoles(object));
        return arrayType;
    }

    public static HolesObjectArray createHolesObjectArray() {
        return HOLES_OBJECT_ARRAY;
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        return this;
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, Object value, boolean condition) {
        throw new RuntimeException("should not call this method, use setInBounds(Non)Hole");
    }

    public boolean isHoleFast(DynamicObject object, int index, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        return isHolePrepared(object, internalIndex, condition);
    }

    public void setInBoundsFastHole(DynamicObject object, int index, Object value, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        assert isHolePrepared(object, internalIndex, condition);
        incrementHolesCount(object, -1);
        setInBoundsFastIntl(object, index, internalIndex, value, condition);
    }

    public void setInBoundsFastNonHole(DynamicObject object, int index, Object value, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        assert !isHolePrepared(object, internalIndex, condition);
        setInBoundsFastIntl(object, index, internalIndex, checkNonNull(value), condition);
    }

    private void setInBoundsFastIntl(DynamicObject object, int index, int internalIndex, Object value, boolean condition) {
        getArray(object, condition)[internalIndex] = value;
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    public boolean containsHoles(DynamicObject object, long index, boolean condition) {
        return arrayGetHoleCount(object, condition) > 0 || !isInBoundsFast(object, index, condition);
    }

    @Override
    public AbstractObjectArray toNonHoles(DynamicObject object, long index, Object value, boolean condition) {
        assert !containsHoles(object, index, condition);
        Object[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        AbstractObjectArray newArray;
        setInBoundsFastNonHole(object, (int) index, value, condition);
        if (isInBoundsFast(object, 0)) {
            newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, length, usedLength, array, integrityLevel);
        } else {
            newArray = ContiguousObjectArray.makeContiguousObjectArray(object, length, array, indexOffset, arrayOffset, usedLength, integrityLevel);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    protected void incrementHolesCount(DynamicObject object, int offset) {
        arraySetHoleCount(object, arrayGetHoleCount(object) + offset);
    }

    @Override
    public int prepareInBounds(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareInBoundsHoles(object, index, condition, profile);
    }

    @Override
    public boolean isSupported(DynamicObject object, long index, boolean condition) {
        return isSupportedHoles(object, index, condition);
    }

    @Override
    public int prepareSupported(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareSupportedHoles(object, index, condition, profile);
    }

    @Override
    public Object getInBoundsFast(DynamicObject object, int index, boolean condition) {
        Object value = getInBoundsFastObject(object, index, condition);
        if (HolesObjectArray.isHoleValue(value)) {
            return Undefined.instance;
        }
        return value;
    }

    @Override
    public HolesObjectArray toHoles(DynamicObject object, long index, Object value, boolean condition) {
        return this;
    }

    public static boolean isHoleValue(Object value) {
        return value == null;
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index0, boolean condition) {
        return nextElementIndexHoles(object, index0, condition);
    }

    @Override
    public long previousElementIndex(DynamicObject object, long index0, boolean condition) {
        return previousElementIndexHoles(object, index0, condition);
    }

    @Override
    public boolean hasElement(DynamicObject object, long index, boolean condition) {
        return super.hasElement(object, index, condition) && !isHolePrepared(object, prepareInBoundsFast(object, (int) index, condition), condition);
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return deleteElementHoles(object, index, condition);
    }

    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        return removeRangeHoles(object, start, end);
    }

    @Override
    protected Object castNonNull(Object value) {
        return value;
    }

    @Override
    protected HolesObjectArray withIntegrityLevel(int newIntegrityLevel) {
        return new HolesObjectArray(newIntegrityLevel, cache);
    }
}

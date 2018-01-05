/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.api.CompilerDirectives.FASTPATH_PROBABILITY;
import static com.oracle.truffle.api.CompilerDirectives.injectBranchProbability;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public abstract class AbstractDoubleArray extends AbstractWritableArray {

    protected AbstractDoubleArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        return HolesDoubleArray.makeHolesDoubleArray(object, length, (double[]) array, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
    }

    public abstract void setInBoundsFast(DynamicObject object, int index, double value, boolean condition);

    @Override
    public final ScriptArray setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
        assert index >= 0;
        if (injectBranchProbability(FASTPATH_PROBABILITY, (value instanceof Integer || value instanceof Double) && isSupported(object, (int) index, condition))) {
            double doubleValue = JSRuntime.doubleValue((Number) value);
            assert !HolesDoubleArray.isHoleValue(doubleValue);
            setSupported(object, (int) index, doubleValue, condition, ProfileHolder.empty());
            return this;
        } else {
            return rewrite(object, index, value, condition).setElementImpl(object, index, value, strict, condition);
        }
    }

    private ScriptArray rewrite(DynamicObject object, long index, Object value, boolean condition) {
        if (value instanceof Integer || value instanceof Double) {
            if (isSupportedContiguous(object, index, condition)) {
                return toContiguous(object, index, value, condition);
            } else if (isSupportedHoles(object, index, condition)) {
                return toHoles(object, index, value, condition);
            } else {
                return toSparse(object, index, value);
            }
        } else {
            return toObject(object, index, value, condition);
        }
    }

    @Override
    public Object getInBoundsFast(DynamicObject object, int index, boolean condition) {
        return getInBoundsFastDouble(object, index, condition);
    }

    @Override
    public abstract double getInBoundsFastDouble(DynamicObject object, int index, boolean condition);

    @Override
    int getArrayLength(Object array) {
        return ((double[]) array).length;
    }

    protected static double[] getArray(DynamicObject object) {
        return getArray(object, arrayCondition());
    }

    protected static double[] getArray(DynamicObject object, boolean condition) {
        return arrayCast(arrayGetArray(object, condition), double[].class, condition);
    }

    public final void setInBounds(DynamicObject object, int index, double value, boolean condition, ProfileHolder profile) {
        getArray(object, condition)[prepareInBounds(object, index, condition, profile)] = value;
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBounds", index, value);
        }
    }

    public final void setSupported(DynamicObject object, int index, double value, boolean condition, ProfileHolder profile) {
        int preparedIndex = prepareSupported(object, index, condition, profile);
        getArray(object, condition)[preparedIndex] = value;
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("Supported", index, value);
        }
    }

    @Override
    void fillWithHoles(Object array, int fromIndex, int toIndex) {
        double[] doubleArray = (double[]) array;
        for (int i = fromIndex; i < toIndex; i++) {
            doubleArray[i] = HolesDoubleArray.HOLE_VALUE_DOUBLE;
        }
    }

    @Override
    protected final void setHoleValue(DynamicObject object, int preparedIndex) {
        getArray(object)[preparedIndex] = HolesDoubleArray.HOLE_VALUE_DOUBLE;
    }

    @Override
    protected final boolean isHolePrepared(DynamicObject object, int preparedIndex, boolean condition) {
        return HolesDoubleArray.isHoleValue(getArray(object, condition)[preparedIndex]);
    }

    @Override
    protected final int getArrayCapacity(DynamicObject object, boolean condition) {
        return getArray(object, condition).length;
    }

    @Override
    protected final void resizeArray(DynamicObject object, int newCapacity, int oldCapacity, int offset, boolean condition) {
        double[] newArray = new double[newCapacity];
        System.arraycopy(getArray(object, condition), 0, newArray, offset, oldCapacity);
        arraySetArray(object, newArray);
    }

    @Override
    public abstract AbstractDoubleArray toHoles(DynamicObject object, long index, Object value, boolean condition);

    @Override
    public final AbstractWritableArray toDouble(DynamicObject object, long index, double value, boolean condition) {
        return this;
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return toHoles(object, index, HolesDoubleArray.HOLE_VALUE, condition).deleteElementImpl(object, index, strict, condition);
    }

    @Override
    protected final void moveRangePrepared(DynamicObject object, int src, int dst, int len) {
        double[] array = getArray(object);
        System.arraycopy(array, src, array, dst, len);
    }

    @Override
    public final Object allocateArray(int length) {
        return new double[length];
    }

    @Override
    protected abstract AbstractDoubleArray withIntegrityLevel(int newIntegrityLevel);
}

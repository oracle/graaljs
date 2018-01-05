/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ConstantDoubleArray extends AbstractConstantArray {
    private static final ConstantDoubleArray CONSTANT_DOUBLE_ARRAY = new ConstantDoubleArray(INTEGRITY_LEVEL_NONE, createCache());

    public static ConstantDoubleArray createConstantDoubleArray() {
        return CONSTANT_DOUBLE_ARRAY;
    }

    private ConstantDoubleArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public Object getElementInBounds(DynamicObject object, int index, boolean condition) {
        return getElementDouble(object, index, condition);
    }

    public static double getElementDouble(DynamicObject object, int index, boolean condition) {
        return getArray(object, condition)[index];
    }

    public static double[] getArray(DynamicObject object) {
        return (double[]) arrayGetArray(object);
    }

    public static double[] getArray(DynamicObject object, boolean condition) {
        return (double[]) arrayGetArray(object, condition);
    }

    @Override
    public int lengthInt(DynamicObject object, boolean condition) {
        return getArray(object, condition).length;
    }

    @Override
    public boolean hasElement(DynamicObject object, long index, boolean condition) {
        return index >= 0 && index < getArray(object, condition).length;
    }

    @Override
    public Object[] toArray(DynamicObject object) {
        return ArrayCopy.doubleToObject(getArray(object));
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return createWriteableDouble(object, index, HolesDoubleArray.HOLE_VALUE_DOUBLE, ProfileHolder.empty()).deleteElementImpl(object, index, condition, condition);
    }

    @Override
    public ScriptArray setLengthImpl(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        return createWriteableDouble(object, length - 1, HolesDoubleArray.HOLE_VALUE_DOUBLE, ProfileHolder.empty()).setLengthImpl(object, length, condition, profile);
    }

    @Override
    public ZeroBasedDoubleArray createWriteableInt(DynamicObject object, long index, int value, ProfileHolder profile) {
        return createWriteableDouble(object, index, value, profile);
    }

    @Override
    public ZeroBasedDoubleArray createWriteableDouble(DynamicObject object, long index, double value, ProfileHolder profile) {
        double[] doubleCopy = ArrayCopy.doubleToDouble(getArray(object));
        ZeroBasedDoubleArray newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, doubleCopy.length, doubleCopy.length, doubleCopy, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray createWriteableJSObject(DynamicObject object, long index, DynamicObject value, ProfileHolder profile) {
        return createWriteableObject(object, index, value, profile);
    }

    @Override
    public ZeroBasedObjectArray createWriteableObject(DynamicObject object, long index, Object value, ProfileHolder profile) {
        Object[] doubleCopy = ArrayCopy.doubleToObject(getArray(object));
        ZeroBasedObjectArray newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, doubleCopy.length, doubleCopy.length, doubleCopy, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        double[] array = getArray(object);
        if ((array.length - (end - start)) == 0) {
            AbstractConstantEmptyArray.setCapacity(object, 0);
        } else {
            double[] newArray = new double[array.length - (int) (end - start)];
            System.arraycopy(array, 0, newArray, 0, (int) start);
            System.arraycopy(array, (int) end, newArray, (int) start, (int) (array.length - end));
            arraySetArray(object, newArray);
        }
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        double[] array = getArray(object);
        if (array.length == 0) {
            AbstractConstantEmptyArray.setCapacity(object, size);
            return this;
        } else {
            double[] newArray = new double[array.length + size];
            System.arraycopy(array, 0, newArray, 0, (int) offset);
            System.arraycopy(array, (int) offset, newArray, (int) offset + size, (int) (array.length - offset));

            arraySetArray(object, newArray);
            return this;
        }
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new ConstantDoubleArray(newIntegrityLevel, cache);
    }
}

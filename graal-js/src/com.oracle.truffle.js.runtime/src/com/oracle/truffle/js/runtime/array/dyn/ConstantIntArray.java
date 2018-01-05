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

public final class ConstantIntArray extends AbstractConstantArray {
    private static final ConstantIntArray CONSTANT_INT_ARRAY = new ConstantIntArray(INTEGRITY_LEVEL_NONE, createCache());

    public static ConstantIntArray createConstantIntArray() {
        return CONSTANT_INT_ARRAY;
    }

    private ConstantIntArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public Object getElementInBounds(DynamicObject object, int index, boolean condition) {
        return getElementInt(object, index, condition);
    }

    public static int getElementInt(DynamicObject object, int index, boolean condition) {
        return getArray(object, condition)[index];
    }

    private static int[] getArray(DynamicObject object) {
        return (int[]) arrayGetArray(object);
    }

    public static int[] getArray(DynamicObject object, boolean condition) {
        return (int[]) arrayGetArray(object, condition);
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
        return ArrayCopy.intToObject(getArray(object));
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return createWriteableInt(object, index, HolesIntArray.HOLE_VALUE, ProfileHolder.empty()).deleteElementImpl(object, index, strict, condition);
    }

    @Override
    public ScriptArray setLengthImpl(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        return createWriteableInt(object, length - 1, HolesIntArray.HOLE_VALUE, ProfileHolder.empty()).setLengthImpl(object, length, condition, profile);
    }

    @Override
    public AbstractIntArray createWriteableInt(DynamicObject object, long index, int value, ProfileHolder profile) {
        int[] copyArray = ArrayCopy.intToInt(getArray(object));
        ZeroBasedIntArray newArray = ZeroBasedIntArray.makeZeroBasedIntArray(object, copyArray.length, copyArray.length, copyArray, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray createWriteableDouble(DynamicObject object, long index, double value, ProfileHolder profile) {
        double[] copyArray = ArrayCopy.intToDouble(getArray(object));
        ZeroBasedDoubleArray newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, copyArray.length, copyArray.length, copyArray, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray createWriteableJSObject(DynamicObject object, long index, DynamicObject value, ProfileHolder profile) {
        return createWriteableObject(object, index, value, ProfileHolder.empty());
    }

    @Override
    public AbstractWritableArray createWriteableObject(DynamicObject object, long index, Object value, ProfileHolder profile) {
        Object[] copyArray = ArrayCopy.intToObject(getArray(object));
        ZeroBasedObjectArray newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, copyArray.length, copyArray.length, copyArray, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        int[] array = getArray(object);
        if ((array.length - (end - start)) == 0) {
            AbstractConstantEmptyArray.setCapacity(object, 0);
        } else {
            int[] newArray = new int[array.length - (int) (end - start)];
            System.arraycopy(array, 0, newArray, 0, (int) start);
            System.arraycopy(array, (int) end, newArray, (int) start, (int) (array.length - end));
            arraySetArray(object, newArray);
        }
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        int[] array = getArray(object);
        if (array.length == 0) {
            AbstractConstantEmptyArray.setCapacity(object, size);
            return this;
        } else {
            int[] newArray = new int[array.length + size];
            System.arraycopy(array, 0, newArray, 0, (int) offset);
            System.arraycopy(array, (int) offset, newArray, (int) offset + size, (int) (array.length - offset));

            arraySetArray(object, newArray);
            return this;
        }
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new ConstantIntArray(newIntegrityLevel, cache);
    }
}

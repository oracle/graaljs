/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

import java.util.Arrays;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class ConstantObjectArray extends AbstractConstantArray {
    private static final ConstantObjectArray CONSTANT_OBJECT_ARRAY = new ConstantObjectArray(false, INTEGRITY_LEVEL_NONE, createCache());
    private static final ConstantObjectArray CONSTANT_HOLES_OBJECT_ARRAY = new ConstantObjectArray(true, INTEGRITY_LEVEL_NONE, createCache());

    public static ConstantObjectArray createConstantObjectArray() {
        return CONSTANT_OBJECT_ARRAY;
    }

    public static AbstractConstantArray createConstantHolesObjectArray() {
        return CONSTANT_HOLES_OBJECT_ARRAY;
    }

    private final boolean holes;

    private ConstantObjectArray(boolean holes, int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
        this.holes = holes;
    }

    private static Object[] getArray(DynamicObject object) {
        return (Object[]) arrayGetArray(object);
    }

    private static Object[] getArray(DynamicObject object, boolean condition) {
        return (Object[]) arrayGetArray(object, condition);
    }

    @Override
    public boolean hasElement(DynamicObject object, long index, boolean condition) {
        if (index >= 0 && index < getArray(object, condition).length) {
            return !holes || getArray(object, condition)[(int) index] != null;
        }
        return false;
    }

    @Override
    public Object getElementInBounds(DynamicObject object, int index, boolean condition) {
        Object value = getElementInBoundsDirect(object, index, condition);
        if (holes && value == null) {
            return Undefined.instance;
        }
        return value;
    }

    private static boolean isEmpty(DynamicObject object, int index, boolean condition) {
        return getArray(object, condition)[index] == null;
    }

    public static Object getElementInBoundsDirect(DynamicObject object, int index, boolean condition) {
        return getArray(object, condition)[index];
    }

    @Override
    public boolean hasHoles(DynamicObject object, boolean condition) {
        return holes;
    }

    @Override
    public int lengthInt(DynamicObject object, boolean condition) {
        return getArray(object, condition).length;
    }

    @Override
    public Object[] toArray(DynamicObject object) {
        if (hasHoles(object)) {
            Object[] array = getArray(object);
            Object[] newArray = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                newArray[i] = array[i] == null ? Undefined.instance : array[i];
            }
            return newArray;
        } else {
            Object[] array = getArray(object);
            return Arrays.copyOf(array, array.length);
        }
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index0, boolean condition) {
        if (!holes) {
            return super.nextElementIndex(object, index0, condition);
        }
        int index = (int) index0;
        do {
            index++;
        } while (index < super.lastElementIndex(object, condition) && isEmpty(object, index, condition));
        return index;
    }

    @Override
    public long previousElementIndex(DynamicObject object, long index0, boolean condition) {
        if (!holes) {
            return super.previousElementIndex(object, index0, condition);
        }
        int index = (int) index0;
        do {
            index--;
        } while (index >= super.firstElementIndex(object, condition) && isEmpty(object, index, condition));
        return index;
    }

    @Override
    public long firstElementIndex(DynamicObject object, boolean condition) {
        if (!holes) {
            return super.firstElementIndex(object, condition);
        }
        int index = 0;
        int length = lengthInt(object, condition);
        while (index < length && isEmpty(object, index, condition)) {
            index++;
        }
        return index;
    }

    @Override
    public long lastElementIndex(DynamicObject object, boolean condition) {
        if (!holes) {
            return super.lastElementIndex(object, condition);
        }
        int index = lengthInt(object, condition);
        do {
            index--;
        } while (index >= 0 && isEmpty(object, index, condition));
        return index;
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return createWriteableObject(object, index, null, ProfileHolder.empty()).deleteElementImpl(object, index, strict, condition);
    }

    @Override
    public ScriptArray setLengthImpl(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        return createWriteableObject(object, length - 1, null, ProfileHolder.empty()).setLengthImpl(object, length, condition, profile);
    }

    @Override
    public AbstractObjectArray createWriteableInt(DynamicObject object, long index, int value, ProfileHolder profile) {
        return createWriteableObject(object, index, value, profile);
    }

    @Override
    public AbstractObjectArray createWriteableDouble(DynamicObject object, long index, double value, ProfileHolder profile) {
        return createWriteableObject(object, index, value, profile);
    }

    @Override
    public AbstractObjectArray createWriteableJSObject(DynamicObject object, long index, DynamicObject value, ProfileHolder profile) {
        return createWriteableObject(object, index, value, profile);
    }

    @Override
    public AbstractObjectArray createWriteableObject(DynamicObject object, long index, Object value, ProfileHolder profile) {
        Object[] array = getArray(object);
        AbstractObjectArray newArray;
        if (holes) {
            int arrayOffset = (int) firstElementIndex(object);
            int usedLength = (int) lastElementIndex(object) + 1 - arrayOffset;
            int holeCount = countHoles(object);
            newArray = HolesObjectArray.makeHolesObjectArray(object, array.length, ArrayCopy.objectToObject(array), 0, arrayOffset, usedLength, holeCount, integrityLevel);
        } else {
            newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, array.length, array.length, ArrayCopy.objectToObject(array), integrityLevel);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    private int countHoles(DynamicObject object) {
        int index = (int) (firstElementIndex(object));
        int lastIndex = (int) (lastElementIndex(object) + 1);
        Object[] objArray = getArray(object);
        int holeCount = 0;
        while (index < lastIndex) {
            if (HolesObjectArray.isHoleValue(objArray[index])) {
                holeCount++;
            }
            index++;
        }
        return holeCount;
    }

    /**
     * This array type can have holes. Use isHoles() to check whether the array does have holes.
     */
    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        Object[] array = getArray(object);
        if ((array.length - (end - start)) == 0) {
            AbstractConstantEmptyArray.setCapacity(object, 0);
        } else {
            Object[] newArray = new Object[array.length - (int) (end - start)];
            System.arraycopy(array, 0, newArray, 0, (int) start);
            System.arraycopy(array, (int) end, newArray, (int) start, (int) (array.length - end));
            arraySetArray(object, newArray);
        }
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        Object[] array = getArray(object);
        if (array.length == 0) {
            AbstractConstantEmptyArray.setCapacity(object, size);
            return this;
        } else {
            Object[] newArray = new Object[array.length + size];
            System.arraycopy(array, 0, newArray, 0, (int) offset);
            System.arraycopy(array, (int) offset, newArray, (int) offset + size, (int) (array.length - offset));

            arraySetArray(object, newArray);
            return this;
        }
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new ConstantObjectArray(holes, newIntegrityLevel, cache);
    }
}

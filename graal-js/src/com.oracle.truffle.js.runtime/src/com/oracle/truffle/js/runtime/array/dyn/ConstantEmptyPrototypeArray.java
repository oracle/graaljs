/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * This array type is reserved for Array.prototype and Object.prototype. It ensures an Assumption is
 * invalidated if the Array.prototype and Object.prototype is ever assigned an indexed property.
 */
public final class ConstantEmptyPrototypeArray extends AbstractConstantEmptyArray {
    public static ScriptArray createConstantEmptyPrototypeArray() {
        return new ConstantEmptyPrototypeArray(INTEGRITY_LEVEL_NONE, createCache());
    }

    private ConstantEmptyPrototypeArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    private static Assumption getArrayPrototypeNoElementsAssumption(DynamicObject object) {
        return JSObject.getJSContext(object).getArrayPrototypeNoElementsAssumption();
    }

    @Override
    public ScriptArray setLengthImpl(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        setCapacity(object, length);
        return this;
    }

    @Override
    public AbstractIntArray createWriteableInt(DynamicObject object, long index, int value, ProfileHolder profile) {
        getArrayPrototypeNoElementsAssumption(object).invalidate();
        return super.createWriteableInt(object, index, value, profile);
    }

    @Override
    public AbstractDoubleArray createWriteableDouble(DynamicObject object, long index, double value, ProfileHolder profile) {
        getArrayPrototypeNoElementsAssumption(object).invalidate();
        return super.createWriteableDouble(object, index, value, profile);
    }

    @Override
    public AbstractJSObjectArray createWriteableJSObject(DynamicObject object, long index, DynamicObject value, ProfileHolder profile) {
        getArrayPrototypeNoElementsAssumption(object).invalidate();
        return super.createWriteableJSObject(object, index, value, profile);
    }

    @Override
    public AbstractObjectArray createWriteableObject(DynamicObject object, long index, Object value, ProfileHolder profile) {
        getArrayPrototypeNoElementsAssumption(object).invalidate();
        return super.createWriteableObject(object, index, value, profile);
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        setCapacity(object, getCapacity(object) - (end - start));
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        setCapacity(object, getCapacity(object) + size);
        return this;
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new ConstantEmptyPrototypeArray(newIntegrityLevel, cache);
    }
}

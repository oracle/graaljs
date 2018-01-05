/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ConstantEmptyArray extends AbstractConstantEmptyArray {
    private static final ConstantEmptyArray EMPTY_ARRAY = new ConstantEmptyArray(INTEGRITY_LEVEL_NONE, createCache());

    public static ConstantEmptyArray createConstantEmptyArray() {
        return EMPTY_ARRAY;
    }

    private ConstantEmptyArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public ScriptArray setLengthImpl(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        setCapacity(object, length);
        return this;
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
        return new ConstantEmptyArray(newIntegrityLevel, cache);
    }
}

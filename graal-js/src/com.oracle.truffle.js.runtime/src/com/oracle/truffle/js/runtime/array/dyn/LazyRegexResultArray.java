/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetRegexResult;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

public final class LazyRegexResultArray extends AbstractConstantArray {

    public static final LazyRegexResultArray LAZY_REGEX_RESULT_ARRAY = new LazyRegexResultArray(INTEGRITY_LEVEL_NONE, createCache());

    private static final TRegexUtil.TRegexMaterializeResultNode SLOW_PATH_RESULT_MATERIALIZE_NODE = TRegexUtil.TRegexMaterializeResultNode.create();

    public static LazyRegexResultArray createLazyRegexResultArray() {
        return LAZY_REGEX_RESULT_ARRAY;
    }

    private LazyRegexResultArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    private static Object[] getArray(DynamicObject object) {
        if (arrayGetArray(object) == ScriptArray.EMPTY_OBJECT_ARRAY) {
            arraySetArray(object, new Object[(int) arrayGetLength(object)]);
        }
        return (Object[]) arrayGetArray(object);
    }

    public static Object materializeGroup(TRegexUtil.TRegexMaterializeResultNode materializeResultNode, DynamicObject object, int index) {
        final Object[] internalArray = getArray(object);
        if (internalArray[index] == null) {
            internalArray[index] = materializeResultNode.materializeGroup(arrayGetRegexResult(object), index);
        }
        return internalArray[index];
    }

    public ScriptArray createWritable(TRegexUtil.TRegexMaterializeResultNode materializeResultNode, DynamicObject object, long index, Object value) {
        for (int i = 0; i < arrayGetLength(object); i++) {
            materializeGroup(materializeResultNode, object, i);
        }
        final Object[] internalArray = getArray(object);
        AbstractObjectArray newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, internalArray.length, internalArray.length, internalArray, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @CompilerDirectives.TruffleBoundary
    private static Object[] slowPathMaterializeFull(DynamicObject object) {
        return SLOW_PATH_RESULT_MATERIALIZE_NODE.materializeFull(arrayGetRegexResult(object));
    }

    @CompilerDirectives.TruffleBoundary
    private static Object slowPathMaterializeGroup(DynamicObject object, int index) {
        return SLOW_PATH_RESULT_MATERIALIZE_NODE.materializeGroup(arrayGetRegexResult(object), index);
    }

    @Override
    public Object getElementInBounds(DynamicObject object, int index, boolean condition) {
        final Object[] internalArray = getArray(object);
        if (internalArray[index] == null) {
            internalArray[index] = slowPathMaterializeGroup(object, index);
        }
        return internalArray[index];
    }

    @Override
    public boolean hasElement(DynamicObject object, long index, boolean condition) {
        return index >= 0 && index < arrayGetLength(object);
    }

    @Override
    public int lengthInt(DynamicObject object, boolean condition) {
        return (int) arrayGetLength(object);
    }

    @Override
    public AbstractObjectArray createWriteableObject(DynamicObject object, long index, Object value, ProfileHolder profile) {
        Object[] array = slowPathMaterializeFull(object);
        AbstractObjectArray newArray;
        newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, array.length, array.length, array, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
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
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return createWriteableObject(object, index, null, ProfileHolder.empty()).deleteElementImpl(object, index, strict, condition);
    }

    @Override
    public ScriptArray setLengthImpl(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        return createWriteableObject(object, length - 1, null, ProfileHolder.empty()).setLengthImpl(object, length, condition, profile);
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        return createWriteableObject(object, offset, null, ProfileHolder.empty()).addRangeImpl(object, offset, size);
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        return createWriteableObject(object, start, null, ProfileHolder.empty()).removeRangeImpl(object, start, end);
    }

    @Override
    public Object[] toArray(DynamicObject object) {
        return slowPathMaterializeFull(object);
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new LazyRegexResultArray(newIntegrityLevel, cache);
    }
}

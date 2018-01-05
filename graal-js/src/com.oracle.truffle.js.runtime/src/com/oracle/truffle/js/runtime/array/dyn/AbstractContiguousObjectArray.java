/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetIndexOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetIndexOffset;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public abstract class AbstractContiguousObjectArray extends AbstractObjectArray {

    protected AbstractContiguousObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public Object getInBoundsFastObject(DynamicObject object, int index, boolean condition) {
        return castNonNull(getArray(object, condition)[(int) (index - getIndexOffset(object, condition))]);
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, Object value, boolean condition) {
        getArray(object, condition)[(int) (index - getIndexOffset(object, condition))] = checkNonNull(value);
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    protected final void setLengthLess(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        setLengthLessContiguous(object, length, condition, profile);
    }

    @Override
    protected final int prepareInBoundsFast(DynamicObject object, long index, boolean condition) {
        return (int) (index - getIndexOffset(object, condition));
    }

    @Override
    protected final void setArrayOffset(DynamicObject object, int arrayOffset) {
        arraySetArrayOffset(object, arrayOffset);
    }

    @Override
    protected final int getArrayOffset(DynamicObject object) {
        return arrayGetArrayOffset(object);
    }

    @Override
    protected final int getArrayOffset(DynamicObject object, boolean condition) {
        return arrayGetArrayOffset(object, condition);
    }

    @Override
    protected final void setIndexOffset(DynamicObject object, long indexOffset) {
        arraySetIndexOffset(object, indexOffset);
    }

    @Override
    protected final long getIndexOffset(DynamicObject object) {
        return arrayGetIndexOffset(object);
    }

    @Override
    protected final long getIndexOffset(DynamicObject object, boolean condition) {
        return arrayGetIndexOffset(object, condition);
    }

    @Override
    public final long firstElementIndex(DynamicObject object, boolean condition) {
        return getIndexOffset(object, condition) + getArrayOffset(object, condition);
    }

    @Override
    public final long lastElementIndex(DynamicObject object, boolean condition) {
        return getIndexOffset(object, condition) + getArrayOffset(object, condition) + getUsedLength(object, condition) - 1;
    }

    @Override
    public boolean hasHoles(DynamicObject object, boolean condition) {
        return true;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        return addRangeImplContiguous(object, offset, size);
    }
}

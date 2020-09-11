/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.api.CompilerDirectives.FASTPATH_PROBABILITY;
import static com.oracle.truffle.api.CompilerDirectives.SLOWPATH_PROBABILITY;
import static com.oracle.truffle.api.CompilerDirectives.injectBranchProbability;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public abstract class AbstractIntArray extends AbstractWritableArray {

    protected AbstractIntArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        return HolesIntArray.makeHolesIntArray(object, length, (int[]) array, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
    }

    @Override
    public final ScriptArray setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
        assert index >= 0;
        if (injectBranchProbability(FASTPATH_PROBABILITY, value instanceof Integer && isSupported(object, index))) {
            int intValue = (int) value;
            if (injectBranchProbability(SLOWPATH_PROBABILITY, intValue == HolesIntArray.HOLE_VALUE)) {
                return toObject(object, index, value).setElementImpl(object, index, value, strict);
            }
            setSupported(object, (int) index, intValue, ProfileHolder.empty());
            return this;
        } else {
            return rewrite(object, index, value).setElementImpl(object, index, value, strict);
        }
    }

    private ScriptArray rewrite(DynamicObject object, long index, Object value) {
        if (value instanceof Integer) {
            if (isSupportedContiguous(object, index)) {
                return toContiguous(object, index, value);
            } else if (isSupportedHoles(object, index)) {
                return toHoles(object, index, value);
            } else {
                return toSparse(object, index, value);
            }
        } else if (value instanceof Double) {
            return toDouble(object, index, (double) value);
        } else {
            return toObject(object, index, value);
        }
    }

    @Override
    public Object getInBoundsFast(DynamicObject object, int index) {
        return getInBoundsFastInt(object, index);
    }

    @Override
    int getArrayLength(Object array) {
        return ((int[]) array).length;
    }

    protected static int[] getArray(DynamicObject object) {
        Object array = arrayGetArray(object);
        if (array.getClass() == int[].class) {
            return (int[]) array;
        } else {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    @Override
    public abstract int getInBoundsFastInt(DynamicObject object, int index);

    public abstract void setInBoundsFast(DynamicObject object, int index, int value);

    public final void setInBounds(DynamicObject object, int index, int value, ProfileHolder profile) {
        getArray(object)[prepareInBounds(object, index, profile)] = value;
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBounds", index, value);
        }
    }

    public final void setSupported(DynamicObject object, int index, int value, ProfileHolder profile) {
        int preparedIndex = prepareSupported(object, index, profile);
        getArray(object)[preparedIndex] = value;
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("Supported", index, value);
        }
    }

    @Override
    void fillWithHoles(Object array, int fromIndex, int toIndex) {
        int[] intArray = (int[]) array;
        for (int i = fromIndex; i < toIndex; i++) {
            intArray[i] = HolesIntArray.HOLE_VALUE;
        }
    }

    @Override
    protected final void setHoleValue(DynamicObject object, int preparedIndex) {
        getArray(object)[preparedIndex] = HolesIntArray.HOLE_VALUE;
    }

    @Override
    protected final boolean isHolePrepared(DynamicObject object, int preparedIndex) {
        return HolesIntArray.isHoleValue(getArray(object)[preparedIndex]);
    }

    @Override
    protected final int getArrayCapacity(DynamicObject object) {
        return getArray(object).length;
    }

    @Override
    protected final void resizeArray(DynamicObject object, int newCapacity, int oldCapacity, int offset) {
        int[] newArray = new int[newCapacity];
        System.arraycopy(getArray(object), 0, newArray, offset, oldCapacity);
        arraySetArray(object, newArray);
    }

    @Override
    public abstract AbstractWritableArray toHoles(DynamicObject object, long index, Object value);

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict) {
        return toHoles(object, index, HolesIntArray.HOLE_VALUE).deleteElementImpl(object, index, strict);
    }

    protected abstract HolesObjectArray toObjectHoles(DynamicObject object);

    protected static Object[] convertToObject(DynamicObject object) {
        int[] array = getArray(object);
        int usedLength = getUsedLength(object);
        Object[] obj = new Object[array.length];
        for (int i = 0; i < usedLength; i++) {
            obj[i] = array[i];
        }
        return obj;
    }

    protected static boolean containsHoleValue(DynamicObject object) {
        int[] array = getArray(object);
        int usedLength = getUsedLength(object);
        for (int i = 0; i < usedLength; i++) {
            if (array[i] == HolesIntArray.HOLE_VALUE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected final void moveRangePrepared(DynamicObject object, int src, int dst, int len) {
        int[] array = getArray(object);
        System.arraycopy(array, src, array, dst, len);
    }

    @Override
    public final Object allocateArray(int length) {
        return new int[length];
    }

    @Override
    public Object cloneArray(DynamicObject object) {
        return getArray(object).clone();
    }

    @Override
    protected abstract AbstractIntArray withIntegrityLevel(int newIntegrityLevel);
}

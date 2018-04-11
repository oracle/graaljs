/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.api.CompilerDirectives.injectBranchProbability;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public abstract class AbstractObjectArray extends AbstractWritableArray {

    protected AbstractObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        return HolesObjectArray.makeHolesObjectArray(object, length, (Object[]) array, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
    }

    public abstract void setInBoundsFast(DynamicObject object, int index, Object value, boolean condition);

    @Override
    public final ScriptArray setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
        assert index >= 0;
        if (injectBranchProbability(FASTPATH_PROBABILITY, isSupported(object, index, condition))) {
            assert value != null;
            setSupported(object, (int) index, value, condition);
            return this;
        } else {
            return rewrite(object, index, value, condition).setElementImpl(object, index, value, strict, condition);
        }
    }

    private ScriptArray rewrite(DynamicObject object, long index, Object value, boolean condition) {
        if (isSupportedContiguous(object, index, condition)) {
            return toContiguous(object, index, value, condition);
        } else if (isSupportedHoles(object, index, condition)) {
            return toHoles(object, index, value, condition);
        } else {
            return toSparse(object, index, value);
        }
    }

    @Override
    public Object getInBoundsFast(DynamicObject object, int index, boolean condition) {
        return getInBoundsFastObject(object, index, condition);
    }

    @Override
    int getArrayLength(Object array) {
        return ((Object[]) array).length;
    }

    protected static Object[] getArray(DynamicObject object) {
        return getArray(object, arrayCondition());
    }

    protected static Object[] getArray(DynamicObject object, boolean condition) {
        return arrayCast(arrayGetArray(object, condition), Object[].class, condition);
    }

    public abstract Object getInBoundsFastObject(DynamicObject object, int index, boolean condition);

    public final void setInBounds(DynamicObject object, int index, Object value, boolean condition, ProfileHolder profile) {
        getArray(object, condition)[prepareInBounds(object, index, condition, profile)] = checkNonNull(value);
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBounds", index, value);
        }
    }

    public final void setSupported(DynamicObject object, int index, Object value, boolean condition) {
        int preparedIndex = prepareSupported(object, index, condition, ProfileHolder.empty());

        getArray(object, condition)[preparedIndex] = checkNonNull(value);
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("Supported", index, value);
        }
    }

    @Override
    void fillWithHoles(Object array, int fromIndex, int toIndex) {
        Object[] objectArray = (Object[]) array;
        for (int i = fromIndex; i < toIndex; i++) {
            objectArray[i] = null;
        }
    }

    @Override
    protected final void setHoleValue(DynamicObject object, int preparedIndex) {
        getArray(object)[preparedIndex] = null;
    }

    @Override
    protected final void fillHoles(DynamicObject object, int internalIndex, int grown, ProfileHolder profile) {
        if (grown != 0) {
            incrementHolesCount(object, Math.abs(grown) - 1);
        }
    }

    @Override
    protected final boolean isHolePrepared(DynamicObject object, int preparedIndex, boolean condition) {
        return HolesObjectArray.isHoleValue(getArray(object, condition)[preparedIndex]);
    }

    @Override
    protected final int getArrayCapacity(DynamicObject object, boolean condition) {
        return getArray(object, condition).length;
    }

    @Override
    protected final void resizeArray(DynamicObject object, int newCapacity, int oldCapacity, int offset, boolean condition) {
        Object[] newArray = new Object[newCapacity];
        System.arraycopy(getArray(object, condition), 0, newArray, offset, oldCapacity);
        arraySetArray(object, newArray);
    }

    @Override
    public abstract AbstractObjectArray toHoles(DynamicObject object, long index, Object value, boolean condition);

    @Override
    public final AbstractWritableArray toDouble(DynamicObject object, long index, double value, boolean condition) {
        return this;
    }

    @Override
    public final AbstractWritableArray toObject(DynamicObject object, long index, Object value, boolean condition) {
        return this;
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return toHoles(object, index, null, condition).deleteElementImpl(object, index, strict, condition);
    }

    @Override
    protected final void moveRangePrepared(DynamicObject object, int src, int dst, int len) {
        Object[] array = getArray(object);
        System.arraycopy(array, src, array, dst, len);
    }

    @Override
    public final Object allocateArray(int length) {
        return new Object[length];
    }

    @Override
    protected abstract AbstractObjectArray withIntegrityLevel(int newIntegrityLevel);

    protected static Object checkNonNull(Object value) {
        assert value != null;
        return value;
    }

    protected Object castNonNull(Object value) {
        if (JSTruffleOptions.MarkElementsNonNull && value == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw Errors.shouldNotReachHere();
        }
        return value;
    }
}

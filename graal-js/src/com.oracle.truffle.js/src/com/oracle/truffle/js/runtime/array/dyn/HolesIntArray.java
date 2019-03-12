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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetHoleCount;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetHoleCount;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class HolesIntArray extends AbstractContiguousIntArray {

    private static final HolesIntArray HOLES_INT_ARRAY = new HolesIntArray(INTEGRITY_LEVEL_NONE, createCache());
    public static final int HOLE_VALUE = Integer.MIN_VALUE;

    private HolesIntArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    public static HolesIntArray makeHolesIntArray(DynamicObject object, int length, int[] array, long indexOffset, int arrayOffset, int usedLength, int holeCount, int integrityLevel) {
        HolesIntArray arrayType = createHolesIntArray().setIntegrityLevel(integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        assert holeCount == arrayType.countHoles(object) : String.format("holeCount, %d, differs from the actual count, %d", holeCount, arrayType.countHoles(object));
        return arrayType;
    }

    private static HolesIntArray createHolesIntArray() {
        return HOLES_INT_ARRAY;
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        return this;
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, int value, boolean condition) {
        throw new RuntimeException("should not call this method, use setInBounds(Non)Hole");
    }

    public boolean isHoleFast(DynamicObject object, int index, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        return isHolePrepared(object, internalIndex, condition);
    }

    public void setInBoundsFastHole(DynamicObject object, int index, int value, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        assert isHolePrepared(object, internalIndex, condition);
        incrementHolesCount(object, -1);
        setInBoundyFastIntl(object, index, internalIndex, value, condition);
    }

    public void setInBoundsFastNonHole(DynamicObject object, int index, int value, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        assert !isHolePrepared(object, internalIndex, condition);
        setInBoundyFastIntl(object, index, internalIndex, value, condition);
    }

    private void setInBoundyFastIntl(DynamicObject object, int index, int internalIndex, int value, boolean condition) {
        getArray(object, condition)[internalIndex] = value;
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    public boolean containsHoles(DynamicObject object, long index, boolean condition) {
        return arrayGetHoleCount(object, condition) > 0 || !isInBoundsFast(object, index, condition);
    }

    @Override
    public int prepareInBounds(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareInBoundsHoles(object, index, condition, profile);
    }

    @Override
    public boolean isSupported(DynamicObject object, long index, boolean condition) {
        return isSupportedHoles(object, index, condition);
    }

    @Override
    public int prepareSupported(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareSupportedHoles(object, index, condition, profile);
    }

    @Override
    public AbstractIntArray toNonHoles(DynamicObject object, long index, Object value, boolean condition) {
        assert !containsHoles(object, index, condition);
        int[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        AbstractIntArray newArray;
        setInBoundsFastNonHole(object, (int) index, (int) value, condition);
        if (indexOffset == 0 && arrayOffset == 0) {
            newArray = ZeroBasedIntArray.makeZeroBasedIntArray(object, length, usedLength, array, integrityLevel);
        } else {
            newArray = ContiguousIntArray.makeContiguousIntArray(object, length, array, indexOffset, arrayOffset, usedLength, integrityLevel);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public Object getInBoundsFast(DynamicObject object, int index, boolean condition) {
        int value = getInBoundsFastInt(object, index, condition);
        if (HolesIntArray.isHoleValue(value)) {
            return Undefined.instance;
        }
        return value;
    }

    @Override
    protected void incrementHolesCount(DynamicObject object, int offset) {
        arraySetHoleCount(object, arrayGetHoleCount(object) + offset);
    }

    @Override
    public HolesIntArray toHoles(DynamicObject object, long index, Object value, boolean condition) {
        return this;
    }

    @Override
    public AbstractWritableArray toDouble(DynamicObject object, long index, double value, boolean condition) {
        int[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);
        int holeCount = arrayGetHoleCount(object, condition);

        double[] doubleCopy = ArrayCopy.intToDoubleHoles(array, arrayOffset, usedLength);
        HolesDoubleArray newArray = HolesDoubleArray.makeHolesDoubleArray(object, length, doubleCopy, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray toObject(DynamicObject object, long index, Object value, boolean condition) {
        int[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);
        int holeCount = arrayGetHoleCount(object, condition);

        Object[] objectCopy = ArrayCopy.intToObjectHoles(array, arrayOffset, usedLength);
        HolesObjectArray newArray = HolesObjectArray.makeHolesObjectArray(object, length, objectCopy, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    public static boolean isHoleValue(int value) {
        return value == HOLE_VALUE;
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index0, boolean condition) {
        return nextElementIndexHoles(object, index0, condition);
    }

    @Override
    public long previousElementIndex(DynamicObject object, long index0, boolean condition) {
        return previousElementIndexHoles(object, index0, condition);
    }

    @Override
    public boolean hasElement(DynamicObject object, long index, boolean condition) {
        return super.hasElement(object, index, condition) && !isHolePrepared(object, prepareInBoundsFast(object, (int) index, condition), condition);
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return deleteElementHoles(object, index, condition);
    }

    @Override
    protected HolesObjectArray toObjectHoles(DynamicObject object, boolean condition) {
        throw new UnsupportedOperationException("already a holes array");
    }

    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        return removeRangeHoles(object, start, end);
    }

    @Override
    protected HolesIntArray withIntegrityLevel(int newIntegrityLevel) {
        return new HolesIntArray(newIntegrityLevel, cache);
    }
}

/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class HolesIntArray extends AbstractContiguousIntArray {

    private static final HolesIntArray HOLES_INT_ARRAY = new HolesIntArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();
    public static final int HOLE_VALUE = Integer.MIN_VALUE;

    private HolesIntArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    public static HolesIntArray makeHolesIntArray(JSDynamicObject object, int length, int[] array, long indexOffset, int arrayOffset, int usedLength, int holeCount, int integrityLevel) {
        HolesIntArray arrayType = createHolesIntArray().setIntegrityLevel(integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        assert arrayType.assertHoleCount(object);
        return arrayType;
    }

    private static HolesIntArray createHolesIntArray() {
        return HOLES_INT_ARRAY;
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(JSDynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        return this;
    }

    @Override
    public void setInBoundsFast(JSDynamicObject object, int index, int value) {
        throw Errors.shouldNotReachHere("should not call this method, use setInBounds(Non)Hole");
    }

    public boolean isHoleFast(JSDynamicObject object, int index) {
        int internalIndex = (int) (index - getIndexOffset(object));
        return isHolePrepared(object, internalIndex);
    }

    public void setInBoundsFastHole(JSDynamicObject object, int index, int value) {
        int internalIndex = (int) (index - getIndexOffset(object));
        assert isHolePrepared(object, internalIndex);
        incrementHolesCount(object, -1);
        setInBoundyFastIntl(object, index, internalIndex, value);
    }

    public void setInBoundsFastNonHole(JSDynamicObject object, int index, int value) {
        int internalIndex = (int) (index - getIndexOffset(object));
        assert !isHolePrepared(object, internalIndex);
        setInBoundyFastIntl(object, index, internalIndex, value);
    }

    private void setInBoundyFastIntl(JSDynamicObject object, int index, int internalIndex, int value) {
        getArray(object)[internalIndex] = value;
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    public boolean containsHoles(JSDynamicObject object, long index) {
        return arrayGetHoleCount(object) > 0 || !isInBoundsFast(object, index);
    }

    @Override
    public int prepareInBounds(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        return prepareInBoundsHoles(object, index, node, profile);
    }

    @Override
    public boolean isSupported(JSDynamicObject object, long index) {
        return isSupportedHoles(object, index);
    }

    @Override
    public int prepareSupported(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        return prepareSupportedHoles(object, index, node, profile);
    }

    @Override
    public AbstractIntArray toNonHoles(JSDynamicObject object, long index, Object value) {
        assert !containsHoles(object, index);
        int[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        int arrayOffset = getArrayOffset(object);
        long indexOffset = getIndexOffset(object);

        AbstractIntArray newArray;
        setInBoundsFastNonHole(object, (int) index, (int) value);
        if (indexOffset == 0 && arrayOffset == 0) {
            newArray = ZeroBasedIntArray.makeZeroBasedIntArray(object, length, usedLength, array, integrityLevel);
        } else {
            newArray = ContiguousIntArray.makeContiguousIntArray(object, length, array, indexOffset, arrayOffset, usedLength, integrityLevel);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public Object getInBoundsFast(JSDynamicObject object, int index) {
        int value = getInBoundsFastInt(object, index);
        if (HolesIntArray.isHoleValue(value)) {
            return Undefined.instance;
        }
        return value;
    }

    @Override
    public HolesIntArray toHoles(JSDynamicObject object, long index, Object value) {
        return this;
    }

    @Override
    public AbstractWritableArray toDouble(JSDynamicObject object, long index, double value) {
        int[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        int arrayOffset = getArrayOffset(object);
        long indexOffset = getIndexOffset(object);
        int holeCount = arrayGetHoleCount(object);

        double[] doubleCopy = ArrayCopy.intToDoubleHoles(array, arrayOffset, usedLength);
        HolesDoubleArray newArray = HolesDoubleArray.makeHolesDoubleArray(object, length, doubleCopy, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray toObject(JSDynamicObject object, long index, Object value) {
        int[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        int arrayOffset = getArrayOffset(object);
        long indexOffset = getIndexOffset(object);
        int holeCount = arrayGetHoleCount(object);

        Object[] objectCopy = ArrayCopy.intToObjectHoles(array, arrayOffset, usedLength);
        HolesObjectArray newArray = HolesObjectArray.makeHolesObjectArray(object, length, objectCopy, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    public static boolean isHoleValue(int value) {
        return value == HOLE_VALUE;
    }

    @Override
    public long nextElementIndex(JSDynamicObject object, long index0) {
        return nextElementIndexHoles(object, index0);
    }

    @Override
    public long previousElementIndex(JSDynamicObject object, long index0) {
        return previousElementIndexHoles(object, index0);
    }

    @Override
    public boolean hasElement(JSDynamicObject object, long index) {
        return super.hasElement(object, index) && !isHolePrepared(object, prepareInBoundsFast(object, (int) index));
    }

    @Override
    public ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict) {
        return deleteElementHoles(object, index);
    }

    @Override
    protected HolesObjectArray toObjectHoles(JSDynamicObject object) {
        throw new UnsupportedOperationException("already a holes array");
    }

    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public boolean hasHoles(JSDynamicObject object) {
        return arrayGetHoleCount(object) > 0;
    }

    @Override
    public ScriptArray removeRangeImpl(JSDynamicObject object, long start, long end) {
        return removeRangeHoles(object, start, end);
    }

    @Override
    protected HolesIntArray withIntegrityLevel(int newIntegrityLevel) {
        return new HolesIntArray(newIntegrityLevel, cache);
    }

    @Override
    public List<Object> ownPropertyKeys(JSDynamicObject object) {
        return ownPropertyKeysHoles(object);
    }
}

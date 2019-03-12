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

public final class HolesJSObjectArray extends AbstractContiguousJSObjectArray {

    private static final HolesJSObjectArray HOLES_JSOBJECT_ARRAY = new HolesJSObjectArray(INTEGRITY_LEVEL_NONE, createCache());

    private HolesJSObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    public static HolesJSObjectArray makeHolesJSObjectArray(DynamicObject object, int length, DynamicObject[] array, long indexOffset, int arrayOffset, int usedLength, int holeCount,
                    int integrityLevel) {
        HolesJSObjectArray arrayType = createHolesJSObjectArray().setIntegrityLevel(integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        assert holeCount == arrayType.countHoles(object);
        return arrayType;
    }

    private static HolesJSObjectArray createHolesJSObjectArray() {
        return HOLES_JSOBJECT_ARRAY;
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        return this;
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, DynamicObject value, boolean condition) {
        throw new RuntimeException("should not call this method, use setInBounds(Non)Hole");
    }

    public boolean isHoleFast(DynamicObject object, int index, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        return isHolePrepared(object, internalIndex, condition);
    }

    public void setInBoundsFastHole(DynamicObject object, int index, DynamicObject value, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        assert isHolePrepared(object, internalIndex, condition);
        incrementHolesCount(object, -1);
        setInBoundsFastIntl(object, index, internalIndex, value, condition);
    }

    public void setInBoundsFastNonHole(DynamicObject object, int index, DynamicObject value, boolean condition) {
        int internalIndex = (int) (index - getIndexOffset(object, condition));
        assert !isHolePrepared(object, internalIndex, condition);
        setInBoundsFastIntl(object, index, internalIndex, value, condition);
    }

    private void setInBoundsFastIntl(DynamicObject object, int index, int internalIndex, DynamicObject value, boolean condition) {
        getArray(object, condition)[internalIndex] = checkNonNull(value);
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    public boolean containsHoles(DynamicObject object, long index, boolean condition) {
        return arrayGetHoleCount(object, condition) > 0 || !isInBoundsFast(object, index, condition);
    }

    @Override
    public AbstractJSObjectArray toNonHoles(DynamicObject object, long index, Object value, boolean condition) {
        assert !containsHoles(object, index, condition);
        DynamicObject[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        AbstractJSObjectArray newArray;
        setInBoundsFastNonHole(object, (int) index, (DynamicObject) value, arrayCondition());
        if (isInBoundsFast(object, 0)) {
            newArray = ZeroBasedJSObjectArray.makeZeroBasedJSObjectArray(object, length, usedLength, array, integrityLevel);
        } else {
            newArray = ContiguousJSObjectArray.makeContiguousJSObjectArray(object, length, array, indexOffset, arrayOffset, usedLength, integrityLevel);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray toObject(DynamicObject object, long index, Object value, boolean condition) {
        DynamicObject[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);
        int holeCount = arrayGetHoleCount(object, condition);

        Object[] objectCopy = ArrayCopy.jsobjectToObjectHoles(array, arrayOffset, usedLength);
        HolesObjectArray newArray = HolesObjectArray.makeHolesObjectArray(object, length, objectCopy, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    protected void incrementHolesCount(DynamicObject object, int offset) {
        arraySetHoleCount(object, arrayGetHoleCount(object) + offset);
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
    public Object getInBoundsFast(DynamicObject object, int index, boolean condition) {
        DynamicObject value = getInBoundsFastJSObject(object, index, condition);
        if (HolesJSObjectArray.isHoleValue(value)) {
            return Undefined.instance;
        }
        return value;
    }

    @Override
    public HolesJSObjectArray toHoles(DynamicObject object, long index, Object value, boolean condition) {
        return this;
    }

    public static boolean isHoleValue(DynamicObject value) {
        return value == null;
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
    public boolean isHolesType() {
        return true;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        return removeRangeHoles(object, start, end);
    }

    @Override
    protected DynamicObject castNonNull(DynamicObject value) {
        return value;
    }

    @Override
    protected HolesJSObjectArray withIntegrityLevel(int newIntegrityLevel) {
        return new HolesJSObjectArray(newIntegrityLevel, cache);
    }
}

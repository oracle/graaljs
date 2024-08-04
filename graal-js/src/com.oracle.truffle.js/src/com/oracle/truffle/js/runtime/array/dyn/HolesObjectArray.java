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

public final class HolesObjectArray extends AbstractContiguousObjectArray {

    private static final HolesObjectArray HOLES_OBJECT_ARRAY = new HolesObjectArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    private HolesObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    public static HolesObjectArray makeHolesObjectArray(JSDynamicObject object, int length, Object[] array, long indexOffset, int arrayOffset, int usedLength, int holeCount, int integrityLevel) {
        HolesObjectArray arrayType = createHolesObjectArray().setIntegrityLevel(integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        assert arrayType.assertHoleCount(object);
        return arrayType;
    }

    public static HolesObjectArray createHolesObjectArray() {
        return HOLES_OBJECT_ARRAY;
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(JSDynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        arraySetHoleCount(object, holeCount);
        return this;
    }

    @Override
    public void setInBoundsFast(JSDynamicObject object, int index, Object value) {
        throw Errors.shouldNotReachHere("should not call this method, use setInBounds(Non)Hole");
    }

    public boolean isHoleFast(JSDynamicObject object, int index) {
        int internalIndex = (int) (index - getIndexOffset(object));
        return isHolePrepared(object, internalIndex);
    }

    public void setInBoundsFastHole(JSDynamicObject object, int index, Object value) {
        int internalIndex = (int) (index - getIndexOffset(object));
        assert isHolePrepared(object, internalIndex);
        incrementHolesCount(object, -1);
        setInBoundsFastIntl(object, index, internalIndex, value);
    }

    public void setInBoundsFastNonHole(JSDynamicObject object, int index, Object value) {
        int internalIndex = (int) (index - getIndexOffset(object));
        assert !isHolePrepared(object, internalIndex);
        setInBoundsFastIntl(object, index, internalIndex, checkNonNull(value));
    }

    private void setInBoundsFastIntl(JSDynamicObject object, int index, int internalIndex, Object value) {
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
    public AbstractObjectArray toNonHoles(JSDynamicObject object, long index, Object value) {
        assert !containsHoles(object, index);
        Object[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        int arrayOffset = getArrayOffset(object);
        long indexOffset = getIndexOffset(object);

        AbstractObjectArray newArray;
        setInBoundsFastNonHole(object, (int) index, value);
        if (indexOffset == 0 && arrayOffset == 0) {
            newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, length, usedLength, array, integrityLevel);
        } else {
            newArray = ContiguousObjectArray.makeContiguousObjectArray(object, length, array, indexOffset, arrayOffset, usedLength, integrityLevel);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
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
    public Object getInBoundsFast(JSDynamicObject object, int index) {
        Object value = getInBoundsFastObject(object, index);
        if (HolesObjectArray.isHoleValue(value)) {
            return Undefined.instance;
        }
        return value;
    }

    @Override
    public HolesObjectArray toHoles(JSDynamicObject object, long index, Object value) {
        return this;
    }

    public static boolean isHoleValue(Object value) {
        return value == null;
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
    protected Object castNonNull(Object value) {
        return value;
    }

    @Override
    protected HolesObjectArray withIntegrityLevel(int newIntegrityLevel) {
        return new HolesObjectArray(newIntegrityLevel, cache);
    }

    @Override
    public List<Object> ownPropertyKeys(JSDynamicObject object) {
        return ownPropertyKeysHoles(object);
    }
}

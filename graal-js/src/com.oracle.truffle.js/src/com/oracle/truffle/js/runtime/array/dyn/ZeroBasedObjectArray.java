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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetUsedLength;

import java.util.Arrays;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public final class ZeroBasedObjectArray extends AbstractObjectArray {

    private static final ZeroBasedObjectArray ZERO_BASED_OBJECT_ARRAY = new ZeroBasedObjectArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    public static ZeroBasedObjectArray makeZeroBasedObjectArray(JSDynamicObject object, int length, int usedLength, Object[] array, int integrityLevel) {
        ZeroBasedObjectArray arrayType = createZeroBasedObjectArray().setIntegrityLevel(integrityLevel);
        arraySetLength(object, length);
        arraySetUsedLength(object, usedLength);
        arraySetArray(object, array);
        return arrayType;
    }

    public static ZeroBasedObjectArray createZeroBasedObjectArray() {
        return ZERO_BASED_OBJECT_ARRAY;
    }

    private ZeroBasedObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public boolean isSupported(JSDynamicObject object, long index) {
        return isSupportedZeroBased(object, (int) index);
    }

    @Override
    public Object getInBoundsFastObject(JSDynamicObject object, int index) {
        return castNonNull(getArray(object)[index]);
    }

    @Override
    public void setInBoundsFast(JSDynamicObject object, int index, Object value) {
        getArray(object)[index] = checkNonNull(value);
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    protected int prepareInBoundsFast(JSDynamicObject object, long index) {
        return (int) index;
    }

    @Override
    protected int prepareInBounds(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        prepareInBoundsZeroBased(object, index, node, profile);
        return index;
    }

    @Override
    protected int prepareSupported(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        prepareSupportedZeroBased(object, index, node, profile);
        return index;
    }

    @Override
    protected void setLengthLess(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile) {
        setLengthLessZeroBased(object, length, node, profile);
    }

    @Override
    public ContiguousObjectArray toContiguous(JSDynamicObject object, long index, Object value) {
        Object[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);

        ContiguousObjectArray newArray = ContiguousObjectArray.makeContiguousObjectArray(object, length, array, 0, 0, usedLength, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public HolesObjectArray toHoles(JSDynamicObject object, long index, Object value) {
        Object[] array = getArray(object);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);

        HolesObjectArray newArray = HolesObjectArray.makeHolesObjectArray(object, length, array, 0, 0, usedLength, 0, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public long firstElementIndex(JSDynamicObject object) {
        return 0;
    }

    @Override
    public long lastElementIndex(JSDynamicObject object) {
        return getUsedLength(object) - 1;
    }

    @Override
    public ScriptArray removeRangeImpl(JSDynamicObject object, long start, long end) {
        Object[] array = getArray(object);
        int usedLength = getUsedLength(object);
        long moveLength = usedLength - end;
        if (moveLength > 0) {
            System.arraycopy(array, (int) end, array, (int) start, (int) moveLength);
        }
        if (start < usedLength) {
            Arrays.fill(array, (int) (start + Math.max(0, moveLength)), usedLength, null);
            int newUsedLength = (int) ((moveLength > 0) ? (usedLength - (end - start)) : start);
            arraySetUsedLength(object, newUsedLength);
        }
        return this;
    }

    @Override
    public ScriptArray shiftRangeImpl(JSDynamicObject object, long from) {
        int usedLength = getUsedLength(object);
        if (from < usedLength) {
            Object[] array = getArray(object);
            Arrays.fill(array, 0, (int) from, null);
            return ContiguousObjectArray.makeContiguousObjectArray(object, lengthInt(object) - from, array, -from, (int) from, (int) (usedLength - from), integrityLevel);
        } else {
            return removeRangeImpl(object, 0, from);
        }
    }

    @Override
    public ScriptArray addRangeImpl(JSDynamicObject object, long offset, int size) {
        return addRangeImplZeroBased(object, offset, size);
    }

    @Override
    protected ZeroBasedObjectArray withIntegrityLevel(int newIntegrityLevel) {
        return new ZeroBasedObjectArray(newIntegrityLevel, cache);
    }

    @Override
    public long nextElementIndex(JSDynamicObject object, long index) {
        return nextElementIndexZeroBased(object, index);
    }
}

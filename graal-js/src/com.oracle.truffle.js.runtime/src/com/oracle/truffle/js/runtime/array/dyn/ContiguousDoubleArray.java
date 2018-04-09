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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ContiguousDoubleArray extends AbstractContiguousDoubleArray {

    private static final ContiguousDoubleArray CONTIGUOUS_DOUBLE_ARRAY = new ContiguousDoubleArray(INTEGRITY_LEVEL_NONE, createCache());

    public static ContiguousDoubleArray makeContiguousDoubleArray(DynamicObject object, long length, double[] array, long indexOffset, int arrayOffset, int usedLength, int integrityLevel) {
        ContiguousDoubleArray arrayType = createContiguousDoubleArray().setIntegrityLevel(integrityLevel);
        setArrayProperties(object, array, length, usedLength, indexOffset, arrayOffset);
        return arrayType;
    }

    private static ContiguousDoubleArray createContiguousDoubleArray() {
        return CONTIGUOUS_DOUBLE_ARRAY;
    }

    private ContiguousDoubleArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    protected int prepareInBounds(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareInBoundsContiguous(object, index, condition, profile);
    }

    @Override
    protected int prepareSupported(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        return prepareSupportedContiguous(object, index, condition, profile);
    }

    @Override
    public boolean isSupported(DynamicObject object, long index, boolean condition) {
        return isSupportedContiguous(object, index, condition);
    }

    @Override
    public ContiguousObjectArray toObject(DynamicObject object, long index, Object value, boolean condition) {
        double[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        Object[] doubleCopy = ArrayCopy.doubleToObject(array, arrayOffset, usedLength);
        ContiguousObjectArray newArray = ContiguousObjectArray.makeContiguousObjectArray(object, length, doubleCopy, indexOffset, arrayOffset, usedLength, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ZeroBasedDoubleArray toNonContiguous(DynamicObject object, int index, Object value, boolean condition, ProfileHolder profile) {
        setSupported(object, index, (double) value, condition, profile);

        double[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        ZeroBasedDoubleArray newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, length, usedLength, array, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public HolesDoubleArray toHoles(DynamicObject object, long index, Object value, boolean condition) {
        double[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        int arrayOffset = getArrayOffset(object, condition);
        long indexOffset = getIndexOffset(object, condition);

        HolesDoubleArray newArray = HolesDoubleArray.makeHolesDoubleArray(object, length, array, indexOffset, arrayOffset, usedLength, 0, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        return removeRangeContiguous(object, start, end);
    }

    @Override
    protected ContiguousDoubleArray withIntegrityLevel(int newIntegrityLevel) {
        return new ContiguousDoubleArray(newIntegrityLevel, cache);
    }
}

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

import static com.oracle.truffle.api.CompilerDirectives.FASTPATH_PROBABILITY;
import static com.oracle.truffle.api.CompilerDirectives.injectBranchProbability;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public abstract class AbstractDoubleArray extends AbstractWritableArray {

    protected AbstractDoubleArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(JSDynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        return HolesDoubleArray.makeHolesDoubleArray(object, length, (double[]) array, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
    }

    public abstract void setInBoundsFast(JSDynamicObject object, int index, double value);

    @Override
    public final ScriptArray setElementImpl(JSDynamicObject object, long index, Object value, boolean strict) {
        assert JSRuntime.isArrayIndex(index) : index;
        if (injectBranchProbability(FASTPATH_PROBABILITY, (value instanceof Integer || value instanceof Double) && isSupported(object, (int) index))) {
            double doubleValue = JSRuntime.doubleValue((Number) value);
            assert !HolesDoubleArray.isHoleValue(doubleValue);
            setSupported(object, (int) index, doubleValue, null, SetSupportedProfileAccess.getUncached());
            return this;
        } else {
            return rewrite(object, index, value).setElementImpl(object, index, value, strict);
        }
    }

    private ScriptArray rewrite(JSDynamicObject object, long index, Object value) {
        if (value instanceof Integer || value instanceof Double) {
            if (isSupportedContiguous(object, index)) {
                return toContiguous(object, index, value);
            } else if (isSupportedHoles(object, index)) {
                return toHoles(object, index, value);
            } else {
                return toSparse(object, index, value);
            }
        } else {
            return toObject(object, index, value);
        }
    }

    @Override
    public Object getInBoundsFast(JSDynamicObject object, int index) {
        return getInBoundsFastDouble(object, index);
    }

    @Override
    public abstract double getInBoundsFastDouble(JSDynamicObject object, int index);

    @Override
    int getArrayLength(Object array) {
        return ((double[]) array).length;
    }

    protected static double[] getArray(JSDynamicObject object) {
        Object array = arrayGetArray(object);
        if (array.getClass() == double[].class) {
            return (double[]) array;
        } else {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    public final void setInBounds(JSDynamicObject object, int index, double value, Node node, SetSupportedProfileAccess profile) {
        getArray(object)[prepareInBounds(object, index, node, profile)] = value;
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBounds", index, value);
        }
    }

    public final void setSupported(JSDynamicObject object, int index, double value, Node node, SetSupportedProfileAccess profile) {
        int preparedIndex = prepareSupported(object, index, node, profile);
        getArray(object)[preparedIndex] = value;
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("Supported", index, value);
        }
    }

    @Override
    void fillWithHoles(Object array, int fromIndex, int toIndex) {
        double[] doubleArray = (double[]) array;
        for (int i = fromIndex; i < toIndex; i++) {
            doubleArray[i] = HolesDoubleArray.HOLE_VALUE_DOUBLE;
        }
    }

    @Override
    protected final void setHoleValue(JSDynamicObject object, int preparedIndex) {
        getArray(object)[preparedIndex] = HolesDoubleArray.HOLE_VALUE_DOUBLE;
    }

    @Override
    protected final boolean isHolePrepared(JSDynamicObject object, int preparedIndex) {
        return HolesDoubleArray.isHoleValue(getArray(object)[preparedIndex]);
    }

    @Override
    protected final int getArrayCapacity(JSDynamicObject object) {
        return getArray(object).length;
    }

    @Override
    protected final void resizeArray(JSDynamicObject object, int newCapacity, int oldCapacity, int offset) {
        double[] newArray = new double[newCapacity];
        System.arraycopy(getArray(object), 0, newArray, offset, oldCapacity);
        arraySetArray(object, newArray);
    }

    @Override
    public abstract AbstractDoubleArray toHoles(JSDynamicObject object, long index, Object value);

    @Override
    public final AbstractWritableArray toDouble(JSDynamicObject object, long index, double value) {
        return this;
    }

    @Override
    public ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict) {
        return toHoles(object, index, HolesDoubleArray.HOLE_VALUE).deleteElementImpl(object, index, strict);
    }

    @Override
    protected final void moveRangePrepared(JSDynamicObject object, int src, int dst, int len) {
        double[] array = getArray(object);
        System.arraycopy(array, src, array, dst, len);
    }

    @Override
    public final Object allocateArray(int length) {
        return new double[length];
    }

    @Override
    public Object cloneArray(JSDynamicObject object) {
        return getArray(object).clone();
    }

    @Override
    protected abstract AbstractDoubleArray withIntegrityLevel(int newIntegrityLevel);
}

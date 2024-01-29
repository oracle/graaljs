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

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public abstract class AbstractJSObjectArray extends AbstractWritableArray {

    protected AbstractJSObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    AbstractWritableArray sameTypeHolesArray(JSDynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount) {
        return HolesJSObjectArray.makeHolesJSObjectArray(object, length, (JSDynamicObject[]) array, indexOffset, arrayOffset, usedLength, holeCount, integrityLevel);
    }

    public abstract void setInBoundsFast(JSDynamicObject object, int index, JSDynamicObject value);

    @Override
    public final ScriptArray setElementImpl(JSDynamicObject object, long index, Object value, boolean strict) {
        assert index >= 0;
        if (injectBranchProbability(FASTPATH_PROBABILITY, JSDynamicObject.isJSDynamicObject(value) && isSupported(object, index))) {
            setSupported(object, (int) index, (JSDynamicObject) value, null, SetSupportedProfileAccess.getUncached());
            return this;
        } else {
            return rewrite(object, index, value).setElementImpl(object, index, value, strict);
        }
    }

    private ScriptArray rewrite(JSDynamicObject object, long index, Object value) {
        if (JSDynamicObject.isJSDynamicObject(value)) {
            assert !isSupported(object, index);
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
        return getInBoundsFastJSObject(object, index);
    }

    @Override
    int getArrayLength(Object array) {
        return ((JSDynamicObject[]) array).length;
    }

    protected static JSDynamicObject[] getArray(JSDynamicObject object) {
        Object array = arrayGetArray(object);
        if (array.getClass() == JSDynamicObject[].class) {
            return CompilerDirectives.castExact(array, JSDynamicObject[].class);
        } else {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    public abstract JSDynamicObject getInBoundsFastJSObject(JSDynamicObject object, int index);

    public final void setInBounds(JSDynamicObject object, int index, JSDynamicObject value, Node node, SetSupportedProfileAccess profile) {
        getArray(object)[prepareInBounds(object, index, node, profile)] = checkNonNull(value);
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBounds", index, value);
        }
    }

    public final void setSupported(JSDynamicObject object, int index, JSDynamicObject value, Node node, SetSupportedProfileAccess profile) {
        int preparedIndex = prepareSupported(object, index, node, profile);
        getArray(object)[preparedIndex] = checkNonNull(value);
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("Supported", index, value);
        }
    }

    @Override
    void fillWithHoles(Object array, int fromIndex, int toIndex) {
        JSDynamicObject[] objectArray = (JSDynamicObject[]) array;
        for (int i = fromIndex; i < toIndex; i++) {
            objectArray[i] = null;
        }
    }

    @Override
    protected final void setHoleValue(JSDynamicObject object, int preparedIndex) {
        getArray(object)[preparedIndex] = null;
    }

    @Override
    protected final void fillHoles(JSDynamicObject object, int internalIndex, int grown, Node node, SetSupportedProfileAccess profile) {
        if (grown != 0) {
            incrementHolesCount(object, Math.abs(grown) - 1);
        }
        assert checkFillHoles(object, internalIndex, grown);
    }

    @Override
    protected final boolean isHolePrepared(JSDynamicObject object, int preparedIndex) {
        return HolesObjectArray.isHoleValue(getArray(object)[preparedIndex]);
    }

    @Override
    protected final int getArrayCapacity(JSDynamicObject object) {
        return getArray(object).length;
    }

    @Override
    protected final void resizeArray(JSDynamicObject object, int newCapacity, int oldCapacity, int offset) {
        JSDynamicObject[] newArray = new JSDynamicObject[newCapacity];
        System.arraycopy(getArray(object), 0, newArray, offset, oldCapacity);
        arraySetArray(object, newArray);
    }

    @Override
    public abstract AbstractJSObjectArray toHoles(JSDynamicObject object, long index, Object value);

    @Override
    public abstract AbstractWritableArray toObject(JSDynamicObject object, long index, Object value);

    @Override
    public final AbstractWritableArray toDouble(JSDynamicObject object, long index, double value) {
        return this;
    }

    @Override
    public ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict) {
        return toHoles(object, index, null).deleteElementImpl(object, index, strict);
    }

    @Override
    protected final void moveRangePrepared(JSDynamicObject object, int src, int dst, int len) {
        JSDynamicObject[] array = getArray(object);
        System.arraycopy(array, src, array, dst, len);
    }

    @Override
    public final Object allocateArray(int length) {
        return new JSDynamicObject[length];
    }

    @Override
    public Object cloneArray(JSDynamicObject object) {
        return getArray(object).clone();
    }

    @Override
    protected abstract AbstractJSObjectArray withIntegrityLevel(int newIntegrityLevel);

    protected static JSDynamicObject checkNonNull(JSDynamicObject value) {
        assert value != null;
        return value;
    }

    protected JSDynamicObject castNonNull(JSDynamicObject value) {
        if (JSConfig.MarkElementsNonNull) {
            return Objects.requireNonNull(value);
        } else {
            return value;
        }
    }
}

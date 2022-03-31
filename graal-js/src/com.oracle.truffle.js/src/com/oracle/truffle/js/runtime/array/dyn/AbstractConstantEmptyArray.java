/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class AbstractConstantEmptyArray extends AbstractConstantArray {

    protected AbstractConstantEmptyArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    protected static void setCapacity(JSDynamicObject object, long length) {
        JSArray.arraySetLength(object, length);
    }

    protected static long getCapacity(JSDynamicObject object) {
        return JSArray.arrayGetLength(object);
    }

    @Override
    public Object getElementInBounds(JSDynamicObject object, int index) {
        return Undefined.instance;
    }

    @Override
    public int lengthInt(JSDynamicObject object) {
        return (int) getCapacity(object);
    }

    @Override
    public Object cloneArray(JSDynamicObject object) {
        return ScriptArray.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean hasElement(JSDynamicObject object, long index) {
        return false;
    }

    @Override
    public ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict) {
        return this;
    }

    @Override
    public long firstElementIndex(JSDynamicObject object) {
        return 0; // there is no element in this array
    }

    @Override
    public long lastElementIndex(JSDynamicObject object) {
        return -1; // there is no element in this array
    }

    @Override
    public long nextElementIndex(JSDynamicObject object, long index) {
        return JSRuntime.MAX_SAFE_INTEGER_LONG;
    }

    @Override
    public long previousElementIndex(JSDynamicObject object, long index) {
        return -1;
    }

    @Override
    public AbstractIntArray createWriteableInt(JSDynamicObject object, long index, int value, ProfileHolder profile) {
        assert index >= 0; // corner case, length would not be int then
        int capacity = lengthInt(object);
        int[] initialArray = new int[calcNewArraySize(capacity, profile)];
        AbstractIntArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedIntArray.makeZeroBasedIntArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableIntContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractIntArray createWritableIntContiguous(JSDynamicObject object, int capacity, long index, int[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousIntArray.makeContiguousIntArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    private static int calcNewArraySize(int capacity, ProfileHolder profile) {
        if (CREATE_WRITABLE_PROFILE.lengthZero(profile, capacity == 0)) {
            return JSConfig.InitialArraySize;
        } else if (CREATE_WRITABLE_PROFILE.lengthBelowLimit(profile, capacity < JSConfig.MaxFlatArraySize)) {
            return capacity;
        } else {
            return JSConfig.InitialArraySize;
        }
    }

    @Override
    public AbstractDoubleArray createWriteableDouble(JSDynamicObject object, long index, double value, ProfileHolder profile) {
        int capacity = lengthInt(object);
        double[] initialArray = new double[calcNewArraySize(capacity, profile)];
        AbstractDoubleArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableDoubleContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractDoubleArray createWritableDoubleContiguous(JSDynamicObject object, int capacity, long index, double[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousDoubleArray.makeContiguousDoubleArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    @Override
    public AbstractJSObjectArray createWriteableJSObject(JSDynamicObject object, long index, JSDynamicObject value, ProfileHolder profile) {
        int capacity = lengthInt(object);
        JSDynamicObject[] initialArray = new JSDynamicObject[calcNewArraySize(capacity, profile)];
        AbstractJSObjectArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedJSObjectArray.makeZeroBasedJSObjectArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableJSObjectContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractJSObjectArray createWritableJSObjectContiguous(JSDynamicObject object, int capacity, long index, JSDynamicObject[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousJSObjectArray.makeContiguousJSObjectArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    @Override
    public AbstractObjectArray createWriteableObject(JSDynamicObject object, long index, Object value, ProfileHolder profile) {
        int capacity = lengthInt(object);
        Object[] initialArray = new Object[calcNewArraySize(capacity, profile)];
        AbstractObjectArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableObjectContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractObjectArray createWritableObjectContiguous(JSDynamicObject object, int capacity, long index, Object[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousObjectArray.makeContiguousObjectArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public boolean hasHoles(JSDynamicObject object) {
        return getCapacity(object) != 0;
    }

    @Override
    public List<Object> ownPropertyKeys(JSDynamicObject object) {
        return ownPropertyKeysContiguous(object);
    }

    private void notifyAllocationSite(JSDynamicObject object, ScriptArray newArray) {
        if (JSConfig.TrackArrayAllocationSites && CompilerDirectives.inInterpreter()) {
            ArrayAllocationSite site = JSAbstractArray.arrayGetAllocationSite(object);
            if (site != null) {
                site.notifyArrayTransition(newArray, lengthInt(object));
            }
        }
    }
}

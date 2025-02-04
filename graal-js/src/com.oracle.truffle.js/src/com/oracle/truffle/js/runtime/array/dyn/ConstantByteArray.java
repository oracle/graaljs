/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public final class ConstantByteArray extends AbstractConstantArray {
    private static final ConstantByteArray CONSTANT_BYTE_ARRAY = new ConstantByteArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    public static ConstantByteArray createConstantByteArray() {
        return CONSTANT_BYTE_ARRAY;
    }

    private ConstantByteArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    private static byte[] getArray(JSDynamicObject object) {
        return (byte[]) arrayGetArray(object);
    }

    @Override
    public Object getElementInBounds(JSDynamicObject object, int index) {
        return getElementByte(object, index);
    }

    public static int getElementByte(JSDynamicObject object, int index) {
        return getArray(object)[index];
    }

    @Override
    public long length(JSDynamicObject object) {
        return lengthInt(object);
    }

    @Override
    public int lengthInt(JSDynamicObject object) {
        return getArray(object).length;
    }

    @Override
    public boolean hasElement(JSDynamicObject object, long index) {
        return index >= 0 && index < getArray(object).length;
    }

    @Override
    public Object cloneArray(JSDynamicObject object) {
        return getArray(object);
    }

    @Override
    public ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict) {
        return createWriteableInt(object, index, HolesIntArray.HOLE_VALUE, null, CreateWritableProfileAccess.getUncached()).deleteElementImpl(object, index, strict);
    }

    @Override
    public ScriptArray setLengthImpl(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile) {
        return createWriteableInt(object, length - 1, HolesIntArray.HOLE_VALUE, node, profile).setLengthImpl(object, length, node, profile);
    }

    @Override
    public ZeroBasedIntArray createWriteableInt(JSDynamicObject object, long index, int value, Node node, CreateWritableProfileAccess profile) {
        int[] intCopy = ArrayCopy.byteToInt(getArray(object));
        ZeroBasedIntArray newArray = ZeroBasedIntArray.makeZeroBasedIntArray(object, intCopy.length, intCopy.length, intCopy, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ZeroBasedDoubleArray createWriteableDouble(JSDynamicObject object, long index, double value, Node node, CreateWritableProfileAccess profile) {
        double[] doubleCopy = ArrayCopy.byteToDouble(getArray(object));
        ZeroBasedDoubleArray newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, doubleCopy.length, doubleCopy.length, doubleCopy, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public AbstractWritableArray createWriteableJSObject(JSDynamicObject object, long index, JSDynamicObject value, Node node, CreateWritableProfileAccess profile) {
        return createWriteableObject(object, index, value, node, profile);
    }

    @Override
    public ZeroBasedObjectArray createWriteableObject(JSDynamicObject object, long index, Object value, Node node, CreateWritableProfileAccess profile) {
        Object[] doubleCopy = ArrayCopy.byteToObject(getArray(object));
        ZeroBasedObjectArray newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, doubleCopy.length, doubleCopy.length, doubleCopy, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public ScriptArray removeRangeImpl(JSDynamicObject object, long start, long end) {
        byte[] array = getArray(object);
        if ((array.length - (end - start)) == 0) {
            AbstractConstantEmptyArray.setCapacity(object, 0);
        } else {
            byte[] newArray = new byte[array.length - (int) (end - start)];
            System.arraycopy(array, 0, newArray, 0, (int) start);
            System.arraycopy(array, (int) end, newArray, (int) start, (int) (array.length - end));
            arraySetArray(object, newArray);
        }
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(JSDynamicObject object, long offset, int size) {
        byte[] array = getArray(object);
        if (array.length == 0) {
            AbstractConstantEmptyArray.setCapacity(object, size);
            return this;
        } else {
            byte[] newArray = new byte[array.length + size];
            System.arraycopy(array, 0, newArray, 0, (int) offset);
            System.arraycopy(array, (int) offset, newArray, (int) offset + size, (int) (array.length - offset));

            arraySetArray(object, newArray);
            return this;
        }
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new ConstantByteArray(newIntegrityLevel, cache);
    }
}

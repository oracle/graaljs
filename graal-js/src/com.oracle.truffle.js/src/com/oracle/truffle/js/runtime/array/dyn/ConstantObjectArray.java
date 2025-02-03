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

import java.util.List;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class ConstantObjectArray extends AbstractConstantArray {
    private static final ConstantObjectArray CONSTANT_OBJECT_ARRAY = new ConstantObjectArray(false, INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();
    private static final ConstantObjectArray CONSTANT_HOLES_OBJECT_ARRAY = new ConstantObjectArray(true, INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    public static ConstantObjectArray createConstantObjectArray() {
        return CONSTANT_OBJECT_ARRAY;
    }

    public static AbstractConstantArray createConstantHolesObjectArray() {
        return CONSTANT_HOLES_OBJECT_ARRAY;
    }

    private final boolean holes;

    private ConstantObjectArray(boolean holes, int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
        this.holes = holes;
    }

    private static Object[] getArray(JSDynamicObject object) {
        return (Object[]) arrayGetArray(object);
    }

    @Override
    public boolean hasElement(JSDynamicObject object, long index) {
        if (index >= 0 && index < getArray(object).length) {
            return !holes || getArray(object)[(int) index] != null;
        }
        return false;
    }

    @Override
    public Object getElementInBounds(JSDynamicObject object, int index) {
        Object value = getElementInBoundsDirect(object, index);
        if (holes && value == null) {
            return Undefined.instance;
        }
        return value;
    }

    private static boolean isEmpty(JSDynamicObject object, int index) {
        return getArray(object)[index] == null;
    }

    public static Object getElementInBoundsDirect(JSDynamicObject object, int index) {
        return getArray(object)[index];
    }

    @Override
    public boolean hasHoles(JSDynamicObject object) {
        return holes;
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
    public Object cloneArray(JSDynamicObject object) {
        return getArray(object);
    }

    @Override
    public long nextElementIndex(JSDynamicObject object, long index0) {
        if (!holes) {
            return super.nextElementIndex(object, index0);
        }
        int index = (int) index0;
        do {
            index++;
        } while (index < super.lastElementIndex(object) && isEmpty(object, index));
        return index;
    }

    @Override
    public long previousElementIndex(JSDynamicObject object, long index0) {
        if (!holes) {
            return super.previousElementIndex(object, index0);
        }
        int index = (int) index0;
        do {
            index--;
        } while (index >= super.firstElementIndex(object) && isEmpty(object, index));
        return index;
    }

    @Override
    public long firstElementIndex(JSDynamicObject object) {
        if (!holes) {
            return super.firstElementIndex(object);
        }
        int index = 0;
        int length = lengthInt(object);
        while (index < length && isEmpty(object, index)) {
            index++;
        }
        return index;
    }

    @Override
    public long lastElementIndex(JSDynamicObject object) {
        if (!holes) {
            return super.lastElementIndex(object);
        }
        int index = lengthInt(object);
        do {
            index--;
        } while (index >= 0 && isEmpty(object, index));
        return index;
    }

    @Override
    public ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict) {
        return createWriteableObject(object, index, null, null, CreateWritableProfileAccess.getUncached()).deleteElementImpl(object, index, strict);
    }

    @Override
    public ScriptArray setLengthImpl(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile) {
        return createWriteableObject(object, length - 1, null, node, profile).setLengthImpl(object, length, node, profile);
    }

    @Override
    public AbstractObjectArray createWriteableInt(JSDynamicObject object, long index, int value, Node node, CreateWritableProfileAccess profile) {
        return createWriteableObject(object, index, value, node, profile);
    }

    @Override
    public AbstractObjectArray createWriteableDouble(JSDynamicObject object, long index, double value, Node node, CreateWritableProfileAccess profile) {
        return createWriteableObject(object, index, value, node, profile);
    }

    @Override
    public AbstractObjectArray createWriteableJSObject(JSDynamicObject object, long index, JSDynamicObject value, Node node, CreateWritableProfileAccess profile) {
        return createWriteableObject(object, index, value, node, profile);
    }

    @Override
    public AbstractObjectArray createWriteableObject(JSDynamicObject object, long index, Object value, Node node, CreateWritableProfileAccess profile) {
        Object[] array = getArray(object);
        AbstractObjectArray newArray;
        if (holes) {
            int arrayOffset = (int) firstElementIndex(object);
            int usedLength = (int) lastElementIndex(object) + 1 - arrayOffset;
            int holeCount = countHoles(object);
            newArray = HolesObjectArray.makeHolesObjectArray(object, array.length, ArrayCopy.objectToObject(array), 0, arrayOffset, usedLength, holeCount, integrityLevel);
        } else {
            newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, array.length, array.length, ArrayCopy.objectToObject(array), integrityLevel);
        }
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    private int countHoles(JSDynamicObject object) {
        int index = (int) (firstElementIndex(object));
        int lastIndex = (int) (lastElementIndex(object) + 1);
        Object[] objArray = getArray(object);
        int holeCount = 0;
        while (index < lastIndex) {
            if (HolesObjectArray.isHoleValue(objArray[index])) {
                holeCount++;
            }
            index++;
        }
        return holeCount;
    }

    /**
     * This array type can have holes. Use isHoles() to check whether the array does have holes.
     */
    @Override
    public boolean isHolesType() {
        return holes;
    }

    @Override
    public ScriptArray removeRangeImpl(JSDynamicObject object, long start, long end) {
        Object[] array = getArray(object);
        if ((array.length - (end - start)) == 0) {
            AbstractConstantEmptyArray.setCapacity(object, 0);
        } else {
            Object[] newArray = new Object[array.length - (int) (end - start)];
            System.arraycopy(array, 0, newArray, 0, (int) start);
            System.arraycopy(array, (int) end, newArray, (int) start, (int) (array.length - end));
            arraySetArray(object, newArray);
        }
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(JSDynamicObject object, long offset, int size) {
        Object[] array = getArray(object);
        if (array.length == 0) {
            AbstractConstantEmptyArray.setCapacity(object, size);
            return this;
        } else {
            Object[] newArray = new Object[array.length + size];
            System.arraycopy(array, 0, newArray, 0, (int) offset);
            System.arraycopy(array, (int) offset, newArray, (int) offset + size, (int) (array.length - offset));

            arraySetArray(object, newArray);
            return this;
        }
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new ConstantObjectArray(holes, newIntegrityLevel, cache);
    }

    @Override
    public List<Object> ownPropertyKeys(JSDynamicObject object) {
        if (holes) {
            return ownPropertyKeysHoles(object);
        } else {
            return ownPropertyKeysContiguous(object);
        }
    }
}

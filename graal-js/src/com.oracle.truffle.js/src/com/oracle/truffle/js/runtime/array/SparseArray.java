/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.array;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetLength;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Array that stores its elements in a HashMap-like structure, e.g. in a TreeMap.
 */
public final class SparseArray extends DynamicArray {

    private static final SparseArray SPARSE_ARRAY = new SparseArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    private SparseArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    public static SparseArray createSparseArray() {
        return SPARSE_ARRAY;
    }

    public static SparseArray makeSparseArray(DynamicObject object, ScriptArray fromArray) {
        assert !(fromArray instanceof SparseArray);
        TreeMap<Long, Object> arrayMap = createArrayMap();
        copyArrayToMap(object, fromArray, arrayMap);
        arraySetLength(object, fromArray.length(object));
        arraySetArray(object, arrayMap);
        return createSparseArray();
    }

    @TruffleBoundary
    public static TreeMap<Long, Object> createArrayMap() {
        return new TreeMap<>();
    }

    protected static void copyArrayToMap(DynamicObject object, ScriptArray fromArray, Map<Long, Object> toMap) {
        for (long index = fromArray.firstElementIndex(object); index <= fromArray.lastElementIndex(object); index = fromArray.nextElementIndex(object, index)) {
            assert fromArray.hasElement(object, index);
            Boundaries.mapPut(toMap, index, fromArray.getElement(object, index));
        }
    }

    @SuppressWarnings("unchecked")
    private static TreeMap<Long, Object> arrayMap(DynamicObject object) {
        return (TreeMap<Long, Object>) arrayGetArray(object);
    }

    @TruffleBoundary
    @Override
    public Object getElement(DynamicObject object, long index) {
        Object value = arrayMap(object).get(index);
        return value != null ? value : Undefined.instance;
    }

    @TruffleBoundary
    @Override
    public Object getElementInBounds(DynamicObject object, long index) {
        Object value = arrayMap(object).get(index);
        assert value != null;
        return value;
    }

    @TruffleBoundary
    @Override
    public ScriptArray setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
        arrayMap(object).put(index, value);
        if (index >= length(object)) {
            arraySetLength(object, index + 1);
        }
        return this;
    }

    @Override
    public long length(DynamicObject object) {
        return arrayGetLength(object);
    }

    @Override
    public int lengthInt(DynamicObject object) {
        long len = arrayGetLength(object);
        if (len > Integer.MAX_VALUE) {
            throw Errors.unsupported("array length too large");
        }
        return (int) len;
    }

    @TruffleBoundary
    @Override
    public SparseArray setLengthImpl(DynamicObject object, long len, ProfileHolder profile) {
        arraySetLength(object, len);
        arrayMap(object).tailMap(len).clear();
        return this;
    }

    @TruffleBoundary
    @Override
    public long firstElementIndex(DynamicObject object) {
        try {
            return arrayMap(object).firstKey();
        } catch (NoSuchElementException ex) {
            return 0;
        }
    }

    @TruffleBoundary
    @Override
    public long lastElementIndex(DynamicObject object) {
        try {
            return arrayMap(object).lastKey();
        } catch (NoSuchElementException ex) {
            return -1;
        }
    }

    @TruffleBoundary
    @Override
    public long nextElementIndex(DynamicObject object, long index) {
        Long nextIndex = arrayMap(object).higherKey(index);
        return nextIndex != null ? nextIndex.longValue() : JSRuntime.MAX_SAFE_INTEGER_LONG;
    }

    @TruffleBoundary
    @Override
    public long previousElementIndex(DynamicObject object, long index) {
        Long nextIndex = arrayMap(object).lowerKey(index);
        return nextIndex != null ? nextIndex.longValue() : -1;
    }

    @Override
    public Object cloneArray(DynamicObject object) {
        return arrayMap(object).clone();
    }

    @TruffleBoundary
    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict) {
        arrayMap(object).remove(index);
        return this;
    }

    @TruffleBoundary
    @Override
    public boolean hasElement(DynamicObject object, long index) {
        return arrayMap(object).containsKey(index);
    }

    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public boolean hasHoles(DynamicObject object) {
        return true;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        assert start <= end;
        assert start >= 0;
        assert end < length(object);

        long delta = end - start + 1;
        long pos = start;
        if (!hasElement(object, pos)) {
            pos = nextElementIndex(object, pos);
        }
        // delete the elements in the removed range
        while (pos <= end) {
            deleteElementImpl(object, pos, false);
            pos = nextElementIndex(object, pos);
        }
        // move all element higher downwards
        while (pos < length(object)) {
            setElement(object, pos - delta, getElement(object, pos), false);
            deleteElementImpl(object, pos, false);
            pos = nextElementIndex(object, pos);
        }
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        assert offset < length(object);

        long pos = length(object);
        if (!hasElement(object, pos)) {
            pos = previousElementIndex(object, pos);
        }
        // move all element higher upwards
        while (pos >= offset) {
            setElement(object, pos + size, getElement(object, pos), false);
            deleteElementImpl(object, pos, false);
            pos = previousElementIndex(object, pos);
        }
        return this;
    }

    @TruffleBoundary
    @Override
    public List<Object> ownPropertyKeys(DynamicObject object) {
        Set<Long> keySet = arrayMap(object).keySet();
        List<Object> list = new ArrayList<>(keySet.size());
        for (long index : keySet) {
            list.add(Boundaries.stringValueOf(index));
        }
        return list;
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new SparseArray(newIntegrityLevel, cache);
    }
}

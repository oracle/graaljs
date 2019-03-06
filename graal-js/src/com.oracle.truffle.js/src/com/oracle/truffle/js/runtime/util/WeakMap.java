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
package com.oracle.truffle.js.runtime.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;

/**
 * JavaScript WeakMap.
 */
public class WeakMap implements Map<DynamicObject, Object> {
    private static final HiddenKey INVERTED_WEAK_MAP_KEY = new HiddenKey("InvertedWeakMap");

    public WeakMap() {
    }

    private static DynamicObject checkKey(Object key) {
        if (!(key instanceof DynamicObject)) {
            throw new IllegalArgumentException("key must be instanceof DynamicObject");
        }
        return (DynamicObject) key;
    }

    @SuppressWarnings("unchecked")
    private static Map<WeakMap, Object> getInvertedMap(DynamicObject k, boolean put) {
        if (k.containsKey(INVERTED_WEAK_MAP_KEY)) {
            return (WeakHashMap<WeakMap, Object>) k.get(INVERTED_WEAK_MAP_KEY);
        } else {
            if (put) {
                return putInvertedMap(k);
            } else {
                return Collections.emptyMap();
            }
        }
    }

    private static WeakHashMap<WeakMap, Object> putInvertedMap(DynamicObject k) {
        WeakHashMap<WeakMap, Object> invertedMap = new WeakHashMap<>();
        boolean wasNotExtensible = !JSShape.isExtensible(k.getShape());
        k.define(INVERTED_WEAK_MAP_KEY, invertedMap);
        if (wasNotExtensible && JSObject.isExtensible(k)) {
            // not-extensible marker property is expected to be the last property; ensure it is.
            k.delete(JSShape.NOT_EXTENSIBLE_KEY);
            JSObject.preventExtensions(k);
            assert !JSObject.isExtensible(k);
        }
        return invertedMap;
    }

    @Override
    public boolean containsKey(Object key) {
        DynamicObject k = checkKey(key);
        return getInvertedMap(k, false).containsKey(this);
    }

    @Override
    public Object get(Object key) {
        DynamicObject k = checkKey(key);
        return getInvertedMap(k, false).get(this);
    }

    @Override
    public Object put(DynamicObject key, Object value) {
        DynamicObject k = checkKey(key);
        return getInvertedMap(k, true).put(this, value);
    }

    @Override
    public Object remove(Object key) {
        DynamicObject k = checkKey(key);
        return getInvertedMap(k, false).remove(this);
    }

    @Override
    public void putAll(Map<? extends DynamicObject, ? extends Object> m) {
        m.forEach(this::put);
    }

    @Override
    public boolean containsValue(Object value) {
        throw unsupported();
    }

    @Override
    public int size() {
        throw unsupported();
    }

    @Override
    public boolean isEmpty() {
        throw unsupported();
    }

    @Override
    public void clear() {
        throw unsupported();
    }

    @Override
    public Set<DynamicObject> keySet() {
        throw unsupported();
    }

    @Override
    public Collection<Object> values() {
        throw unsupported();
    }

    @Override
    public Set<java.util.Map.Entry<DynamicObject, Object>> entrySet() {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by WeakMap");
    }
}

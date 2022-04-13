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
package com.oracle.truffle.js.runtime.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

/**
 * JavaScript WeakMap that emulates ephemeron semantics by storing the value in the key itself
 * (i.e., in a hidden property within the key object).
 */
public final class WeakMap implements Map<JSObject, Object> {
    public static final HiddenKey INVERTED_WEAK_MAP_KEY = new HiddenKey("InvertedWeakMap");

    public WeakMap() {
    }

    private static JSObject checkKey(Object key) {
        if (!(key instanceof JSObject)) {
            throw new IllegalArgumentException("key must be instanceof JSObject");
        }
        return (JSObject) key;
    }

    @SuppressWarnings("unchecked")
    private static Map<WeakMap, Object> getInvertedMap(JSObject k) {
        return (Map<WeakMap, Object>) JSDynamicObject.getOrNull(k, INVERTED_WEAK_MAP_KEY);
    }

    private static Map<WeakMap, Object> putInvertedMap(JSObject k) {
        Map<WeakMap, Object> invertedMap = newInvertedMap();
        JSObjectUtil.putHiddenProperty(k, INVERTED_WEAK_MAP_KEY, invertedMap);
        return invertedMap;
    }

    @TruffleBoundary
    public static Map<WeakMap, Object> newInvertedMap() {
        return new WeakHashMap<>();
    }

    @TruffleBoundary
    public Map<WeakMap, Object> newInvertedMapWithEntry(JSObject key, Object value) {
        assert getInvertedMap(key) == null;
        Map<WeakMap, Object> map = new WeakHashMap<>();
        map.put(this, value);
        return map;
    }

    @Override
    public boolean containsKey(Object key) {
        JSObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        return invertedMap == null ? false : invertedMap.containsKey(this);
    }

    @Override
    public Object get(Object key) {
        JSObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        return invertedMap == null ? null : invertedMap.get(this);
    }

    @Override
    public Object put(JSObject key, Object value) {
        JSObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        if (invertedMap == null) {
            invertedMap = putInvertedMap(k);
        }
        return invertedMap.put(this, value);
    }

    @Override
    public Object remove(Object key) {
        JSObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        return invertedMap == null ? null : invertedMap.remove(this);
    }

    @Override
    public void putAll(Map<? extends JSObject, ? extends Object> m) {
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
    public Set<JSObject> keySet() {
        throw unsupported();
    }

    @Override
    public Collection<Object> values() {
        throw unsupported();
    }

    @Override
    public Set<java.util.Map.Entry<JSObject, Object>> entrySet() {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by WeakMap");
    }
}

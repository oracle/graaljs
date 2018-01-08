/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

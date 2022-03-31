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

import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

/**
 * JavaScript WeakMap.
 */
public class WeakMap implements Map<JSDynamicObject, Object> {
    private static final HiddenKey INVERTED_WEAK_MAP_KEY = new HiddenKey("InvertedWeakMap");

    public WeakMap() {
    }

    public static PropertyGetNode createInvertedKeyMapGetNode(JSContext context) {
        return PropertyGetNode.createGetHidden(WeakMap.INVERTED_WEAK_MAP_KEY, context);
    }

    public static HasHiddenKeyCacheNode createInvertedKeyMapHasNode() {
        return HasHiddenKeyCacheNode.create(WeakMap.INVERTED_WEAK_MAP_KEY);
    }

    private static JSDynamicObject checkKey(Object key) {
        if (!(key instanceof JSDynamicObject)) {
            throw new IllegalArgumentException("key must be instanceof JSDynamicObject");
        }
        return (JSDynamicObject) key;
    }

    @SuppressWarnings("unchecked")
    private static Map<WeakMap, Object> getInvertedMap(JSDynamicObject k) {
        Object invertedMap = JSDynamicObject.getOrNull(k, INVERTED_WEAK_MAP_KEY);
        if (invertedMap != null) {
            return (WeakHashMap<WeakMap, Object>) invertedMap;
        } else {
            return null;
        }
    }

    private static Map<WeakMap, Object> putInvertedMap(JSDynamicObject k) {
        Map<WeakMap, Object> invertedMap = newInvertedMap();
        boolean wasExtensible = false;
        assert (wasExtensible = ((JSDynamicObject.getObjectFlags(k) & JSShape.NOT_EXTENSIBLE_FLAG) == 0)) || Boolean.TRUE;
        JSObjectUtil.putHiddenProperty(k, INVERTED_WEAK_MAP_KEY, invertedMap);
        assert wasExtensible == ((JSDynamicObject.getObjectFlags(k) & JSShape.NOT_EXTENSIBLE_FLAG) == 0);
        return invertedMap;
    }

    private static Map<WeakMap, Object> newInvertedMap() {
        return new WeakHashMap<>();
    }

    @Override
    public boolean containsKey(Object key) {
        JSDynamicObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        return invertedMap == null ? false : invertedMap.containsKey(this);
    }

    @Override
    public Object get(Object key) {
        JSDynamicObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        return invertedMap == null ? null : invertedMap.get(this);
    }

    @Override
    public Object put(JSDynamicObject key, Object value) {
        JSDynamicObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        if (invertedMap == null) {
            invertedMap = putInvertedMap(k);
        }
        return invertedMap.put(this, value);
    }

    @Override
    public Object remove(Object key) {
        JSDynamicObject k = checkKey(key);
        Map<WeakMap, Object> invertedMap = getInvertedMap(k);
        return invertedMap == null ? null : invertedMap.remove(this);
    }

    @Override
    public void putAll(Map<? extends JSDynamicObject, ? extends Object> m) {
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
    public Set<JSDynamicObject> keySet() {
        throw unsupported();
    }

    @Override
    public Collection<Object> values() {
        throw unsupported();
    }

    @Override
    public Set<java.util.Map.Entry<JSDynamicObject, Object>> entrySet() {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by WeakMap");
    }
}

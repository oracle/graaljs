/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.ReferenceQueue;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Thread-safe WeakHashMap alternative based on {@link ConcurrentHashMap} to avoid lock contention
 * for lookups.
 *
 * Keys are compared by object identity. Keys and values must not be null.
 *
 * Inspired by {@code jdk.internal.util.ReferencedKeyMap}.
 *
 * Only partially implements the {@link Map} interface; unused methods are omitted for simplicity.
 */
public final class ConcurrentWeakIdentityHashMap<K, V> implements Map<K, V> {

    private final Map<ReferenceKey<K>, V> map;
    private final ReferenceQueue<K> stale;

    private ConcurrentWeakIdentityHashMap() {
        this.map = new ConcurrentHashMap<>();
        this.stale = new ReferenceQueue<>();
    }

    @TruffleBoundary
    public static <K, V> ConcurrentWeakIdentityHashMap<K, V> create() {
        return new ConcurrentWeakIdentityHashMap<>();
    }

    /**
     * Returns a key suitable for a map entry.
     *
     * @param key unwrapped key
     */
    private ReferenceKey<K> entryKey(K key) {
        return new ReferenceKey.WeakKeyWithIdentity<>(key, stale);
    }

    /**
     * Returns a key suitable for lookup.
     *
     * @param key unwrapped key
     */
    @SuppressWarnings("unchecked")
    private ReferenceKey<K> lookupKey(Object key) {
        return new ReferenceKey.StrongKeyWithIdentity<>((K) key);
    }

    @Override
    public V get(Object key) {
        removeStaleReferences();
        return map.get(lookupKey(key));
    }

    @Override
    public boolean containsKey(Object key) {
        removeStaleReferences();
        return map.containsKey(lookupKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        removeStaleReferences();
        return map.containsValue(value);
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        removeStaleReferences();
        ReferenceKey<K> entryKey = entryKey(key);
        V oldValue = map.put(entryKey, value);
        if (oldValue != null) {
            // new key was not inserted, clear the weak reference
            entryKey.clear();
        }
        return oldValue;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        removeStaleReferences();
        ReferenceKey<K> entryKey = entryKey(key);
        V oldValue = map.putIfAbsent(entryKey, value);
        if (oldValue != null) {
            // new key was not inserted, clear the weak reference
            entryKey.clear();
        }
        return oldValue;
    }

    @Override
    public void clear() {
        removeStaleReferences();
        map.clear();
    }

    @Override
    public V remove(Object key) {
        removeStaleReferences();
        return map.remove(lookupKey(key));
    }

    @Override
    public boolean remove(Object key, Object value) {
        removeStaleReferences();
        return map.remove(lookupKey(key), value);
    }

    /**
     * Removes enqueued stale weak reference keys from map.
     */
    private void removeStaleReferences() {
        for (Object key; (key = stale.poll()) != null;) {
            map.remove(key);
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        removeStaleReferences();
        map.forEach(new BiConsumer<>() {
            @Override
            public void accept(ReferenceKey<K> refKey, V value) {
                K key = refKey.get();
                if (key == null) {
                    return;
                }
                action.accept(key, value);
            }
        });
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
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
    public Set<K> keySet() {
        throw unsupported();
    }

    @Override
    public Collection<V> values() {
        throw unsupported();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw unsupported();
    }

    @TruffleBoundary
    private static UnsupportedOperationException unsupported() {
        throw new UnsupportedOperationException();
    }
}

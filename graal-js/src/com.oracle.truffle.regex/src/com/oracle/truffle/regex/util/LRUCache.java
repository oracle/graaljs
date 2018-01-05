/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.util;

import java.util.LinkedHashMap;

/**
 * An implementation of a cache with a least-recently-used policy via LinkedHashMap.
 */
public final class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 7813848977534444613L;
    private final int maxCacheSize;

    public LRUCache(int maxCacheSize) {
        super(16, 0.75F, true);
        this.maxCacheSize = maxCacheSize;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > maxCacheSize;
    }
}

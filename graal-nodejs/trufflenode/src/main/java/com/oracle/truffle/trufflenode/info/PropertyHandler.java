/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.info;

/**
 *
 * @author Jan Stola
 */
public final class PropertyHandler {

    private final long getter;
    private final long setter;
    private final long query;
    private final long deleter;
    private final long enumerator;
    private final Object data;

    public PropertyHandler(long getter, long setter, long query, long deleter, long enumerator, Object data) {
        this.getter = getter;
        this.setter = setter;
        this.query = query;
        this.deleter = deleter;
        this.enumerator = enumerator;
        this.data = data;
    }

    public long getGetter() {
        return getter;
    }

    public long getSetter() {
        return setter;
    }

    public long getQuery() {
        return query;
    }

    public long getDeleter() {
        return deleter;
    }

    public long getEnumerator() {
        return enumerator;
    }

    public Object getData() {
        return data;
    }

}

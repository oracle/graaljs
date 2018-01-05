/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.buffer;

import java.util.Arrays;
import java.util.Iterator;

public final class ObjectBuffer implements Iterable<Object> {

    private Object[] buf;
    private int size = 0;

    public ObjectBuffer() {
        this(16);
    }

    public ObjectBuffer(int initialSize) {
        buf = new Object[initialSize];
    }

    public void clear() {
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public Object get(int i) {
        return buf[i];
    }

    public void add(Object o) {
        if (size == buf.length) {
            grow(size * 2);
        }
        buf[size] = o;
        size++;
    }

    private void grow(int newSize) {
        buf = Arrays.copyOf(buf, newSize);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            return (T[]) Arrays.copyOf(buf, size, a.getClass());
        }
        System.arraycopy(buf, 0, a, 0, size);
        return a;
    }

    @Override
    public Iterator<Object> iterator() {
        return new ObjectBufferIterator(buf, size);
    }

    private static final class ObjectBufferIterator implements Iterator<Object> {

        private final Object[] buf;
        private final int size;
        private int i = 0;

        private ObjectBufferIterator(Object[] buf, int size) {
            this.buf = buf;
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return i < size;
        }

        @Override
        public Object next() {
            return buf[i++];
        }
    }
}

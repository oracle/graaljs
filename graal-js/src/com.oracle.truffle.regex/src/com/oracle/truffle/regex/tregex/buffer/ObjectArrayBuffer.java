/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.buffer;

import java.util.Arrays;
import java.util.Iterator;

/**
 * This class is designed as a "scratchpad" for generating many Object arrays of unknown size. It
 * will never shrink its internal buffer, so it should be disposed as soon as it is no longer
 * needed.
 * <p>
 * Usage Example:
 * </p>
 * 
 * <pre>
 * SomeClass[] typedArray = new SomeClass[0];
 * ObjectArrayBuffer buf = new ObjectArrayBuffer();
 * List<SomeClass[]> results = new ArrayList<>();
 * for (Object obj : listOfThingsToProcess) {
 *     for (Object x : obj.thingsThatShouldBecomeSomeClass()) {
 *         buf.add(someCalculation(x));
 *     }
 *     results.add(buf.toArray(typedArray));
 *     buf.clear();
 * }
 * </pre>
 */
public final class ObjectArrayBuffer implements Iterable<Object> {

    private Object[] buf;
    private int size = 0;

    public ObjectArrayBuffer() {
        this(16);
    }

    public ObjectArrayBuffer(int initialSize) {
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

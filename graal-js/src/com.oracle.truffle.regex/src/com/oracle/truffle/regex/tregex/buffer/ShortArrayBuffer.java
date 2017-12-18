/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.buffer;

import java.util.Arrays;

public class ShortArrayBuffer {

    private short[] buf;
    private int size = 0;

    public ShortArrayBuffer() {
        this(16);
    }

    public ShortArrayBuffer(int initialSize) {
        buf = new short[initialSize];
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public short get(int i) {
        return buf[i];
    }

    public void add(short s) {
        if (size == buf.length) {
            grow(size * 2);
        }
        buf[size] = s;
        size++;
    }

    private void grow(int newSize) {
        buf = Arrays.copyOf(buf, newSize);
    }

    public short[] toArray() {
        return Arrays.copyOf(buf, size);
    }
}

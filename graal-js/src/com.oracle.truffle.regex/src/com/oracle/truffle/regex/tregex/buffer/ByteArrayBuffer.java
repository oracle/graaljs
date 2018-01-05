/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.buffer;

import java.util.Arrays;

public class ByteArrayBuffer {

    private byte[] buf;
    private int size = 0;

    public ByteArrayBuffer() {
        this(16);
    }

    public ByteArrayBuffer(int initialSize) {
        buf = new byte[initialSize];
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public byte get(int i) {
        return buf[i];
    }

    public void add(byte b) {
        if (size == buf.length) {
            grow(size * 2);
        }
        buf[size] = b;
        size++;
    }

    private void grow(int newSize) {
        buf = Arrays.copyOf(buf, newSize);
    }

    public byte[] toArray() {
        return Arrays.copyOf(buf, size);
    }
}

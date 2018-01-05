/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.buffer;

import java.util.Arrays;

public class RangesArrayBuffer {

    private char[] ranges;
    private int size = 0;

    public RangesArrayBuffer(int initialSize) {
        ranges = new char[initialSize];
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

    public void setSize(int size) {
        this.size = size;
    }

    public char[] getBuffer() {
        return ranges;
    }

    public void addRange(int rLo, int rHi) {
        if (size == ranges.length) {
            grow(size * 2);
        }
        ranges[size++] = (char) rLo;
        ranges[size++] = (char) rHi;
    }

    public void ensureCapacity(int newSize) {
        if (ranges.length < newSize) {
            int newBufferSize = ranges.length * 2;
            while (newBufferSize < newSize) {
                newBufferSize *= 2;
            }
            grow(newBufferSize);
        }
    }

    private void grow(int newSize) {
        ranges = Arrays.copyOf(ranges, newSize);
    }

    public char[] toArray() {
        return Arrays.copyOf(ranges, size);
    }
}

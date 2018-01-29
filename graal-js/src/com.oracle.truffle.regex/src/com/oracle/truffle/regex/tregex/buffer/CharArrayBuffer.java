/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.buffer;

import java.util.Arrays;

/**
 * This class is designed as a "scratchpad" for generating many char arrays of unknown size. It will
 * never shrink its internal buffer, so it should be disposed as soon as it is no longer needed.
 * <p>
 * Usage Example:
 * </p>
 * 
 * <pre>
 * CharArrayBuffer buf = new CharArrayBuffer();
 * List<char[]> results = new ArrayList<>();
 * for (Object obj : listOfThingsToProcess) {
 *     for (Object x : obj.thingsThatShouldBecomeChars()) {
 *         buf.add(someCalculation(x));
 *     }
 *     results.add(buf.toArray());
 *     buf.clear();
 * }
 * </pre>
 */
public class CharArrayBuffer {

    private char[] buf;
    private int size = 0;

    public CharArrayBuffer(int initialSize) {
        buf = new char[initialSize];
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
        return buf;
    }

    public void add(char c) {
        if (size == buf.length) {
            grow(size * 2);
        }
        buf[size++] = c;
    }

    public void ensureCapacity(int newSize) {
        if (buf.length < newSize) {
            int newBufferSize = buf.length * 2;
            while (newBufferSize < newSize) {
                newBufferSize *= 2;
            }
            grow(newBufferSize);
        }
    }

    private void grow(int newSize) {
        buf = Arrays.copyOf(buf, newSize);
    }

    public char[] toArray() {
        return Arrays.copyOf(buf, size);
    }
}

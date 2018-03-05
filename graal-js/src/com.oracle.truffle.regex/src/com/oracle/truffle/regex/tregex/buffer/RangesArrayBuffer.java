/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.buffer;

/**
 * Extension of {@link CharArrayBuffer} that adds convenience functions for arrays of character
 * ranges in the form:
 * 
 * <pre>
 * [
 *     inclusive lower bound of range 1, inclusive upper bound of range 1,
 *     inclusive lower bound of range 2, inclusive upper bound of range 2,
 *     inclusive lower bound of range 3, inclusive upper bound of range 3,
 *     ...
 * ]
 * </pre>
 */
public class RangesArrayBuffer extends CharArrayBuffer {

    public RangesArrayBuffer(int initialSize) {
        super(initialSize);
    }

    public void addRange(int rLo, int rHi) {
        add((char) rLo);
        add((char) rHi);
    }

    public int sizeRanges() {
        return size() / 2;
    }
}

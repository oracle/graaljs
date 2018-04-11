/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex.nashorn.regexp.joni;

// @formatter:off

public final class BitSet {
    static final int BITS_PER_BYTE = 8;
    public static final int SINGLE_BYTE_SIZE = (1 << BITS_PER_BYTE);
    private static final int BITS_IN_ROOM = 4 * BITS_PER_BYTE;
    static final int BITSET_SIZE = (SINGLE_BYTE_SIZE / BITS_IN_ROOM);
    static final int ROOM_SHIFT = log2(BITS_IN_ROOM);

    final int[] bits = new int[BITSET_SIZE];

    private static final int BITS_TO_STRING_WRAP = 4;
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("BitSet");
        for (int i=0; i<SINGLE_BYTE_SIZE; i++) {
            if ((i % (SINGLE_BYTE_SIZE / BITS_TO_STRING_WRAP)) == 0) {
                buffer.append("\n  ");
            }
            buffer.append(at(i) ? "1" : "0");
        }
        return buffer.toString();
    }

    public boolean at(final int pos) {
        return (bits[pos >>> ROOM_SHIFT] & bit(pos)) != 0;
    }

    public void set(final int pos) {
        bits[pos >>> ROOM_SHIFT] |= bit(pos);
    }

    public void clear(final int pos) {
        bits[pos >>> ROOM_SHIFT] &= ~bit(pos);
    }

    public void clear() {
        for (int i=0; i<BITSET_SIZE; i++) {
            bits[i]=0;
        }
    }

    public boolean isEmpty() {
        for (int i=0; i<BITSET_SIZE; i++) {
            if (bits[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public void setRange(final int from, final int to) {
        for (int i=from; i<=to && i < SINGLE_BYTE_SIZE; i++) {
            set(i);
        }
    }

    public void invert() {
        for (int i=0; i<BITSET_SIZE; i++) {
            bits[i] = ~bits[i];
        }
    }

    public void invertTo(final BitSet to) {
        for (int i=0; i<BITSET_SIZE; i++) {
            to.bits[i] = ~bits[i];
        }
    }

    public void and(final BitSet other) {
        for (int i=0; i<BITSET_SIZE; i++) {
            bits[i] &= other.bits[i];
        }
    }

    public void or(final BitSet other) {
        for (int i=0; i<BITSET_SIZE; i++) {
            bits[i] |= other.bits[i];
        }
    }

    public void copy(final BitSet other) {
        for (int i=0; i<BITSET_SIZE; i++) {
            bits[i] = other.bits[i];
        }
    }

    public int numOn() {
        int num = 0;
        for (int i=0; i<SINGLE_BYTE_SIZE; i++) {
            if (at(i)) {
                num++;
            }
        }
        return num;
    }

    static int bit(final int pos){
        return 1 << (pos % SINGLE_BYTE_SIZE);
    }

    private static int log2(final int np) {
        int log = 0;
        int n = np;
        while ((n >>>= 1) != 0) {
            log++;
        }
        return log;
    }

}

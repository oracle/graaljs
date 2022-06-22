/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.array;

import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class ByteBufferAccess {

    @SuppressWarnings("static-method")
    public final int getInt8(ByteBuffer buffer, int index) {
        return buffer.get(index);
    }

    public final int getUint8(ByteBuffer buffer, int index) {
        return getInt8(buffer, index) & 0xff;
    }

    public abstract int getInt16(ByteBuffer buffer, int index);

    public final int getUint16(ByteBuffer buffer, int index) {
        return getInt16(buffer, index) & 0xffff;
    }

    public abstract int getInt32(ByteBuffer buffer, int index);

    public abstract float getFloat(ByteBuffer buffer, int index);

    public abstract double getDouble(ByteBuffer buffer, int index);

    public abstract long getInt64(ByteBuffer buffer, int index);

    @SuppressWarnings("static-method")
    public final void putInt8(ByteBuffer buffer, int index, int value) {
        buffer.put(index, (byte) value);
    }

    public abstract void putInt16(ByteBuffer buffer, int index, int value);

    public abstract void putInt32(ByteBuffer buffer, int index, int value);

    public abstract void putFloat(ByteBuffer buffer, int index, float value);

    public abstract void putDouble(ByteBuffer buffer, int index, double value);

    public abstract void putInt64(ByteBuffer buffer, int index, long value);

    public abstract int compareExchangeInt32(ByteBuffer buffer, int index, int expectedValue, int newValue);

    public abstract long compareExchangeInt64(ByteBuffer buffer, int index, long expectedValue, long newValue);

    /**
     * Emulate 8-bit CAS using 32-bit CAS. Cannot be used if the buffer length is not a multiple of
     * 4 and too short for the 32-bit access to be fully in bounds.
     */
    public int compareExchangeInt8(ByteBuffer buffer, int index, int expectedValue, int newValue) {
        int wordOffset = index & ~3;
        assert wordOffset <= buffer.capacity() - Integer.BYTES;
        int shift = (index & 3) << 3;
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            shift = 24 - shift;
        }
        int mask = 0xFF << shift;
        int maskedExpected = (expectedValue & 0xFF) << shift;
        int maskedReplacement = (newValue & 0xFF) << shift;
        int fullWord;
        int exchanged;
        do {
            fullWord = getInt32(buffer, wordOffset);
            VarHandle.acquireFence();
            if ((fullWord & mask) != maskedExpected) {
                return (byte) ((fullWord & mask) >> shift);
            }
            exchanged = compareExchangeInt32(buffer, wordOffset, fullWord, (fullWord & ~mask) | maskedReplacement);
        } while (exchanged != fullWord);
        return expectedValue;
    }

    /**
     * Emulate 16-bit CAS using 32-bit CAS. Cannot be used if the buffer length is not a multiple of
     * 4 and too short for the 32-bit access to be fully in bounds or if {@code (index % 4) == 3}.
     */
    public int compareExchangeInt16(ByteBuffer buffer, int index, int expectedValue, int newValue) {
        assert (index & 3) != 3 : "Update spans the word, not supported";
        int wordOffset = index & ~3;
        assert wordOffset <= buffer.capacity() - Integer.BYTES;
        int shift = (index & 3) << 3;
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            shift = 16 - shift;
        }
        int mask = 0xFFFF << shift;
        int maskedExpected = (expectedValue & 0xFFFF) << shift;
        int maskedReplacement = (newValue & 0xFFFF) << shift;
        int fullWord;
        int exchanged;
        do {
            fullWord = getInt32(buffer, wordOffset);
            VarHandle.acquireFence();
            if ((fullWord & mask) != maskedExpected) {
                return (short) ((fullWord & mask) >> shift);
            }
            exchanged = compareExchangeInt32(buffer, wordOffset, fullWord, (fullWord & ~mask) | maskedReplacement);
        } while (exchanged != fullWord);
        return expectedValue;
    }

    public static final ByteBufferAccess littleEndian() {
        return ByteBufferSupport.littleEndian();
    }

    public static final ByteBufferAccess bigEndian() {
        return ByteBufferSupport.bigEndian();
    }

    public static final ByteBufferAccess nativeOrder() {
        return ByteBufferSupport.nativeOrder();
    }

    public static final ByteBufferAccess forOrder(boolean littleEndian) {
        return littleEndian ? littleEndian() : bigEndian();
    }
}

/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

abstract class ByteArrayAccess {
    public abstract int getInt8(byte[] buffer, int offset, int index, int bytesPerElement);

    public final int getUint8(byte[] buffer, int offset, int index, int bytesPerElement) {
        return getInt8(buffer, offset, index, bytesPerElement) & 0xff;
    }

    public abstract int getInt16(byte[] buffer, int offset, int index, int bytesPerElement);

    public final int getUint16(byte[] buffer, int offset, int index, int bytesPerElement) {
        return getInt16(buffer, offset, index, bytesPerElement) & 0xffff;
    }

    public abstract int getInt32(byte[] buffer, int offset, int index, int bytesPerElement);

    public final long getUint32(byte[] buffer, int offset, int index, int bytesPerElement) {
        return getInt32(buffer, offset, index, bytesPerElement) & 0xffffffffL;
    }

    public abstract float getFloat(byte[] buffer, int offset, int index, int bytesPerElement);

    public abstract double getDouble(byte[] buffer, int offset, int index, int bytesPerElement);

    public abstract long getInt64(byte[] buffer, int offset, int index, int bytesPerElement);

    public abstract void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value);

    public abstract void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value);

    public abstract void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value);

    public abstract void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value);

    public abstract void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value);

    public abstract void putInt64(byte[] buffer, int offset, int index, int bytesPerElement, long value);
}

abstract class NormalByteArrayAccess extends ByteArrayAccess {
    private static int makeInt16(byte b0, byte b1) {
        return (b1 << 8) | (b0 & 0xff);
    }

    private static int makeInt32(byte b0, byte b1, byte b2, byte b3) {
        return (((b3) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff)));
    }

    private static long makeInt64(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
        return ((((long) b7) << 56) | (((long) b6 & 0xff) << 48) | (((long) b5 & 0xff) << 40) | (((long) b4 & 0xff) << 32) | (((long) b3 & 0xff) << 24) | (((long) b2 & 0xff) << 16) |
                        (((long) b1 & 0xff) << 8) | (((long) b0 & 0xff)));
    }

    @Override
    public final int getInt8(byte[] buffer, int offset, int index, int bytesPerElement) {
        int byteIndex = offset + index * bytesPerElement;
        return buffer[byteIndex];
    }

    @Override
    public final int getInt16(byte[] buffer, int offset, int index, int bytesPerElement) {
        int byteIndex = offset + index * bytesPerElement;
        return makeInt16(buffer[byteIndex + b(0, 2)], buffer[byteIndex + b(1, 2)]);
    }

    @Override
    public final int getInt32(byte[] buffer, int offset, int index, int bytesPerElement) {
        int byteIndex = offset + index * bytesPerElement;
        return makeInt32(buffer[byteIndex + b(0, 4)], buffer[byteIndex + b(1, 4)], buffer[byteIndex + b(2, 4)], buffer[byteIndex + b(3, 4)]);
    }

    @Override
    public final float getFloat(byte[] buffer, int offset, int index, int bytesPerElement) {
        return Float.intBitsToFloat(getInt32(buffer, offset, index, bytesPerElement));
    }

    @Override
    public final double getDouble(byte[] buffer, int offset, int index, int bytesPerElement) {
        int byteIndex = offset + index * bytesPerElement;
        return Double.longBitsToDouble(makeInt64(buffer[byteIndex + b(0, 8)], buffer[byteIndex + b(1, 8)], buffer[byteIndex + b(2, 8)], buffer[byteIndex + b(3, 8)], buffer[byteIndex + b(4, 8)],
                        buffer[byteIndex + b(5, 8)], buffer[byteIndex + b(6, 8)], buffer[byteIndex + b(7, 8)]));
    }

    @Override
    public long getInt64(byte[] buffer, int offset, int index, int bytesPerElement) {
        int byteIndex = offset + index * bytesPerElement;
        return makeInt64(buffer[byteIndex + b(0, 8)], buffer[byteIndex + b(1, 8)], buffer[byteIndex + b(2, 8)], buffer[byteIndex + b(3, 8)], buffer[byteIndex + b(4, 8)],
                        buffer[byteIndex + b(5, 8)], buffer[byteIndex + b(6, 8)], buffer[byteIndex + b(7, 8)]);
    }

    @Override
    public final void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        int byteIndex = offset + index * bytesPerElement;
        buffer[byteIndex] = (byte) value;
    }

    @Override
    public final void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        int byteIndex = offset + index * bytesPerElement;
        buffer[byteIndex + b(0, 2)] = (byte) (value);
        buffer[byteIndex + b(1, 2)] = (byte) (value >> 8);
    }

    @Override
    public final void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        int byteIndex = offset + index * bytesPerElement;
        buffer[byteIndex + b(0, 4)] = (byte) (value);
        buffer[byteIndex + b(1, 4)] = (byte) (value >> 8);
        buffer[byteIndex + b(2, 4)] = (byte) (value >> 16);
        buffer[byteIndex + b(3, 4)] = (byte) (value >> 24);
    }

    @Override
    public final void putInt64(byte[] buffer, int offset, int index, int bytesPerElement, long value) {
        int byteIndex = offset + index * bytesPerElement;
        buffer[byteIndex + b(0, 8)] = (byte) (value);
        buffer[byteIndex + b(1, 8)] = (byte) (value >> 8);
        buffer[byteIndex + b(2, 8)] = (byte) (value >> 16);
        buffer[byteIndex + b(3, 8)] = (byte) (value >> 24);
        buffer[byteIndex + b(4, 8)] = (byte) (value >> 32);
        buffer[byteIndex + b(5, 8)] = (byte) (value >> 40);
        buffer[byteIndex + b(6, 8)] = (byte) (value >> 48);
        buffer[byteIndex + b(7, 8)] = (byte) (value >> 56);
    }

    @Override
    public final void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value) {
        putInt32(buffer, offset, index, bytesPerElement, Float.floatToRawIntBits(value));
    }

    @Override
    public final void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value) {
        putInt64(buffer, offset, index, bytesPerElement, Double.doubleToRawLongBits(value));
    }

    /**
     * Byte order.
     *
     * @param bytePos byte position in little endian byte order
     * @param size size of type in bytes
     */
    protected abstract int b(int bytePos, int size);
}

final class LittleEndianByteArrayAccess extends NormalByteArrayAccess {
    @Override
    protected int b(int bytePos, int size) {
        return bytePos;
    }
}

final class BigEndianByteArrayAccess extends NormalByteArrayAccess {
    @Override
    protected int b(int bytePos, int size) {
        return size - 1 - bytePos;
    }
}

final class ByteBufferNativeOrderByteArrayAccess extends ByteArrayAccess {
    @Override
    public int getInt8(byte[] buffer, int offset, int index, int bytesPerElement) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).get(byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public int getInt16(byte[] buffer, int offset, int index, int bytesPerElement) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getShort(byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public int getInt32(byte[] buffer, int offset, int index, int bytesPerElement) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getInt(byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public float getFloat(byte[] buffer, int offset, int index, int bytesPerElement) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getFloat(byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public double getDouble(byte[] buffer, int offset, int index, int bytesPerElement) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getDouble(byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public long getInt64(byte[] buffer, int offset, int index, int bytesPerElement) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getLong(byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).put(byteIndex(offset, index, bytesPerElement), (byte) value);
    }

    @Override
    public void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putShort(byteIndex(offset, index, bytesPerElement), (short) value);
    }

    @Override
    public void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putInt(byteIndex(offset, index, bytesPerElement), value);
    }

    @Override
    public void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value) {
        ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putFloat(byteIndex(offset, index, bytesPerElement), value);
    }

    @Override
    public void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value) {
        ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putDouble(byteIndex(offset, index, bytesPerElement), value);
    }

    @Override
    public void putInt64(byte[] buffer, int offset, int index, int bytesPerElement, long value) {
        ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putLong(byteIndex(offset, index, bytesPerElement), value);
    }

    private static int byteIndex(int offset, int index, int bytesPerElement) {
        return offset + index * bytesPerElement;
    }
}

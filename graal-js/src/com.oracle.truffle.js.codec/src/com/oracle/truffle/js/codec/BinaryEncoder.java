/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.codec;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Utility for encoding values to a ByteBuffer.
 */
public class BinaryEncoder {
    private static final int INITIAL_BUFFER_SIZE = 8 * 1024;
    private ByteBuffer buffer;

    public BinaryEncoder() {
        this.buffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    }

    public ByteBuffer getBuffer() {
        return (ByteBuffer) buffer.duplicate().flip();
    }

    protected void putU1(long value) {
        ensureCapacity(Byte.BYTES);
        buffer.put((byte) value);
    }

    private void ensureCapacity(int increase) {
        if (buffer.position() + increase >= buffer.limit()) {
            ByteBuffer oldBuffer = buffer;
            ByteBuffer newBuffer = ByteBuffer.allocate(Math.max(2 * oldBuffer.capacity(), oldBuffer.position() + increase)).order(ByteOrder.LITTLE_ENDIAN);
            newBuffer.put((ByteBuffer) oldBuffer.duplicate().flip());
            assert newBuffer.position() == oldBuffer.position();
            assert newBuffer.order() == ByteOrder.LITTLE_ENDIAN;
            buffer = newBuffer;
        }
    }

    /**
     * Writes a signed value in a variable byte size encoding.
     */
    private void putSV(long value) {
        long cur = value;
        while (true) {
            if (cur >= -64 && cur < 64) {
                putU1(cur & 0x7f);
                return;
            }
            putU1(0x80 | (cur & 0x7f));
            cur = cur >> 7;
        }
    }

    /**
     * Writes an unsigned value in a variable byte size encoding.
     */
    private void putUV(long value) {
        long cur = value;
        while (true) {
            assert cur >= 0;
            if (cur < 128) {
                putU1(cur & 0x7f);
                return;
            }
            putU1(0x80 | (cur & 0x7f));
            cur = cur >> 7;
        }
    }

    public void putInt(int value) {
        putSV(value);
    }

    public void putUInt(int value) {
        putUV(value);
    }

    public void putLong(long value) {
        putSV(value);
    }

    public void putDouble(double value) {
        putInt64(Double.doubleToLongBits(value));
    }

    public void putInt64(long value) {
        ensureCapacity(Long.BYTES);
        long cur = value;
        for (int i = 0; i < Long.BYTES; i++) {
            putU1(cur & 0xffL);
            cur >>>= 8;
        }
    }

    public void putUTF8(String value) {
        putByteArray(value.getBytes(StandardCharsets.UTF_8));
    }

    public void putByteArray(byte[] value) {
        putUV(value.length);
        for (int i = 0; i < value.length; i++) {
            putU1(value[i]);
        }
    }

    public void putBigInteger(BigInteger value) {
        BigInteger cur = value;
        while (true) {
            int intValue = cur.intValue();
            if (intValue >= -64 && intValue < 64) {
                putU1(intValue & 0x7f);
                return;
            }
            putU1(0x80 | (intValue & 0x7f));
            cur = cur.shiftRight(7);
        }
    }

    public void putInt32(int value) {
        ensureCapacity(Integer.BYTES);
        int cur = value;
        for (int i = 0; i < Integer.BYTES; i++) {
            putU1(cur & 0xffL);
            cur >>>= 8;
        }
    }

    public int getPosition() {
        return buffer.position();
    }
}

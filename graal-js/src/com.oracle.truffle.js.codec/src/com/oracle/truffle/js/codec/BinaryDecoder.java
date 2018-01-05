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
 * Utility for decoding values from a ByteBuffer.
 */
public class BinaryDecoder {

    private ByteBuffer buffer;

    public BinaryDecoder(ByteBuffer buffer) {
        this.buffer = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }

    private int getU1() {
        return Byte.toUnsignedInt(buffer.get());
    }

    /**
     * Reads a signed value that has been written using {@link BinaryEncoder#putSV variable byte
     * size encoding}.
     */
    private long getSV() {
        long result = 0;
        int shift = 0;
        long b;
        do {
            b = getU1();
            result |= (b & 0x7f) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);

        if ((b & 0x40) != 0 && shift < 64) {
            result |= -1L << shift;
        }
        return result;
    }

    /**
     * Reads a signed variable byte size encoded value that is known to fit into the range of int.
     */
    public int getInt() {
        return (int) getSV();
    }

    /**
     * Reads an unsigned value that has been written using {@link BinaryEncoder#putSV variable byte
     * size encoding}.
     */
    private long getUV() {
        long result = 0;
        int shift = 0;
        long b;
        do {
            b = getU1();
            result |= (b & 0x7f) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);

        return result;
    }

    /**
     * Reads an unsigned variable byte size encoded value that is known to fit into the range of
     * int.
     */
    public int getUInt() {
        return (int) getUV();
    }

    public long getLong() {
        return getSV();
    }

    public String getUTF8() {
        return new String(getByteArray(), StandardCharsets.UTF_8);
    }

    public byte[] getByteArray() {
        int size = getUInt();
        byte[] array = new byte[size];
        for (int i = 0; i < size; i++) {
            array[i] = (byte) getU1();
        }
        return array;
    }

    public BigInteger getBigInteger() {
        BigInteger result = BigInteger.ZERO;
        int shift = 0;
        long b;
        do {
            b = getU1();
            result = result.or(BigInteger.valueOf(b & 0x7f).shiftLeft(shift));
            shift += 7;
        } while ((b & 0x80) != 0);

        if ((b & 0x40) != 0) {
            result = result.negate();
        }
        return result;
    }

    public double getDouble() {
        return Double.longBitsToDouble(getInt64());
    }

    public long getInt64() {
        long result = 0;
        int shift = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            long b = getU1();
            result |= b << shift;
            shift += 8;
        }
        return result;
    }

    public int getInt32() {
        int result = 0;
        int shift = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            long b = getU1();
            result |= b << shift;
            shift += 8;
        }
        return result;
    }

    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}

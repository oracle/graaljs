/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.codec;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.strings.TruffleString;

/**
 * Utility for decoding values from a ByteBuffer.
 */
public final class BinaryDecoder {

    private static final VarHandle INT32 = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT64 = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private final ByteBuffer buffer;
    private int pos;

    public BinaryDecoder(ByteBuffer buffer, int position) {
        this.buffer = buffer;
        this.pos = position;
    }

    public BinaryDecoder(ByteBuffer buffer) {
        this(buffer, 0);
    }

    private int getU1() {
        int u1 = Byte.toUnsignedInt(buffer.get(pos));
        pos += Byte.BYTES;
        return u1;
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

    public TruffleString getString() {
        byte[] byteArray = getByteArray();
        return TruffleString.fromByteArrayUncached(byteArray, TruffleString.Encoding.UTF_16);
    }

    public byte[] getByteArray() {
        int size = getUInt();
        byte[] array = new byte[size];
        buffer.get(pos, array);
        pos += size;
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
        long i64 = (long) INT64.get(buffer, pos);
        pos += Long.BYTES;
        return i64;
    }

    public int getInt32() {
        int i32 = (int) INT32.get(buffer, pos);
        pos += Integer.BYTES;
        return i32;
    }

    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}

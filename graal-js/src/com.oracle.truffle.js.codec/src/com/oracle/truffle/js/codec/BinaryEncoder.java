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

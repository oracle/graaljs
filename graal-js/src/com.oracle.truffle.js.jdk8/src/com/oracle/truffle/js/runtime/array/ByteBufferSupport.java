/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;

import sun.misc.Unsafe;

final class ByteBufferSupport {
    private static final ByteBufferAccess LITTLE_ENDIAN;
    private static final ByteBufferAccess BIG_ENDIAN;
    private static final ByteBufferAccess NATIVE_ORDER;

    private ByteBufferSupport() {
    }

    static ByteBufferAccess littleEndian() {
        return LITTLE_ENDIAN;
    }

    static ByteBufferAccess bigEndian() {
        return BIG_ENDIAN;
    }

    static ByteBufferAccess nativeOrder() {
        return NATIVE_ORDER;
    }

    static {
        // We only use Unsafe for architectures that we know support unaligned accesses.
        String arch = System.getProperty("os.arch");
        boolean unaligned = arch.equals("amd64") ||
                        arch.equals("aarch64") ||
                        arch.equals("x86_64");
        if (unaligned) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                LITTLE_ENDIAN = NativeUnsafeByteBufferAccess.INSTANCE;
                BIG_ENDIAN = ReservedUnsafeByteBufferAccess.INSTANCE;
            } else {
                LITTLE_ENDIAN = ReservedUnsafeByteBufferAccess.INSTANCE;
                BIG_ENDIAN = NativeUnsafeByteBufferAccess.INSTANCE;
            }
            NATIVE_ORDER = NativeUnsafeByteBufferAccess.INSTANCE;
        } else {
            LITTLE_ENDIAN = LittleEndianByteBufferAccess.INSTANCE;
            BIG_ENDIAN = BigEndianByteBufferAccess.INSTANCE;
            NATIVE_ORDER = NativeByteBufferAccess.INSTANCE;
        }
    }
}

abstract class UnsafeByteBufferAccess extends ByteBufferAccess {

    private static final Unsafe UNSAFE;
    private static final long BUFFER_ADDRESS_FIELD_OFFSET;

    private static int checkIndex(ByteBuffer buffer, int i, int nb) {
        if (nb < 1 || i < 0 || i > buffer.limit() - nb) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IndexOutOfBoundsException();
        }
        return i;
    }

    private static long getBufferAddress(ByteBuffer buffer) {
        return UNSAFE.getLong(buffer, BUFFER_ADDRESS_FIELD_OFFSET);
    }

    private static long getAddress(ByteBuffer buffer, int index) {
        return getBufferAddress(buffer) + index;
    }

    @Override
    public int getInt16(ByteBuffer buffer, int index) {
        return UNSAFE.getShort(getAddress(buffer, checkIndex(buffer, index, Short.BYTES)));
    }

    @Override
    public int getInt32(ByteBuffer buffer, int index) {
        return UNSAFE.getInt(getAddress(buffer, checkIndex(buffer, index, Integer.BYTES)));
    }

    @Override
    public long getInt64(ByteBuffer buffer, int index) {
        return UNSAFE.getLong(getAddress(buffer, checkIndex(buffer, index, Long.BYTES)));
    }

    @Override
    public float getFloat(ByteBuffer buffer, int index) {
        return UNSAFE.getFloat(getAddress(buffer, checkIndex(buffer, index, Float.BYTES)));
    }

    @Override
    public double getDouble(ByteBuffer buffer, int index) {
        return UNSAFE.getDouble(getAddress(buffer, checkIndex(buffer, index, Double.BYTES)));
    }

    @Override
    public void putInt16(ByteBuffer buffer, int index, int value) {
        UNSAFE.putShort(getAddress(buffer, checkIndex(buffer, index, Short.BYTES)), (short) value);
    }

    @Override
    public void putInt32(ByteBuffer buffer, int index, int value) {
        UNSAFE.putInt(getAddress(buffer, checkIndex(buffer, index, Integer.BYTES)), value);
    }

    @Override
    public void putInt64(ByteBuffer buffer, int index, long value) {
        UNSAFE.putLong(getAddress(buffer, checkIndex(buffer, index, Long.BYTES)), value);
    }

    @Override
    public void putFloat(ByteBuffer buffer, int index, float value) {
        UNSAFE.putFloat(getAddress(buffer, checkIndex(buffer, index, Float.BYTES)), value);
    }

    @Override
    public void putDouble(ByteBuffer buffer, int index, double value) {
        UNSAFE.putDouble(getAddress(buffer, checkIndex(buffer, index, Double.BYTES)), value);
    }

    static {
        try {
            Field theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeInstance.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafeInstance.get(Unsafe.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("exception while trying to get Unsafe.theUnsafe via reflection:", e);
        }
        try {
            Field bufferAddressField = Buffer.class.getDeclaredField("address");
            BUFFER_ADDRESS_FIELD_OFFSET = UNSAFE.objectFieldOffset(bufferAddressField);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}

final class NativeUnsafeByteBufferAccess extends UnsafeByteBufferAccess {
    static final ByteBufferAccess INSTANCE = new NativeUnsafeByteBufferAccess();
}

final class ReservedUnsafeByteBufferAccess extends UnsafeByteBufferAccess {
    static final ByteBufferAccess INSTANCE = new ReservedUnsafeByteBufferAccess();

    @Override
    public int getInt16(ByteBuffer buffer, int index) {
        return Short.reverseBytes((short) super.getInt16(buffer, index));
    }

    @Override
    public int getInt32(ByteBuffer buffer, int index) {
        return Integer.reverseBytes(super.getInt32(buffer, index));
    }

    @Override
    public long getInt64(ByteBuffer buffer, int index) {
        return Long.reverseBytes(super.getInt64(buffer, index));
    }

    @Override
    public float getFloat(ByteBuffer buffer, int index) {
        return Float.intBitsToFloat(getInt32(buffer, index));
    }

    @Override
    public double getDouble(ByteBuffer buffer, int index) {
        return Double.longBitsToDouble(getInt64(buffer, index));
    }

    @Override
    public void putInt16(ByteBuffer buffer, int index, int value) {
        super.putInt16(buffer, index, Short.reverseBytes((short) value));
    }

    @Override
    public void putInt32(ByteBuffer buffer, int index, int value) {
        super.putInt32(buffer, index, Integer.reverseBytes(value));
    }

    @Override
    public void putInt64(ByteBuffer buffer, int index, long value) {
        super.putInt64(buffer, index, Long.reverseBytes(value));
    }

    @Override
    public void putFloat(ByteBuffer buffer, int index, float value) {
        putInt32(buffer, index, Float.floatToRawIntBits(value));
    }

    @Override
    public void putDouble(ByteBuffer buffer, int index, double value) {
        putInt64(buffer, index, Double.doubleToRawLongBits(value));
    }
}

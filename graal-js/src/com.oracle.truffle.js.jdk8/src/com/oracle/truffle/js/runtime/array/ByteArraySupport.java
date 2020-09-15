/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.oracle.truffle.api.CompilerDirectives;

import sun.misc.Unsafe;

final class ByteArraySupport {
    private ByteArraySupport() {
    }

    static ByteArrayAccess littleEndian() {
        return LittleEndianByteArrayAccess.INSTANCE;
    }

    static ByteArrayAccess bigEndian() {
        return BigEndianByteArrayAccess.INSTANCE;
    }

    static ByteArrayAccess nativeOrder() {
        return SunMiscUnsafeNativeOrderByteArrayAccess.INSTANCE;
    }
}

final class SunMiscUnsafeNativeOrderByteArrayAccess extends ByteArrayAccess {
    static final ByteArrayAccess INSTANCE = new SunMiscUnsafeNativeOrderByteArrayAccess();

    @Override
    public int getInt16(byte[] buffer, int byteIndex) {
        return UNSAFE.getShort(buffer, offset(byteIndex, buffer, Short.BYTES));
    }

    @Override
    public int getInt32(byte[] buffer, int byteIndex) {
        return UNSAFE.getInt(buffer, offset(byteIndex, buffer, Integer.BYTES));
    }

    @Override
    public long getInt64(byte[] buffer, int byteIndex) {
        return UNSAFE.getLong(buffer, offset(byteIndex, buffer, Long.BYTES));
    }

    @Override
    public float getFloat(byte[] buffer, int byteIndex) {
        return UNSAFE.getFloat(buffer, offset(byteIndex, buffer, Float.BYTES));
    }

    @Override
    public double getDouble(byte[] buffer, int byteIndex) {
        return UNSAFE.getDouble(buffer, offset(byteIndex, buffer, Double.BYTES));
    }

    @Override
    public void putInt16(byte[] buffer, int byteIndex, int value) {
        UNSAFE.putShort(buffer, offset(byteIndex, buffer, Short.BYTES), (short) value);
    }

    @Override
    public void putInt32(byte[] buffer, int byteIndex, int value) {
        UNSAFE.putInt(buffer, offset(byteIndex, buffer, Integer.BYTES), value);
    }

    @Override
    public void putInt64(byte[] buffer, int byteIndex, long value) {
        UNSAFE.putLong(buffer, offset(byteIndex, buffer, Long.BYTES), value);
    }

    @Override
    public void putFloat(byte[] buffer, int byteIndex, float value) {
        UNSAFE.putFloat(buffer, offset(byteIndex, buffer, Float.BYTES), value);
    }

    @Override
    public void putDouble(byte[] buffer, int byteIndex, double value) {
        UNSAFE.putDouble(buffer, offset(byteIndex, buffer, Double.BYTES), value);
    }

    private static long offset(int byteIndex, byte[] buffer, int elementSize) {
        if (byteIndex < 0 || byteIndex > buffer.length - elementSize) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IndexOutOfBoundsException();
        }
        return (long) byteIndex * Unsafe.ARRAY_BYTE_INDEX_SCALE + Unsafe.ARRAY_BYTE_BASE_OFFSET;
    }

    private static final Unsafe UNSAFE = AccessController.doPrivileged(new PrivilegedAction<Unsafe>() {
        @Override
        public Unsafe run() {
            try {
                Field theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafeInstance.setAccessible(true);
                return (Unsafe) theUnsafeInstance.get(Unsafe.class);
            } catch (Exception e) {
                throw new RuntimeException("exception while trying to get Unsafe.theUnsafe via reflection:", e);
            }
        }
    });
}

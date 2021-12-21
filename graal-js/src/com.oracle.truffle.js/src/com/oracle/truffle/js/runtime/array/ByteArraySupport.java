/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

final class ByteArraySupport {
    private ByteArraySupport() {
    }

    static ByteArrayAccess littleEndian() {
        return VarHandleLittleEndianByteArrayAccess.INSTANCE;
    }

    static ByteArrayAccess bigEndian() {
        return VarHandleBigEndianByteArrayAccess.INSTANCE;
    }

    static ByteArrayAccess nativeOrder() {
        return VarHandleNativeOrderByteArrayAccess.INSTANCE;
    }
}

final class VarHandleNativeOrderByteArrayAccess extends ByteArrayAccess {
    private static final VarHandle INT16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.nativeOrder());
    private static final VarHandle INT32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());
    private static final VarHandle INT64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private static final VarHandle FLOAT = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.nativeOrder());
    private static final VarHandle DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.nativeOrder());

    static final ByteArrayAccess INSTANCE = new VarHandleNativeOrderByteArrayAccess();

    @Override
    public int getInt16(byte[] buffer, int byteIndex) {
        return (short) INT16.get(buffer, byteIndex);
    }

    @Override
    public int getInt32(byte[] buffer, int byteIndex) {
        return (int) INT32.get(buffer, byteIndex);
    }

    @Override
    public long getInt64(byte[] buffer, int byteIndex) {
        return (long) INT64.get(buffer, byteIndex);
    }

    @Override
    public float getFloat(byte[] buffer, int byteIndex) {
        return (float) FLOAT.get(buffer, byteIndex);
    }

    @Override
    public double getDouble(byte[] buffer, int byteIndex) {
        return (double) DOUBLE.get(buffer, byteIndex);
    }

    @Override
    public void putInt16(byte[] buffer, int byteIndex, int value) {
        INT16.set(buffer, byteIndex, (short) value);
    }

    @Override
    public void putInt32(byte[] buffer, int byteIndex, int value) {
        INT32.set(buffer, byteIndex, value);
    }

    @Override
    public void putInt64(byte[] buffer, int byteIndex, long value) {
        INT64.set(buffer, byteIndex, value);
    }

    @Override
    public void putFloat(byte[] buffer, int byteIndex, float value) {
        FLOAT.set(buffer, byteIndex, value);
    }

    @Override
    public void putDouble(byte[] buffer, int byteIndex, double value) {
        DOUBLE.set(buffer, byteIndex, value);
    }
}

final class VarHandleLittleEndianByteArrayAccess extends ByteArrayAccess {
    private static final VarHandle INT16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle FLOAT = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    static final ByteArrayAccess INSTANCE = new VarHandleLittleEndianByteArrayAccess();

    @Override
    public int getInt16(byte[] buffer, int byteIndex) {
        return (short) INT16.get(buffer, byteIndex);
    }

    @Override
    public int getInt32(byte[] buffer, int byteIndex) {
        return (int) INT32.get(buffer, byteIndex);
    }

    @Override
    public long getInt64(byte[] buffer, int byteIndex) {
        return (long) INT64.get(buffer, byteIndex);
    }

    @Override
    public float getFloat(byte[] buffer, int byteIndex) {
        return (float) FLOAT.get(buffer, byteIndex);
    }

    @Override
    public double getDouble(byte[] buffer, int byteIndex) {
        return (double) DOUBLE.get(buffer, byteIndex);
    }

    @Override
    public void putInt16(byte[] buffer, int byteIndex, int value) {
        INT16.set(buffer, byteIndex, (short) value);
    }

    @Override
    public void putInt32(byte[] buffer, int byteIndex, int value) {
        INT32.set(buffer, byteIndex, value);
    }

    @Override
    public void putInt64(byte[] buffer, int byteIndex, long value) {
        INT64.set(buffer, byteIndex, value);
    }

    @Override
    public void putFloat(byte[] buffer, int byteIndex, float value) {
        FLOAT.set(buffer, byteIndex, value);
    }

    @Override
    public void putDouble(byte[] buffer, int byteIndex, double value) {
        DOUBLE.set(buffer, byteIndex, value);
    }
}

final class VarHandleBigEndianByteArrayAccess extends ByteArrayAccess {
    private static final VarHandle INT16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle FLOAT = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    static final ByteArrayAccess INSTANCE = new VarHandleBigEndianByteArrayAccess();

    @Override
    public int getInt16(byte[] buffer, int byteIndex) {
        return (short) INT16.get(buffer, byteIndex);
    }

    @Override
    public int getInt32(byte[] buffer, int byteIndex) {
        return (int) INT32.get(buffer, byteIndex);
    }

    @Override
    public long getInt64(byte[] buffer, int byteIndex) {
        return (long) INT64.get(buffer, byteIndex);
    }

    @Override
    public float getFloat(byte[] buffer, int byteIndex) {
        return (float) FLOAT.get(buffer, byteIndex);
    }

    @Override
    public double getDouble(byte[] buffer, int byteIndex) {
        return (double) DOUBLE.get(buffer, byteIndex);
    }

    @Override
    public void putInt16(byte[] buffer, int byteIndex, int value) {
        INT16.set(buffer, byteIndex, (short) value);
    }

    @Override
    public void putInt32(byte[] buffer, int byteIndex, int value) {
        INT32.set(buffer, byteIndex, value);
    }

    @Override
    public void putInt64(byte[] buffer, int byteIndex, long value) {
        INT64.set(buffer, byteIndex, value);
    }

    @Override
    public void putFloat(byte[] buffer, int byteIndex, float value) {
        FLOAT.set(buffer, byteIndex, value);
    }

    @Override
    public void putDouble(byte[] buffer, int byteIndex, double value) {
        DOUBLE.set(buffer, byteIndex, value);
    }
}

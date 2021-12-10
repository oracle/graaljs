/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ByteBufferSupport {
    private ByteBufferSupport() {
    }

    static ByteBufferAccess nativeOrder() {
        return NativeVarHandleByteBufferAccess.INSTANCE;
    }

    static ByteBufferAccess littleEndian() {
        return LittleEndianVarHandleByteBufferAccess.INSTANCE;
    }

    static ByteBufferAccess bigEndian() {
        return BigEndianVarHandleByteBufferAccess.INSTANCE;
    }
}

final class NativeVarHandleByteBufferAccess extends ByteBufferAccess {
    private static final VarHandle INT16 = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.nativeOrder());
    private static final VarHandle INT32 = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());
    private static final VarHandle INT64 = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private static final VarHandle FLOAT = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.nativeOrder());
    private static final VarHandle DOUBLE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.nativeOrder());

    static final ByteBufferAccess INSTANCE = new NativeVarHandleByteBufferAccess();

    private NativeVarHandleByteBufferAccess() {
    }

    @Override
    public int getInt16(ByteBuffer buffer, int index) {
        return (short) INT16.get(buffer, index);
    }

    @Override
    public int getInt32(ByteBuffer buffer, int index) {
        return (int) INT32.get(buffer, index);
    }

    @Override
    public long getInt64(ByteBuffer buffer, int index) {
        return (long) INT64.get(buffer, index);
    }

    @Override
    public float getFloat(ByteBuffer buffer, int index) {
        return (float) FLOAT.get(buffer, index);
    }

    @Override
    public double getDouble(ByteBuffer buffer, int index) {
        return (double) DOUBLE.get(buffer, index);
    }

    @Override
    public void putInt16(ByteBuffer buffer, int index, int value) {
        INT16.set(buffer, index, (short) value);
    }

    @Override
    public void putInt32(ByteBuffer buffer, int index, int value) {
        INT32.set(buffer, index, value);
    }

    @Override
    public void putInt64(ByteBuffer buffer, int index, long value) {
        INT64.set(buffer, index, value);
    }

    @Override
    public void putFloat(ByteBuffer buffer, int index, float value) {
        FLOAT.set(buffer, index, value);
    }

    @Override
    public void putDouble(ByteBuffer buffer, int index, double value) {
        DOUBLE.set(buffer, index, value);
    }
}

final class LittleEndianVarHandleByteBufferAccess extends ByteBufferAccess {
    private static final VarHandle INT16 = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT32 = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT64 = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle FLOAT = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle DOUBLE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    static final ByteBufferAccess INSTANCE = new LittleEndianVarHandleByteBufferAccess();

    private LittleEndianVarHandleByteBufferAccess() {
    }

    @Override
    public int getInt16(ByteBuffer buffer, int index) {
        return (short) INT16.get(buffer, index);
    }

    @Override
    public int getInt32(ByteBuffer buffer, int index) {
        return (int) INT32.get(buffer, index);
    }

    @Override
    public long getInt64(ByteBuffer buffer, int index) {
        return (long) INT64.get(buffer, index);
    }

    @Override
    public float getFloat(ByteBuffer buffer, int index) {
        return (float) FLOAT.get(buffer, index);
    }

    @Override
    public double getDouble(ByteBuffer buffer, int index) {
        return (double) DOUBLE.get(buffer, index);
    }

    @Override
    public void putInt16(ByteBuffer buffer, int index, int value) {
        INT16.set(buffer, index, (short) value);
    }

    @Override
    public void putInt32(ByteBuffer buffer, int index, int value) {
        INT32.set(buffer, index, value);
    }

    @Override
    public void putInt64(ByteBuffer buffer, int index, long value) {
        INT64.set(buffer, index, value);
    }

    @Override
    public void putFloat(ByteBuffer buffer, int index, float value) {
        FLOAT.set(buffer, index, value);
    }

    @Override
    public void putDouble(ByteBuffer buffer, int index, double value) {
        DOUBLE.set(buffer, index, value);
    }
}

final class BigEndianVarHandleByteBufferAccess extends ByteBufferAccess {
    private static final VarHandle INT16 = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT32 = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT64 = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle FLOAT = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle DOUBLE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    static final ByteBufferAccess INSTANCE = new BigEndianVarHandleByteBufferAccess();

    private BigEndianVarHandleByteBufferAccess() {
    }

    @Override
    public int getInt16(ByteBuffer buffer, int index) {
        return (short) INT16.get(buffer, index);
    }

    @Override
    public int getInt32(ByteBuffer buffer, int index) {
        return (int) INT32.get(buffer, index);
    }

    @Override
    public long getInt64(ByteBuffer buffer, int index) {
        return (long) INT64.get(buffer, index);
    }

    @Override
    public float getFloat(ByteBuffer buffer, int index) {
        return (float) FLOAT.get(buffer, index);
    }

    @Override
    public double getDouble(ByteBuffer buffer, int index) {
        return (double) DOUBLE.get(buffer, index);
    }

    @Override
    public void putInt16(ByteBuffer buffer, int index, int value) {
        INT16.set(buffer, index, (short) value);
    }

    @Override
    public void putInt32(ByteBuffer buffer, int index, int value) {
        INT32.set(buffer, index, value);
    }

    @Override
    public void putInt64(ByteBuffer buffer, int index, long value) {
        INT64.set(buffer, index, value);
    }

    @Override
    public void putFloat(ByteBuffer buffer, int index, float value) {
        FLOAT.set(buffer, index, value);
    }

    @Override
    public void putDouble(ByteBuffer buffer, int index, double value) {
        DOUBLE.set(buffer, index, value);
    }
}

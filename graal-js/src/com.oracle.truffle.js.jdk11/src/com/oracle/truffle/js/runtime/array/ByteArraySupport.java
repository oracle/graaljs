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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

final class ByteArraySupport {
    private ByteArraySupport() {
    }

    static final ByteArrayAccess LITTLE_ENDIAN_ORDER = new LittleEndianByteArrayAccess();
    static final ByteArrayAccess BIG_ENDIAN_ORDER = new BigEndianByteArrayAccess();
    static final ByteArrayAccess NATIVE_ORDER = new VarHandleNativeOrderByteArrayAccess();
}

final class VarHandleNativeOrderByteArrayAccess extends ByteArrayAccess {
    private static final VarHandle INT16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.nativeOrder());
    private static final VarHandle INT32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());
    private static final VarHandle INT64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private static final VarHandle FLOAT = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.nativeOrder());
    private static final VarHandle DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.nativeOrder());

    @Override
    public int getInt8(byte[] buffer, int offset, int index, int bytesPerElement) {
        return buffer[byteIndex(offset, index, bytesPerElement)];
    }

    @Override
    public int getInt16(byte[] buffer, int offset, int index, int bytesPerElement) {
        return (short) INT16.get(buffer, byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public int getInt32(byte[] buffer, int offset, int index, int bytesPerElement) {
        return (int) INT32.get(buffer, byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public long getInt64(byte[] buffer, int offset, int index, int bytesPerElement) {
        return (long) INT64.get(buffer, byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public float getFloat(byte[] buffer, int offset, int index, int bytesPerElement) {
        return (float) FLOAT.get(buffer, byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public double getDouble(byte[] buffer, int offset, int index, int bytesPerElement) {
        return (double) DOUBLE.get(buffer, byteIndex(offset, index, bytesPerElement));
    }

    @Override
    public void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        buffer[byteIndex(offset, index, bytesPerElement)] = (byte) value;
    }

    @Override
    public void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        INT16.set(buffer, byteIndex(offset, index, bytesPerElement), (short) value);
    }

    @Override
    public void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
        INT32.set(buffer, byteIndex(offset, index, bytesPerElement), value);
    }

    @Override
    public void putInt64(byte[] buffer, int offset, int index, int bytesPerElement, long value) {
        INT64.set(buffer, byteIndex(offset, index, bytesPerElement), value);
    }

    @Override
    public void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value) {
        FLOAT.set(buffer, byteIndex(offset, index, bytesPerElement), value);
    }

    @Override
    public void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value) {
        DOUBLE.set(buffer, byteIndex(offset, index, bytesPerElement), value);
    }

    private static int byteIndex(int offset, int index, int bytesPerElement) {
        return offset + index * bytesPerElement;
    }
}

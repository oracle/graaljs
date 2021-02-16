/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

public abstract class ByteArrayAccess {

    @SuppressWarnings("static-method")
    public final int getInt8(byte[] buffer, int byteIndex) {
        return buffer[byteIndex];
    }

    public final int getUint8(byte[] buffer, int byteIndex) {
        return getInt8(buffer, byteIndex) & 0xff;
    }

    public abstract int getInt16(byte[] buffer, int byteIndex);

    public final int getUint16(byte[] buffer, int byteIndex) {
        return getInt16(buffer, byteIndex) & 0xffff;
    }

    public abstract int getInt32(byte[] buffer, int byteIndex);

    public final long getUint32(byte[] buffer, int byteIndex) {
        return getInt32(buffer, byteIndex) & 0xffffffffL;
    }

    public abstract float getFloat(byte[] buffer, int byteIndex);

    public abstract double getDouble(byte[] buffer, int byteIndex);

    public abstract long getInt64(byte[] buffer, int byteIndex);

    @SuppressWarnings("static-method")
    public final void putInt8(byte[] buffer, int byteIndex, int value) {
        buffer[byteIndex] = (byte) value;
    }

    public abstract void putInt16(byte[] buffer, int byteIndex, int value);

    public abstract void putInt32(byte[] buffer, int byteIndex, int value);

    public abstract void putFloat(byte[] buffer, int byteIndex, float value);

    public abstract void putDouble(byte[] buffer, int byteIndex, double value);

    public abstract void putInt64(byte[] buffer, int byteIndex, long value);

    public static final ByteArrayAccess littleEndian() {
        return ByteArraySupport.littleEndian();
    }

    public static final ByteArrayAccess bigEndian() {
        return ByteArraySupport.bigEndian();
    }

    public static final ByteArrayAccess nativeOrder() {
        return ByteArraySupport.nativeOrder();
    }

    public static final ByteArrayAccess forOrder(boolean littleEndian) {
        return littleEndian ? littleEndian() : bigEndian();
    }
}

final class TruffleByteArrayAccess extends ByteArrayAccess {
    private final com.oracle.truffle.api.memory.ByteArraySupport support;

    TruffleByteArrayAccess(com.oracle.truffle.api.memory.ByteArraySupport support) {
        this.support = support;
    }

    @Override
    public int getInt16(byte[] buffer, int byteIndex) {
        return support.getShort(buffer, byteIndex);
    }

    @Override
    public int getInt32(byte[] buffer, int byteIndex) {
        return support.getInt(buffer, byteIndex);
    }

    @Override
    public float getFloat(byte[] buffer, int byteIndex) {
        return support.getFloat(buffer, byteIndex);
    }

    @Override
    public double getDouble(byte[] buffer, int byteIndex) {
        return support.getDouble(buffer, byteIndex);
    }

    @Override
    public long getInt64(byte[] buffer, int byteIndex) {
        return support.getLong(buffer, byteIndex);
    }

    @Override
    public void putInt16(byte[] buffer, int byteIndex, int value) {
        support.putShort(buffer, byteIndex, (short) value);
    }

    @Override
    public void putInt32(byte[] buffer, int byteIndex, int value) {
        support.putInt(buffer, byteIndex, value);
    }

    @Override
    public void putInt64(byte[] buffer, int byteIndex, long value) {
        support.putLong(buffer, byteIndex, value);
    }

    @Override
    public void putFloat(byte[] buffer, int byteIndex, float value) {
        support.putFloat(buffer, byteIndex, value);
    }

    @Override
    public void putDouble(byte[] buffer, int byteIndex, double value) {
        support.putDouble(buffer, byteIndex, value);
    }
}

/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.interop;

import java.nio.ByteOrder;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;

/**
 * A read-only view on an ArrayBuffer with a fixed offset and length. Delegates other Interop
 * messages to the provided source array.
 *
 * Used to transfer module byte sources from JS to WebAssembly.
 */
@SuppressWarnings({"static-method", "unused"})
@ExportLibrary(value = InteropLibrary.class, delegateTo = "arrayView")
public final class InteropBufferView implements TruffleObject {

    private final int viewByteOffset;
    private final int viewByteLength;
    final JSArrayBufferObject arrayBuffer;
    final JSTypedArrayObject arrayView;

    public InteropBufferView(JSArrayBufferObject arrayBuffer, int byteOffset, int byteLength, JSTypedArrayObject arrayView) {
        assert byteOffset >= 0 : byteOffset;
        assert byteLength >= 0 : byteLength;
        this.viewByteOffset = byteOffset;
        this.viewByteLength = byteLength;
        this.arrayBuffer = arrayBuffer;
        this.arrayView = arrayView;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasBufferElements() {
        return true;
    }

    @ExportMessage
    public long getBufferSize() {
        return viewByteLength;
    }

    private long checkFromIndexSize(long fromIndex, int size) throws InvalidBufferOffsetException {
        long bufferByteOffset = viewByteOffset + fromIndex;
        long bufferByteLength = viewByteLength;
        if ((fromIndex | size) < 0 || size > bufferByteLength - fromIndex) {
            throw InvalidBufferOffsetException.create(fromIndex, size);
        }
        return bufferByteOffset;
    }

    @ExportMessage
    byte readBufferByte(long byteOffset,
                    @CachedLibrary("this.arrayBuffer") InteropLibrary bufferInterop) throws UnsupportedMessageException, InvalidBufferOffsetException {
        return bufferInterop.readBufferByte(arrayBuffer, checkFromIndexSize(byteOffset, Byte.BYTES));
    }

    @ExportMessage
    short readBufferShort(ByteOrder order, long byteOffset,
                    @CachedLibrary("this.arrayBuffer") InteropLibrary bufferInterop) throws UnsupportedMessageException, InvalidBufferOffsetException {
        return bufferInterop.readBufferShort(arrayBuffer, order, checkFromIndexSize(byteOffset, Short.BYTES));
    }

    @ExportMessage
    int readBufferInt(ByteOrder order, long byteOffset,
                    @CachedLibrary("this.arrayBuffer") InteropLibrary bufferInterop) throws UnsupportedMessageException, InvalidBufferOffsetException {
        return bufferInterop.readBufferInt(arrayBuffer, order, checkFromIndexSize(byteOffset, Integer.BYTES));
    }

    @ExportMessage
    long readBufferLong(ByteOrder order, long byteOffset,
                    @CachedLibrary("this.arrayBuffer") InteropLibrary bufferInterop) throws UnsupportedMessageException, InvalidBufferOffsetException {
        return bufferInterop.readBufferLong(arrayBuffer, order, checkFromIndexSize(byteOffset, Long.BYTES));
    }

    @ExportMessage
    float readBufferFloat(ByteOrder order, long byteOffset,
                    @CachedLibrary("this.arrayBuffer") InteropLibrary bufferInterop) throws UnsupportedMessageException, InvalidBufferOffsetException {
        return bufferInterop.readBufferFloat(arrayBuffer, order, checkFromIndexSize(byteOffset, Float.BYTES));
    }

    @ExportMessage
    double readBufferDouble(ByteOrder order, long byteOffset,
                    @CachedLibrary("this.arrayBuffer") InteropLibrary bufferInterop) throws UnsupportedMessageException, InvalidBufferOffsetException {
        return bufferInterop.readBufferDouble(arrayBuffer, order, checkFromIndexSize(byteOffset, Double.BYTES));
    }

    @ExportMessage
    boolean isBufferWritable() {
        return false;
    }

    @ExportMessage
    void writeBufferByte(long byteOffset, byte value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferShort(ByteOrder order, long byteOffset, short value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferInt(ByteOrder order, long byteOffset, int value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferLong(ByteOrder order, long byteOffset, long value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferFloat(ByteOrder order, long byteOffset, float value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferDouble(ByteOrder order, long byteOffset, double value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }
}

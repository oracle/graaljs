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
package com.oracle.truffle.js.runtime.array;

import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetByteArray;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetByteBuffer;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetLength;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetOffset;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.objects.Undefined;

import sun.misc.Unsafe;

public abstract class TypedArray extends ScriptArray {
    // protected final int length;
    // protected final int offset;
    // protected final byte[] buffer;

    private final boolean offset;
    private final int bytesPerElement;
    private final String name;
    private final TypedArrayFactory factory;

    protected TypedArray(TypedArrayFactory factory, boolean offset) {
        this.offset = offset;
        this.bytesPerElement = factory.bytesPerElement();
        this.name = factory.getName();
        this.factory = factory;
    }

    @Override
    public final long length(DynamicObject object, boolean condition) {
        return lengthInt(object, condition);
    }

    @Override
    public final int lengthInt(DynamicObject object, boolean condition) {
        return typedArrayGetLength(object, condition);
    }

    @Override
    public final TypedArray setLengthImpl(DynamicObject object, long len, boolean condition, ProfileHolder profile) {
        return this;
    }

    @Override
    public final long firstElementIndex(DynamicObject object, boolean condition) {
        return 0;
    }

    @Override
    public final long lastElementIndex(DynamicObject object, boolean condition) {
        return length(object, condition) - 1;
    }

    @Override
    public final long nextElementIndex(DynamicObject object, long index, boolean condition) {
        return index + 1;
    }

    @Override
    public final long previousElementIndex(DynamicObject object, long index, boolean condition) {
        return index - 1;
    }

    @Override
    public final Object[] toArray(DynamicObject object) {
        int length = lengthInt(object);
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            result[i] = getElement(object, i);
        }
        return result;
    }

    @Override
    public final ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return this;
    }

    @Override
    public final boolean hasElement(DynamicObject object, long index, boolean condition) {
        return 0 <= index && index < length(object, condition);
    }

    protected static byte[] getByteArray(DynamicObject object, boolean condition) {
        return typedArrayGetByteArray(object, condition);
    }

    /**
     * Get ByteBuffer from TypedArray with unspecified byte order.
     */
    protected static ByteBuffer getByteBuffer(DynamicObject object, boolean condition) {
        return typedArrayGetByteBuffer(object, condition);
    }

    /**
     * Use when native byte order is required.
     */
    protected static ByteBuffer withNativeOrder(ByteBuffer buffer) {
        return buffer.duplicate().order(ByteOrder.nativeOrder());
    }

    public final Object getBufferFromTypedArray(DynamicObject object, boolean condition) {
        return isDirect() ? getByteBuffer(object, condition) : getByteArray(object, condition);
    }

    protected final int getOffset(DynamicObject object, boolean condition) {
        if (offset) {
            return typedArrayGetOffset(object, condition);
        } else {
            return 0;
        }
    }

    public final TypedArrayFactory getFactory() {
        return factory;
    }

    public final int bytesPerElement() {
        return bytesPerElement;
    }

    public final String getName() {
        return name;
    }

    @Override
    public boolean isHolesType() {
        return false;
    }

    @Override
    public boolean hasHoles(DynamicObject object, boolean condition) {
        return false;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        throw Errors.unsupported("cannot removeRange() on TypedArray");
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long atOffset, int size) {
        throw Errors.unsupported("cannot addRange() on TypedArray");
    }

    @Override
    public boolean isSealed() {
        return false;
    }

    @Override
    public boolean isFrozen() {
        return false;
    }

    @Override
    public boolean isLengthNotWritable() {
        return false;
    }

    @Override
    public ScriptArray seal() {
        return this;
    }

    @Override
    public ScriptArray freeze() {
        return this;
    }

    @Override
    public ScriptArray setLengthNotWritable() {
        return this;
    }

    @Override
    public ScriptArray preventExtensions() {
        return this;
    }

    @Override
    public final boolean isStatelessType() {
        return true;
    }

    public boolean isDirect() {
        return false;
    }

    public final boolean hasOffset() {
        return offset;
    }

    protected static BufferAccess getBufferAccess(boolean littleEndian) {
        return littleEndian ? TypedArray.LITTLE_ENDIAN_ORDER : TypedArray.BIG_ENDIAN_ORDER;
    }

    protected static ByteBuffer getByteBufferFromBuffer(DynamicObject buffer, boolean littleEndian, boolean condition) {
        ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(buffer, condition);
        ByteOrder byteOrder = littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        return byteBuffer.duplicate().order(byteOrder);
    }

    public abstract Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition);

    public abstract void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value);

    public static TypedArrayFactory[] factories() {
        return TypedArrayFactory.FACTORIES;
    }

    public abstract static class TypedIntArray<T> extends TypedArray {
        protected TypedIntArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public Object getElement(DynamicObject object, long index, boolean condition) {
            if (hasElement(object, index, condition)) {
                return getInt(object, (int) index, condition);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index, boolean condition) {
            assert hasElement(object, index, condition);
            return getInt(object, (int) index, condition);
        }

        @Override
        public TypedIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setInt(object, (int) index, JSRuntime.toInt32(value), condition);
            }
            return this;
        }

        public final int getInt(DynamicObject object, int index, boolean condition) {
            return getIntImpl(getBufferFromTypedArrayT(object, condition), getOffset(object, condition), index);
        }

        public final void setInt(DynamicObject object, int index, int value, boolean condition) {
            setIntImpl(getBufferFromTypedArrayT(object, condition), getOffset(object, condition), index, value);
        }

        @SuppressWarnings("unchecked")
        private T getBufferFromTypedArrayT(DynamicObject object, boolean condition) {
            return (T) super.getBufferFromTypedArray(object, condition);
        }

        public abstract int getIntImpl(T buffer, int offset, int index);

        public abstract void setIntImpl(T buffer, int offset, int index, int value);
    }

    static final int INT8_BYTES_PER_ELEMENT = 1;

    public static final class Int8Array extends TypedIntArray<byte[]> {
        Int8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getInt8(array, offset, index, INT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(byte[] array, int offset, int index, int value) {
            NATIVE_ORDER.putInt8(array, offset, index, INT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getInt8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectInt8Array extends TypedIntArray<ByteBuffer> {
        DirectInt8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(ByteBuffer buffer, int offset, int index) {
            return buffer.get(offset + index * INT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(ByteBuffer buffer, int offset, int index, int value) {
            buffer.put(offset + index * INT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian, condition).get(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).put(index, (byte) JSRuntime.toInt32(value));
        }
    }

    static final int UINT8_BYTES_PER_ELEMENT = 1;

    public static final class Uint8Array extends TypedIntArray<byte[]> {
        Uint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getUint8(array, offset, index, UINT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(byte[] array, int offset, int index, int value) {
            NATIVE_ORDER.putInt8(array, offset, index, UINT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getUint8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectUint8Array extends TypedIntArray<ByteBuffer> {
        DirectUint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(ByteBuffer buffer, int offset, int index) {
            return buffer.get(offset + index * UINT8_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setIntImpl(ByteBuffer buffer, int offset, int index, int value) {
            buffer.put(offset + index * UINT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getByteBufferFromBuffer(buffer, littleEndian, condition).get(index) & 0xff;
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).put(index, (byte) JSRuntime.toInt32(value));
        }
    }

    public abstract static class AbstractUint8ClampedArray<T> extends TypedIntArray<T> {
        private AbstractUint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public TypedIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setInt(object, (int) index, toInt(JSRuntime.toDouble(value)), condition);
            }
            return this;
        }

        protected static int uint8Clamp(int value) {
            return value < 0 ? 0 : (value > 0xff ? 0xff : value);
        }

        public static int toInt(double value) {
            return (int) JSRuntime.mathRint(value);
        }
    }

    public static final class Uint8ClampedArray extends AbstractUint8ClampedArray<byte[]> {
        Uint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getUint8(array, offset, index, UINT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(byte[] array, int offset, int index, int value) {
            NATIVE_ORDER.putInt8(array, offset, index, UINT8_BYTES_PER_ELEMENT, uint8Clamp(value));
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getUint8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, uint8Clamp(toInt(JSRuntime.toDouble(value))));
        }
    }

    public static final class DirectUint8ClampedArray extends AbstractUint8ClampedArray<ByteBuffer> {
        DirectUint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(ByteBuffer buffer, int offset, int index) {
            return buffer.get(offset + index * UINT8_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setIntImpl(ByteBuffer buffer, int offset, int index, int value) {
            buffer.put(offset + index * UINT8_BYTES_PER_ELEMENT, (byte) uint8Clamp(value));
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getByteBufferFromBuffer(buffer, littleEndian, condition).get(index) & 0xff;
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).put(index, (byte) uint8Clamp(toInt(JSRuntime.toDouble(value))));
        }
    }

    static final int INT16_BYTES_PER_ELEMENT = 2;

    public static final class Int16Array extends TypedIntArray<byte[]> {
        Int16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getInt16(array, offset, index, INT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(byte[] array, int offset, int index, int value) {
            NATIVE_ORDER.putInt16(array, offset, index, INT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getInt16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectInt16Array extends TypedIntArray<ByteBuffer> {
        DirectInt16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(ByteBuffer buffer, int offset, int index) {
            return withNativeOrder(buffer).getShort(offset + index * INT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(ByteBuffer buffer, int offset, int index, int value) {
            withNativeOrder(buffer).putShort(offset + index * INT16_BYTES_PER_ELEMENT, (short) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian, condition).getShort(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putShort(index, (short) JSRuntime.toInt32(value));
        }
    }

    static final int UINT16_BYTES_PER_ELEMENT = 2;

    public static final class Uint16Array extends TypedIntArray<byte[]> {
        Uint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getUint16(array, offset, index, UINT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(byte[] array, int offset, int index, int value) {
            NATIVE_ORDER.putInt16(array, offset, index, UINT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getUint16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectUint16Array extends TypedIntArray<ByteBuffer> {
        DirectUint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(ByteBuffer buffer, int offset, int index) {
            return withNativeOrder(buffer).getChar(offset + index * UINT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(ByteBuffer buffer, int offset, int index, int value) {
            withNativeOrder(buffer).putChar(offset + index * UINT16_BYTES_PER_ELEMENT, (char) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian, condition).getChar(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putChar(index, (char) JSRuntime.toInt32(value));
        }
    }

    static final int INT32_BYTES_PER_ELEMENT = 4;

    public static final class Int32Array extends TypedIntArray<byte[]> {
        Int32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getInt32(array, offset, index, INT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(byte[] array, int offset, int index, int value) {
            NATIVE_ORDER.putInt32(array, offset, index, INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getInt32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectInt32Array extends TypedIntArray<ByteBuffer> {
        DirectInt32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(ByteBuffer buffer, int offset, int index) {
            return withNativeOrder(buffer).getInt(offset + index * INT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(ByteBuffer buffer, int offset, int index, int value) {
            withNativeOrder(buffer).putInt(offset + index * INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getByteBufferFromBuffer(buffer, littleEndian, condition).getInt(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putInt(index, JSRuntime.toInt32(value));
        }
    }

    static final int UINT32_BYTES_PER_ELEMENT = 4;

    public abstract static class AbstractUint32Array<T> extends TypedIntArray<T> {
        private AbstractUint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public Object getElement(DynamicObject object, long index, boolean condition) {
            if (hasElement(object, index, condition)) {
                int value = getInt(object, (int) index, condition);
                return toUint32(value);
            } else {
                return Undefined.instance;
            }
        }

        protected static Number toUint32(int value) {
            if (value >= 0) {
                return value;
            } else {
                return (double) (value & 0xFFFFFFFFL);
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index, boolean condition) {
            assert hasElement(object, index, condition);
            return toUint32(getInt(object, (int) index, condition));
        }
    }

    public static final class Uint32Array extends AbstractUint32Array<byte[]> {
        Uint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getInt32(array, offset, index, UINT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(byte[] array, int offset, int index, int value) {
            NATIVE_ORDER.putInt32(array, offset, index, UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return toUint32((int) getBufferAccess(littleEndian).getUint32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectUint32Array extends AbstractUint32Array<ByteBuffer> {
        DirectUint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(ByteBuffer buffer, int offset, int index) {
            return withNativeOrder(buffer).getInt(offset + index * UINT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(ByteBuffer buffer, int offset, int index, int value) {
            withNativeOrder(buffer).putInt(offset + index * UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return toUint32(getByteBufferFromBuffer(buffer, littleEndian, condition).getInt(index));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putInt(index, JSRuntime.toInt32(value));
        }
    }

    public abstract static class TypedFloatArray<T> extends TypedArray {
        protected TypedFloatArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public final Object getElement(DynamicObject object, long index, boolean condition) {
            if (hasElement(object, index, condition)) {
                return getDouble(object, (int) index, condition);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index, boolean condition) {
            assert hasElement(object, index, condition);
            return getDouble(object, (int) index, condition);
        }

        @Override
        public final TypedFloatArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setDouble(object, (int) index, JSRuntime.toDouble(value), condition);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        private T getBufferFromTypedArrayT(DynamicObject object, boolean condition) {
            return (T) super.getBufferFromTypedArray(object, condition);
        }

        public final double getDouble(DynamicObject object, int index, boolean condition) {
            return getDoubleImpl(getBufferFromTypedArrayT(object, condition), getOffset(object, condition), index);
        }

        public final void setDouble(DynamicObject object, int index, double value, boolean condition) {
            setDoubleImpl(getBufferFromTypedArrayT(object, condition), getOffset(object, condition), index, value);
        }

        public abstract double getDoubleImpl(T buffer, int offset, int index);

        public abstract void setDoubleImpl(T buffer, int offset, int index, double value);
    }

    static final int FLOAT32_BYTES_PER_ELEMENT = 4;

    public static final class Float32Array extends TypedFloatArray<byte[]> {
        Float32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public double getDoubleImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getFloat(array, offset, index, FLOAT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(byte[] array, int offset, int index, double value) {
            NATIVE_ORDER.putFloat(array, offset, index, FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (double) getBufferAccess(littleEndian).getFloat(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putFloat(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.floatValue(value));
        }
    }

    public static final class DirectFloat32Array extends TypedFloatArray<ByteBuffer> {
        DirectFloat32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public double getDoubleImpl(ByteBuffer buffer, int offset, int index) {
            return withNativeOrder(buffer).getFloat(offset + index * FLOAT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(ByteBuffer buffer, int offset, int index, double value) {
            withNativeOrder(buffer).putFloat(offset + index * FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (double) getByteBufferFromBuffer(buffer, littleEndian, condition).getFloat(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putFloat(index, JSRuntime.floatValue(value));
        }
    }

    static final int FLOAT64_BYTES_PER_ELEMENT = 8;

    public static final class Float64Array extends TypedFloatArray<byte[]> {
        Float64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public double getDoubleImpl(byte[] array, int offset, int index) {
            return NATIVE_ORDER.getDouble(array, offset, index, FLOAT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(byte[] array, int offset, int index, double value) {
            NATIVE_ORDER.putDouble(array, offset, index, FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getDouble(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putDouble(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.doubleValue(value));
        }
    }

    public static final class DirectFloat64Array extends TypedFloatArray<ByteBuffer> {
        DirectFloat64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public double getDoubleImpl(ByteBuffer buffer, int offset, int index) {
            return withNativeOrder(buffer).getDouble(offset + index * FLOAT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(ByteBuffer buffer, int offset, int index, double value) {
            withNativeOrder(buffer).putDouble(offset + index * FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getByteBufferFromBuffer(buffer, littleEndian, condition).getDouble(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putDouble(index, JSRuntime.doubleValue(value));
        }
    }

    public abstract static class BufferAccess {
        public abstract int getInt8(byte[] buffer, int offset, int index, int bytesPerElement);

        public final int getUint8(byte[] buffer, int offset, int index, int bytesPerElement) {
            return getInt8(buffer, offset, index, bytesPerElement) & 0xff;
        }

        public abstract int getInt16(byte[] buffer, int offset, int index, int bytesPerElement);

        public final int getUint16(byte[] buffer, int offset, int index, int bytesPerElement) {
            return getInt16(buffer, offset, index, bytesPerElement) & 0xffff;
        }

        public abstract int getInt32(byte[] buffer, int offset, int index, int bytesPerElement);

        public final long getUint32(byte[] buffer, int offset, int index, int bytesPerElement) {
            return getInt32(buffer, offset, index, bytesPerElement) & 0xffffffffL;
        }

        public abstract float getFloat(byte[] buffer, int offset, int index, int bytesPerElement);

        public abstract double getDouble(byte[] buffer, int offset, int index, int bytesPerElement);

        public abstract void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value);

        public abstract void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value);

        public abstract void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value);

        public abstract void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value);

        public abstract void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value);
    }

    protected abstract static class NormalBufferAccess extends BufferAccess {
        private static int makeInt16(byte b0, byte b1) {
            return (b1 << 8) | (b0 & 0xff);
        }

        private static int makeInt32(byte b0, byte b1, byte b2, byte b3) {
            return (((b3) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff)));
        }

        private static long makeInt64(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            return ((((long) b7) << 56) | (((long) b6 & 0xff) << 48) | (((long) b5 & 0xff) << 40) | (((long) b4 & 0xff) << 32) | (((long) b3 & 0xff) << 24) | (((long) b2 & 0xff) << 16) |
                            (((long) b1 & 0xff) << 8) | (((long) b0 & 0xff)));
        }

        @Override
        public final int getInt8(byte[] buffer, int offset, int index, int bytesPerElement) {
            int byteIndex = offset + index * bytesPerElement;
            return buffer[byteIndex];
        }

        @Override
        public final int getInt16(byte[] buffer, int offset, int index, int bytesPerElement) {
            int byteIndex = offset + index * bytesPerElement;
            return makeInt16(buffer[byteIndex + b(0, 2)], buffer[byteIndex + b(1, 2)]);
        }

        @Override
        public final int getInt32(byte[] buffer, int offset, int index, int bytesPerElement) {
            int byteIndex = offset + index * bytesPerElement;
            return makeInt32(buffer[byteIndex + b(0, 4)], buffer[byteIndex + b(1, 4)], buffer[byteIndex + b(2, 4)], buffer[byteIndex + b(3, 4)]);
        }

        @Override
        public final float getFloat(byte[] buffer, int offset, int index, int bytesPerElement) {
            return Float.intBitsToFloat(getInt32(buffer, offset, index, bytesPerElement));
        }

        @Override
        public final double getDouble(byte[] buffer, int offset, int index, int bytesPerElement) {
            int byteIndex = offset + index * bytesPerElement;
            return Double.longBitsToDouble(makeInt64(buffer[byteIndex + b(0, 8)], buffer[byteIndex + b(1, 8)], buffer[byteIndex + b(2, 8)], buffer[byteIndex + b(3, 8)], buffer[byteIndex + b(4, 8)],
                            buffer[byteIndex + b(5, 8)], buffer[byteIndex + b(6, 8)], buffer[byteIndex + b(7, 8)]));
        }

        @Override
        public final void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            int byteIndex = offset + index * bytesPerElement;
            buffer[byteIndex] = (byte) value;
        }

        @Override
        public final void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            int byteIndex = offset + index * bytesPerElement;
            buffer[byteIndex + b(0, 2)] = (byte) (value);
            buffer[byteIndex + b(1, 2)] = (byte) (value >> 8);
        }

        @Override
        public final void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            int byteIndex = offset + index * bytesPerElement;
            buffer[byteIndex + b(0, 4)] = (byte) (value);
            buffer[byteIndex + b(1, 4)] = (byte) (value >> 8);
            buffer[byteIndex + b(2, 4)] = (byte) (value >> 16);
            buffer[byteIndex + b(3, 4)] = (byte) (value >> 24);
        }

        private void putInt64(byte[] buffer, int offset, int index, int bytesPerElement, long value) {
            int byteIndex = offset + index * bytesPerElement;
            buffer[byteIndex + b(0, 8)] = (byte) (value);
            buffer[byteIndex + b(1, 8)] = (byte) (value >> 8);
            buffer[byteIndex + b(2, 8)] = (byte) (value >> 16);
            buffer[byteIndex + b(3, 8)] = (byte) (value >> 24);
            buffer[byteIndex + b(4, 8)] = (byte) (value >> 32);
            buffer[byteIndex + b(5, 8)] = (byte) (value >> 40);
            buffer[byteIndex + b(6, 8)] = (byte) (value >> 48);
            buffer[byteIndex + b(7, 8)] = (byte) (value >> 56);
        }

        @Override
        public final void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value) {
            putInt32(buffer, offset, index, bytesPerElement, Float.floatToIntBits(value));
        }

        @Override
        public final void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value) {
            putInt64(buffer, offset, index, bytesPerElement, Double.doubleToRawLongBits(value));
        }

        /**
         * Byte order.
         *
         * @param bytePos byte position in little endian byte order
         * @param size size of type in bytes
         */
        protected abstract int b(int bytePos, int size);
    }

    protected static final class LittleEndianBufferAccess extends NormalBufferAccess {
        @Override
        protected int b(int bytePos, int size) {
            return bytePos;
        }
    }

    protected static final class BigEndianBufferAccess extends NormalBufferAccess {
        @Override
        protected int b(int bytePos, int size) {
            return size - 1 - bytePos;
        }
    }

    protected static final class SunMiscUnsafeNativeOrderBufferAccess extends BufferAccess {
        @Override
        public int getInt8(byte[] buffer, int offset, int index, int bytesPerElement) {
            return UNSAFE.getByte(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public int getInt16(byte[] buffer, int offset, int index, int bytesPerElement) {
            return UNSAFE.getShort(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public int getInt32(byte[] buffer, int offset, int index, int bytesPerElement) {
            return UNSAFE.getInt(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public float getFloat(byte[] buffer, int offset, int index, int bytesPerElement) {
            return UNSAFE.getFloat(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public double getDouble(byte[] buffer, int offset, int index, int bytesPerElement) {
            return UNSAFE.getDouble(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            UNSAFE.putByte(buffer, offset(offset, index, bytesPerElement), (byte) value);
        }

        @Override
        public void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            UNSAFE.putShort(buffer, offset(offset, index, bytesPerElement), (short) value);
        }

        @Override
        public void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            UNSAFE.putInt(buffer, offset(offset, index, bytesPerElement), value);
        }

        @Override
        public void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value) {
            UNSAFE.putFloat(buffer, offset(offset, index, bytesPerElement), value);
        }

        @Override
        public void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value) {
            UNSAFE.putDouble(buffer, offset(offset, index, bytesPerElement), value);
        }

        private static long offset(int offset, int index, int bytesPerElement) {
            long byteIndex = offset + (long) index * bytesPerElement;
            return byteIndex * Unsafe.ARRAY_BYTE_INDEX_SCALE + Unsafe.ARRAY_BYTE_BASE_OFFSET;
        }

        private static final Unsafe UNSAFE = AccessController.doPrivileged(new PrivilegedAction<Unsafe>() {
            @Override
            public Unsafe run() {
                try {
                    return Unsafe.getUnsafe();
                } catch (SecurityException e) {
                }
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

    protected static final class ByteBufferNativeOrderBufferAccess extends BufferAccess {
        @Override
        public int getInt8(byte[] buffer, int offset, int index, int bytesPerElement) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).get(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public int getInt16(byte[] buffer, int offset, int index, int bytesPerElement) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getShort(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public int getInt32(byte[] buffer, int offset, int index, int bytesPerElement) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getInt(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public float getFloat(byte[] buffer, int offset, int index, int bytesPerElement) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getFloat(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public double getDouble(byte[] buffer, int offset, int index, int bytesPerElement) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getDouble(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public void putInt8(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).put(byteIndex(offset, index, bytesPerElement), (byte) value);
        }

        @Override
        public void putInt16(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putShort(byteIndex(offset, index, bytesPerElement), (short) value);
        }

        @Override
        public void putInt32(byte[] buffer, int offset, int index, int bytesPerElement, int value) {
            ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putInt(byteIndex(offset, index, bytesPerElement), value);
        }

        @Override
        public void putFloat(byte[] buffer, int offset, int index, int bytesPerElement, float value) {
            ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putFloat(byteIndex(offset, index, bytesPerElement), value);
        }

        @Override
        public void putDouble(byte[] buffer, int offset, int index, int bytesPerElement, double value) {
            ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).putDouble(byteIndex(offset, index, bytesPerElement), value);
        }

        private static int byteIndex(int offset, int index, int bytesPerElement) {
            return offset + index * bytesPerElement;
        }
    }

    static final BufferAccess NATIVE_ORDER = new SunMiscUnsafeNativeOrderBufferAccess();
    private static final BufferAccess LITTLE_ENDIAN_ORDER = new LittleEndianBufferAccess();
    private static final BufferAccess BIG_ENDIAN_ORDER = new BigEndianBufferAccess();
}

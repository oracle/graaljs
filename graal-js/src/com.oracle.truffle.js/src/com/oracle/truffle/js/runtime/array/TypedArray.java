/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.array.ByteArraySupport.NATIVE_ORDER;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetByteArray;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetByteBuffer;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetLength;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetOffset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class TypedArray extends ScriptArray {

    private final boolean offset;
    private final int bytesPerElement;
    private final String name;
    private final TypedArrayFactory factory;

    protected TypedArray(TypedArrayFactory factory, boolean offset) {
        this.offset = offset;
        this.bytesPerElement = factory.getBytesPerElement();
        this.name = factory.getName();
        this.factory = factory;
    }

    @Override
    public final long length(DynamicObject object) {
        return lengthInt(object);
    }

    @Override
    public final int lengthInt(DynamicObject object) {
        return typedArrayGetLength(object);
    }

    @Override
    public final TypedArray setLengthImpl(DynamicObject object, long len, boolean condition, ProfileHolder profile) {
        return this;
    }

    @Override
    public final long firstElementIndex(DynamicObject object) {
        return 0;
    }

    @Override
    public final long lastElementIndex(DynamicObject object) {
        return length(object) - 1;
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
        return 0 <= index && index < length(object);
    }

    protected static byte[] getByteArray(DynamicObject object) {
        return typedArrayGetByteArray(object);
    }

    /**
     * Get ByteBuffer from TypedArray with unspecified byte order.
     */
    protected static ByteBuffer getByteBuffer(DynamicObject object) {
        return typedArrayGetByteBuffer(object);
    }

    /**
     * Use when native byte order is required.
     */
    protected static ByteBuffer withNativeOrder(ByteBuffer buffer) {
        return buffer.duplicate().order(ByteOrder.nativeOrder());
    }

    public final Object getBufferFromTypedArray(DynamicObject object) {
        return isDirect() ? getByteBuffer(object) : getByteArray(object);
    }

    protected final int getOffset(DynamicObject object) {
        if (offset) {
            return typedArrayGetOffset(object);
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

    protected static ByteArrayAccess getBufferAccess(boolean littleEndian) {
        return littleEndian ? ByteArraySupport.LITTLE_ENDIAN_ORDER : ByteArraySupport.BIG_ENDIAN_ORDER;
    }

    protected static ByteBuffer getByteBufferFromBuffer(DynamicObject buffer, boolean littleEndian) {
        ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(buffer);
        ByteOrder byteOrder = littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        return byteBuffer.duplicate().order(byteOrder);
    }

    public abstract Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian);

    public abstract void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value);

    public static TypedArrayFactory[] factories(JSContext context) {
        if (context.getContextOptions().isBigInt()) {
            return TypedArrayFactory.FACTORIES;
        } else {
            return TypedArrayFactory.getNoBigIntFactories();
        }
    }

    public abstract static class TypedIntArray<T> extends TypedArray {
        protected TypedIntArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public Object getElement(DynamicObject object, long index, boolean condition) {
            if (hasElement(object, index, condition)) {
                return getInt(object, (int) index);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index, boolean condition) {
            assert hasElement(object, index, condition);
            return getInt(object, (int) index);
        }

        @Override
        public TypedIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setInt(object, (int) index, JSRuntime.toInt32(value));
            }
            return this;
        }

        public final int getInt(DynamicObject object, int index) {
            return getIntImpl(getBufferFromTypedArrayT(object), getOffset(object), index);
        }

        public final void setInt(DynamicObject object, int index, int value) {
            setIntImpl(getBufferFromTypedArrayT(object), getOffset(object), index, value);
        }

        @SuppressWarnings("unchecked")
        private T getBufferFromTypedArrayT(DynamicObject object) {
            return (T) super.getBufferFromTypedArray(object);
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getBufferAccess(littleEndian).getInt8(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian).get(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).put(index, (byte) JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getBufferAccess(littleEndian).getUint8(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getByteBufferFromBuffer(buffer, littleEndian).get(index) & 0xff;
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).put(index, (byte) JSRuntime.toInt32((Number) value));
        }
    }

    public abstract static class AbstractUint8ClampedArray<T> extends TypedIntArray<T> {
        private AbstractUint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public TypedIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setInt(object, (int) index, toInt(JSRuntime.toDouble(value)));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getBufferAccess(littleEndian).getUint8(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer), 0, index, 1, uint8Clamp(toInt(JSRuntime.toDouble((Number) value))));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getByteBufferFromBuffer(buffer, littleEndian).get(index) & 0xff;
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).put(index, (byte) uint8Clamp(toInt(JSRuntime.toDouble((Number) value))));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getBufferAccess(littleEndian).getInt16(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt16(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian).getShort(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putShort(index, (short) JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getBufferAccess(littleEndian).getUint16(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt16(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian).getChar(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putChar(index, (char) JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getBufferAccess(littleEndian).getInt32(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt32(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getByteBufferFromBuffer(buffer, littleEndian).getInt(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putInt(index, JSRuntime.toInt32((Number) value));
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
                int value = getInt(object, (int) index);
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
            return toUint32(getInt(object, (int) index));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return toUint32((int) getBufferAccess(littleEndian).getUint32(JSArrayBuffer.getByteArray(buffer), 0, index, 1));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt32(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toInt32((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return toUint32(getByteBufferFromBuffer(buffer, littleEndian).getInt(index));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putInt(index, JSRuntime.toInt32((Number) value));
        }
    }

    public abstract static class TypedBigIntArray<T> extends TypedArray {
        protected TypedBigIntArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public Object getElement(DynamicObject object, long index, boolean condition) {
            if (hasElement(object, index, condition)) {
                return getBigInt(object, (int) index);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index, boolean condition) {
            assert hasElement(object, index, condition);
            return getBigInt(object, (int) index);
        }

        @Override
        public TypedBigIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setBigInt(object, (int) index, JSRuntime.toBigInt(value));
            }
            return this;
        }

        public final BigInt getBigInt(DynamicObject object, int index) {
            return getBigIntImpl(getBufferFromTypedArrayT(object), getOffset(object), index);
        }

        public final void setBigInt(DynamicObject object, int index, BigInt value) {
            setBigIntImpl(getBufferFromTypedArrayT(object), getOffset(object), index, value);
        }

        @SuppressWarnings("unchecked")
        private T getBufferFromTypedArrayT(DynamicObject object) {
            return (T) super.getBufferFromTypedArray(object);
        }

        public abstract BigInt getBigIntImpl(T buffer, int offset, int index);

        public abstract void setBigIntImpl(T buffer, int offset, int index, BigInt value);
    }

    static final int BIGINT64_BYTES_PER_ELEMENT = 8;

    public static final class BigInt64Array extends TypedBigIntArray<byte[]> {
        BigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public BigInt getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return BigInt.valueOf(getBufferAccess(littleEndian).getInt64(JSArrayBuffer.getByteArray(buffer), 0, index, 1));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt64(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(byte[] buffer, int offset, int index) {
            return BigInt.valueOf(NATIVE_ORDER.getInt64(buffer, offset, index, BIGINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(byte[] buffer, int offset, int index, BigInt value) {
            NATIVE_ORDER.putInt64(buffer, offset, index, BIGINT64_BYTES_PER_ELEMENT, value.longValue());
        }
    }

    public static final class DirectBigInt64Array extends TypedBigIntArray<ByteBuffer> {
        DirectBigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public BigInt getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return BigInt.valueOf(getByteBufferFromBuffer(buffer, littleEndian).getLong(index));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putLong(index, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(ByteBuffer buffer, int offset, int index) {
            return BigInt.valueOf(withNativeOrder(buffer).getLong(offset + index * BIGINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(ByteBuffer buffer, int offset, int index, BigInt value) {
            withNativeOrder(buffer).putLong(offset + index * BIGINT64_BYTES_PER_ELEMENT, value.longValue());
        }
    }

    static final int BIGUINT64_BYTES_PER_ELEMENT = 8;

    public static final class BigUint64Array extends TypedBigIntArray<byte[]> {
        BigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public BigInt getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return BigInt.valueOfUnsigned(getBufferAccess(littleEndian).getInt64(JSArrayBuffer.getByteArray(buffer), 0, index, 1));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putInt64(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(byte[] buffer, int offset, int index) {
            return BigInt.valueOfUnsigned(NATIVE_ORDER.getInt64(buffer, offset, index, BIGUINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(byte[] buffer, int offset, int index, BigInt value) {
            NATIVE_ORDER.putInt64(buffer, offset, index, BIGUINT64_BYTES_PER_ELEMENT, value.longValue());
        }

    }

    public static final class DirectBigUint64Array extends TypedBigIntArray<ByteBuffer> {
        DirectBigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public BigInt getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return BigInt.valueOfUnsigned(getByteBufferFromBuffer(buffer, littleEndian).getLong(index));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putLong(index, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(ByteBuffer buffer, int offset, int index) {
            return BigInt.valueOfUnsigned(withNativeOrder(buffer).getLong(offset + index * BIGUINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(ByteBuffer buffer, int offset, int index, BigInt value) {
            withNativeOrder(buffer).putLong(offset + index * BIGUINT64_BYTES_PER_ELEMENT, value.longValue());
        }
    }

    public abstract static class TypedFloatArray<T> extends TypedArray {
        protected TypedFloatArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public final Object getElement(DynamicObject object, long index, boolean condition) {
            if (hasElement(object, index, condition)) {
                return getDouble(object, (int) index);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index, boolean condition) {
            assert hasElement(object, index, condition);
            return getDouble(object, (int) index);
        }

        @Override
        public final TypedFloatArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setDouble(object, (int) index, JSRuntime.toDouble(value));
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        private T getBufferFromTypedArrayT(DynamicObject object) {
            return (T) super.getBufferFromTypedArray(object);
        }

        public final double getDouble(DynamicObject object, int index) {
            return getDoubleImpl(getBufferFromTypedArrayT(object), getOffset(object), index);
        }

        public final void setDouble(DynamicObject object, int index, double value) {
            setDoubleImpl(getBufferFromTypedArrayT(object), getOffset(object), index, value);
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return (double) getBufferAccess(littleEndian).getFloat(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putFloat(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.floatValue((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return (double) getByteBufferFromBuffer(buffer, littleEndian).getFloat(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putFloat(index, JSRuntime.floatValue((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getBufferAccess(littleEndian).getDouble(JSArrayBuffer.getByteArray(buffer), 0, index, 1);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getBufferAccess(littleEndian).putDouble(JSArrayBuffer.getByteArray(buffer), 0, index, 1, JSRuntime.doubleValue((Number) value));
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
        public Number getBufferElement(DynamicObject buffer, int index, boolean littleEndian) {
            return getByteBufferFromBuffer(buffer, littleEndian).getDouble(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, Object value) {
            getByteBufferFromBuffer(buffer, littleEndian).putDouble(index, JSRuntime.doubleValue((Number) value));
        }
    }
}

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

import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetLength;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetOffset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class TypedArray extends ScriptArray {

    private final int bytesPerElement;
    private final boolean offset;
    private final byte bufferType;
    private final String name;
    private final TypedArrayFactory factory;

    protected static final byte BUFFER_TYPE_ARRAY = 0;
    protected static final byte BUFFER_TYPE_DIRECT = 1;
    protected static final byte BUFFER_TYPE_INTEROP = -1;

    protected TypedArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
        this.bytesPerElement = factory.getBytesPerElement();
        this.offset = offset;
        this.bufferType = bufferType;
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
    public final TypedArray setLengthImpl(DynamicObject object, long len, ProfileHolder profile) {
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
    public final long nextElementIndex(DynamicObject object, long index) {
        return index + 1;
    }

    @Override
    public final long previousElementIndex(DynamicObject object, long index) {
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
    public final ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict) {
        return this;
    }

    @Override
    public final boolean hasElement(DynamicObject object, long index) {
        return 0 <= index && index < length(object);
    }

    /**
     * Get ByteBuffer from ArrayBuffer with unspecified byte order.
     */
    protected static ByteBuffer getDirectByteBuffer(Object buffer) {
        assert !JSArrayBuffer.isDetachedBuffer(buffer); // must be checked by caller
        return JSArrayBuffer.getDirectByteBuffer(buffer);
    }

    /**
     * Get byte[] from ArrayBuffer.
     */
    protected static byte[] getByteArray(Object buffer) {
        assert !JSArrayBuffer.isDetachedBuffer(buffer); // must be checked by caller
        return JSArrayBuffer.getByteArray(buffer);
    }

    /**
     * Get ArrayBuffer from TypedArray.
     */
    public static DynamicObject getBufferFromTypedArray(DynamicObject typedArray) {
        return JSArrayBufferView.getArrayBuffer(typedArray);
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
    public boolean hasHoles(DynamicObject object) {
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

    public final boolean isDirect() {
        return bufferType > 0;
    }

    public final boolean isInterop() {
        return bufferType < 0;
    }

    public final boolean hasOffset() {
        return offset;
    }

    public abstract Object getBufferElement(Object buffer, int index, boolean littleEndian);

    public abstract void setBufferElement(Object buffer, int index, boolean littleEndian, Object value);

    public static TypedArrayFactory[] factories(JSContext context) {
        if (context.getContextOptions().isBigInt()) {
            return TypedArrayFactory.FACTORIES;
        } else {
            return TypedArrayFactory.getNoBigIntFactories();
        }
    }

    protected static RuntimeException indexOfOutBoundsException() {
        CompilerDirectives.transferToInterpreter();
        throw new IndexOutOfBoundsException();
    }

    public abstract static class TypedIntArray<T> extends TypedArray {
        protected TypedIntArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
                return getInt(object, (int) index);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index) {
            assert hasElement(object, index);
            return getInt(object, (int) index);
        }

        @Override
        public TypedIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
                setInt(object, (int) index, JSRuntime.toInt32(value));
            }
            return this;
        }

        public final int getInt(DynamicObject object, int index) {
            return getIntImpl(getBufferFromTypedArray(object), getOffset(object), index);
        }

        public final void setInt(DynamicObject object, int index, int value) {
            setIntImpl(getBufferFromTypedArray(object), getOffset(object), index, value);
        }

        public abstract int getIntImpl(Object buffer, int offset, int index);

        public abstract void setIntImpl(Object buffer, int offset, int index, int value);
    }

    static final int INT8_BYTES_PER_ELEMENT = 1;

    public static final class Int8Array extends TypedIntArray<byte[]> {
        Int8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getInt8(getByteArray(buffer), offset + index * INT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteArrayAccess.nativeOrder().putInt8(getByteArray(buffer), offset + index * INT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteArrayAccess.forOrder(littleEndian).getInt8(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt8(getByteArray(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class DirectInt8Array extends TypedIntArray<ByteBuffer> {
        DirectInt8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return getDirectByteBuffer(buffer).get(offset + index * INT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            getDirectByteBuffer(buffer).put(offset + index * INT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return (int) getDirectByteBuffer(buffer).get(index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            getDirectByteBuffer(buffer).put(index, (byte) JSRuntime.toInt32((Number) value));
        }
    }

    public static final class InteropInt8Array extends TypedIntArray<Object> {
        InteropInt8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferByte(buffer, offset + index * INT8_BYTES_PER_ELEMENT);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferByte(buffer, offset + index * INT8_BYTES_PER_ELEMENT, (byte) value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return (int) interop.readBufferByte(buffer, index);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferByte(buffer, index, (byte) JSRuntime.toInt32((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    static final int UINT8_BYTES_PER_ELEMENT = 1;

    public static final class Uint8Array extends TypedIntArray<byte[]> {
        Uint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getUint8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteArrayAccess.nativeOrder().putInt8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteArrayAccess.forOrder(littleEndian).getUint8(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt8(getByteArray(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class DirectUint8Array extends TypedIntArray<ByteBuffer> {
        DirectUint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return getDirectByteBuffer(buffer).get(offset + index * UINT8_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            getDirectByteBuffer(buffer).put(offset + index * UINT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return getDirectByteBuffer(buffer).get(index) & 0xff;
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            getDirectByteBuffer(buffer).put(index, (byte) JSRuntime.toInt32((Number) value));
        }
    }

    public static final class InteropUint8Array extends TypedIntArray<Object> {
        InteropUint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return Byte.toUnsignedInt(interop.readBufferByte(buffer, offset + index * UINT8_BYTES_PER_ELEMENT));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferByte(buffer, offset + index * UINT8_BYTES_PER_ELEMENT, (byte) value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return Byte.toUnsignedInt(interop.readBufferByte(buffer, index));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferByte(buffer, index, (byte) JSRuntime.toInt32((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    public abstract static class AbstractUint8ClampedArray<T> extends TypedIntArray<T> {
        private AbstractUint8ClampedArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public TypedIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
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
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getUint8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteArrayAccess.nativeOrder().putInt8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT, uint8Clamp(value));
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteArrayAccess.forOrder(littleEndian).getUint8(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt8(getByteArray(buffer), index, uint8Clamp(toInt(JSRuntime.toDouble((Number) value))));
        }
    }

    public static final class DirectUint8ClampedArray extends AbstractUint8ClampedArray<ByteBuffer> {
        DirectUint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return getDirectByteBuffer(buffer).get(offset + index * UINT8_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            getDirectByteBuffer(buffer).put(offset + index * UINT8_BYTES_PER_ELEMENT, (byte) uint8Clamp(value));
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return getDirectByteBuffer(buffer).get(index) & 0xff;
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            getDirectByteBuffer(buffer).put(index, (byte) uint8Clamp(toInt(JSRuntime.toDouble((Number) value))));
        }
    }

    public static final class InteropUint8ClampedArray extends AbstractUint8ClampedArray<Object> {
        InteropUint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return Byte.toUnsignedInt(interop.readBufferByte(buffer, offset + index * UINT8_BYTES_PER_ELEMENT));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferByte(buffer, offset + index * UINT8_BYTES_PER_ELEMENT, (byte) uint8Clamp(value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return Byte.toUnsignedInt(interop.readBufferByte(buffer, index));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferByte(buffer, index, (byte) uint8Clamp(toInt(JSRuntime.toDouble((Number) value))));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    static final int INT16_BYTES_PER_ELEMENT = 2;

    public static final class Int16Array extends TypedIntArray<byte[]> {
        Int16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getInt16(getByteArray(buffer), offset + index * INT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteArrayAccess.nativeOrder().putInt16(getByteArray(buffer), offset + index * INT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteArrayAccess.forOrder(littleEndian).getInt16(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt16(getByteArray(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class DirectInt16Array extends TypedIntArray<ByteBuffer> {
        DirectInt16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteBufferAccess.nativeOrder().getInt16(getDirectByteBuffer(buffer), offset + index * INT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteBufferAccess.nativeOrder().putInt16(getDirectByteBuffer(buffer), offset + index * INT16_BYTES_PER_ELEMENT, (short) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return (int) ByteBufferAccess.forOrder(littleEndian).getInt16(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putInt16(getDirectByteBuffer(buffer), index, (short) JSRuntime.toInt32((Number) value));
        }
    }

    public static final class InteropInt16Array extends TypedIntArray<Object> {
        InteropInt16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferShort(buffer, ByteOrder.nativeOrder(), offset + index * INT16_BYTES_PER_ELEMENT);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferShort(buffer, ByteOrder.nativeOrder(), offset + index * INT16_BYTES_PER_ELEMENT, (short) value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return (int) interop.readBufferShort(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferShort(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, (short) JSRuntime.toInt32((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    static final int UINT16_BYTES_PER_ELEMENT = 2;

    public static final class Uint16Array extends TypedIntArray<byte[]> {
        Uint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getUint16(getByteArray(buffer), offset + index * UINT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteArrayAccess.nativeOrder().putInt16(getByteArray(buffer), offset + index * UINT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteArrayAccess.forOrder(littleEndian).getUint16(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt16(getByteArray(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class DirectUint16Array extends TypedIntArray<ByteBuffer> {
        DirectUint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteBufferAccess.nativeOrder().getUint16(getDirectByteBuffer(buffer), offset + index * UINT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteBufferAccess.nativeOrder().putInt16(getDirectByteBuffer(buffer), offset + index * UINT16_BYTES_PER_ELEMENT, (char) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return (int) ByteBufferAccess.forOrder(littleEndian).getUint16(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putInt16(getDirectByteBuffer(buffer), index, (char) JSRuntime.toInt32((Number) value));
        }
    }

    public static final class InteropUint16Array extends TypedIntArray<Object> {
        InteropUint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return Short.toUnsignedInt(interop.readBufferShort(buffer, ByteOrder.nativeOrder(), offset + index * UINT16_BYTES_PER_ELEMENT));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferShort(buffer, ByteOrder.nativeOrder(), offset + index * UINT16_BYTES_PER_ELEMENT, (short) value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return Short.toUnsignedInt(interop.readBufferShort(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferShort(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, (short) JSRuntime.toInt32((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    static final int INT32_BYTES_PER_ELEMENT = 4;

    public static final class Int32Array extends TypedIntArray<byte[]> {
        Int32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getInt32(getByteArray(buffer), offset + index * INT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteArrayAccess.nativeOrder().putInt32(getByteArray(buffer), offset + index * INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteArrayAccess.forOrder(littleEndian).getInt32(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt32(getByteArray(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class DirectInt32Array extends TypedIntArray<ByteBuffer> {
        DirectInt32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteBufferAccess.nativeOrder().getInt32(getDirectByteBuffer(buffer), offset + index * INT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteBufferAccess.nativeOrder().putInt32(getDirectByteBuffer(buffer), offset + index * INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteBufferAccess.forOrder(littleEndian).getInt32(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putInt32(getDirectByteBuffer(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class InteropInt32Array extends TypedIntArray<Object> {
        InteropInt32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferInt(buffer, ByteOrder.nativeOrder(), offset + index * INT32_BYTES_PER_ELEMENT);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferInt(buffer, ByteOrder.nativeOrder(), offset + index * INT32_BYTES_PER_ELEMENT, value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferInt(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferInt(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, JSRuntime.toInt32((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    static final int UINT32_BYTES_PER_ELEMENT = 4;

    public abstract static class AbstractUint32Array<T> extends TypedIntArray<T> {
        private AbstractUint32Array(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
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
        public Object getElementInBounds(DynamicObject object, long index) {
            assert hasElement(object, index);
            return toUint32(getInt(object, (int) index));
        }
    }

    public static final class Uint32Array extends AbstractUint32Array<byte[]> {
        Uint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getInt32(getByteArray(buffer), offset + index * UINT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteArrayAccess.nativeOrder().putInt32(getByteArray(buffer), offset + index * UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return toUint32((int) ByteArrayAccess.forOrder(littleEndian).getUint32(getByteArray(buffer), index));
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt32(getByteArray(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class DirectUint32Array extends AbstractUint32Array<ByteBuffer> {
        DirectUint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            return ByteBufferAccess.nativeOrder().getInt32(getDirectByteBuffer(buffer), offset + index * UINT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            ByteBufferAccess.nativeOrder().putInt32(getDirectByteBuffer(buffer), offset + index * UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return toUint32(ByteBufferAccess.forOrder(littleEndian).getInt32(getDirectByteBuffer(buffer), index));
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putInt32(getDirectByteBuffer(buffer), index, JSRuntime.toInt32((Number) value));
        }
    }

    public static final class InteropUint32Array extends AbstractUint32Array<Object> {
        InteropUint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferInt(buffer, ByteOrder.nativeOrder(), offset + index * UINT32_BYTES_PER_ELEMENT);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferInt(buffer, ByteOrder.nativeOrder(), offset + index * UINT32_BYTES_PER_ELEMENT, value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferInt(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferInt(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, JSRuntime.toInt32((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    public abstract static class TypedBigIntArray<T> extends TypedArray {
        protected TypedBigIntArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
                return getBigInt(object, (int) index);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index) {
            assert hasElement(object, index);
            return getBigInt(object, (int) index);
        }

        @Override
        public TypedBigIntArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
                setBigInt(object, (int) index, JSRuntime.toBigInt(value));
            }
            return this;
        }

        public final BigInt getBigInt(DynamicObject object, int index) {
            return getBigIntImpl(getBufferFromTypedArray(object), getOffset(object), index);
        }

        public final void setBigInt(DynamicObject object, int index, BigInt value) {
            setBigIntImpl(getBufferFromTypedArray(object), getOffset(object), index, value);
        }

        public abstract BigInt getBigIntImpl(Object buffer, int offset, int index);

        public abstract void setBigIntImpl(Object buffer, int offset, int index, BigInt value);
    }

    static final int BIGINT64_BYTES_PER_ELEMENT = 8;

    public static final class BigInt64Array extends TypedBigIntArray<byte[]> {
        BigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public BigInt getBufferElement(Object buffer, int index, boolean littleEndian) {
            return BigInt.valueOf(ByteArrayAccess.forOrder(littleEndian).getInt64(getByteArray(buffer), index));
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt64(getByteArray(buffer), index, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index) {
            return BigInt.valueOf(ByteArrayAccess.nativeOrder().getInt64(getByteArray(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(Object buffer, int offset, int index, BigInt value) {
            ByteArrayAccess.nativeOrder().putInt64(getByteArray(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT, value.longValue());
        }
    }

    public static final class DirectBigInt64Array extends TypedBigIntArray<ByteBuffer> {
        DirectBigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public BigInt getBufferElement(Object buffer, int index, boolean littleEndian) {
            return BigInt.valueOf(ByteBufferAccess.forOrder(littleEndian).getInt64(getDirectByteBuffer(buffer), index));
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putInt64(getDirectByteBuffer(buffer), index, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index) {
            return BigInt.valueOf(ByteBufferAccess.nativeOrder().getInt64(getDirectByteBuffer(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(Object buffer, int offset, int index, BigInt value) {
            ByteBufferAccess.nativeOrder().putInt64(getDirectByteBuffer(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT, value.longValue());
        }
    }

    public static final class InteropBigInt64Array extends TypedBigIntArray<Object> {
        InteropBigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return BigInt.valueOf(interop.readBufferLong(buffer, ByteOrder.nativeOrder(), offset + index * BIGINT64_BYTES_PER_ELEMENT));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBigIntImpl(Object buffer, int offset, int index, BigInt value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferLong(buffer, ByteOrder.nativeOrder(), offset + index * BIGINT64_BYTES_PER_ELEMENT, value.longValue());
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public BigInt getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return BigInt.valueOf(interop.readBufferLong(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferLong(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, JSRuntime.toBigInt(value).longValue());
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    static final int BIGUINT64_BYTES_PER_ELEMENT = 8;

    public static final class BigUint64Array extends TypedBigIntArray<byte[]> {
        BigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public BigInt getBufferElement(Object buffer, int index, boolean littleEndian) {
            return BigInt.valueOfUnsigned(ByteArrayAccess.forOrder(littleEndian).getInt64(getByteArray(buffer), index));
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putInt64(getByteArray(buffer), index, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index) {
            return BigInt.valueOfUnsigned(ByteArrayAccess.nativeOrder().getInt64(getByteArray(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(Object buffer, int offset, int index, BigInt value) {
            ByteArrayAccess.nativeOrder().putInt64(getByteArray(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT, value.longValue());
        }

    }

    public static final class DirectBigUint64Array extends TypedBigIntArray<ByteBuffer> {
        DirectBigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public BigInt getBufferElement(Object buffer, int index, boolean littleEndian) {
            return BigInt.valueOfUnsigned(ByteBufferAccess.forOrder(littleEndian).getInt64(getDirectByteBuffer(buffer), index));
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putInt64(getDirectByteBuffer(buffer), index, JSRuntime.toBigInt(value).longValue());
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index) {
            return BigInt.valueOfUnsigned(ByteBufferAccess.nativeOrder().getInt64(getDirectByteBuffer(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT));
        }

        @Override
        public void setBigIntImpl(Object buffer, int offset, int index, BigInt value) {
            ByteBufferAccess.nativeOrder().putInt64(getDirectByteBuffer(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT, value.longValue());
        }
    }

    public static final class InteropBigUint64Array extends TypedBigIntArray<Object> {
        InteropBigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return BigInt.valueOfUnsigned(interop.readBufferLong(buffer, ByteOrder.nativeOrder(), offset + index * BIGUINT64_BYTES_PER_ELEMENT));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBigIntImpl(Object buffer, int offset, int index, BigInt value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferLong(buffer, ByteOrder.nativeOrder(), offset + index * BIGUINT64_BYTES_PER_ELEMENT, value.longValue());
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public BigInt getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return BigInt.valueOfUnsigned(interop.readBufferLong(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferLong(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, JSRuntime.toBigInt(value).longValue());
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    public abstract static class TypedFloatArray<T> extends TypedArray {
        protected TypedFloatArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public final Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
                return getDouble(object, (int) index);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index) {
            assert hasElement(object, index);
            return getDouble(object, (int) index);
        }

        @Override
        public final TypedFloatArray<T> setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
                setDouble(object, (int) index, JSRuntime.toDouble(value));
            }
            return this;
        }

        public final double getDouble(DynamicObject object, int index) {
            return getDoubleImpl(getBufferFromTypedArray(object), getOffset(object), index);
        }

        public final void setDouble(DynamicObject object, int index, double value) {
            setDoubleImpl(getBufferFromTypedArray(object), getOffset(object), index, value);
        }

        public abstract double getDoubleImpl(Object buffer, int offset, int index);

        public abstract void setDoubleImpl(Object buffer, int offset, int index, double value);
    }

    static final int FLOAT32_BYTES_PER_ELEMENT = 4;

    public static final class Float32Array extends TypedFloatArray<byte[]> {
        Float32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getFloat(getByteArray(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value) {
            ByteArrayAccess.nativeOrder().putFloat(getByteArray(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return (double) ByteArrayAccess.forOrder(littleEndian).getFloat(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putFloat(getByteArray(buffer), index, JSRuntime.floatValue((Number) value));
        }
    }

    public static final class DirectFloat32Array extends TypedFloatArray<ByteBuffer> {
        DirectFloat32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index) {
            return ByteBufferAccess.nativeOrder().getFloat(getDirectByteBuffer(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value) {
            ByteBufferAccess.nativeOrder().putFloat(getDirectByteBuffer(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return (double) ByteBufferAccess.forOrder(littleEndian).getFloat(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putFloat(getDirectByteBuffer(buffer), index, JSRuntime.floatValue((Number) value));
        }
    }

    public static final class InteropFloat32Array extends TypedFloatArray<Object> {
        InteropFloat32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferFloat(buffer, ByteOrder.nativeOrder(), offset + index * FLOAT32_BYTES_PER_ELEMENT);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferFloat(buffer, ByteOrder.nativeOrder(), offset + index * FLOAT32_BYTES_PER_ELEMENT, (float) value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return (double) interop.readBufferFloat(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferFloat(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, JSRuntime.floatValue((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }

    static final int FLOAT64_BYTES_PER_ELEMENT = 8;

    public static final class Float64Array extends TypedFloatArray<byte[]> {
        Float64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index) {
            return ByteArrayAccess.nativeOrder().getDouble(getByteArray(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value) {
            ByteArrayAccess.nativeOrder().putDouble(getByteArray(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteArrayAccess.forOrder(littleEndian).getDouble(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteArrayAccess.forOrder(littleEndian).putDouble(getByteArray(buffer), index, JSRuntime.doubleValue((Number) value));
        }
    }

    public static final class DirectFloat64Array extends TypedFloatArray<ByteBuffer> {
        DirectFloat64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index) {
            return ByteBufferAccess.nativeOrder().getDouble(getDirectByteBuffer(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value) {
            ByteBufferAccess.nativeOrder().putDouble(getDirectByteBuffer(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            return ByteBufferAccess.forOrder(littleEndian).getDouble(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            ByteBufferAccess.forOrder(littleEndian).putDouble(getDirectByteBuffer(buffer), index, JSRuntime.doubleValue((Number) value));
        }
    }

    public static final class InteropFloat64Array extends TypedFloatArray<Object> {
        InteropFloat64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferDouble(buffer, ByteOrder.nativeOrder(), offset + index * FLOAT64_BYTES_PER_ELEMENT);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferDouble(buffer, ByteOrder.nativeOrder(), offset + index * FLOAT64_BYTES_PER_ELEMENT, value);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                return interop.readBufferDouble(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index);
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value) {
            InteropLibrary interop = InteropLibrary.getUncached(buffer);
            try {
                interop.writeBufferDouble(buffer, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, index, JSRuntime.doubleValue((Number) value));
            } catch (UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw indexOfOutBoundsException();
            }
        }
    }
}

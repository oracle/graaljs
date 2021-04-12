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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
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

    public abstract Object getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop);

    public abstract void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop);

    public static TypedArrayFactory[] factories() {
        return TypedArrayFactory.FACTORIES;
    }

    public static TypedArrayFactory[] factories(JSContext context) {
        if (context.getContextOptions().isBigInt()) {
            return TypedArrayFactory.FACTORIES;
        } else {
            return TypedArrayFactory.getNoBigIntFactories();
        }
    }

    @TruffleBoundary
    protected static JSException unsupportedBufferAccess(Object buffer, UnsupportedMessageException e) {
        return Errors.createTypeErrorInteropException(buffer, e, "buffer access", null);
    }

    public abstract static class TypedIntArray extends TypedArray {
        protected TypedIntArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
                return getInt(object, (int) index, InteropLibrary.getUncached());
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index) {
            assert hasElement(object, index);
            return getInt(object, (int) index, InteropLibrary.getUncached());
        }

        @Override
        public TypedIntArray setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
                setInt(object, (int) index, JSRuntime.toInt32(value), InteropLibrary.getUncached());
            }
            return this;
        }

        public final int getInt(DynamicObject object, int index, InteropLibrary interop) {
            return getIntImpl(getBufferFromTypedArray(object), getOffset(object), index, interop);
        }

        public final void setInt(DynamicObject object, int index, int value, InteropLibrary interop) {
            setIntImpl(getBufferFromTypedArray(object), getOffset(object), index, value, interop);
        }

        public abstract int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop);

        public abstract void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop);

        @Override
        public Object getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return getBufferElementIntImpl(buffer, index, littleEndian, interop);
        }

        public abstract int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop);

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            setBufferElementIntImpl(buffer, index, littleEndian, JSRuntime.toInt32((Number) value), interop);
        }

        public abstract void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop);
    }

    static final int INT8_BYTES_PER_ELEMENT = 1;

    public static final class Int8Array extends TypedIntArray {
        Int8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getInt8(getByteArray(buffer), offset + index * INT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt8(getByteArray(buffer), offset + index * INT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getInt8(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt8(getByteArray(buffer), index, value);
        }
    }

    public static final class DirectInt8Array extends TypedIntArray {
        DirectInt8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return getDirectByteBuffer(buffer).get(offset + index * INT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            getDirectByteBuffer(buffer).put(offset + index * INT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return getDirectByteBuffer(buffer).get(index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            getDirectByteBuffer(buffer).put(index, (byte) value);
        }
    }

    public static class InteropInt8Array extends InteropOneByteIntArray {
        InteropInt8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }
    }

    public static class InteropOneByteIntArray extends TypedIntArray {
        InteropOneByteIntArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return readBufferByte(buffer, offset + index * INT8_BYTES_PER_ELEMENT, interop);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            writeBufferByte(buffer, offset + index * INT8_BYTES_PER_ELEMENT, (byte) value, interop);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return readBufferByte(buffer, index, interop);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            writeBufferByte(buffer, index, (byte) value, interop);
        }

        static byte readBufferByte(Object buffer, int byteIndex, InteropLibrary interop) {
            try {
                return interop.readBufferByte(buffer, byteIndex);
            } catch (UnsupportedMessageException e) {
                throw unsupportedBufferAccess(buffer, e);
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }

        static void writeBufferByte(Object buffer, int byteIndex, byte value, InteropLibrary interop) {
            try {
                interop.writeBufferByte(buffer, byteIndex, value);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorReadOnlyBuffer();
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }
    }

    static final int UINT8_BYTES_PER_ELEMENT = 1;

    public static final class Uint8Array extends TypedIntArray {
        Uint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getUint8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getUint8(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt8(getByteArray(buffer), index, value);
        }
    }

    public static final class DirectUint8Array extends TypedIntArray {
        DirectUint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return getDirectByteBuffer(buffer).get(offset + index * UINT8_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            getDirectByteBuffer(buffer).put(offset + index * UINT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return getDirectByteBuffer(buffer).get(index) & 0xff;
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            getDirectByteBuffer(buffer).put(index, (byte) value);
        }
    }

    public static final class InteropUint8Array extends InteropOneByteIntArray {
        InteropUint8Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return super.getIntImpl(buffer, offset, index, interop) & 0xff;
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return super.getBufferElementIntImpl(buffer, index, littleEndian, interop) & 0xff;
        }
    }

    public abstract static class AbstractUint8ClampedArray extends TypedIntArray {
        private AbstractUint8ClampedArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public TypedIntArray setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
                setInt(object, (int) index, toInt(JSRuntime.toDouble(value)), InteropLibrary.getUncached());
            }
            return this;
        }

        protected static int uint8Clamp(int value) {
            return value < 0 ? 0 : (value > 0xff ? 0xff : value);
        }

        public static int toInt(double value) {
            return (int) Math.rint(value);
        }

        @Override
        public final void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            setBufferElementIntImpl(buffer, index, littleEndian, toInt(JSRuntime.toDouble((Number) value)), interop);
        }
    }

    public static final class Uint8ClampedArray extends AbstractUint8ClampedArray {
        Uint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getUint8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt8(getByteArray(buffer), offset + index * UINT8_BYTES_PER_ELEMENT, uint8Clamp(value));
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getUint8(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt8(getByteArray(buffer), index, uint8Clamp(value));
        }
    }

    public static final class DirectUint8ClampedArray extends AbstractUint8ClampedArray {
        DirectUint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return getDirectByteBuffer(buffer).get(offset + index * UINT8_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            getDirectByteBuffer(buffer).put(offset + index * UINT8_BYTES_PER_ELEMENT, (byte) uint8Clamp(value));
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return getDirectByteBuffer(buffer).get(index) & 0xff;
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            getDirectByteBuffer(buffer).put(index, (byte) uint8Clamp(value));
        }
    }

    public static final class InteropUint8ClampedArray extends AbstractUint8ClampedArray {
        InteropUint8ClampedArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return InteropInt8Array.readBufferByte(buffer, offset + index * UINT8_BYTES_PER_ELEMENT, interop) & 0xff;
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            InteropInt8Array.writeBufferByte(buffer, offset + index * UINT8_BYTES_PER_ELEMENT, (byte) uint8Clamp(value), interop);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return InteropInt8Array.readBufferByte(buffer, index, interop) & 0xff;
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            InteropInt8Array.writeBufferByte(buffer, index, (byte) uint8Clamp(value), interop);
        }
    }

    static final int INT16_BYTES_PER_ELEMENT = 2;

    public static final class Int16Array extends TypedIntArray {
        Int16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getInt16(getByteArray(buffer), offset + index * INT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt16(getByteArray(buffer), offset + index * INT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getInt16(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt16(getByteArray(buffer), index, value);
        }
    }

    public static final class DirectInt16Array extends TypedIntArray {
        DirectInt16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getInt16(getDirectByteBuffer(buffer), offset + index * INT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putInt16(getDirectByteBuffer(buffer), offset + index * INT16_BYTES_PER_ELEMENT, (short) value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteBufferAccess.forOrder(littleEndian).getInt16(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putInt16(getDirectByteBuffer(buffer), index, (short) value);
        }
    }

    public static class InteropInt16Array extends InteropTwoByteIntArray {
        InteropInt16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }
    }

    public static class InteropTwoByteIntArray extends TypedIntArray {
        InteropTwoByteIntArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return readBufferShort(buffer, offset + index * INT16_BYTES_PER_ELEMENT, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            writeBufferShort(buffer, offset + index * INT16_BYTES_PER_ELEMENT, (short) value, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return readBufferShort(buffer, index, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            writeBufferShort(buffer, index, (short) value, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        static short readBufferShort(Object buffer, int byteIndex, ByteOrder order, InteropLibrary interop) {
            try {
                return interop.readBufferShort(buffer, order, byteIndex);
            } catch (UnsupportedMessageException e) {
                throw unsupportedBufferAccess(buffer, e);
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }

        static void writeBufferShort(Object buffer, int byteIndex, short value, ByteOrder order, InteropLibrary interop) {
            try {
                interop.writeBufferShort(buffer, order, byteIndex, value);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorReadOnlyBuffer();
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }
    }

    static final int UINT16_BYTES_PER_ELEMENT = 2;

    public static final class Uint16Array extends TypedIntArray {
        Uint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getUint16(getByteArray(buffer), offset + index * UINT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt16(getByteArray(buffer), offset + index * UINT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getUint16(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt16(getByteArray(buffer), index, value);
        }
    }

    public static final class DirectUint16Array extends TypedIntArray {
        DirectUint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getUint16(getDirectByteBuffer(buffer), offset + index * UINT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putInt16(getDirectByteBuffer(buffer), offset + index * UINT16_BYTES_PER_ELEMENT, (char) value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteBufferAccess.forOrder(littleEndian).getUint16(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putInt16(getDirectByteBuffer(buffer), index, (char) value);
        }
    }

    public static final class InteropUint16Array extends InteropTwoByteIntArray {
        InteropUint16Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return super.getIntImpl(buffer, offset, index, interop) & 0xffff;
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return super.getBufferElementIntImpl(buffer, index, littleEndian, interop) & 0xffff;
        }
    }

    static final int INT32_BYTES_PER_ELEMENT = 4;

    public static final class Int32Array extends TypedIntArray {
        Int32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getInt32(getByteArray(buffer), offset + index * INT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt32(getByteArray(buffer), offset + index * INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getInt32(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt32(getByteArray(buffer), index, value);
        }
    }

    public static final class DirectInt32Array extends TypedIntArray {
        DirectInt32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getInt32(getDirectByteBuffer(buffer), offset + index * INT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putInt32(getDirectByteBuffer(buffer), offset + index * INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteBufferAccess.forOrder(littleEndian).getInt32(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putInt32(getDirectByteBuffer(buffer), index, value);
        }
    }

    public static final class InteropInt32Array extends TypedIntArray {
        InteropInt32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return readBufferInt(buffer, offset + index * INT32_BYTES_PER_ELEMENT, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            writeBufferInt(buffer, offset + index * INT32_BYTES_PER_ELEMENT, value, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return readBufferInt(buffer, index, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            writeBufferInt(buffer, index, value, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        static int readBufferInt(Object buffer, int byteIndex, ByteOrder order, InteropLibrary interop) {
            try {
                return interop.readBufferInt(buffer, order, byteIndex);
            } catch (UnsupportedMessageException e) {
                throw unsupportedBufferAccess(buffer, e);
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }

        static void writeBufferInt(Object buffer, int byteIndex, int value, ByteOrder order, InteropLibrary interop) {
            try {
                interop.writeBufferInt(buffer, order, byteIndex, value);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorReadOnlyBuffer();
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }
    }

    static final int UINT32_BYTES_PER_ELEMENT = 4;

    public abstract static class AbstractUint32Array extends TypedIntArray {
        private AbstractUint32Array(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
                int value = getInt(object, (int) index, InteropLibrary.getUncached());
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
            return toUint32(getInt(object, (int) index, InteropLibrary.getUncached()));
        }

        @Override
        public Object getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return toUint32(getBufferElementIntImpl(buffer, index, littleEndian, interop));
        }
    }

    public static final class Uint32Array extends AbstractUint32Array {
        Uint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getInt32(getByteArray(buffer), offset + index * UINT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt32(getByteArray(buffer), offset + index * UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getInt32(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt32(getByteArray(buffer), index, value);
        }
    }

    public static final class DirectUint32Array extends AbstractUint32Array {
        DirectUint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getInt32(getDirectByteBuffer(buffer), offset + index * UINT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putInt32(getDirectByteBuffer(buffer), offset + index * UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteBufferAccess.forOrder(littleEndian).getInt32(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putInt32(getDirectByteBuffer(buffer), index, value);
        }
    }

    public static final class InteropUint32Array extends AbstractUint32Array {
        InteropUint32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public int getIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return InteropInt32Array.readBufferInt(buffer, offset + index * UINT32_BYTES_PER_ELEMENT, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public void setIntImpl(Object buffer, int offset, int index, int value, InteropLibrary interop) {
            InteropInt32Array.writeBufferInt(buffer, offset + index * UINT32_BYTES_PER_ELEMENT, value, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public int getBufferElementIntImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return InteropInt32Array.readBufferInt(buffer, index, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        @Override
        public void setBufferElementIntImpl(Object buffer, int index, boolean littleEndian, int value, InteropLibrary interop) {
            InteropInt32Array.writeBufferInt(buffer, index, value, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }
    }

    public abstract static class TypedBigIntArray extends TypedArray {
        protected TypedBigIntArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
                return getBigInt(object, (int) index, InteropLibrary.getUncached());
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index) {
            assert hasElement(object, index);
            return getBigInt(object, (int) index, InteropLibrary.getUncached());
        }

        @Override
        public TypedBigIntArray setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
                setBigInt(object, (int) index, JSRuntime.toBigInt(value), InteropLibrary.getUncached());
            }
            return this;
        }

        public final BigInt getBigInt(DynamicObject object, int index, InteropLibrary interop) {
            return getBigIntImpl(getBufferFromTypedArray(object), getOffset(object), index, interop);
        }

        public final void setBigInt(DynamicObject object, int index, BigInt value, InteropLibrary interop) {
            setLongImpl(getBufferFromTypedArray(object), getOffset(object), index, value.longValue(), interop);
        }

        public BigInt getBigIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return BigInt.valueOf(getLongImpl(buffer, offset, index, interop));
        }

        public abstract long getLongImpl(Object buffer, int offset, int index, InteropLibrary interop);

        public abstract void setLongImpl(Object buffer, int offset, int index, long value, InteropLibrary interop);

        @Override
        public Object getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return BigInt.valueOf(getBufferElementLongImpl(buffer, index, littleEndian, interop));
        }

        public abstract long getBufferElementLongImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop);

        @Override
        public final void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            setBufferElementLongImpl(buffer, index, littleEndian, JSRuntime.toBigInt(value).longValue(), interop);
        }

        public abstract void setBufferElementLongImpl(Object buffer, int index, boolean littleEndian, long value, InteropLibrary interop);
    }

    static final int BIGINT64_BYTES_PER_ELEMENT = 8;

    public static final class BigInt64Array extends TypedBigIntArray {
        BigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public long getBufferElementLongImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getInt64(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementLongImpl(Object buffer, int index, boolean littleEndian, long value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt64(getByteArray(buffer), index, value);
        }

        @Override
        public long getLongImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getInt64(getByteArray(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setLongImpl(Object buffer, int offset, int index, long value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt64(getByteArray(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT, value);
        }
    }

    public static final class DirectBigInt64Array extends TypedBigIntArray {
        DirectBigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public long getBufferElementLongImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteBufferAccess.forOrder(littleEndian).getInt64(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElementLongImpl(Object buffer, int index, boolean littleEndian, long value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putInt64(getDirectByteBuffer(buffer), index, value);
        }

        @Override
        public long getLongImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getInt64(getDirectByteBuffer(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setLongImpl(Object buffer, int offset, int index, long value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putInt64(getDirectByteBuffer(buffer), offset + index * BIGINT64_BYTES_PER_ELEMENT, value);
        }
    }

    public static class InteropBigInt64Array extends InteropBigIntArray {
        InteropBigInt64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }
    }

    public static class InteropBigIntArray extends TypedBigIntArray {
        InteropBigIntArray(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public long getLongImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return readBufferLong(buffer, offset + index * BIGINT64_BYTES_PER_ELEMENT, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public void setLongImpl(Object buffer, int offset, int index, long value, InteropLibrary interop) {
            writeBufferLong(buffer, offset + index * BIGINT64_BYTES_PER_ELEMENT, value, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public long getBufferElementLongImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return readBufferLong(buffer, index, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        @Override
        public void setBufferElementLongImpl(Object buffer, int index, boolean littleEndian, long value, InteropLibrary interop) {
            writeBufferLong(buffer, index, value, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        static long readBufferLong(Object buffer, int byteIndex, ByteOrder order, InteropLibrary interop) {
            try {
                return interop.readBufferLong(buffer, order, byteIndex);
            } catch (UnsupportedMessageException e) {
                throw unsupportedBufferAccess(buffer, e);
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }

        static void writeBufferLong(Object buffer, int byteIndex, long value, ByteOrder order, InteropLibrary interop) {
            try {
                interop.writeBufferLong(buffer, order, byteIndex, value);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorReadOnlyBuffer();
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }
    }

    static final int BIGUINT64_BYTES_PER_ELEMENT = 8;

    public static final class BigUint64Array extends TypedBigIntArray {
        BigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public Object getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return BigInt.valueOfUnsigned(getBufferElementLongImpl(buffer, index, littleEndian, interop));
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return BigInt.valueOfUnsigned(getLongImpl(buffer, offset, index, interop));
        }

        @Override
        public long getBufferElementLongImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getInt64(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElementLongImpl(Object buffer, int index, boolean littleEndian, long value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putInt64(getByteArray(buffer), index, value);
        }

        @Override
        public long getLongImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getInt64(getByteArray(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setLongImpl(Object buffer, int offset, int index, long value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putInt64(getByteArray(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT, value);
        }
    }

    public static final class DirectBigUint64Array extends TypedBigIntArray {
        DirectBigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public Object getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return BigInt.valueOfUnsigned(getBufferElementLongImpl(buffer, index, littleEndian, interop));
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return BigInt.valueOfUnsigned(getLongImpl(buffer, offset, index, interop));
        }

        @Override
        public long getBufferElementLongImpl(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteBufferAccess.forOrder(littleEndian).getInt64(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElementLongImpl(Object buffer, int index, boolean littleEndian, long value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putInt64(getDirectByteBuffer(buffer), index, value);
        }

        @Override
        public long getLongImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getInt64(getDirectByteBuffer(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setLongImpl(Object buffer, int offset, int index, long value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putInt64(getDirectByteBuffer(buffer), offset + index * BIGUINT64_BYTES_PER_ELEMENT, value);
        }
    }

    public static final class InteropBigUint64Array extends InteropBigIntArray {
        InteropBigUint64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset);
        }

        @Override
        public BigInt getBigIntImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return BigInt.valueOfUnsigned(getLongImpl(buffer, offset, index, interop));
        }

        @Override
        public Object getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return BigInt.valueOfUnsigned(getBufferElementLongImpl(buffer, index, littleEndian, interop));
        }
    }

    public abstract static class TypedFloatArray extends TypedArray {
        protected TypedFloatArray(TypedArrayFactory factory, boolean offset, byte bufferType) {
            super(factory, offset, bufferType);
        }

        @Override
        public final Object getElement(DynamicObject object, long index) {
            if (hasElement(object, index)) {
                return getDouble(object, (int) index, InteropLibrary.getUncached());
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public Object getElementInBounds(DynamicObject object, long index) {
            assert hasElement(object, index);
            return getDouble(object, (int) index, InteropLibrary.getUncached());
        }

        @Override
        public final TypedFloatArray setElementImpl(DynamicObject object, long index, Object value, boolean strict) {
            if (hasElement(object, index)) {
                setDouble(object, (int) index, JSRuntime.toDouble(value), InteropLibrary.getUncached());
            }
            return this;
        }

        public final double getDouble(DynamicObject object, int index, InteropLibrary interop) {
            return getDoubleImpl(getBufferFromTypedArray(object), getOffset(object), index, interop);
        }

        public final void setDouble(DynamicObject object, int index, double value, InteropLibrary interop) {
            setDoubleImpl(getBufferFromTypedArray(object), getOffset(object), index, value, interop);
        }

        public abstract double getDoubleImpl(Object buffer, int offset, int index, InteropLibrary interop);

        public abstract void setDoubleImpl(Object buffer, int offset, int index, double value, InteropLibrary interop);
    }

    static final int FLOAT32_BYTES_PER_ELEMENT = 4;

    public static final class Float32Array extends TypedFloatArray {
        Float32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getFloat(getByteArray(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putFloat(getByteArray(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return (double) ByteArrayAccess.forOrder(littleEndian).getFloat(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putFloat(getByteArray(buffer), index, JSRuntime.floatValue((Number) value));
        }
    }

    public static final class DirectFloat32Array extends TypedFloatArray {
        DirectFloat32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getFloat(getDirectByteBuffer(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putFloat(getDirectByteBuffer(buffer), offset + index * FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return (double) ByteBufferAccess.forOrder(littleEndian).getFloat(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putFloat(getDirectByteBuffer(buffer), index, JSRuntime.floatValue((Number) value));
        }
    }

    public static final class InteropFloat32Array extends TypedFloatArray {
        InteropFloat32Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return readBufferFloat(buffer, offset + index * FLOAT32_BYTES_PER_ELEMENT, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value, InteropLibrary interop) {
            writeBufferFloat(buffer, offset + index * FLOAT32_BYTES_PER_ELEMENT, (float) value, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return (double) readBufferFloat(buffer, index, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            writeBufferFloat(buffer, index, JSRuntime.floatValue((Number) value), littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        static float readBufferFloat(Object buffer, int byteIndex, ByteOrder order, InteropLibrary interop) {
            try {
                return interop.readBufferFloat(buffer, order, byteIndex);
            } catch (UnsupportedMessageException e) {
                throw unsupportedBufferAccess(buffer, e);
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }

        static void writeBufferFloat(Object buffer, int byteIndex, float value, ByteOrder order, InteropLibrary interop) {
            try {
                interop.writeBufferFloat(buffer, order, byteIndex, value);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorReadOnlyBuffer();
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }
    }

    static final int FLOAT64_BYTES_PER_ELEMENT = 8;

    public static final class Float64Array extends TypedFloatArray {
        Float64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_ARRAY);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteArrayAccess.nativeOrder().getDouble(getByteArray(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value, InteropLibrary interop) {
            ByteArrayAccess.nativeOrder().putDouble(getByteArray(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteArrayAccess.forOrder(littleEndian).getDouble(getByteArray(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            ByteArrayAccess.forOrder(littleEndian).putDouble(getByteArray(buffer), index, JSRuntime.doubleValue((Number) value));
        }
    }

    public static final class DirectFloat64Array extends TypedFloatArray {
        DirectFloat64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_DIRECT);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return ByteBufferAccess.nativeOrder().getDouble(getDirectByteBuffer(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value, InteropLibrary interop) {
            ByteBufferAccess.nativeOrder().putDouble(getDirectByteBuffer(buffer), offset + index * FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return ByteBufferAccess.forOrder(littleEndian).getDouble(getDirectByteBuffer(buffer), index);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            ByteBufferAccess.forOrder(littleEndian).putDouble(getDirectByteBuffer(buffer), index, JSRuntime.doubleValue((Number) value));
        }
    }

    public static final class InteropFloat64Array extends TypedFloatArray {
        InteropFloat64Array(TypedArrayFactory factory, boolean offset) {
            super(factory, offset, BUFFER_TYPE_INTEROP);
        }

        @Override
        public double getDoubleImpl(Object buffer, int offset, int index, InteropLibrary interop) {
            return readBufferDouble(buffer, offset + index * FLOAT64_BYTES_PER_ELEMENT, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public void setDoubleImpl(Object buffer, int offset, int index, double value, InteropLibrary interop) {
            writeBufferDouble(buffer, offset + index * FLOAT64_BYTES_PER_ELEMENT, (float) value, ByteOrder.nativeOrder(), interop);
        }

        @Override
        public Number getBufferElement(Object buffer, int index, boolean littleEndian, InteropLibrary interop) {
            return readBufferDouble(buffer, index, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        @Override
        public void setBufferElement(Object buffer, int index, boolean littleEndian, Object value, InteropLibrary interop) {
            writeBufferDouble(buffer, index, JSRuntime.doubleValue((Number) value), littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, interop);
        }

        static double readBufferDouble(Object buffer, int byteIndex, ByteOrder order, InteropLibrary interop) {
            try {
                return interop.readBufferDouble(buffer, order, byteIndex);
            } catch (UnsupportedMessageException e) {
                throw unsupportedBufferAccess(buffer, e);
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }

        static void writeBufferDouble(Object buffer, int byteIndex, double value, ByteOrder order, InteropLibrary interop) {
            try {
                interop.writeBufferDouble(buffer, order, byteIndex, value);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorReadOnlyBuffer();
            } catch (InvalidBufferOffsetException e) {
                throw Errors.createRangeErrorInvalidBufferOffset();
            }
        }
    }
}

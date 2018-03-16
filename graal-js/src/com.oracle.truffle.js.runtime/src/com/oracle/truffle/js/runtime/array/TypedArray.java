/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
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

    protected TypedArray(boolean offset) {
        this.offset = offset;
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
     * Use when byte order does not matter.
     */
    protected static ByteBuffer getByteBuffer(DynamicObject object, boolean condition) {
        return typedArrayGetByteBuffer(object, condition);
    }

    /**
     * Use when native byte order is required.
     */
    protected static ByteBuffer getByteBufferNativeOrder(DynamicObject object, boolean condition) {
        ByteBuffer buffer = typedArrayGetByteBuffer(object, condition);
        return buffer.duplicate().order(ByteOrder.nativeOrder());
    }

    protected final int getOffset(DynamicObject object, boolean condition) {
        if (offset) {
            return typedArrayGetOffset(object, condition);
        } else {
            return 0;
        }
    }

    public abstract TypedArrayFactory getFactory();

    public final int bytesPerElement() {
        return getFactory().bytesPerElement();
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
        throw Errors.notYetImplemented("cannot removeRange() on TypedArray");
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long atOffset, int size) {
        throw Errors.notYetImplemented("cannot addRange() on TypedArray");
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

    public abstract Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition);

    public abstract void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value);

    public abstract static class TypedIntArray extends TypedArray {
        protected TypedIntArray(boolean offset) {
            super(offset);
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
        public TypedIntArray setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setInt(object, (int) index, JSRuntime.toInt32(value), condition);
            }
            return this;
        }

        public abstract int getInt(DynamicObject object, int index, boolean floatingCondition);

        public abstract void setInt(DynamicObject object, int index, int value, boolean condition);
    }

    private static final int INT8_BYTES_PER_ELEMENT = 1;
    public static final TypedArrayFactory INT8_FACTORY = new TypedArrayFactory(INT8_BYTES_PER_ELEMENT, "Int8Array",
                    new Int8Array(false), new Int8Array(true), new DirectInt8Array(false), new DirectInt8Array(true));

    public static final class Int8Array extends TypedIntArray {
        private Int8Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return INT8_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getInt8(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, INT8_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            NATIVE_ORDER.putInt8(getByteArray(object, condition), getOffset(object, condition), index, INT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getInt8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectInt8Array extends TypedIntArray {
        private DirectInt8Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return INT8_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean condition) {
            return getByteBuffer(object, condition).get(getOffset(object, condition) + index * INT8_BYTES_PER_ELEMENT);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            getByteBuffer(object, condition).put(getOffset(object, condition) + index * INT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian, condition).get(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).put(index, (byte) JSRuntime.toInt32(value));
        }
    }

    private static final int UINT8_BYTES_PER_ELEMENT = 1;
    public static final TypedArrayFactory UINT8_FACTORY = new TypedArrayFactory(UINT8_BYTES_PER_ELEMENT, "Uint8Array",
                    new Uint8Array(false), new Uint8Array(true), new DirectUint8Array(false), new DirectUint8Array(true));

    public static final class Uint8Array extends TypedIntArray {
        private Uint8Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT8_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getUint8(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, UINT8_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            NATIVE_ORDER.putInt8(getByteArray(object, condition), getOffset(object, condition), index, UINT8_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getUint8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt8(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectUint8Array extends TypedIntArray {
        private DirectUint8Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT8_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean condition) {
            return getByteBuffer(object, condition).get(getOffset(object, condition) + index * UINT8_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            getByteBuffer(object, condition).put(getOffset(object, condition) + index * UINT8_BYTES_PER_ELEMENT, (byte) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getByteBufferFromBuffer(buffer, littleEndian, condition).get(index) & 0xff;
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).put(index, (byte) JSRuntime.toInt32(value));
        }
    }

    private static final int UINT8_CLAMPED_BYTES_PER_ELEMENT = 1;
    public static final TypedArrayFactory UINT8_CLAMPED_FACTORY = new TypedArrayFactory(UINT8_CLAMPED_BYTES_PER_ELEMENT, "Uint8ClampedArray",
                    new Uint8ClampedArray(false), new Uint8ClampedArray(true), new DirectUint8ClampedArray(false), new DirectUint8ClampedArray(true));

    public abstract static class AbstractUint8ClampedArray extends TypedIntArray {
        private AbstractUint8ClampedArray(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT8_CLAMPED_FACTORY;
        }

        @Override
        public TypedIntArray setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setInt(object, (int) index, (int) JSRuntime.mathRint(JSRuntime.toDouble(value)), condition);
            }
            return this;
        }

        protected static int clamp(int value) {
            return value < 0 ? 0 : (value > 0xff ? 0xff : value);
        }

        public static int clamp(double value) {
            return (int) JSRuntime.mathRint(value);
        }

        @Override
        public final Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            throw Errors.shouldNotReachHere();
        }

        @Override
        public final void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            throw Errors.shouldNotReachHere();
        }
    }

    public static final class Uint8ClampedArray extends AbstractUint8ClampedArray {
        private Uint8ClampedArray(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT8_CLAMPED_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getUint8(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, UINT8_CLAMPED_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            NATIVE_ORDER.putInt8(getByteArray(object, condition), getOffset(object, condition), index, UINT8_CLAMPED_BYTES_PER_ELEMENT, clamp(value));
        }
    }

    public static final class DirectUint8ClampedArray extends AbstractUint8ClampedArray {
        private DirectUint8ClampedArray(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT8_CLAMPED_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean condition) {
            return getByteBuffer(object, condition).get(getOffset(object, condition) + index * UINT8_CLAMPED_BYTES_PER_ELEMENT) & 0xff;
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            getByteBuffer(object, condition).put(getOffset(object, condition) + index * UINT8_CLAMPED_BYTES_PER_ELEMENT, (byte) clamp(value));
        }

        @Override
        public boolean isDirect() {
            return true;
        }
    }

    private static final int INT16_BYTES_PER_ELEMENT = 2;
    public static final TypedArrayFactory INT16_FACTORY = new TypedArrayFactory(INT16_BYTES_PER_ELEMENT, "Int16Array",
                    new Int16Array(false), new Int16Array(true), new DirectInt16Array(false), new DirectInt16Array(true));

    public static final class Int16Array extends TypedIntArray {
        private Int16Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return INT16_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getInt16(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, INT16_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            NATIVE_ORDER.putInt16(getByteArray(object, condition), getOffset(object, condition), index, INT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getInt16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectInt16Array extends TypedIntArray {
        private DirectInt16Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return INT16_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean condition) {
            return getByteBufferNativeOrder(object, condition).getShort(getOffset(object, condition) + index * INT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            getByteBufferNativeOrder(object, condition).putShort(getOffset(object, condition) + index * INT16_BYTES_PER_ELEMENT, (short) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian, condition).getShort(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putShort(index, (short) JSRuntime.toInt32(value));
        }
    }

    private static final int UINT16_BYTES_PER_ELEMENT = 2;
    public static final TypedArrayFactory UINT16_FACTORY = new TypedArrayFactory(UINT16_BYTES_PER_ELEMENT, "Uint16Array",
                    new Uint16Array(false), new Uint16Array(true), new DirectUint16Array(false), new DirectUint16Array(true));

    public static final class Uint16Array extends TypedIntArray {
        private Uint16Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT16_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getUint16(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, UINT16_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            NATIVE_ORDER.putInt16(getByteArray(object, condition), getOffset(object, condition), index, UINT16_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getUint16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt16(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectUint16Array extends TypedIntArray {
        private DirectUint16Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT16_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean condition) {
            return getByteBufferNativeOrder(object, condition).getChar(getOffset(object, condition) + index * UINT16_BYTES_PER_ELEMENT);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            getByteBufferNativeOrder(object, condition).putChar(getOffset(object, condition) + index * UINT16_BYTES_PER_ELEMENT, (char) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (int) getByteBufferFromBuffer(buffer, littleEndian, condition).getChar(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putChar(index, (char) JSRuntime.toInt32(value));
        }
    }

    private static final int INT32_BYTES_PER_ELEMENT = 4;
    public static final TypedArrayFactory INT32_FACTORY = new TypedArrayFactory(INT32_BYTES_PER_ELEMENT, "Int32Array",
                    new Int32Array(false), new Int32Array(true), new DirectInt32Array(false), new DirectInt32Array(true));

    public static final class Int32Array extends TypedIntArray {
        private Int32Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return INT32_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getInt32(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, INT32_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            NATIVE_ORDER.putInt32(getByteArray(object, condition), getOffset(object, condition), index, INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getInt32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectInt32Array extends TypedIntArray {
        private DirectInt32Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return INT32_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean condition) {
            return getByteBufferNativeOrder(object, condition).getInt(getOffset(object, condition) + index * INT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            getByteBufferNativeOrder(object, condition).putInt(getOffset(object, condition) + index * INT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getByteBufferFromBuffer(buffer, littleEndian, condition).getInt(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putInt(index, JSRuntime.toInt32(value));
        }
    }

    private static final int UINT32_BYTES_PER_ELEMENT = 4;
    public static final TypedArrayFactory UINT32_FACTORY = new TypedArrayFactory(UINT32_BYTES_PER_ELEMENT, "Uint32Array",
                    new Uint32Array(false), new Uint32Array(true), new DirectUint32Array(false), new DirectUint32Array(true));

    public abstract static class AbstractUint32Array extends TypedIntArray {
        private AbstractUint32Array(boolean offset) {
            super(offset);
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

        protected static Object toUint32(int value) {
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

    public static final class Uint32Array extends AbstractUint32Array {
        private Uint32Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT32_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getInt32(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, UINT32_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            NATIVE_ORDER.putInt32(getByteArray(object, condition), getOffset(object, condition), index, UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return toUint32((int) getBufferAccess(littleEndian).getUint32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putInt32(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.toInt32(value));
        }
    }

    public static final class DirectUint32Array extends AbstractUint32Array {
        private DirectUint32Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return UINT32_FACTORY;
        }

        @Override
        public int getInt(DynamicObject object, int index, boolean condition) {
            return getByteBufferNativeOrder(object, condition).getInt(getOffset(object, condition) + index * UINT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setInt(DynamicObject object, int index, int value, boolean condition) {
            getByteBufferNativeOrder(object, condition).putInt(getOffset(object, condition) + index * UINT32_BYTES_PER_ELEMENT, value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return toUint32(getByteBufferFromBuffer(buffer, littleEndian, condition).getInt(index));
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putInt(index, JSRuntime.toInt32(value));
        }
    }

    public abstract static class TypedFloatArray extends TypedArray {
        protected TypedFloatArray(boolean offset) {
            super(offset);
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
        public final TypedFloatArray setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
            if (hasElement(object, index, condition)) {
                setDouble(object, (int) index, JSRuntime.toDouble(value), condition);
            }
            return this;
        }

        public abstract double getDouble(DynamicObject object, int index, boolean floatingCondition);

        public abstract void setDouble(DynamicObject object, int index, double value, boolean condition);
    }

    private static final int FLOAT32_BYTES_PER_ELEMENT = 4;
    public static final TypedArrayFactory FLOAT32_FACTORY = new TypedArrayFactory(FLOAT32_BYTES_PER_ELEMENT, "Float32Array",
                    new Float32Array(false), new Float32Array(true), new DirectFloat32Array(false), new DirectFloat32Array(true));

    public static final class Float32Array extends TypedFloatArray {
        private Float32Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return FLOAT32_FACTORY;
        }

        @Override
        public double getDouble(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getFloat(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, FLOAT32_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setDouble(DynamicObject object, int index, double value, boolean condition) {
            NATIVE_ORDER.putFloat(getByteArray(object, condition), getOffset(object, condition), index, FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (double) getBufferAccess(littleEndian).getFloat(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putFloat(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.floatValue(value));
        }
    }

    public static final class DirectFloat32Array extends TypedFloatArray {
        private DirectFloat32Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return FLOAT32_FACTORY;
        }

        @Override
        public double getDouble(DynamicObject object, int index, boolean condition) {
            return getByteBufferNativeOrder(object, condition).getFloat(getOffset(object, condition) + index * FLOAT32_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDouble(DynamicObject object, int index, double value, boolean condition) {
            getByteBufferNativeOrder(object, condition).putFloat(getOffset(object, condition) + index * FLOAT32_BYTES_PER_ELEMENT, (float) value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return (double) getByteBufferFromBuffer(buffer, littleEndian, condition).getFloat(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putFloat(index, JSRuntime.floatValue(value));
        }
    }

    private static final int FLOAT64_BYTES_PER_ELEMENT = 8;
    public static final TypedArrayFactory FLOAT64_FACTORY = new TypedArrayFactory(FLOAT64_BYTES_PER_ELEMENT, "Float64Array",
                    new Float64Array(false), new Float64Array(true), new DirectFloat64Array(false), new DirectFloat64Array(true));

    public static final class Float64Array extends TypedFloatArray {
        private Float64Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return FLOAT64_FACTORY;
        }

        @Override
        public double getDouble(DynamicObject object, int index, boolean floatingCondition) {
            return NATIVE_ORDER.getDouble(getByteArray(object, floatingCondition), getOffset(object, floatingCondition), index, FLOAT64_BYTES_PER_ELEMENT, floatingCondition);
        }

        @Override
        public void setDouble(DynamicObject object, int index, double value, boolean condition) {
            NATIVE_ORDER.putDouble(getByteArray(object, condition), getOffset(object, condition), index, FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getBufferAccess(littleEndian).getDouble(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, condition);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getBufferAccess(littleEndian).putDouble(JSArrayBuffer.getByteArray(buffer, condition), 0, index, 1, JSRuntime.doubleValue(value));
        }
    }

    public static final class DirectFloat64Array extends TypedFloatArray {
        private DirectFloat64Array(boolean offset) {
            super(offset);
        }

        @Override
        public TypedArrayFactory getFactory() {
            return FLOAT64_FACTORY;
        }

        @Override
        public double getDouble(DynamicObject object, int index, boolean condition) {
            return getByteBufferNativeOrder(object, condition).getDouble(getOffset(object, condition) + index * FLOAT64_BYTES_PER_ELEMENT);
        }

        @Override
        public void setDouble(DynamicObject object, int index, double value, boolean condition) {
            getByteBufferNativeOrder(object, condition).putDouble(getOffset(object, condition) + index * FLOAT64_BYTES_PER_ELEMENT, value);
        }

        @Override
        public boolean isDirect() {
            return true;
        }

        @Override
        public Object getBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition) {
            return getByteBufferFromBuffer(buffer, littleEndian, condition).getDouble(index);
        }

        @Override
        public void setBufferElement(DynamicObject buffer, int index, boolean littleEndian, boolean condition, Number value) {
            getByteBufferFromBuffer(buffer, littleEndian, condition).putDouble(index, JSRuntime.doubleValue(value));
        }
    }

    public static final class TypedArrayFactory {
        private final int bytesPerElement;
        @CompilationFinal private int factoryIndex;
        private final String name;
        private final TypedArray arrayType;
        private final TypedArray arrayTypeWithOffset;
        private final TypedArray directArrayType;
        private final TypedArray directArrayTypeWithOffset;

        TypedArrayFactory(int bytesPerElement, String name, TypedArray arrayType, TypedArray arrayTypeWithOffset, TypedArray directArrayType, TypedArray directArrayTypeWithOffset) {
            this.bytesPerElement = bytesPerElement;
            this.name = name;
            this.arrayType = arrayType;
            this.arrayTypeWithOffset = arrayTypeWithOffset;
            this.directArrayType = directArrayType;
            this.directArrayTypeWithOffset = directArrayTypeWithOffset;
            assert !arrayType.hasOffset() && arrayTypeWithOffset.hasOffset() && !directArrayType.hasOffset() && directArrayTypeWithOffset.hasOffset();
        }

        public TypedArray createArrayType(boolean direct, boolean offset) {
            if (direct) {
                if (offset) {
                    return directArrayTypeWithOffset;
                } else {
                    return directArrayType;
                }
            } else {
                if (offset) {
                    return arrayTypeWithOffset;
                } else {
                    return arrayType;
                }
            }
        }

        public int bytesPerElement() {
            return bytesPerElement;
        }

        public int getFactoryIndex() {
            return factoryIndex;
        }

        public String getName() {
            return name;
        }
    }

    public abstract static class BufferAccess {
        public abstract int getInt8(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition);

        public final int getUint8(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return getInt8(buffer, offset, index, bytesPerElement, floatingCondition) & 0xff;
        }

        public abstract int getInt16(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition);

        public final int getUint16(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return getInt16(buffer, offset, index, bytesPerElement, floatingCondition) & 0xffff;
        }

        public abstract int getInt32(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition);

        public final long getUint32(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return getInt32(buffer, offset, index, bytesPerElement, floatingCondition) & 0xffffffffL;
        }

        public abstract float getFloat(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition);

        public abstract double getDouble(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition);

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
        public final int getInt8(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            int byteIndex = offset + index * bytesPerElement;
            return buffer[byteIndex];
        }

        @Override
        public final int getInt16(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            int byteIndex = offset + index * bytesPerElement;
            return makeInt16(buffer[byteIndex + b(0, 2)], buffer[byteIndex + b(1, 2)]);
        }

        @Override
        public final int getInt32(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            int byteIndex = offset + index * bytesPerElement;
            return makeInt32(buffer[byteIndex + b(0, 4)], buffer[byteIndex + b(1, 4)], buffer[byteIndex + b(2, 4)], buffer[byteIndex + b(3, 4)]);
        }

        @Override
        public final float getFloat(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return Float.intBitsToFloat(getInt32(buffer, offset, index, bytesPerElement, floatingCondition));
        }

        @Override
        public final double getDouble(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
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
        public int getInt8(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return UNSAFE.getByte(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public int getInt16(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return UNSAFE.getShort(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public int getInt32(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return UNSAFE.getInt(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public float getFloat(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return UNSAFE.getFloat(buffer, offset(offset, index, bytesPerElement));
        }

        @Override
        public double getDouble(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
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
        public int getInt8(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).get(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public int getInt16(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getShort(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public int getInt32(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getInt(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public float getFloat(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
            return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getFloat(byteIndex(offset, index, bytesPerElement));
        }

        @Override
        public double getDouble(byte[] buffer, int offset, int index, int bytesPerElement, boolean floatingCondition) {
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
    public static final TypedArrayFactory[] FACTORIES = createFactories();

    private static TypedArrayFactory[] createFactories() {
        TypedArrayFactory[] factories = new TypedArrayFactory[]{INT8_FACTORY, UINT8_FACTORY, UINT8_CLAMPED_FACTORY, INT16_FACTORY, UINT16_FACTORY, INT32_FACTORY, UINT32_FACTORY,
                        FLOAT32_FACTORY, FLOAT64_FACTORY};
        for (int i = 0; i < factories.length; i++) {
            factories[i].factoryIndex = i;
        }
        return factories;
    }
}

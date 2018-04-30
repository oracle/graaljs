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
package com.oracle.truffle.js.runtime.builtins;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class SIMDType {
    static final boolean LittleEndian = isLittleEndian();

    private static boolean isLittleEndian() {
        return ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);
    }

    public abstract SIMDTypeFactory<? extends SIMDType> getFactory();

    public abstract void serialize(byte[] block, int offset, Object n);

    public abstract Object deserialize(byte[] block, int offset);

    public abstract Object cast(Number o);

    public abstract int getNumberOfElements();

    public abstract int getBytesPerElement();

    public abstract static class SIMDTypeInt extends SIMDType {

        @Override
        public void serialize(byte[] block, int offset, Object n) {
            int val = (int) JSRuntime.toInteger(n);
            if (LittleEndian) {
                val = Integer.reverseBytes(val);
            }
            ByteBuffer.wrap(block).putInt(offset, val);
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            int val = 0;
            val = ByteBuffer.wrap(block).getInt(offset);
            if (LittleEndian) {
                val = Integer.reverseBytes(val);
            }
            return val;
        }

        public abstract long getMax();

        public abstract long getMin();
    }

    private static final int FLOAT32X4_BYTES_PER_ELEMENT = 4;
    private static final int FLOAT32X4_NUMBER_OF_ELEMENTS = 4;
    public static final SIMDTypeFactory<SIMDFloat32x4> FLOAT32X4_FACTORY = new SIMDTypeFactory<>(FLOAT32X4_BYTES_PER_ELEMENT, FLOAT32X4_NUMBER_OF_ELEMENTS, "Float32x4", new SIMDFloat32x4());

    public static final class SIMDFloat32x4 extends SIMDTypedFloat {
        private SIMDFloat32x4() {
        }

        @Override
        public SIMDTypeFactory<SIMDFloat32x4> getFactory() {
            return FLOAT32X4_FACTORY;
        }

        @Override
        public Object cast(Number o) {
            if (o instanceof Float) {
                return o;
            }
            if (o instanceof Double) {
                return (float) (double) o;
            }
            return JSRuntime.floatValue(o);
        }

        @Override
        public void serialize(byte[] block, int offset, Object n) {
            int val = Float.floatToRawIntBits((float) n);
            if (LittleEndian) {
                val = Integer.reverseBytes(val);
            }

            ByteBuffer.wrap(block, offset, getFactory().bytesPerElement).putInt(val);
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            int val = 0;
            val = ByteBuffer.wrap(block).getInt(offset);

            if (LittleEndian) {
                val = Integer.reverseBytes(val);
            }

            return Float.intBitsToFloat(val);
        }

        @Override
        public int getNumberOfElements() {
            return FLOAT32X4_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return FLOAT32X4_BYTES_PER_ELEMENT;
        }
    }

    private static final int INT32X4_BYTES_PER_ELEMENT = 4;
    private static final int INT32X4_NUMBER_OF_ELEMENTS = 4;
    public static final SIMDTypeFactory<SIMDInt32x4> INT32X4_FACTORY = new SIMDTypeFactory<>(INT32X4_BYTES_PER_ELEMENT, INT32X4_NUMBER_OF_ELEMENTS, "Int32x4", new SIMDInt32x4());

    public static final class SIMDInt32x4 extends SIMDTypeInt {
        private SIMDInt32x4() {
        }

        @Override
        public SIMDTypeFactory<SIMDInt32x4> getFactory() {
            return INT32X4_FACTORY;
        }

        @Override
        public Object cast(Number o) {
            if (o instanceof Float) {
                return JSRuntime.toInt32((float) o);
            }
            return JSRuntime.toInt32(o);
        }

        @Override
        public long getMax() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long getMin() {
            return Integer.MIN_VALUE;
        }

        @Override
        public int getNumberOfElements() {
            return INT32X4_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return INT32X4_BYTES_PER_ELEMENT;
        }
    }

    private static final int INT16X8_BYTES_PER_ELEMENT = 2;
    private static final int INT16X8_NUMBER_OF_ELEMENTS = 8;
    public static final SIMDTypeFactory<SIMDInt16x8> INT16X8_FACTORY = new SIMDTypeFactory<>(INT16X8_BYTES_PER_ELEMENT, INT16X8_NUMBER_OF_ELEMENTS, "Int16x8", new SIMDInt16x8());

    public static final class SIMDInt16x8 extends SIMDTypeInt {
        private SIMDInt16x8() {
        }

        @Override
        public SIMDTypeFactory<SIMDInt16x8> getFactory() {
            return INT16X8_FACTORY;
        }

        @Override
        public Object cast(Number o) {
            if (o instanceof Short) {
                return (int) (short) o;
            }
            if (o instanceof Float) {
                return JSRuntime.toInt16((float) o);
            }
            return JSRuntime.toInt16(o);
        }

        @Override
        public long getMax() {
            return Short.MAX_VALUE;
        }

        @Override
        public long getMin() {
            return Short.MIN_VALUE;
        }

        @Override
        public void serialize(byte[] block, int offset, Object n) {
            short val = (short) (int) n;
            if (LittleEndian) {
                val = Short.reverseBytes(val);
            }
            ByteBuffer.wrap(block).putShort(offset, val);
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            short val = 0;
            val = ByteBuffer.wrap(block).getShort(offset);
            if (LittleEndian) {
                val = Short.reverseBytes(val);
            }
            return (int) val;
        }

        @Override
        public int getNumberOfElements() {
            return INT16X8_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return INT16X8_BYTES_PER_ELEMENT;
        }
    }

    private static final int INT8X16_BYTES_PER_ELEMENT = 1;
    private static final int INT8X16_NUMBER_OF_ELEMENTS = 16;
    public static final SIMDTypeFactory<SIMDInt8x16> INT8X16_FACTORY = new SIMDTypeFactory<>(INT8X16_BYTES_PER_ELEMENT, INT8X16_NUMBER_OF_ELEMENTS, "Int8x16", new SIMDInt8x16());

    public static final class SIMDInt8x16 extends SIMDTypeInt {
        private SIMDInt8x16() {
        }

        @Override
        public SIMDTypeFactory<SIMDInt8x16> getFactory() {
            return INT8X16_FACTORY;
        }

        @Override
        public Object cast(Number o) {
            if (o instanceof Byte) {
                return (int) (byte) o;
            }
            if (o instanceof Float) {
                return JSRuntime.toInt8((float) o);
            }
            return JSRuntime.toInt8(o);
        }

        @Override
        public long getMax() {
            return Byte.MAX_VALUE;
        }

        @Override
        public long getMin() {
            return Byte.MIN_VALUE;
        }

        @Override
        public void serialize(byte[] block, int offset, Object n) {
            byte val = (byte) (int) n;

            ByteBuffer.wrap(block).put(offset, val);
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            byte val = 0;
            val = ByteBuffer.wrap(block).get(offset);
            return (int) val;
        }

        @Override
        public int getNumberOfElements() {
            return INT8X16_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return INT8X16_BYTES_PER_ELEMENT;
        }
    }

    public abstract static class SIMDTypedUInt extends SIMDTypeInt {

        @Override
        public long getMin() {
            return 0;
        }

    }

    private static final int UINT32X4_BYTES_PER_ELEMENT = 4;
    private static final int UINT32X4_NUMBER_OF_ELEMENTS = 4;
    public static final SIMDTypeFactory<SIMDUint32x4> UINT32X4_FACTORY = new SIMDTypeFactory<>(UINT32X4_BYTES_PER_ELEMENT, UINT32X4_NUMBER_OF_ELEMENTS, "Uint32x4", new SIMDUint32x4());

    public static final class SIMDUint32x4 extends SIMDTypedUInt {
        private SIMDUint32x4() {
        }

        @Override
        public SIMDTypeFactory<SIMDUint32x4> getFactory() {
            return UINT32X4_FACTORY;
        }

        @Override
        public Object cast(Number o) {
            if (o instanceof Float) {
                return (int) JSRuntime.toUInt32((float) o);
            }
            return (int) JSRuntime.toUInt32(o);
        }

        @Override
        public long getMax() {
            return (long) (Math.pow(2, 32) - 1);
        }

        @Override
        public void serialize(byte[] block, int offset, Object n) {
            int val = (int) JSRuntime.toInteger(n);
            if (LittleEndian) {
                val = Integer.reverseBytes(val);
            }
            ByteBuffer.wrap(block).putInt(offset, val);
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            int val = 0;
            val = ByteBuffer.wrap(block).getInt(offset);
            if (LittleEndian) {
                val = Integer.reverseBytes(val);
            }
            return val;
        }

        @Override
        public int getNumberOfElements() {
            return UINT32X4_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return UINT32X4_BYTES_PER_ELEMENT;
        }
    }

    private static final int UINT16X8_BYTES_PER_ELEMENT = 2;
    private static final int UINT16X8_NUMBER_OF_ELEMENTS = 8;
    public static final SIMDTypeFactory<SIMDUint16x8> UINT16X8_FACTORY = new SIMDTypeFactory<>(UINT16X8_BYTES_PER_ELEMENT, UINT16X8_NUMBER_OF_ELEMENTS, "Uint16x8", new SIMDUint16x8());

    public static final class SIMDUint16x8 extends SIMDTypedUInt {
        private SIMDUint16x8() {
        }

        @Override
        public SIMDTypeFactory<SIMDUint16x8> getFactory() {
            return UINT16X8_FACTORY;
        }

        @Override
        public Object cast(Number o) {
            if (o instanceof Short) {
                return (int) (short) o;
            }
            if (o instanceof Float) {
                return (int) JSRuntime.toUInt16((float) o);
            }
            return JSRuntime.toUInt16(o);
        }

        @Override
        public long getMax() {
            return (long) (Math.pow(2, 16) - 1);
        }

        @Override
        public void serialize(byte[] block, int offset, Object n) {
            short val = (short) (int) n;
            if (LittleEndian) {
                val = Short.reverseBytes(val);
            }
            ByteBuffer.wrap(block).putShort(offset, val);
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            short val = 0;
            val = ByteBuffer.wrap(block).getShort(offset);
            if (LittleEndian) {
                val = Short.reverseBytes(val);
            }
            return (int) val;
        }

        @Override
        public int getNumberOfElements() {
            return UINT16X8_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return UINT16X8_BYTES_PER_ELEMENT;
        }
    }

    private static final int UINT8X16_BYTES_PER_ELEMENT = 1;
    private static final int UINT8X16_NUMBER_OF_ELEMENTS = 16;
    public static final SIMDTypeFactory<SIMDUint8x16> UINT8X16_FACTORY = new SIMDTypeFactory<>(UINT8X16_BYTES_PER_ELEMENT, UINT8X16_NUMBER_OF_ELEMENTS, "Uint8x16", new SIMDUint8x16());

    public static final class SIMDUint8x16 extends SIMDTypedUInt {
        private SIMDUint8x16() {
        }

        @Override
        public SIMDTypeFactory<SIMDUint8x16> getFactory() {
            return UINT8X16_FACTORY;
        }

        @Override
        public Object cast(Number o) {
            if (o instanceof Byte) {
                return (int) (byte) o;
            }
            if (o instanceof Float) {
                return (int) JSRuntime.toUInt8((float) o);
            }
            return JSRuntime.toUInt8(o);
        }

        @Override
        public long getMax() {
            return 255;
        }

        @Override
        public void serialize(byte[] block, int offset, Object n) {
            byte val = (byte) (int) n;
            ByteBuffer.wrap(block).put(offset, val);
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            byte val = 0;
            val = ByteBuffer.wrap(block).get(offset);
            return (int) val;
        }

        @Override
        public int getNumberOfElements() {
            return UINT8X16_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return UINT8X16_BYTES_PER_ELEMENT;
        }
    }

    public abstract static class SIMDTypedFloat extends SIMDType {

    }

    public abstract static class SIMDTypedBoolean extends SIMDType {
        @Override
        public void serialize(byte[] block, int offset, Object n) {
            throw new UnsupportedOperationException("Operation not defined for Boolean SIMD");
        }

        @Override
        public Object deserialize(byte[] block, int offset) {
            throw new UnsupportedOperationException("Operation not defined for Boolean SIMD");
        }

        @Override
        public Object cast(Number o) {
            return JSRuntime.toBoolean(o);
        }
    }

    private static final int BOOL32X4_BYTES_PER_ELEMENT = 4;
    private static final int BOOL32X4_NUMBER_OF_ELEMENTS = 4;
    public static final SIMDTypeFactory<SIMDBool32x4> BOOL32X4_FACTORY = new SIMDTypeFactory<>(BOOL32X4_BYTES_PER_ELEMENT, BOOL32X4_NUMBER_OF_ELEMENTS, "Bool32x4", new SIMDBool32x4());

    public static final class SIMDBool32x4 extends SIMDTypedBoolean {
        private SIMDBool32x4() {
        }

        @Override
        public SIMDTypeFactory<SIMDBool32x4> getFactory() {
            return BOOL32X4_FACTORY;
        }

        @Override
        public int getNumberOfElements() {
            return BOOL32X4_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return BOOL32X4_BYTES_PER_ELEMENT;
        }
    }

    private static final int BOOL16X8_BYTES_PER_ELEMENT = 2;
    private static final int BOOL16X8_NUMBER_OF_ELEMENTS = 8;
    public static final SIMDTypeFactory<SIMDBool16x8> BOOL16X8_FACTORY = new SIMDTypeFactory<>(BOOL16X8_BYTES_PER_ELEMENT, BOOL16X8_NUMBER_OF_ELEMENTS, "Bool16x8", new SIMDBool16x8());

    public static final class SIMDBool16x8 extends SIMDTypedBoolean {
        private SIMDBool16x8() {
        }

        @Override
        public SIMDTypeFactory<SIMDBool16x8> getFactory() {
            return BOOL16X8_FACTORY;
        }

        @Override
        public int getNumberOfElements() {
            return BOOL16X8_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return BOOL16X8_BYTES_PER_ELEMENT;
        }
    }

    private static final int BOOL8X16_BYTES_PER_ELEMENT = 1;
    private static final int BOOL8X16_NUMBER_OF_ELEMENTS = 16;
    public static final SIMDTypeFactory<SIMDBool8x16> BOOL8X16_FACTORY = new SIMDTypeFactory<>(BOOL8X16_BYTES_PER_ELEMENT, BOOL8X16_NUMBER_OF_ELEMENTS, "Bool8x16", new SIMDBool8x16());

    public static final class SIMDBool8x16 extends SIMDTypedBoolean {
        private SIMDBool8x16() {
        }

        @Override
        public SIMDTypeFactory<SIMDBool8x16> getFactory() {
            return BOOL8X16_FACTORY;
        }

        @Override
        public int getNumberOfElements() {
            return BOOL8X16_NUMBER_OF_ELEMENTS;
        }

        @Override
        public int getBytesPerElement() {
            return BOOL8X16_BYTES_PER_ELEMENT;
        }
    }

    public static final class SIMDTypeFactory<T extends SIMDType> implements PrototypeSupplier {
        private final int bytesPerElement;
        private final int numberOfElements;
        @CompilationFinal private int factoryIndex;
        private final String name;
        private final T simdType;

        SIMDTypeFactory(int bytesPerElement, int numberOfElements, String name, T simdType) {
            this.bytesPerElement = bytesPerElement;
            this.numberOfElements = numberOfElements;
            this.name = name;
            this.simdType = simdType;
        }

        public SIMDType createSimdType() {
            return simdType;
        }

        public int bytesPerElement() {
            return bytesPerElement;
        }

        public int numberOfElements() {
            return numberOfElements;
        }

        public int getFactoryIndex() {
            return factoryIndex;
        }

        public String getName() {
            return name;
        }

        @Override
        public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getSIMDTypeConstructor(this).getPrototype();
        }
    }

    public static final SIMDTypeFactory<? extends SIMDType>[] FACTORIES = createFactories();

    private static SIMDTypeFactory<? extends SIMDType>[] createFactories() {
        SIMDTypeFactory<?>[] factories = new SIMDTypeFactory<?>[]{FLOAT32X4_FACTORY, INT32X4_FACTORY, INT16X8_FACTORY, INT8X16_FACTORY, UINT32X4_FACTORY, UINT16X8_FACTORY, UINT8X16_FACTORY,
                        BOOL32X4_FACTORY, BOOL16X8_FACTORY, BOOL8X16_FACTORY};
        for (int i = 0; i < factories.length; i++) {
            factories[i].factoryIndex = i;
        }
        return factories;
    }
}

/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.test.interop.JavaScriptHostInteropTest.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Assume;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class InteropByteBufferTest {

    @Test
    public void testJavaBufferToTypedArray() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            context.getBindings("js").putMember("buffer", buffer);
            Value jsBuffer = context.eval(ID, "new Int8Array(new ArrayBuffer(buffer));");
            assertEquals(jsBuffer.getArraySize(), 3);
            assertEquals(jsBuffer.getArrayElement(0).asByte(), 1);
            assertEquals(jsBuffer.getArrayElement(1).asByte(), 2);
            assertEquals(jsBuffer.getArrayElement(2).asByte(), 3);
        }
    }

    @Test
    public void testDirectBufferToTypedArray() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(3);
        buffer.put(new byte[]{1, 2, 3});
        buffer.position(0);
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            context.getBindings("js").putMember("buffer", buffer);
            Value jsBuffer = context.eval(ID, "new Int8Array(new ArrayBuffer(buffer));");
            assertEquals(jsBuffer.getArraySize(), 3);
            assertEquals(jsBuffer.getArrayElement(0).asByte(), 1);
            assertEquals(jsBuffer.getArrayElement(1).asByte(), 2);
            assertEquals(jsBuffer.getArrayElement(2).asByte(), 3);
        }
    }

    @Test
    public void testJavaScriptCanWrite() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            context.getBindings("js").putMember("buffer", buffer);
            Value jsBuffer = context.eval(ID, "(new Int8Array(new ArrayBuffer(buffer))).map(x => 42);");
            assertEquals(jsBuffer.getArraySize(), 3);
            assertEquals(jsBuffer.getArrayElement(0).asByte(), 42);
            assertEquals(jsBuffer.getArrayElement(1).asByte(), 42);
            assertEquals(jsBuffer.getArrayElement(2).asByte(), 42);
        }
    }

    @Test
    public void testSameBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            context.getBindings("js").putMember("buffer", buffer);
            Value jsBuffer = context.eval(ID, "new Int8Array(new ArrayBuffer(buffer));");
            buffer.position(0);
            assertEquals(jsBuffer.getArraySize(), 3);
            assertEquals(jsBuffer.getArrayElement(0).asByte(), buffer.get());
            assertEquals(jsBuffer.getArrayElement(1).asByte(), buffer.get());
            assertEquals(jsBuffer.getArrayElement(2).asByte(), buffer.get());
        }
    }

    @Test
    public void testBufferAsArgument() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            Value fun = context.eval(ID, "(function fun(buff) {return new Int8Array(new ArrayBuffer(buff))})");
            Value jsBuffer = fun.execute(buffer);
            buffer.position(0);
            assertEquals(jsBuffer.getArraySize(), 3);
            assertEquals(jsBuffer.getArrayElement(0).asByte(), buffer.get());
            assertEquals(jsBuffer.getArrayElement(1).asByte(), buffer.get());
            assertEquals(jsBuffer.getArrayElement(2).asByte(), buffer.get());
        }
    }

    @Test
    public void testJavaInteropDirect() {
        testJavaInteropCommon(true);
    }

    @Test
    public void testJavaInteropHeap() {
        testJavaInteropCommon(false);
    }

    private static void testJavaInteropCommon(boolean direct) {
        try (Context cx = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).allowHostClassLookup(className -> true).build()) {
            Value buffer = cx.eval("js", "const ByteBuffer = Java.type('java.nio.ByteBuffer');" +
                            "const bb = ByteBuffer.allocate" + (direct ? "Direct" : "") + "(32);" +
                            "const ab = new ArrayBuffer(bb);" +
                            "let ia = new Int8Array(ab);" +
                            "ia[0] = 41;" +
                            "ia[1] = 42;" +
                            "ia[2] = 43;" +
                            "const ua = new Uint8Array(32);" +
                            "ua.set(ia);" +
                            "ua[3] = 212;" +
                            "ia.set(ua);" +
                            "ia = new Int32Array(ab, 4, 2);" +
                            "ia[0] = 45;" +
                            "ia[1] = 46;" +
                            "ia[2] = 47;" +
                            "ia = new BigInt64Array(ab, 16);" +
                            "ia[0] = 2n ** 63n - 43n;" +
                            "bb;");
            ByteBuffer jBuffer = buffer.as(ByteBuffer.class);
            assertEquals(direct, jBuffer.isDirect());
            assertEquals(jBuffer.get(0), 41);
            assertEquals(jBuffer.get(1), 42);
            assertEquals(jBuffer.get(2), 43);
            assertEquals(jBuffer.get(3), -44);
            jBuffer.order(ByteOrder.nativeOrder());
            assertEquals(jBuffer.getInt(4), 45);
            assertEquals(jBuffer.getInt(8), 46);
            assertEquals(jBuffer.getInt(12), 0);
            assertEquals(jBuffer.getLong(16), Long.MAX_VALUE - 42);
        }
    }

    @Test
    public void testArrayBufferInteropDirect() {
        testArrayBufferInteropCommon(true);
    }

    @Test
    public void testArrayBufferInteropHeap() {
        testArrayBufferInteropCommon(false);
    }

    private static void testArrayBufferInteropCommon(boolean direct) {
        try (Context cx = JSTest.newContextBuilder().option(JSContextOptions.DIRECT_BYTE_BUFFER_NAME, Boolean.toString(direct)).build()) {
            Value buffer = cx.eval("js", "" +
                            "const ab = new ArrayBuffer(25);" +
                            "let ia = new Int8Array(ab);" +
                            "ia[0] = 41;" +
                            "ia[1] = 42;" +
                            "ia[2] = 43;" +
                            "ia[3] = 44;" +
                            "ia = new Int32Array(ab, 4, 2);" +
                            "ia[0] = 45;" +
                            "ia[1] = 46;" +
                            "ia = new BigInt64Array(ab, 16, 1);" +
                            "ia[0] = 2n ** 63n - 43n;" +
                            "ab;");
            assertEquals(25, buffer.getBufferSize());
            assertEquals(buffer.readBufferByte(0), 41);
            assertEquals(buffer.readBufferByte(1), 42);
            assertEquals(buffer.readBufferByte(2), 43);
            assertEquals(buffer.readBufferByte(3), 44);
            assertEquals(buffer.readBufferShort(ByteOrder.LITTLE_ENDIAN, 0), 0x2a29);
            assertEquals(buffer.readBufferShort(ByteOrder.BIG_ENDIAN, 0), 0x292a);
            assertEquals(buffer.readBufferInt(ByteOrder.LITTLE_ENDIAN, 0), 0x2c2b2a29);
            assertEquals(buffer.readBufferInt(ByteOrder.BIG_ENDIAN, 0), 0x292a2b2c);

            assertEquals(buffer.readBufferFloat(ByteOrder.LITTLE_ENDIAN, 0), 2.4323965e-12f, 0f);
            assertEquals(buffer.readBufferFloat(ByteOrder.BIG_ENDIAN, 0), 3.778503e-14f, 0f);

            assertEquals(buffer.readBufferInt(ByteOrder.nativeOrder(), 4), 45);
            assertEquals(buffer.readBufferInt(ByteOrder.nativeOrder(), 8), 46);
            assertEquals(buffer.readBufferLong(ByteOrder.nativeOrder(), 16), Long.MAX_VALUE - 42);

            assertTrue(buffer.isBufferWritable());
            buffer.writeBufferLong(ByteOrder.nativeOrder(), 16, Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            assertEquals(buffer.readBufferDouble(ByteOrder.nativeOrder(), 16), Double.NEGATIVE_INFINITY, 0.0);
        }
    }

    @Test
    public void testDataViewBackedByHostByteBuffer() {
        testDataViewBackedByHostByteBuffer(false, true);
        testDataViewBackedByHostByteBuffer(true, true);
        testDataViewBackedByHostByteBuffer(false, false);
        testDataViewBackedByHostByteBuffer(true, false);
    }

    private static void testDataViewBackedByHostByteBuffer(boolean direct, boolean viaArrayBuffer) {
        ByteBuffer buffer = direct ? ByteBuffer.allocateDirect(32) : ByteBuffer.allocate(32);
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            context.getBindings(ID).putMember("bb", buffer);
            Value dataView = context.eval(ID, "" +
                            "const ab = " + (viaArrayBuffer ? "new ArrayBuffer(bb)" : "bb") + ";" +
                            "let dv = new DataView(ab);" +
                            "dv;");
            context.eval(ID, "" +
                            "dv.setInt8(0, 41);" +
                            "dv.setInt8(1, dv.getInt8(0) + 1);" +
                            "dv.setUint8(2, dv.getUint8(1) + 1);" +
                            "dv.setInt32(3, -44, true);");
            assertEquals(41, buffer.get(0));
            assertEquals(42, buffer.get(1));
            assertEquals(43, buffer.get(2));
            assertEquals(-44, buffer.get(3));
            assertEquals(-1, buffer.get(4));
            buffer.put(3, (byte) 44);
            assertEquals(0x2a29, dataView.invokeMember("getInt16", 0, true).asInt());
            assertEquals(0x292a, dataView.invokeMember("getInt16", 0, false).asInt());
            assertEquals(0x2c2b2a29, dataView.invokeMember("getInt32", 0, true).asInt());
            assertEquals(0x292a2b2c, dataView.invokeMember("getInt32", 0, false).asInt());
            assertEquals(2.4323965e-12f, dataView.invokeMember("getFloat32", 0, true).asFloat(), 0f);
            assertEquals(3.778503e-14f, dataView.invokeMember("getFloat32", 0, false).asFloat(), 0f);
            assertEquals(0x00ffffff2c2b2a29L, dataView.invokeMember("getBigInt64", 0, true).asLong());
            assertEquals(0x292a2b2cffffff00L, dataView.invokeMember("getBigInt64", 0, false).asLong());
            assertEquals(0x00ffffff2c2b2a29L, dataView.invokeMember("getBigUint64", 0, true).asLong());

            context.eval(ID, "new DataView(ab, 8).setFloat64(8, -Infinity, true);");
            assertEquals(Double.NEGATIVE_INFINITY, dataView.invokeMember("getFloat64", 16, true).asDouble(), 0.0);
            assertEquals(0xfff0000000000000L, dataView.invokeMember("getBigInt64", 16, true).asLong());

            context.eval(ID, "dv.setBigInt64(24, 1742123762643437888n, false);");
            assertEquals(4614256656552045848L, dataView.invokeMember("getBigInt64", 24, true).asLong());
            assertEquals(Math.PI, dataView.invokeMember("getFloat64", 24, true).asDouble(), 0.0);
        }
    }

    @Test
    public void testReadOnlyByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3}).asReadOnlyBuffer();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            context.getBindings("js").putMember("buffer", buffer);
            Value jsBuffer = context.eval(ID, "new Int8Array(new ArrayBuffer(buffer));");
            buffer.position(0);
            assertEquals(jsBuffer.getArraySize(), 3);
            assertEquals(jsBuffer.getArrayElement(0).asByte(), buffer.get());
            assertThrows(() -> jsBuffer.setArrayElement(0, 42), e -> assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError")));
        }
    }

    @Test
    public void testDetachedInteropArrayBuffer() {
        HostAccess hostAccess = HostAccess.newBuilder().allowBufferAccess(true).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).allowExperimentalOptions(true).option("js.debug-builtin", "true").option("js.v8-compat", "true").build()) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
            context.getBindings("js").putMember("buffer", buffer);
            Value byteLength = context.eval(ID, "" +
                            "var arrayBuffer = new ArrayBuffer(buffer);\n" +
                            "Debug.typedArrayDetachBuffer(arrayBuffer);\n" +
                            "arrayBuffer.byteLength;");
            assertEquals(0, byteLength.asInt());
        }
    }

    @Test
    public void testAtomics() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder().allowBufferAccess(true).build()).build()) {
            context.getBindings("js").putMember("buffer", buffer);
            context.eval(ID, "const buff = new Int8Array(buffer);");
            Value loaded = context.eval(ID, "Atomics.load(buff, 2);");
            assertEquals(3, loaded.asInt());
            Value stored = context.eval(ID, "Atomics.store(buff, 2, 42);");
            assertEquals(42, stored.asInt());
            Value added = context.eval(ID, "Atomics.add(buff, 2, 2);");
            assertEquals(42, added.asInt());
            loaded = context.eval(ID, "Atomics.load(buff, 2);");
            assertEquals(44, loaded.asInt());
            Value subbed = context.eval(ID, "Atomics.sub(buff, 2, 2);");
            assertEquals(44, subbed.asInt());
            loaded = context.eval(ID, "Atomics.load(buff, 2);");
            assertEquals(42, loaded.asInt());
            Value compexed = context.eval(ID, "Atomics.compareExchange(buff, 2, 42, 24);");
            assertEquals(42, compexed.asInt());
            Value exed = context.eval(ID, "Atomics.exchange(buff, 2, 42);");
            assertEquals(24, exed.asInt());
            loaded = context.eval(ID, "Atomics.load(buff, 2);");
            assertEquals(42, loaded.asInt());
            Value ored = context.eval(ID, "Atomics.or(buff, 4, 2);");
            assertEquals(5, ored.asInt());
            loaded = context.eval(ID, "Atomics.load(buff, 4);");
            assertEquals(7, loaded.asInt());
            Value xored = context.eval(ID, "Atomics.xor(buff, 4, 2);");
            assertEquals(7, xored.asInt());
            loaded = context.eval(ID, "Atomics.load(buff, 4);");
            assertEquals(5, loaded.asInt());
        }
    }

    @Test
    public void testLargeInteropBuffer() {
        HostAccess hostAccess = HostAccess.newBuilder().allowBufferAccess(true).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            int maxByteLength = JSContextOptions.MAX_TYPED_ARRAY_LENGTH.getDefaultValue();
            ByteBuffer buffer = allocate(maxByteLength);
            context.getBindings(ID).putMember("buffer", buffer);

            Value byteLength = context.eval(ID, "new Uint8Array(buffer).byteLength");
            assertEquals(maxByteLength, byteLength.asInt());

            if (maxByteLength % 8 != 0) {
                maxByteLength -= (maxByteLength % 8);
                buffer = allocate(maxByteLength);
                context.getBindings(ID).putMember("buffer", buffer);
            }
            byteLength = context.eval(ID, "new Float64Array(buffer).byteLength");
            assertEquals(maxByteLength, byteLength.asInt());

            int maxByteLengthPlusOne = JSContextOptions.MAX_TYPED_ARRAY_LENGTH.getDefaultValue() + 1;
            buffer = allocate(maxByteLengthPlusOne);
            context.getBindings(ID).putMember("buffer", buffer);
            Value rangeErrorThrown = context.eval(ID, "try { new Uint8Array(buffer); false; } catch (e) { e instanceof RangeError; }");
            assertTrue(rangeErrorThrown.asBoolean());
        }
    }

    private static ByteBuffer allocate(int capacity) {
        try {
            return ByteBuffer.allocate(capacity);
        } catch (OutOfMemoryError oom) {
            System.err.println("Warning: Unable to allocate ByteBuffer with capacity: " + capacity);
            Assume.assumeNoException(oom);
            return null;
        }
    }

}

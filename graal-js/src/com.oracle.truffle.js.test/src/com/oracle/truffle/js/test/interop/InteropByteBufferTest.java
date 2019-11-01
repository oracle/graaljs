/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.nio.ByteBuffer;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InteropByteBufferTest {

    @Test
    public void testJavaBufferToTypedArray() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        try (Context context = Context.create(ID)) {
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
        try (Context context = Context.create(ID)) {
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
        try (Context context = Context.create(ID)) {
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
        try (Context context = Context.create(ID)) {
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
        try (Context context = Context.create(ID)) {
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
        try (Context cx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowHostClassLookup(className -> true).build()) {
            Value buffer = cx.eval("js", "const ByteBuffer = Java.type('java.nio.ByteBuffer');" +
                            "const bb = ByteBuffer.allocateDirect(3);" +
                            "const ab = new ArrayBuffer(bb);" +
                            "const ia = new Int8Array(ab);" +
                            "ia[0] = 41;" +
                            "ia[1] = 42;" +
                            "ia[2] = 43;" +
                            "bb;");
            ByteBuffer jBuffer = buffer.as(ByteBuffer.class);
            assertTrue(jBuffer.isDirect());
            assertEquals(jBuffer.get(0), 41);
            assertEquals(jBuffer.get(1), 42);
            assertEquals(jBuffer.get(2), 43);
        }
    }

    @Test
    public void testJavaInteropHeap() {
        try (Context cx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowHostClassLookup(className -> true).build()) {
            Value buffer = cx.eval("js", "const ByteBuffer = Java.type('java.nio.ByteBuffer');" +
                            "const bb = ByteBuffer.allocate(3);" +
                            "const ab = new ArrayBuffer(bb);" +
                            "const ia = new Int8Array(ab);" +
                            "ia[0] = 41;" +
                            "ia[1] = 42;" +
                            "ia[2] = 43;" +
                            "bb;");
            ByteBuffer jBuffer = buffer.as(ByteBuffer.class);
            assertTrue(!jBuffer.isDirect());
            assertEquals(jBuffer.get(0), 41);
            assertEquals(jBuffer.get(1), 42);
            assertEquals(jBuffer.get(2), 43);
        }
    }
}

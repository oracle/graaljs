/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.js.api.ArrayBufferLibrary;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

@RunWith(Parameterized.class)
public class ArrayBufferLibraryTest extends JSTest {
    private static final String[] INVALID_EXPRESSIONS = new String[]{"42", "undefined", "new Object()"};

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return List.of(Boolean.TRUE, Boolean.FALSE);
    }

    @Parameter(value = 0) public boolean cached;

    private ArrayBufferLibrary getLibrary(Object receiver) {
        if (cached) {
            return TestHelper.adopt(ArrayBufferLibrary.getFactory().create(receiver));
        } else {
            return ArrayBufferLibrary.getUncached();
        }
    }

    @Test
    public void testIsArrayBufferHeap() {
        testIsArrayBuffer();
    }

    @Test
    public void testIsArrayBufferDirect() {
        testHelper = new TestHelper(newContextBuilder().option("js.direct-byte-buffer", "true"));
        testIsArrayBuffer();
    }

    private void testIsArrayBuffer() {
        testIsArrayBuffer("42", false);
        testIsArrayBuffer("undefined", false);
        testIsArrayBuffer("new ArrayBuffer()", true);
        testIsArrayBuffer("new SharedArrayBuffer()", true);
        testIsArrayBuffer("new Object()", false);
        testIsArrayBuffer("new Uint8Array()", false);
        testIsArrayBuffer("new Proxy(new ArrayBuffer(), {})", false);
    }

    private void testIsArrayBuffer(String expression, boolean expected) {
        Object value = testHelper.runNoPolyglot(expression);
        boolean actual = getLibrary(value).isArrayBuffer(value);
        assertSame(expected, actual);
    }

    @Test
    public void testGetByteLengthInvalid() {
        for (String expression : INVALID_EXPRESSIONS) {
            try {
                invokeGetByteLength(expression);
                fail();
            } catch (UnsupportedMessageException ex) {
                // expected
            }
        }
    }

    @Test
    public void testGetByteLengthHeap() {
        testGetByteLength();
    }

    @Test
    public void testGetByteLengthDirect() {
        testHelper = new TestHelper(newContextBuilder().option("js.direct-byte-buffer", "true"));
        testGetByteLength();
    }

    @Test
    public void testGetByteLengthShared() {
        testGetByteLength("new SharedArrayBuffer(8)", 8);
        testGetByteLength("var buf = new SharedArrayBuffer(8, {maxByteLength: 32}); buf.grow(16); buf", 16);
    }

    private void testGetByteLength() {
        testGetByteLength("new ArrayBuffer(8)", 8);
        testGetByteLength("var buf = new ArrayBuffer(8); buf.transfer(); buf", 0);
        testGetByteLength("var buf = new ArrayBuffer(8, {maxByteLength: 32}); buf.resize(16); buf", 16);
    }

    private void testGetByteLength(String expression, int expected) {
        try {
            Object actual = invokeGetByteLength(expression);
            assertEquals(expected, actual);
        } catch (UnsupportedMessageException umex) {
            fail();
        }
    }

    private Object invokeGetByteLength(String expression) throws UnsupportedMessageException {
        Object value = testHelper.runNoPolyglot(expression);
        return getLibrary(value).getByteLength(value);
    }

    @Test
    public void testGetContentsInvalid() {
        for (String expression : INVALID_EXPRESSIONS) {
            try {
                invokeGetContents(expression);
                fail();
            } catch (UnsupportedMessageException ex) {
                // expected
            }
        }
    }

    @Test
    public void testGetContentsHeap() {
        testGetContents();
    }

    @Test
    public void testGetContentsDirect() {
        testHelper = new TestHelper(newContextBuilder().option("js.direct-byte-buffer", "true"));
        testGetContents();
    }

    @Test
    public void testGetContents() {
        testGetContents("new Uint8Array([42,211]).buffer", new byte[]{42, (byte) 211});
        testGetContents("var buf = new ArrayBuffer(8); buf.transfer(); buf", null);
    }

    private void testGetContents(String expression, byte[] expected) {
        try {
            ByteBuffer actual = invokeGetContents(expression);
            if (expected == null) {
                assertNull(actual);
            } else {
                assertSameContent(actual, expected);
            }
        } catch (UnsupportedMessageException umex) {
            fail();
        }
    }

    private ByteBuffer invokeGetContents(String expression) throws UnsupportedMessageException {
        Object value = testHelper.runNoPolyglot(expression);
        return getLibrary(value).getContents(value);
    }

    private static void assertSameContent(ByteBuffer buffer, byte[] expected) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], buffer.get(i));
        }
    }

}

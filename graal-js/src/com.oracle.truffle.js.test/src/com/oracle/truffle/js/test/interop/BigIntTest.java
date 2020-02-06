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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class BigIntTest {

    @Test
    public void testByte() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-129n");
            assertFalse(value.fitsInByte());

            value = context.eval(JavaScriptLanguage.ID, "-128n");
            assertTrue(value.fitsInByte());
            assertEquals(-128, value.asByte());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInByte());
            assertEquals(0, value.asByte());

            value = context.eval(JavaScriptLanguage.ID, "127n");
            assertTrue(value.fitsInByte());
            assertEquals(127, value.asByte());

            value = context.eval(JavaScriptLanguage.ID, "128n");
            assertFalse(value.fitsInByte());
        }
    }

    @Test
    public void testShort() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-32769n");
            assertFalse(value.fitsInShort());

            value = context.eval(JavaScriptLanguage.ID, "-32768n");
            assertTrue(value.fitsInShort());
            assertEquals(-32768, value.asShort());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInShort());
            assertEquals(0, value.asShort());

            value = context.eval(JavaScriptLanguage.ID, "32767n");
            assertTrue(value.fitsInShort());
            assertEquals(32767, value.asShort());

            value = context.eval(JavaScriptLanguage.ID, "32768n");
            assertFalse(value.fitsInShort());
        }
    }

    @Test
    public void testInt() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-2147483649n");
            assertFalse(value.fitsInInt());

            value = context.eval(JavaScriptLanguage.ID, "-2147483648n");
            assertTrue(value.fitsInInt());
            assertEquals(-2147483648, value.asInt());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInInt());
            assertEquals(0, value.asInt());

            value = context.eval(JavaScriptLanguage.ID, "2147483647n");
            assertTrue(value.fitsInInt());
            assertEquals(2147483647, value.asInt());

            value = context.eval(JavaScriptLanguage.ID, "2147483648n");
            assertFalse(value.fitsInInt());
        }
    }

    @Test
    public void testLong() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-9223372036854775809n");
            assertFalse(value.fitsInLong());

            value = context.eval(JavaScriptLanguage.ID, "-9223372036854775808n");
            assertTrue(value.fitsInLong());
            assertEquals(-9223372036854775808L, value.asLong());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInLong());
            assertEquals(0, value.asLong());

            value = context.eval(JavaScriptLanguage.ID, "9223372036854775807n");
            assertTrue(value.fitsInLong());
            assertEquals(9223372036854775807L, value.asLong());

            value = context.eval(JavaScriptLanguage.ID, "9223372036854775808n");
            assertFalse(value.fitsInLong());
        }
    }

    @Test
    public void testFloat() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInFloat());
            assertTrue(0 == value.asFloat());

            value = context.eval(JavaScriptLanguage.ID, "2n**120n");
            assertTrue(value.fitsInFloat());
            assertTrue(Math.pow(2, 120) == value.asFloat());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**120n)");
            assertTrue(value.fitsInFloat());
            assertTrue(-Math.pow(2, 120) == value.asFloat());

            value = context.eval(JavaScriptLanguage.ID, "2n**160n");
            assertFalse(value.fitsInFloat());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**160n)");
            assertFalse(value.fitsInFloat());
        }
    }

    @Test
    public void testDouble() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInDouble());
            assertTrue(0 == value.asDouble());

            value = context.eval(JavaScriptLanguage.ID, "2n**1000n");
            assertTrue(value.fitsInDouble());
            assertTrue(Math.pow(2, 1000) == value.asDouble());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**1000n)");
            assertTrue(value.fitsInDouble());
            assertTrue(-Math.pow(2, 1000) == value.asDouble());

            value = context.eval(JavaScriptLanguage.ID, "2n**1100n");
            assertFalse(value.fitsInDouble());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**1100n)");
            assertFalse(value.fitsInDouble());
        }
    }

}

/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.builtins;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

import static org.junit.Assert.assertEquals;

public class AtomicsBuiltinsTest {

    @Test
    public void testNotify() {
        int agentCount = 10;
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.TEST262_MODE_NAME, "true").build()) {
            String code = "let agentCount = " + agentCount + ";\n" //
                            + "for (let i = 0; i < agentCount; i++) {\n" //
                            + "  $262.agent.start(`\n" //
                            + "    $262.agent.receiveBroadcast(function(sab) {\n" //
                            + "      const i32a = new Int32Array(sab);\n" //
                            + "      Atomics.wait(i32a, 0, 0);\n" //
                            + "      $262.agent.leaving();\n" //
                            + "    });\n" //
                            + "  `);\n" //
                            + "}\n" //
                            + "const sab = new SharedArrayBuffer(Int32Array.BYTES_PER_ELEMENT * 4);\n" //
                            + "$262.agent.broadcast(sab);\n" //
                            + "const i32a = new Int32Array(sab);\n" //
                            + "let count = 0;\n" //
                            + "while (count < agentCount) {\n" //
                            + "  count += Atomics.notify(i32a, 0);\n" //
                            + "}\n" //
                            + "count + Atomics.notify(i32a, 0);"; //
            Value result = context.eval(JavaScriptLanguage.ID, code);
            Assert.assertTrue(result.isNumber());
            // Atomics.notify() should report all agents eventually (but not more)
            assertEquals(agentCount, result.asInt());
        }
    }

    @Test
    public void testInfiniteWaitAsyncNotAwoken() {
        // Atomics.waitAsync() should not freeze when not awoken
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.TEST262_MODE_NAME, "true");
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            String code = "const sab = new SharedArrayBuffer(Int32Array.BYTES_PER_ELEMENT * 4);\n" //
                            + "const i32a = new Int32Array(sab);\n" //
                            + "const result = Atomics.waitAsync(i32a, 0, 0);" //
                            + "result;"; //
            Value result = context.eval(JavaScriptLanguage.ID, code);
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.hasMember("async"));
            Assert.assertTrue(result.getMember("async").asBoolean());
        }
    }

    @Test
    public void testWaitAsyncNotifyNonZeroOffsetViews() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.TEST262_MODE_NAME, "true");
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            String code = "const sab = new SharedArrayBuffer(32);\n" //
                            + "const arr1 = new Int32Array(sab, 4, 4);\n" //
                            + "const arr2 = new Int32Array(sab, 8, 3);\n" //
                            + "Atomics.waitAsync(arr1, 2, 0, 1000);\n" //
                            + "const result = Atomics.notify(arr2, 1, 1);" //
                            + "result;"; //
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertEquals(1, result.asInt());
        }
    }

    @Test
    public void testWaitAsyncSmallPositiveRealTimeout() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.TEST262_MODE_NAME, "true");
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            String code = "const sab = new SharedArrayBuffer(Int32Array.BYTES_PER_ELEMENT * 4);\n" //
                            + "const i32a = new Int32Array(sab);\n" //
                            + "const result = Atomics.waitAsync(i32a, 0, 0, 0.01).async;" //
                            + "result;"; //
            Value result = context.eval(JavaScriptLanguage.ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testToIntegerOrInfinity() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.TEST262_MODE_NAME, "true");
        try (Context context = builder.build()) {
            assertThrows(context, "Atomics.store(new Uint8Array(8), 0, 42n);");
            assertThrows(context, "Atomics.store(new Uint8Array(8), 0, Symbol());");
            assertEquals(0, context.eval(JavaScriptLanguage.ID, "Atomics.store(new Uint8Array(8), 0, NaN);").asInt());
            assertEquals(3, context.eval(JavaScriptLanguage.ID, "Atomics.store(new Uint8Array(8), 0, 3.14);").asInt());
            assertEquals(0, context.eval(JavaScriptLanguage.ID, "Atomics.store(new Uint8Array(8), 0, 0);").asInt());
            assertEquals(0, context.eval(JavaScriptLanguage.ID, "Atomics.store(new Uint8Array(8), 0, null);").asInt());
            assertEquals(0, context.eval(JavaScriptLanguage.ID, "Atomics.store(new Uint8Array(8), 0, undefined);").asInt());
            assertEquals(0, context.eval(JavaScriptLanguage.ID, "Atomics.store(new Uint8Array(8), 0, false);").asInt());
            assertEquals("Infinity", context.eval(JavaScriptLanguage.ID, "1/Atomics.store(new Uint8Array(8), 0, -0);").toString());
        }
    }

    private static void assertThrows(Context context, String code) {
        try {
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("TypeError exception expected");
        } catch (PolyglotException e) {
            Assert.assertTrue(e.isGuestException());
        }
    }

    @Test
    public void testCasOnBigValueNotShared() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.TEST262_MODE_NAME, "true");
        try (Context context = builder.build()) {
            assertBigValueWithArray(context, "Int8Array(8)");
            assertBigValueWithArray(context, "Uint8Array(8)");
            assertBigValueWithArray(context, "Int16Array(16)");
            assertBigValueWithArray(context, "Uint16Array(16)");
            assertBigValueWithArray(context, "Int32Array(32)");
            assertBigValueWithArray(context, "Uint32Array(32)");
        }
    }

    @Test
    public void testCasOnBigValueShared() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.TEST262_MODE_NAME, "true");
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "const sab = SharedArrayBuffer;");
            assertBigValueWithArray(context, "Int8Array(new sab(32))");
            assertBigValueWithArray(context, "Uint8Array(new sab(32))");
            assertBigValueWithArray(context, "Int16Array(new sab(32))");
            assertBigValueWithArray(context, "Uint16Array(new sab(32))");
            assertBigValueWithArray(context, "Int32Array(new sab(32))");
            assertBigValueWithArray(context, "Uint32Array(new sab(32))");
        }
    }

    private static void assertBigValueWithArray(Context context, String arrayType) {
        String code = "var array = new " + arrayType + ";" //
                        + "Atomics.compareExchange(array, 0, 2**100, 1);" //
                        + "array[0];"; //
        Value result = context.eval(JavaScriptLanguage.ID, code);
        assertEquals(1, result.asInt());
        code = "var array = new " + arrayType + ";" //
                        + "array[0] = 1;" //
                        + "Atomics.exchange(array, 0, 2**100);"; //
        result = context.eval(JavaScriptLanguage.ID, code);
        assertEquals(1, result.asInt());
        result = context.eval(JavaScriptLanguage.ID, "array[0];");
        assertEquals(0, result.asInt());
        code = "var array = new " + arrayType + ";" //
                        + "array[0] = 42;" //
                        + "var expected = 2**100;" //
                        + "var result = Atomics.store(array, 0, 2**100);" //
                        + "expected == result;"; //
        result = context.eval(JavaScriptLanguage.ID, code);
        Assert.assertTrue(result.asBoolean());

        result = context.eval(JavaScriptLanguage.ID, "array[0];");
        assertEquals(0, result.asInt());

        for (int exp = 11; exp < 32; exp++) {
            code = "var array = new " + arrayType + ";" //
                            + "array[0] = 2**" + exp + ";" //
                            + "Atomics.compareExchange(array, 0, 2**(" + exp + "+52)+2**" + exp + ", 1);" //
                            + "array[0];"; //

            result = context.eval(JavaScriptLanguage.ID, code);
            assertEquals(1, result.asInt());

            code = "var array = new " + arrayType + ";" //
                            + "array[0] = 2**" + exp + ";" //
                            + "var expected = array[0];" //
                            + "Atomics.exchange(array, 0, 2**(" + exp + "+52)+2**" + exp + ");" //
                            + "array[0] === expected;"; //

            result = context.eval(JavaScriptLanguage.ID, code);
            Assert.assertTrue(result.asBoolean());

            code = "var array = new " + arrayType + ";" //
                            + "array[0] = 2**" + exp + ";" //
                            + "var expected = array[0];" //
                            + "Atomics.store(array, 0, 2**(" + exp + "+52)+2**" + exp + ");" //
                            + "array[0] === expected;"; //

            result = context.eval(JavaScriptLanguage.ID, code);
            Assert.assertTrue(result.asBoolean());
        }

        code = "var array = new " + arrayType + ";" //
                        + "array[0] = 2**53;" //
                        + "var expected = array[0];" //
                        + "Atomics.store(new " + arrayType + ", 0, 2**53);" //
                        + "array[0] === expected;"; //
        result = context.eval(JavaScriptLanguage.ID, code);
        Assert.assertTrue(result.asBoolean());

        code = "var array = new " + arrayType + ";" //
                        + "var result = Atomics.store(new " + arrayType + ", 0, Infinity);" //
                        + "result === Infinity && array[0] === 0;"; //
        result = context.eval(JavaScriptLanguage.ID, code);
        Assert.assertTrue(result.asBoolean());

        code = "var array = new " + arrayType + ";" //
                        + "var result = Atomics.store(new " + arrayType + ", 0, -Infinity);" //
                        + "result === -Infinity && array[0] === 0;"; //
        result = context.eval(JavaScriptLanguage.ID, code);
        Assert.assertTrue(result.asBoolean());
    }

}

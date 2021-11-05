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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;

/**
 * Tests for the global builtin.
 */
public class GlobalBuiltinTest extends JSTest {

    @Test
    public void testParseInt() {
        // radix conversion, int input
        assertEquals(-9, testHelper.run("parseInt(-11,8)"));
        assertEquals(9, testHelper.run("parseInt(11,8)"));
        assertEquals(15, testHelper.run("parseInt(17,8)"));
        assertEquals(Double.NaN, testHelper.run("parseInt(18,8)"));

        // radix conversion, double input
        assertEquals(-9, testHelper.run("parseInt(-11.1415,8)"));
        assertEquals(9, testHelper.run("parseInt(11.1415,8)"));
        assertEquals(15, testHelper.run("parseInt(17.777,8)"));
        assertEquals(Double.NaN, testHelper.run("parseInt(18.777,8)"));

        // hex start
        assertEquals(255, testHelper.run("parseInt('0xFF')"));
        assertEquals(-255, testHelper.run("parseInt('-0xFF')"));
        assertEquals(536870911, testHelper.run("parseInt('0x00000001FFFFFFF')"));

        // empty string
        assertEquals(Double.NaN, testHelper.run("parseInt('')"));
        assertEquals(Double.NaN, testHelper.run("parseInt('-',10)"));

        // radix==0
        assertEquals(123, testHelper.run("parseInt('123',0)"));

        // illegal radix
        assertEquals(Double.NaN, testHelper.run("parseInt('123',1)"));
        assertEquals(Double.NaN, testHelper.run("parseInt('123',37)"));

        // int input, no radix
        assertEquals(123, testHelper.run("parseInt(123)"));
    }

    @Test
    public void testQuitInPromiseJob() {
        Engine engine = JSTest.newEngineBuilder().build();
        try (Context context = JSTest.newContextBuilder().engine(engine).option(JSContextOptions.SHELL_NAME, "true").build()) {
            // Schedule a promise job that exits while the promise job queue is not empty.
            context.eval(JavaScriptLanguage.ID, "var promise = Promise.resolve(42); promise.then(x => quit()); promise.then(x => x*x)");
            fail("Exception expected");
        } catch (PolyglotException pex) {
            assertTrue(pex.isExit());
        }
        // Create a new Context that shares JSContext with the original Context.
        // It should not attempt to execute the remaining promise jobs
        // (it would lead to IllegalStateException: The Context is already closed)
        try (Context context2 = JSTest.newContextBuilder().engine(engine).option(JSContextOptions.SHELL_NAME, "true").build()) {
            Value result = context2.eval(JavaScriptLanguage.ID, "6*7");
            assertTrue(result.isNumber());
            assertEquals(42, result.asInt());
        }
    }

    // GR-29654 an empty InputStream should not result in Java `null` being returned.
    @Test
    public void testReadlineEmpty() {
        InputStream instream = new ByteArrayInputStream(new byte[]{});

        // V8 compatible `readline()`, returns `undefined`
        try (Context ctx = Context.newBuilder("js").allowExperimentalOptions(true).option("js.shell", "true").in(instream).build()) {
            assertTrue(ctx.eval("js", "var res = readline(); res===undefined;").asBoolean());
        }

        // Nashorn compatible `readLine()`, returns (JS) `null`
        try (Context ctx = Context.newBuilder("js").allowExperimentalOptions(true).option("js.scripting", "true").in(instream).build()) {
            assertTrue(ctx.eval("js", "var res = readLine(); res===null;").asBoolean());
        }
    }

    @Test
    public void testPrintAppendsNewlineByDefault() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Context ctx = Context.newBuilder("js").out(baos).build()) {
            ctx.eval("js", "print('hello '); print('world');");
            assertEquals("hello \nworld\n", baos.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testPrintDoesNotAppendNewlineWithOptionPrintNoNewline() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Context ctx = Context.newBuilder("js").allowExperimentalOptions(true).option("js.print-no-newline", "true").out(baos).build()) {
            ctx.eval("js", "print('hello '); print('world');");
            assertEquals("hello world", baos.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testPrintErrAppendsNewlineByDefault() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Context ctx = Context.newBuilder("js").err(baos).build()) {
            ctx.eval("js", "printErr('hello '); printErr('world');");
            assertEquals("hello \nworld\n", baos.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testPrintErrDoesNotAppendNewlineWithOptionPrintNoNewline() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Context ctx = Context.newBuilder("js").allowExperimentalOptions(true).option("js.print-no-newline", "true").err(baos).build()) {
            ctx.eval("js", "printErr('hello '); printErr('world');");
            assertEquals("hello world", baos.toString(StandardCharsets.UTF_8));
        }
    }
}

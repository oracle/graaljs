/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Test;

import com.oracle.truffle.js.test.polyglot.MockFileSystem;

public class ModuleTest {

    @Test
    public void testImportFromVirtualFileSystem() {
        Map<String, String> modules = Map.of("other.mjs", "export default 42;");
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().fileSystem(new MockFileSystem(modules)).build()).build()) {
            Value result;
            result = context.eval(Source.newBuilder("js", "import answer from '/other.mjs'; answer;", "test1.mjs").buildLiteral());
            assertTrue(result.isNumber());
            assertEquals(42, result.asInt());
            result = context.eval(Source.newBuilder("js", "import answer from './other.mjs'; answer;", "test2.mjs").buildLiteral());
            assertTrue(result.isNumber());
            assertEquals(42, result.asInt());
            result = context.eval(Source.newBuilder("js", "import answer from 'other.mjs'; answer;", "test3.mjs").buildLiteral());
            assertTrue(result.isNumber());
            assertEquals(42, result.asInt());
        }
    }

    @Test
    public void testReadModuleImportBinding() {
        Map<String, String> modules = Map.of(
                        "module1.mjs", """
                                        import {a, getb, getc} from 'module2.mjs';
                                        export var b = 20;
                                        export let c = 20;
                                        var passed = a + b === 42 && a + getb() === 42 && a + getc() === 42;
                                        export var result = passed;
                                        """,
                        "module2.mjs", """
                                        import {b, c} from 'module1.mjs';
                                        export let a = 22;
                                        export function getb() { return b; }
                                        export function getc() { return c; }
                                        var passed = getb() === undefined && a === 22;
                                        try { getc(); passed = false; } catch (e) { passed &= e instanceof ReferenceError }
                                        if (!passed) throw new Error('fail');
                                        """);
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().fileSystem(new MockFileSystem(modules)).build()).build()) {
            Value result = context.eval(Source.newBuilder("js", "import { result } from 'module1.mjs'; result;", "test.mjs").buildLiteral());
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testLiteralSource() {
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.NONE).build()) {
            context.eval(Source.newBuilder("js", "export default 42;", "test1.mjs").buildLiteral());
        }
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            context.eval(Source.newBuilder("js", "export default 42;", "test1.mjs").buildLiteral());
        }
    }

    @Test
    public void testDynamicImportFromVirtualFileSystem() throws IOException {
        Map<String, String> modules = Map.of("other.mjs", "export const answer = 42;");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                        Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().fileSystem(new MockFileSystem(modules)).build()).out(out).build()) {
            context.eval(Source.newBuilder("js", "import('other.mjs').then(({answer}) => console.log(answer));", "test1.mjs").buildLiteral());
            assertEquals("42", out.toString().trim());
        }
    }

    @Test
    public void testLoadFromVirtualFileSystem() {
        Map<String, String> modules = Map.of("other.js", "var answer = 42;");
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().fileSystem(new MockFileSystem(modules)).build()).build()) {
            Value result = context.eval(Source.newBuilder("js", "load('other.js'); answer;", "test1.mjs").buildLiteral());
            assertTrue(result.isNumber());
            assertEquals(42, result.asInt());
        }
    }

    @Test
    public void testImportWithoutIOPermission() {
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.NONE).build()) {
            context.eval(Source.newBuilder("js", "export default 42;", "other.mjs").buildLiteral());
            Value result = context.eval(Source.newBuilder("js", "import answer from 'other.mjs'; answer;", "main.mjs").buildLiteral());
            assertTrue(result.isNumber());
            assertEquals(42, result.asInt());

            try {
                context.eval(Source.newBuilder("js", "import 'non-existent.mjs';", "error.mjs").buildLiteral());
                fail("should have thrown");
            } catch (PolyglotException expected) {
            }
        }
    }

    @Test
    public void testDynamicImportWithoutIOPermission() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                        Context context = JSTest.newContextBuilder().allowIO(IOAccess.NONE).out(out).build()) {
            context.eval(Source.newBuilder("js", "export const answer = 42;", "other.mjs").buildLiteral());
            context.eval(Source.newBuilder("js", "import('other.mjs').then(({answer}) => console.log(answer));", "test1.js").buildLiteral());
            context.eval(Source.newBuilder("js", "import('other.mjs').then(({answer}) => console.log(answer + 1));", "test1.mjs").buildLiteral());
            assertEquals("42\n43", out.toString().trim());
        }
    }

    @Test
    public void testNotCachedModuleSource() {
        Map<String, String> modules = Map.of("test1.mjs", "globalThis.evaluated++;");
        for (boolean cached : new boolean[]{false, true}) {
            try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().fileSystem(new MockFileSystem(modules)).build()).build()) {

                Source initSrc = Source.newBuilder("js", "globalThis.evaluated = 0;", "init.js").buildLiteral();
                Source test1Src = Source.newBuilder("js", new File("./test1.mjs")).content(modules.get("test1.mjs")).cached(cached).buildLiteral();
                Source test2Src = Source.newBuilder("js", "import './test1.mjs';", "test2.mjs").buildLiteral();
                String msg = "cached=" + cached;

                context.eval(initSrc);
                context.eval(test1Src);
                context.eval(test1Src);
                assertEquals(msg, 2, context.getBindings("js").getMember("evaluated").asInt());

                context.eval(test2Src);
                assertEquals(msg, cached ? 2 : 3, context.getBindings("js").getMember("evaluated").asInt());

                context.eval(test2Src);
                assertEquals(msg, cached ? 2 : 3, context.getBindings("js").getMember("evaluated").asInt());
            }
        }
    }
}

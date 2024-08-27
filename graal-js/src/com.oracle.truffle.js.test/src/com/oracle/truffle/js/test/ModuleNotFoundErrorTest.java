/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;

public class ModuleNotFoundErrorTest {

    @Test
    public void testModuleNotFoundError() {
        Source fileSource = Source.newBuilder(JavaScriptLanguage.ID, new File("main.mjs")).content("""
                        import "./not-found.mjs";
                        """).buildLiteral();
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build()).build()) {
            try {
                context.eval(fileSource);
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertThat(e.getMessage(), allOf(containsString("Cannot find module"), containsString("not-found.mjs")));
                assertThat(e.getMessage(), allOf(containsString("imported from"), containsString("main.mjs")));
            }
        }
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.NONE).build()) {
            try {
                context.eval(fileSource);
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertThat(e.getMessage(), containsString("not allowed"));
            }
        }
    }

    @Test
    public void testModuleNotFoundErrorFromLiteralSource() {
        Source literalSource = Source.newBuilder(JavaScriptLanguage.ID, """
                        import "./not-found.mjs";
                        """,
                        "main.mjs").buildLiteral();
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build()).build()) {
            try {
                context.eval(literalSource);
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertThat(e.getMessage(), allOf(containsString("Cannot find module"), containsString("not-found.mjs")));
                assertThat(e.getMessage(), allOf(containsString("imported from"), containsString("main.mjs")));
            }
        }
    }

    @Test
    public void testModuleNotFoundErrorFromDynamicImport() {
        String mainCode = """
                        await import("./not-found.mjs");
                        """;
        Source fileSource = Source.newBuilder(JavaScriptLanguage.ID, new File("main.mjs")).content(mainCode).buildLiteral();
        Source literalSource = Source.newBuilder(JavaScriptLanguage.ID, mainCode, "main.mjs").buildLiteral();
        for (Source mainSource : List.of(fileSource, literalSource)) {
            try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build()).build()) {
                try {
                    context.eval(mainSource);
                    fail("should have thrown");
                } catch (PolyglotException e) {
                    assertTrue(e.isGuestException());
                    assertThat(e.getMessage(), allOf(containsString("Cannot find module"), containsString("not-found.mjs")));
                    assertThat(e.getMessage(), allOf(containsString("imported from"), containsString("main.mjs")));
                }
            }

            try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.NONE).build()) {
                try {
                    context.eval(mainSource);
                    fail("should have thrown");
                } catch (PolyglotException e) {
                    assertTrue(e.isGuestException());
                    assertThat(e.getMessage(), containsString("not allowed"));
                }
            }
        }
    }

    @Test
    public void testAsyncStackTraceOfModuleNotFoundErrorFromDynamicImport() {
        String mainCode = """
                        async function caller() {
                            await Promise.resolve().then(() => import("./not-found.mjs"));
                        }
                        try {
                            await caller();
                        } catch (e) {
                            console.log(e.stack);
                        }
                        """;
        Source fileSource = Source.newBuilder(JavaScriptLanguage.ID, new File("main.mjs")).content(mainCode).buildLiteral();
        Source literalSource = Source.newBuilder(JavaScriptLanguage.ID, mainCode, "main.mjs").buildLiteral();
        for (Source mainSource : List.of(fileSource, literalSource)) {
            ByteArrayOutputStream outs = new ByteArrayOutputStream();
            try (Context context = JSTest.newContextBuilder().out(outs).allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build()).build()) {
                context.eval(mainSource);

                String output = outs.toString(StandardCharsets.UTF_8);
                assertThat(output, allOf(containsString("Cannot find module"), containsString("not-found.mjs")));
                assertThat(output, allOf(containsString("imported from"), containsString("main.mjs")));
                assertThat(output, allOf(containsString("at async caller"), containsString("main.mjs:2")));
            }
        }
    }

    @Test
    public void testSyncStackTraceOfModuleNotFoundErrorFromDynamicImport() {
        String mainCode = """
                        async function caller() {
                            await import("./not-found.mjs");
                        }
                        try {
                            await caller();
                        } catch (e) {
                            console.log(e.stack);
                        }
                        """;
        Source fileSource = Source.newBuilder(JavaScriptLanguage.ID, new File("main.mjs")).content(mainCode).buildLiteral();
        Source literalSource = Source.newBuilder(JavaScriptLanguage.ID, mainCode, "main.mjs").buildLiteral();
        for (Source mainSource : List.of(fileSource, literalSource)) {
            ByteArrayOutputStream outs = new ByteArrayOutputStream();
            try (Context context = JSTest.newContextBuilder().out(outs).allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build()).build()) {
                context.eval(mainSource);

                String output = outs.toString(StandardCharsets.UTF_8);
                assertThat(output, allOf(containsString("Cannot find module"), containsString("not-found.mjs")));
                assertThat(output, allOf(containsString("imported from"), containsString("main.mjs")));
                assertThat(output, allOf(containsString("at caller"), containsString("main.mjs:2")));
            }
        }
    }
}

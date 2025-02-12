/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.regress;

import static com.oracle.truffle.js.runtime.JSContextOptions.UNHANDLED_REJECTIONS_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.MockFileSystem;

@RunWith(Parameterized.class)
public class GR61889 {

    @Parameters(name = "unhandled-rejections={0}")
    public static Iterable<String> data() {
        return List.of("none", "throw", "warn");
    }

    @Parameter(0) public String unhandledRejections;

    /**
     * Do not treat rejected module evaluation promises as unhandled rejections.
     */
    @Test
    public void testRejectedModulePromiseIsHandled() throws IOException {
        var fs = new MockFileSystem(Map.of(
                        "eval-throws-error.mjs", """
                                        export function foo() {return 42}
                                        throw new Error("Thrown from within the library");
                                        """,
                        "eval-throws-non-error.mjs", """
                                        export function foo() {return 42}
                                        throw "Thrown from within the library";
                                        """));

        try (var out = new ByteArrayOutputStream();
                        Context c = JSTest.newContextBuilder().//
                                        option(UNHANDLED_REJECTIONS_NAME, unhandledRejections).//
                                        allowIO(IOAccess.newBuilder().fileSystem(fs).build()).//
                                        out(out).err(out).build()) {
            // Error thrown from another synchronously executed module.
            for (String moduleCode : List.of(
                            "import * as throws from './eval-throws-error.mjs';",
                            "import * as throws from './eval-throws-non-error.mjs';")) {
                try {
                    c.eval(Source.newBuilder("js", moduleCode, "main.mjs").buildLiteral());
                    fail("should have thrown");
                } catch (PolyglotException e) {
                    assertFalse(e.getMessage(), e.isSyntaxError());
                    assertTrue(e.getMessage(), e.isGuestException());
                    assertThat(e.getMessage(), containsString("Thrown from within the library"));
                    assertThat(e.getMessage(), not(containsString("Unhandled promise rejection")));
                }
            }
            assertThat("No unhandled rejection warnings", out.toString(), equalTo(""));
        }

        try (var out = new ByteArrayOutputStream();
                        Context c = JSTest.newContextBuilder().//
                                        option(UNHANDLED_REJECTIONS_NAME, unhandledRejections).//
                                        allowIO(IOAccess.newBuilder().fileSystem(fs).build()).//
                                        out(out).err(out).build()) {
            // Error object thrown from the main module.
            for (String moduleCode : List.of("""
                            export function foo() {return 42}
                            throw new Error("Thrown from within main module");
                            """, """
                            export function foo() {return 42}
                            throw "Thrown from within main module";
                            """)) {
                try {
                    c.eval(Source.newBuilder("js", moduleCode, "main.mjs").buildLiteral());
                    fail("should have thrown");
                } catch (PolyglotException e) {
                    assertFalse(e.getMessage(), e.isSyntaxError());
                    assertTrue(e.getMessage(), e.isGuestException());
                    assertThat(e.getMessage(), containsString("Thrown from within main module"));
                    assertThat(e.getMessage(), not(containsString("Unhandled promise rejection")));
                }
                assertThat("No unhandled rejection warnings", out.toString(), equalTo(""));
            }
        }
    }

    /**
     * Do not treat rejected module loading promises as unhandled rejections.
     */
    @Test
    public void testRejectedLoadRequestedModulesPromiseIsHandled() throws IOException {
        var fs = new MockFileSystem(Map.of());
        try (var out = new ByteArrayOutputStream();
                        Context c = JSTest.newContextBuilder().//
                                        option(UNHANDLED_REJECTIONS_NAME, unhandledRejections).//
                                        allowIO(IOAccess.newBuilder().fileSystem(fs).build()).//
                                        out(out).err(out).build()) {
            try {
                c.eval(Source.newBuilder("js", """
                                import * as throws from "./does_not_exist.mjs";
                                """, "./throws-during-load.mjs").buildLiteral());
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertFalse(e.getMessage(), e.isSyntaxError());
                assertTrue(e.getMessage(), e.isGuestException());
                assertThat(e.getMessage(), containsString("Cannot find module"));
                assertThat(e.getMessage(), not(containsString("Unhandled promise rejection")));
            }
            assertThat(out.toString(), equalTo(""));
        }
    }
}

/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.test.JSTest;

@RunWith(JUnit4.class)
public class StringFunctionBuiltinsTest {
    private static final String SOURCE_NAME = StringFunctionBuiltinsTest.class.getSimpleName() + ".js";

    private static Context createContext() {
        return JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING).build();
    }

    @Test
    public void testDedentThrowErrorIfNoOpeningLine() {
        String sourceText = "String.dedent`value`";
        try (Context context = createContext()) {
            JSTest.assertThrows(() -> {
                context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            }, JSErrorType.TypeError);
        }
    }

    @Test
    public void testDedentThrowErrorIfNoClosingLine() {
        String sourceText = "String.dedent`\nvalue`";
        try (Context context = createContext()) {
            JSTest.assertThrows(() -> {
                context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            }, JSErrorType.TypeError);
        }
    }

    @Test
    public void testDedentThrowErrorIfNotObject() {
        String sourceText = "String.dedent(42)";
        try (Context context = createContext()) {
            JSTest.assertThrows(() -> {
                context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            }, JSErrorType.TypeError);
        }
    }

    @Test
    public void testDedentThrowErrorEmptyArray() {
        String sourceText = "String.dedent({raw: []})";
        try (Context context = createContext()) {
            JSTest.assertThrows(() -> {
                context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            }, JSErrorType.TypeError);
        }
    }

    @Test
    public void testDedentThrowErrorNonStringElement() {
        String sourceText = "String.dedent({raw: [42]})";
        try (Context context = createContext()) {
            JSTest.assertThrows(() -> {
                context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            }, JSErrorType.TypeError);
        }
    }

    @Test
    public void testDedentSingleLineWithTab() {
        String sourceText = "String.dedent`\n\tvalue\n`;";
        try (Context context = createContext()) {
            Value result = context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            assertEquals(result.asString(), "value");
        }
    }

    @Test
    public void testDedentWithSubstitutions() {
        String sourceText = """
                        String.dedent`
                                                    create table student(
                                                      key: \\t${1+2},\\r
                                                      name: ${"John"}\\r
                                                    )

                                                    create table student(
                                                      key: ${8},
                                                      name: ${"Doe"}
                                                    )

                        `
                        """;
        String expected = """
                        create table student(
                          key: \t3,\r
                          name: John\r
                        )

                        create table student(
                          key: 8,
                          name: Doe
                        )
                        """;
        try (Context context = createContext()) {
            Value result = context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            assertEquals(expected, result.asString());
        }
    }

    @Test
    public void testDedentWithCallback() {
        String sourceText = """
                        String.dedent(String.raw)`
                                                    create table student(
                                                      key: \\t${1+2},\\r
                                                      name: ${"John"}\\r
                                                    )

                                                    create table student(
                                                      key: ${8},
                                                      name: ${"Doe"}
                                                    )

                        `""";
        String expected = """
                        create table student(
                          key: \\t3,\\r
                          name: John\\r
                        )

                        create table student(
                          key: 8,
                          name: Doe
                        )
                        """;
        try (Context context = createContext()) {
            Value result = context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            assertEquals(expected, result.asString());
        }
    }

    @Test
    public void testDedentIdentity() {
        String sourceText = """
                        let first;
                        function interceptRaw(template, ...substitutions) {
                            if (!first) {
                                first = template;
                            } else if (template !== first) {
                                throw new Error(`expected same identity`);
                            }
                            if (template.length !== 5 || substitutions.length !== 4) {
                                throw new Error(`unexpected length: ${template.length !== 5 || substitutions.length !== 4}`);
                            }
                            return String.raw(template, ...substitutions);
                        }

                        for (let i = 0; i < 2; i++) {
                            String.dedent(interceptRaw)`
                                                        create table student(
                                                          key: \\t${1+2},\\r
                                                          name: ${"John"}\\r
                                                        )

                                                        create table student(
                                                          key: ${8},
                                                          name: ${"Doe"}
                                                        )
                            `;
                        }
                        """;
        String expected = """
                        create table student(
                          key: \\t3,\\r
                          name: John\\r
                        )

                        create table student(
                          key: 8,
                          name: Doe
                        )""";
        try (Context context = createContext()) {
            Value result = context.eval(Source.newBuilder(ID, sourceText, SOURCE_NAME).buildLiteral());
            assertEquals(expected, result.asString());
        }
    }
}

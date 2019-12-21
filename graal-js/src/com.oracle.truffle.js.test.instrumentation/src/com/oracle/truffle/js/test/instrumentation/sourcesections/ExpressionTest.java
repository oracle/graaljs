/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.instrumentation.sourcesections;

import org.junit.Test;

public class ExpressionTest extends SourceSectionInstrumentationTest {

    @Test
    public void basicVarExpressions() {
        evalExpressions("a = 3; b = 2; a + b;");
        assertSourceSections(new String[]{
                        "3",
                        "a = 3",
                        "2",
                        "b = 2",
                        "a",
                        "b",
                        "a + b",
        });
    }

    @Test
    public void callExpression() {
        evalExpressions("String('bla');");

        assertSourceSections(new String[]{
                        "String",
                        "'bla'",
                        "String('bla')",
        });
    }

    @Test
    public void newExpression() {
        evalExpressions("new String('bla');");

        assertSourceSections(new String[]{
                        "String",
                        "'bla'",
                        "new String('bla')",
        });
    }

    @Test
    public void callExpressionComment() {
        evalExpressions("String('bla'); // this is a comment");

        assertSourceSections(new String[]{
                        "String",
                        "'bla'",
                        "String('bla')",
        });
    }

    @Test
    public void callExpressionNewLine() {
        evalExpressions("String('bla'   )   " + '\n' +
                        ", 42;");

        assertSourceSections(new String[]{
                        "String",
                        "'bla'",
                        "String('bla'   )",
                        "42",
                        "String('bla'   )   " + '\n' + ", 42"
        });
    }

    @Test
    public void callExpressionCommentNewLine() {
        evalExpressions("String('bla'    )   // this is a comment   " + '\n' +
                        ", 42;");

        assertSourceSections(new String[]{
                        "String",
                        "'bla'",
                        "String('bla'    )",
                        "42",
                        "String('bla'    )   // this is a comment   " + '\n' + ", 42"
        });
    }

    @Test
    public void strLitCallExpression() {
        evalExpressions("'bar'.replace(/bar/, 'baz');");

        assertSourceSections(new String[]{
                        "'bar'",
                        "'bar'.replace",
                        "/bar/",
                        "'baz'",
                        "'bar'.replace(/bar/, 'baz')",
        });
    }

    @Test
    public void chainedCallExpressions() {
        evalExpressions("'foobar'.replace(/bar/, 'baz').replace(/foo/, 'boo');");

        assertSourceSections(new String[]{
                        "'foobar'",
                        "'foobar'.replace",
                        "/bar/",
                        "'baz'",
                        "'foobar'.replace(/bar/, 'baz')",
                        "'foobar'.replace(/bar/, 'baz').replace",
                        "/foo/",
                        "'boo'",
                        "'foobar'.replace(/bar/, 'baz').replace(/foo/, 'boo')",
        });
    }

    @Test
    public void expressionsApplyCall() {
        evalExpressions("function foo(a) {" +
                        "    return a;" +
                        "};" +
                        "function bar() {" +
                        "    return foo.apply(undefined, arguments);" +
                        "};" +
                        "console.log(bar(1));");
        assertSourceSections(new String[]{
                        "console",
                        "console.log",
                        "bar",
                        "1",
                        "foo",
                        "foo.apply",
                        "undefined",
                        "arguments",
                        "a",
                        "foo.apply(undefined, arguments)",
                        "bar(1)",
                        "console.log(bar(1))",
        });
    }
}

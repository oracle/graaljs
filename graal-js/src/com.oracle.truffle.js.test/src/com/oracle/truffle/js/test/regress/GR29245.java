/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;

public class GR29245 {

    private static final String EMPTY = "ConstantEmptyArray";
    private static final String CONT = "ContiguousIntArray";
    private static final String SPARSE = "SparseArray";
    private static final String ZERO_BASED = "ZeroBasedIntArray";

    @Test
    public void testCanVirtualize() {
        // Keeping a ContiguousIntArray hints that we are not copying.
        expectTransitions("var arr = new Array();",
                        EMPTY,
                        "for (var i=0; i<1000; i++) arr.push(i);",
                        ZERO_BASED,
                        "arr.shift();",
                        CONT,
                        "arr.shift();",
                        CONT);
    }

    @Test
    public void testBigIndex01() {
        expectTransitions("var arr = new Array();",
                        EMPTY,
                        "arr[UINT32_MAX - 1] = 42;",
                        SPARSE,
                        "arr[UINT32_MAX] = 42;",
                        SPARSE);
    }

    @Test
    public void testBigIndex02() {
        expectTransitions("var arr = new Array(INT_MAX_VALUE);",
                        EMPTY,
                        "arr[INT_MAX_VALUE - 1] = 42;",
                        CONT,
                        "arr[INT_MAX_VALUE] = 42;",
                        SPARSE);
    }

    @Test
    public void testBigIndex03() {
        expectTransitions("var arr = new Array(UINT32_MAX);",
                        SPARSE,
                        "arr[0] = 42;",
                        SPARSE,
                        "arr[UINT32_MAX - 1] = 42;",
                        SPARSE,
                        "arr[UINT32_MAX] = 42;",
                        SPARSE);
    }

    @Test
    public void testBigIndex04() {
        expectTransitions("var arr = new Array();",
                        EMPTY,
                        "arr[0] = 42;",
                        ZERO_BASED,
                        "arr[UINT32_MAX - 1] = 42;",
                        SPARSE,
                        "arr[UINT32_MAX] = 42;",
                        SPARSE);
    }

    @Test
    public void testBigIndex05() {
        expectTransitions("var arr = new Array();",
                        EMPTY,
                        "arr[UINT32_MAX + 1] = 42;",
                        EMPTY,
                        "arr[UINT32_MAX] = 42;",
                        EMPTY);
    }

    @Test
    public void testBigIndex06() {
        expectTransitions("var arr = new Array();",
                        EMPTY,
                        "arr[0] = 42;",
                        ZERO_BASED,
                        "arr[UINT32_MAX -1] = 42;",
                        SPARSE,
                        "arr[UINT32_MAX + 1] = 42;",
                        SPARSE);
    }

    private static void expectTransitions(String... testSteps) {
        if (testSteps.length % 2 != 0) {
            throw new AssertionError("Malformed test");
        }
        try (Context context = JSTest.newContextBuilder().option("js.debug-builtin", "true").build()) {
            context.eval(ID, "const INT_MAX_VALUE = 2147483647; const UINT32_MAX = 4294967295;");
            for (int i = 0; i < testSteps.length; i += 2) {
                String action = testSteps[i];
                String expected = testSteps[i + 1];
                context.eval(ID, action);
                Value type = context.eval(ID, "Debug.arraytype(arr);");
                assertEquals(expected, type.asString());
            }
        }
    }
}

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
package com.oracle.truffle.js.test.simd;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;

/**
 * Those tests are not a comprehensive testsuite for SIMD.js code. This testsuite is (or was) part
 * of Test262. Purpose of those tests is to ensure basic test coverage of the functionality.
 */
public class SIMDTest {
    private static void testIntl(String sourceText) {
        try (Context context = Context.newBuilder(JavaScriptLanguage.ID).allowExperimentalOptions(true).option(JSContextOptions.SIMDJS_NAME, "true").build()) {
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, "simd-test").buildLiteral());
            Assert.assertTrue(result.asBoolean());
        }
    }

    private static void testArithmeticOp(String type, String size, String inputA, String inputB, String op, String expected) {
        String cType = type + size;
        String bType = "Bool" + size;
        testIntl("var result = SIMD." + cType + "." + op + "(SIMD." + cType + "(" + inputA + "), SIMD." + cType + "(" + inputB + ")); SIMD." + bType + ".allTrue(SIMD." + cType +
                        ".equal(result, SIMD." + cType + "(" + expected + ")));");
    }

    @Test
    public void testSimdFloat32() {
        testArithmeticOp("Float", "32x4", "1.0, 2.0, 3.0, 4.0", "5.0, 6.0, 7.0, 8.0", "add", "6.0, 8.0, 10.0, 12.0");
        testArithmeticOp("Float", "32x4", "1.0, 2.0, 3.0, 4.0", "5.0, 6.0, 7.0, 8.0", "sub", "-4.0, -4.0, -4.0, -4.0");
        testArithmeticOp("Float", "32x4", "1.0, 2.0, 3.0, 4.0", "5.0, 6.0, 7.0, 8.0", "mul", "5.0, 12.0, 21.0, 32.0");
        testArithmeticOp("Float", "32x4", "30.0, 30.0, 30.0, 30.0", "1.0, 2.0, 3.0, 5.0", "div", "30.0, 15.0, 10.0, 6.0");
    }

    @Test
    public void testSimdInt32x4() {
        testArithmeticOp("Int", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "add", "6, 8, 10, 12");
        testArithmeticOp("Int", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "sub", "-4, -4, -4, -4");
        testArithmeticOp("Int", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "mul", "5, 12, 21, 32");
        // testArithmeticOp("Int", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "div", "6, 8, 10, 12");
    }

    @Test
    public void testSimdInt16x8() {
        testArithmeticOp("Int", "16x8", "1, 2, 3, 4, 5, 6, 7, 8", "5, 6, 7, 8, 9, 10, 11, 12", "add", "6, 8, 10, 12, 14, 16, 18, 20");
        testArithmeticOp("Int", "16x8", "1, 2, 3, 4, 5, 6, 7, 8", "5, 6, 7, 8, 9, 10, 11, 12", "sub", "-4, -4, -4, -4, -4, -4, -4, -4");
        testArithmeticOp("Int", "16x8", "1, 2, 3, 4, 5, 6, 7, 8", "2, 2, 2, 2, 2, 2, 2, 2", "mul", "2, 4, 6, 8, 10, 12, 14, 16");
        // testArithmeticOp("Int", "16x8", "1, 2, 3, 4, 5, 6, 7, 8", "5, 6, 7, 8, 9, 10, 11, 12",
        // "div", "6, 8, 10, 12, 14, 16, 18, 20");
    }

    @Test
    public void testSimdInt8x16() {
        testArithmeticOp("Int", "8x16", "1, 2, 3, 4, 5, 6, 7, 8, 8, 7, 6, 5, 4, 3, 2, 1", "5, 6, 7, 8, 9, 10, 11, 12, 5, 6, 7, 8, 9, 10, 11, 12", "add",
                        "6, 8, 10, 12, 14, 16, 18, 20, 13, 13, 13, 13, 13, 13, 13, 13");
        testArithmeticOp("Int", "8x16", "1, 2, 3, 4, 5, 6, 7, 8, 8, 7, 6, 5, 4, 3, 2, 1", "5, 6, 7, 8, 9, 10, 11, 12, 5, 6, 7, 8, 9, 10, 11, 12", "sub",
                        "-4, -4, -4, -4, -4, -4, -4, -4, 3, 1, -1, -3, -5, -7, -9, -11");
        testArithmeticOp("Int", "8x16", "1, 2, 3, 4, 5, 6, 7, 8, 8, 7, 6, 5, 4, 3, 2, 1", "2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2", "mul",
                        "2, 4, 6, 8, 10, 12, 14, 16, 16, 14, 12, 10, 8, 6, 4, 2");
    }

    @Test
    public void testSimdUInt32x4() {
        testArithmeticOp("Uint", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "add", "6, 8, 10, 12");
        testArithmeticOp("Uint", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "sub", "-4, -4, -4, -4");
        testArithmeticOp("Uint", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "mul", "5, 12, 21, 32");
        // testArithmeticOp("Uint", "32x4", "1, 2, 3, 4", "5, 6, 7, 8", "div", "6, 8, 10, 12");
    }

    @Test
    public void testSimdUInt16x8() {
        testArithmeticOp("Uint", "16x8", "1, 2, 3, 4, 5, 6, 7, 8", "5, 6, 7, 8, 9, 10, 11, 12",
                        "add", "6, 8, 10, 12, 14, 16, 18, 20");
        // this tests failing because of missing conversion
        // testArithmeticOp("Uint", "16x8", "1, 2, 3, 4, 5, 6, 7, 8", "5, 6, 7, 8, 9, 10, 11, 12",
        // "sub", "-4, -4, -4, -4, -4, -4, -4, -4");
        testArithmeticOp("Uint", "16x8", "5, 6, 7, 8, 9, 10, 11, 12", "1, 2, 3, 4, 5, 6, 7, 8", "sub", "4, 4, 4, 4, 4, 4, 4, 4");
        testArithmeticOp("Uint", "16x8", "1, 2, 3, 4, 5, 6, 7, 8", "2, 2, 2, 2, 2, 2, 2, 2",
                        "mul", "2, 4, 6, 8, 10, 12, 14, 16");
    }

    @Ignore
    @Test
    public void failingTest1() {
        testIntl("SIMD.Uint16x8.equal();");
    }

    @Ignore
    @Test
    public void failingTest2() {
        testIntl("SIMD.Float32x4.add(0,0);");
    }

}

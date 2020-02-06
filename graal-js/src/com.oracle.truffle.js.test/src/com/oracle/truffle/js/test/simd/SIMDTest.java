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
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

/**
 * Those tests are not a comprehensive testsuite for SIMD.js code. This testsuite is (or was) part
 * of Test262. Purpose of those tests is to ensure basic test coverage of the functionality.
 */
public class SIMDTest {
    private static void test(String sourceText) {
        Assert.assertTrue(testIntl(sourceText));
    }

    private static boolean testIntl(String sourceText) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.SIMDJS_NAME, "true").build()) {
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, "simd-test").buildLiteral());
            return result.asBoolean();
        }
    }

    private static void testIntlFailed(String sourceText, String expectedMessage) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.SIMDJS_NAME, "true").build()) {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, "simd-test").buildLiteral());
        } catch (PolyglotException ex) {
            Assert.assertTrue(ex.getMessage().contains(expectedMessage));
            return;
        }
        Assert.fail();
    }

    private static void testArithmeticOp(String type, String size, String inputA, String inputB, String op, String expected) {
        String cType = type + size;
        String bType = "Bool" + size;
        test("var result = SIMD." + cType + "." + op + "(SIMD." + cType + "(" + inputA + "), SIMD." + cType + "(" + inputB + ")); SIMD." + bType + ".allTrue(SIMD." + cType +
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

    @Test
    public void testSimdCheck() {
        testIntlFailed("SIMD.Int32x4.check({})", "SIMD type expected");
        test("var a = SIMD.Int32x4(1, 2, 3, 4); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(a, SIMD.Int32x4.check(a)));");
    }

    @Test
    public void testSimdSplat() {
        test("var a = SIMD.Int32x4(42, 42, 42, 42); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(a, SIMD.Int32x4.splat(42)));");
    }

    @Test
    public void testSimdMin() {
        test("var a = SIMD.Float32x4(1, 2, 3, 4); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(a, SIMD.Float32x4.min(a, a)));");
    }

    @Test
    public void testSimdMax() {
        test("var a = SIMD.Float32x4(1, 2, 3, 4); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(a, SIMD.Float32x4.max(a, a)));");
    }

    @Test
    public void testSimdMinNum() {
        test("var a = SIMD.Float32x4(1, 2, 3, 4); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(a, SIMD.Float32x4.minNum(a, a)));");
    }

    @Test
    public void testSimdMaxNum() {
        test("var a = SIMD.Float32x4(1, 2, 3, 4); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(a, SIMD.Float32x4.maxNum(a, a)));");
    }

    @Test
    public void testSimdNeg() {
        test("var inp = SIMD.Float32x4(1, 2, 3, 4); var exp = SIMD.Float32x4(-1, -2, -3, -4); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(exp, SIMD.Float32x4.neg(inp)));");
        test("var inp = SIMD.Int32x4(1, 2, 3, 4); var exp = SIMD.Int32x4(-1, -2, -3, -4); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, SIMD.Int32x4.neg(inp)));");
        // conv missing
        test("var inp = SIMD.Uint32x4(1, 2, 3, 4); var exp = SIMD.Uint32x4(-1, -2, -3, -4); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, SIMD.Uint32x4.neg(inp)));");
        test("var inp = SIMD.Int16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = SIMD.Int16x8(-1, -2, -3, -4, -5, -6, -7, -8); SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, SIMD.Int16x8.neg(inp)));");
        // testIntl("var inp = SIMD.Uint16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = SIMD.Uint16x8(-1,
        // -2, -3, -4, -5, -6, -7, -8); SIMD.Bool16x8.allTrue(SIMD.Uint16x8.equal(exp,
        // SIMD.Uint16x8.neg(inp)));");
        test("var inp = SIMD.Int8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var exp = SIMD.Int8x16(-1, -2, -3, -4, -5, -6, -7, -8, -1, -2, -3, -4, -5, -6, -7, -8); SIMD.Bool8x16.allTrue(SIMD.Int8x16.equal(exp, SIMD.Int8x16.neg(inp)));");
        // testIntl("var inp = SIMD.Uint8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var
        // exp = SIMD.Uint8x16(-1, -2, -3, -4, -5, -6, -7, -8, -1, -2, -3, -4, -5, -6, -7, -8);
        // SIMD.Bool8x16.allTrue(SIMD.Uint8x16.equal(exp, SIMD.Uint8x16.neg(inp)));");
    }

    @Test
    public void testSimdSqrt() {
        test("var inp = SIMD.Float32x4(9, 16, 25, 36); var exp = SIMD.Float32x4(3, 4, 5, 6); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(exp, SIMD.Float32x4.sqrt(inp)));");
    }

    @Test
    public void testSimdReciprocalApproximation() {
        test("var inp = SIMD.Float32x4(2, 4, 8, 16); var exp = SIMD.Float32x4(0.5, 0.25, 0.125, 0.0625); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(exp, SIMD.Float32x4.reciprocalApproximation(inp)));");
    }

    @Test
    public void testSimdReciprocalSqrtApproximation() {
        test("var inp = SIMD.Float32x4(4, 16, 64, 256); var exp = SIMD.Float32x4(0.5, 0.25, 0.125, 0.0625); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(exp, SIMD.Float32x4.reciprocalSqrtApproximation(inp)));");
    }

    @Test
    public void testSimdAbs() {
        test("var inp = SIMD.Float32x4(-1, 2, -3, 4); var exp = SIMD.Float32x4(1, 2, 3, 4); SIMD.Bool32x4.allTrue(SIMD.Float32x4.equal(exp, SIMD.Float32x4.abs(inp)));");
    }

    @Test
    public void testSimdAnd() {
        test("var inp = SIMD.Int32x4(1, 2, 3, 4); var exp = inp; SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, SIMD.Int32x4.and(inp, inp)));");
        test("var inp = SIMD.Uint32x4(1, 2, 3, 4); var exp = inp; SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, SIMD.Uint32x4.and(inp, inp)));");
        test("var inp = SIMD.Int16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, SIMD.Int16x8.and(inp, inp)));");
        test("var inp = SIMD.Uint16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool16x8.allTrue(SIMD.Uint16x8.equal(exp, SIMD.Uint16x8.and(inp, inp)));");
        test("var inp = SIMD.Int8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool8x16.allTrue(SIMD.Int8x16.equal(exp, SIMD.Int8x16.and(inp, inp)));");
        test("var inp = SIMD.Uint8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool8x16.allTrue(SIMD.Uint8x16.equal(exp, SIMD.Uint8x16.and(inp, inp)));");
        test("var inp = SIMD.Bool32x4(true, true, true, true); var exp = inp; SIMD.Bool32x4.allTrue(SIMD.Bool32x4.and(inp, inp));");
    }

    @Test
    public void testSimdXor() {
        test("var inp = SIMD.Int32x4(1, 2, 3, 4); var exp = SIMD.Int32x4.splat(0); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, SIMD.Int32x4.xor(inp, inp)));");
        test("var inp = SIMD.Uint32x4(1, 2, 3, 4); var exp = SIMD.Uint32x4.splat(0); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, SIMD.Uint32x4.xor(inp, inp)));");
        test("var inp = SIMD.Int16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = SIMD.Int16x8.splat(0); SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, SIMD.Int16x8.xor(inp, inp)));");
        test("var inp = SIMD.Uint16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = SIMD.Uint16x8.splat(0); SIMD.Bool16x8.allTrue(SIMD.Uint16x8.equal(exp, SIMD.Uint16x8.xor(inp, inp)));");
        test("var inp = SIMD.Int8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var exp = SIMD.Int8x16.splat(0); SIMD.Bool8x16.allTrue(SIMD.Int8x16.equal(exp, SIMD.Int8x16.xor(inp, inp)));");
        test("var inp = SIMD.Uint8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var exp = SIMD.Uint8x16.splat(0); SIMD.Bool8x16.allTrue(SIMD.Uint8x16.equal(exp, SIMD.Uint8x16.xor(inp, inp)));");
        test("var inp = SIMD.Bool32x4(true, false, true, false); var exp = inp; !SIMD.Bool32x4.anyTrue(SIMD.Bool32x4.xor(inp, inp));");
    }

    @Test
    public void testSimdOr() {
        test("var inp = SIMD.Int32x4(1, 2, 3, 4); var exp = inp; SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, SIMD.Int32x4.or(inp, inp)));");
        test("var inp = SIMD.Uint32x4(1, 2, 3, 4); var exp = inp; SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, SIMD.Uint32x4.or(inp, inp)));");
        test("var inp = SIMD.Int16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, SIMD.Int16x8.or(inp, inp)));");
        test("var inp = SIMD.Uint16x8(1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool16x8.allTrue(SIMD.Uint16x8.equal(exp, SIMD.Uint16x8.or(inp, inp)));");
        test("var inp = SIMD.Int8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool8x16.allTrue(SIMD.Int8x16.equal(exp, SIMD.Int8x16.or(inp, inp)));");
        test("var inp = SIMD.Uint8x16(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8); var exp = inp; SIMD.Bool8x16.allTrue(SIMD.Uint8x16.equal(exp, SIMD.Uint8x16.or(inp, inp)));");
        test("var inp = SIMD.Bool32x4(true, true, true, true); var exp = inp; SIMD.Bool32x4.allTrue(exp, SIMD.Bool32x4.or(inp, inp));");
    }

    @Test
    public void testSimdNot() {
        test("var inp = SIMD.Int32x4.splat(1); var exp = SIMD.Int32x4.splat(-2); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, SIMD.Int32x4.not(inp)));");
        test("var inp = SIMD.Uint32x4.splat(1); var exp = SIMD.Uint32x4.splat(-2); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, SIMD.Uint32x4.not(inp)));");
        test("var inp = SIMD.Int16x8.splat(1); var exp = SIMD.Int16x8.splat(-2); SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, SIMD.Int16x8.not(inp)));");
        // testIntl("var inp = SIMD.Uint16x8.splat(1); var exp = SIMD.Uint16x8.splat(-2);
        // SIMD.Bool16x8.allTrue(SIMD.Uint16x8.equal(exp, SIMD.Uint16x8.not(inp)));");
        test("var inp = SIMD.Int8x16.splat(1); var exp = SIMD.Int8x16.splat(-2); SIMD.Bool8x16.allTrue(SIMD.Int8x16.equal(exp, SIMD.Int8x16.not(inp)));");
        // testIntl("var inp = SIMD.Uint8x16.splat(1); var exp = SIMD.Uint8x16.splat(-2);
        // SIMD.Bool8x16.allTrue(SIMD.Uint8x16.equal(exp, SIMD.Uint8x16.not(inp)));");
        test("var inp = SIMD.Bool32x4.splat(false); SIMD.Bool32x4.allTrue(SIMD.Bool32x4.not(inp));");
    }

    private static boolean testBoolean(String type, String size, int inputA, int inputB, String op) {
        String cType = type + size;
        String bType = "Bool" + size;
        return testIntl("var inpA = SIMD." + cType + ".splat(" + inputA + "); var inpB = SIMD." + cType + ".splat(" + inputB + "); SIMD." + bType + ".allTrue(SIMD." + cType + "." + op +
                        "(inpA, inpB));");
    }

    @Test
    public void testSimdCompare() {
        testCompare("Int", "32x4");
        testCompare("Int", "16x8");
        testCompare("Int", "8x16");
        testCompare("Uint", "32x4");
        testCompare("Uint", "16x8");
        testCompare("Uint", "8x16");
        testCompare("Float", "32x4");
    }

    public void testCompare(String type, String size) {
        String lessThan = "lessThan";
        assertTrue(testBoolean(type, size, 2, 3, lessThan));
        assertFalse(testBoolean(type, size, 3, 3, lessThan));
        assertFalse(testBoolean(type, size, 4, 3, lessThan));

        String lessThanOrEqual = "lessThanOrEqual";
        assertTrue(testBoolean(type, size, 2, 3, lessThanOrEqual));
        assertTrue(testBoolean(type, size, 3, 3, lessThanOrEqual));
        assertFalse(testBoolean(type, size, 4, 3, lessThanOrEqual));

        String greaterThan = "greaterThan";
        assertFalse(testBoolean(type, size, 2, 3, greaterThan));
        assertFalse(testBoolean(type, size, 3, 3, greaterThan));
        assertTrue(testBoolean(type, size, 4, 3, greaterThan));

        String greaterThanOrEqual = "greaterThanOrEqual";
        assertFalse(testBoolean(type, size, 2, 3, greaterThanOrEqual));
        assertTrue(testBoolean(type, size, 3, 3, greaterThanOrEqual));
        assertTrue(testBoolean(type, size, 4, 3, greaterThanOrEqual));
    }

    @Test
    public void testSimdNotEqual() {
        test("SIMD.Bool32x4.allTrue(SIMD.Float32x4.notEqual(SIMD.Float32x4.splat(1), SIMD.Float32x4.splat(2)));");
        test("SIMD.Bool32x4.allTrue(SIMD.Int32x4.notEqual(SIMD.Int32x4.splat(1), SIMD.Int32x4.splat(2)));");
        test("SIMD.Bool32x4.allTrue(SIMD.Uint32x4.notEqual(SIMD.Uint32x4.splat(1), SIMD.Uint32x4.splat(2)));");
        test("SIMD.Bool16x8.allTrue(SIMD.Int16x8.notEqual(SIMD.Int16x8.splat(1), SIMD.Int16x8.splat(2)));");
        test("SIMD.Bool16x8.allTrue(SIMD.Uint16x8.notEqual(SIMD.Uint16x8.splat(1), SIMD.Uint16x8.splat(2)));");
        test("SIMD.Bool8x16.allTrue(SIMD.Int8x16.notEqual(SIMD.Int8x16.splat(1), SIMD.Int8x16.splat(2)));");
        test("SIMD.Bool8x16.allTrue(SIMD.Uint8x16.notEqual(SIMD.Uint8x16.splat(1), SIMD.Uint8x16.splat(2)));");
    }

    @Test
    public void testSimdAnyTrue() {
        assertFalse(testIntl("SIMD.Bool32x4.anyTrue(SIMD.Bool32x4(false, false, false, false));"));
        assertTrue(testIntl("SIMD.Bool32x4.anyTrue(SIMD.Bool32x4(true, false, false, false));"));
        assertTrue(testIntl("SIMD.Bool32x4.anyTrue(SIMD.Bool32x4(false, true, false, false));"));
        assertTrue(testIntl("SIMD.Bool32x4.anyTrue(SIMD.Bool32x4(false, false, true, false));"));
        assertTrue(testIntl("SIMD.Bool32x4.anyTrue(SIMD.Bool32x4(false, false, false, true));"));
    }

    @Test
    public void testSimdShiftLeftByScalar() {
        test("var inp = SIMD.Int32x4(1, 2, 3, 4); var exp = SIMD.Int32x4(2, 4, 6, 8); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, SIMD.Int32x4.shiftLeftByScalar(inp, 1)));");
        test("var inp = SIMD.Uint32x4.splat(2); var exp = SIMD.Uint32x4.splat(4); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, SIMD.Uint32x4.shiftLeftByScalar(inp, 1)));");
        test("var inp = SIMD.Int16x8.splat(2); var exp = SIMD.Int16x8.splat(4); SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, SIMD.Int16x8.shiftLeftByScalar(inp, 1)));");
        test("var inp = SIMD.Uint16x8.splat(2); var exp = SIMD.Uint16x8.splat(4); SIMD.Bool16x8.allTrue(SIMD.Uint16x8.equal(exp, SIMD.Uint16x8.shiftLeftByScalar(inp, 1)));");
        test("var inp = SIMD.Int8x16.splat(2); var exp = SIMD.Int8x16.splat(4); SIMD.Bool8x16.allTrue(SIMD.Int8x16.equal(exp, SIMD.Int8x16.shiftLeftByScalar(inp, 1)));");
        test("var inp = SIMD.Uint8x16.splat(2); var exp = SIMD.Uint8x16.splat(4); SIMD.Bool8x16.allTrue(SIMD.Uint8x16.equal(exp, SIMD.Uint8x16.shiftLeftByScalar(inp, 1)));");
    }

    @Test
    public void testSimdShiftRightByScalar() {
        test("var inp = SIMD.Int32x4(2, 4, 6, 8); var exp = SIMD.Int32x4(1, 2, 3, 4); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, SIMD.Int32x4.shiftRightByScalar(inp, 1)));");
        test("var inp = SIMD.Uint32x4.splat(4); var exp = SIMD.Uint32x4.splat(2); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, SIMD.Uint32x4.shiftRightByScalar(inp, 1)));");
        test("var inp = SIMD.Int16x8.splat(4); var exp = SIMD.Int16x8.splat(2); SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, SIMD.Int16x8.shiftRightByScalar(inp, 1)));");
        test("var inp = SIMD.Uint16x8.splat(4); var exp = SIMD.Uint16x8.splat(2); SIMD.Bool16x8.allTrue(SIMD.Uint16x8.equal(exp, SIMD.Uint16x8.shiftRightByScalar(inp, 1)));");
        test("var inp = SIMD.Int8x16.splat(4); var exp = SIMD.Int8x16.splat(2); SIMD.Bool8x16.allTrue(SIMD.Int8x16.equal(exp, SIMD.Int8x16.shiftRightByScalar(inp, 1)));");
        test("var inp = SIMD.Uint8x16.splat(4); var exp = SIMD.Uint8x16.splat(2); SIMD.Bool8x16.allTrue(SIMD.Uint8x16.equal(exp, SIMD.Uint8x16.shiftRightByScalar(inp, 1)));");
    }

    @Test
    public void testSimdExtractLane() {
        test("var inp = SIMD.Int32x4.splat(0); SIMD.Int32x4.extractLane(SIMD.Int32x4.replaceLane(inp, 0, 42), 0) === 42;");
        test("var inp = SIMD.Uint32x4.splat(0); SIMD.Uint32x4.extractLane(SIMD.Uint32x4.replaceLane(inp, 0, 42), 0) === 42;");
        test("var inp = SIMD.Int16x8.splat(0); SIMD.Int16x8.extractLane(SIMD.Int16x8.replaceLane(inp, 0, 42), 0) === 42;");
        test("var inp = SIMD.Uint16x8.splat(0); SIMD.Uint16x8.extractLane(SIMD.Uint16x8.replaceLane(inp, 0, 42), 0) === 42;");
        test("var inp = SIMD.Int8x16.splat(0); SIMD.Int8x16.extractLane(SIMD.Int8x16.replaceLane(inp, 0, 42), 0) === 42;");
        test("var inp = SIMD.Uint8x16.splat(0); SIMD.Uint8x16.extractLane(SIMD.Uint8x16.replaceLane(inp, 0, 42), 0) === 42;");
        test("var inp = SIMD.Float32x4.splat(0); SIMD.Float32x4.extractLane(SIMD.Float32x4.replaceLane(inp, 0, 42), 0) === 42;");

        test("var inp = SIMD.Float32x4.splat(0); SIMD.Float32x4.extractLane(SIMD.Float32x4.replaceLane(inp, 0, 42), '0') === 42;");
    }

    @Test
    public void testSimdLoad() {
        testIntlFailed("SIMD.Int32x4.load({}, 0);", "TypedArray expected");
        test("var ta = new Int32Array(10); ta[0]=42; var inp = SIMD.Int32x4.load(ta, 0); SIMD.Int32x4.extractLane(inp, 0) === 42;");
        test("var ta = new Int32Array(10); ta[0]=42; var inp = SIMD.Int32x4.load1(ta, 0); SIMD.Int32x4.extractLane(inp, 0) === 42;");
    }

    @Test
    public void testSimdStore() {
        testIntlFailed("var inp = SIMD.Int32x4.splat(0); SIMD.Int32x4.store({}, 0, inp);", "TypedArray expected");
        testIntlFailed("var ta = new Int32Array(10); SIMD.Int32x4.store(ta, 0, {});", "SIMD type expected");
        test("var inp = SIMD.Int32x4.splat(42); var ta = new Int32Array(10); SIMD.Int32x4.store(ta, 0, inp); ta[0] === 42;");
        test("var inp = SIMD.Int32x4.splat(42); var ta = new Int32Array(10); SIMD.Int32x4.store1(ta, 0, inp); ta[0] === 42;");
    }

    @Test
    public void testSimdSwizzle() {
        test("var inp = SIMD.Int32x4(2, 4, 6, 8); var res = SIMD.Int32x4.swizzle(inp, 3, 2, 1, 0); var exp = SIMD.Int32x4(8, 6, 4, 2); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, res))");
    }

    @Test
    public void testSimdShuffle() {
        test("var inp = SIMD.Int32x4(2, 4, 6, 8); var inp2 = SIMD.Int32x4(1, 3, 5, 7); var res = SIMD.Int32x4.shuffle(inp, inp2, 0, 1, 6, 7); var exp = SIMD.Int32x4(2, 4, 5, 7); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, res))");
    }

    @Test
    public void testSimdFromTIMD() {
        testIntlFailed("SIMD.Int32x4.fromInt32x4({})", "SIMD type expected");
        testIntlFailed("var inp = SIMD.Int32x4(2, 4, 6, 8); SIMD.Int32x4.fromInt32x4(inp, inp)", "Should be undefined");
        testIntlFailed("var inp = SIMD.Float32x4(NaN, 0, 0, 0); var res = SIMD.Int32x4.fromFloat32x4(inp);", "NaN");

        test("var inp = SIMD.Int32x4(2, 4, 6, 8); var res = SIMD.Uint32x4.fromInt32x4(inp); var exp = SIMD.Uint32x4(2, 4, 6, 8); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, res))");
        test("var inp = SIMD.Uint32x4(2, 4, 6, 8); var res = SIMD.Int32x4.fromUint32x4(inp); var exp = SIMD.Int32x4(2, 4, 6, 8); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, res))");
        test("var inp = SIMD.Float32x4(2.6, 4.4, 6, 8); var res = SIMD.Int32x4.fromFloat32x4(inp); var exp = SIMD.Int32x4(2, 4, 6, 8); SIMD.Bool32x4.allTrue(SIMD.Int32x4.equal(exp, res))");
    }

    @Test
    public void testSimdFromTIMDBits() {
        testIntlFailed("SIMD.Int32x4.fromInt32x4Bits({})", "SIMD type expected");

        test("var inp = SIMD.Int32x4(2, 4, 6, 8); var res = SIMD.Uint32x4.fromInt32x4Bits(inp); var exp = SIMD.Uint32x4(2, 4, 6, 8); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, res))");
    }

    @Test
    public void testSimdSelect() {
        testIntlFailed("var res = SIMD.Int32x4.select({}, {}, {});", "SIMD type expected");
        testIntlFailed("var inp1 = SIMD.Int32x4(2, 4, 6, 8); var res = SIMD.Int32x4.select({}, inp1, inp1);", "invalid argument Type");

        test("var inp1 = SIMD.Int32x4(2, 4, 6, 8); var inp2 = SIMD.Int32x4(1, 3, 5, 7); var sel = SIMD.Bool32x4(true, false, true, false); var res = SIMD.Int32x4.select(sel, inp1, inp2); " +
                        "var exp = SIMD.Int32x4(2, 3, 6, 7); SIMD.Bool32x4.allTrue(SIMD.Uint32x4.equal(exp, res))");
    }

    @Test
    public void testSimdAddSaturate() {
        test("var inp1 = SIMD.Int16x8(2, 4, 32000, -32000); var inp2 = SIMD.Int16x8(1, 3, 32000, -32000); var res = SIMD.Int16x8.addSaturate(inp1, inp2); var exp = SIMD.Int16x8(3, 7, 32767, -32768);" +
                        " SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, res))");
    }

    @Test
    public void testSimdSubSaturate() {
        test("var inp1 = SIMD.Int16x8(2, 4, 32000, -32000); var inp2 = SIMD.Int16x8(1, 3, -32000, 32000); var res = SIMD.Int16x8.subSaturate(inp1, inp2); var exp = SIMD.Int16x8(1, 1, 32767, -32768);" +
                        "SIMD.Bool16x8.allTrue(SIMD.Int16x8.equal(exp, res))");
    }

    @Test
    public void testSimdToString() {
        test("var inp = SIMD.Int16x8.splat(42); inp.toString()==='SIMD.Int16x8(42,42,42,42,42,42,42,42)';");
    }

    // failing because of assertion
    @Ignore
    @Test
    public void testSimdToLocaleString() {
        test("var inp = SIMD.Int16x8.splat(42); inp.toLocaleString()==='SIMD.Int16x8(42,42,42,42,42,42,42,42)';");
    }

    @Test
    public void testSimdValueOf() {
        test("var inp = SIMD.Int16x8.splat(42); inp.valueOf(inp) === inp;");
    }

    @Ignore
    @Test
    public void failingTest1() {
        test("SIMD.Uint16x8.equal();");
    }

    @Ignore
    @Test
    public void failingTest2() {
        test("SIMD.Float32x4.add(0,0);");
    }

}

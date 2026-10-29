/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.js.nodes.binary.JSModuloNode;

public class JSModuloNodeTest {

    private static final int[] INT_VALUES = {
                    Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -7, -4, -3, -2, -1,
                    0, 1, 2, 3, 4, 7, 65536, 1 << 30, Integer.MAX_VALUE
    };

    @Test
    public void testNonNegativeIntegers() {
        JSModuloNode node = JSModuloNode.create();
        assertEquals(Integer.valueOf(1), node.execute(5, 4));
        assertEquals(Integer.valueOf(0), node.execute(4, 4));
        assertEquals(Integer.valueOf(0), node.execute(0, 1));
        assertEquals(Integer.valueOf(2), node.execute(5, 3));
        assertEquals(Integer.valueOf(0), node.execute(6, 3));
        assertEquals(Integer.valueOf(0), node.execute(0, -3));
        assertEquals(Integer.valueOf(2), node.execute(5, -3));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), node.execute(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void testNegativeIntegers() {
        JSModuloNode node = JSModuloNode.create();
        assertEquals(Integer.valueOf(-1), node.execute(-5, 4));
        assertEquals(Integer.valueOf(-2), node.execute(-5, 3));
        assertEquals(Integer.valueOf(-1), node.execute(-5, -4));
        assertEquals(Integer.valueOf(-2), node.execute(-5, -3));
        assertEquals(Integer.valueOf(-2), node.execute(Integer.MIN_VALUE, 3));
        assertEquals(Integer.valueOf(-1), node.execute(Integer.MIN_VALUE + 1, 2));
        assertEquals(Integer.valueOf(-1), node.execute(-1, Integer.MIN_VALUE));
        assertNonNegativeSpecializations(node);
    }

    @Test
    public void testNegativeZeroRewrite() {
        int[][] inputs = {
                        {-4, 2}, {-6, 3}, {-4, -2}, {-1, 1}, {-1, -1},
                        {Integer.MIN_VALUE, 1}, {Integer.MIN_VALUE, -1},
                        {Integer.MIN_VALUE, 1 << 30}, {Integer.MIN_VALUE, Integer.MIN_VALUE}
        };
        for (int[] pair : inputs) {
            JSModuloNode node = initializedNode();
            assertEquals(Double.valueOf(-0.0), node.execute(pair[0], pair[1]));
            assertNonNegativeSpecializations(node);
            assertEquals(Double.valueOf(-1.0), node.execute(-5, 4));
            assertEquals(Double.valueOf(-2.0), node.execute(-5, 3));
        }
    }

    @Test
    public void testZeroDivisorRewrite() {
        for (int a : new int[]{0, 5, -5, Integer.MIN_VALUE}) {
            JSModuloNode node = initializedNode();
            assertEquals(Double.valueOf(Double.NaN), node.execute(a, 0));
            assertEquals(Integer.valueOf(1), node.execute(5, 4));
            if (a >= 0) {
                assertEquals(Double.valueOf(2.0), node.execute(5, 3));
                assertEquals(Integer.valueOf(-2), node.execute(-5, 3));
                assertEquals(Integer.valueOf(-1), node.execute(-5, 4));
            } else {
                assertNonNegativeSpecializations(node);
                assertEquals(Double.valueOf(-2.0), node.execute(-5, 3));
                assertEquals(Double.valueOf(-1.0), node.execute(-5, 4));
            }
        }
    }

    @Test
    public void testIntegerRemainders() {
        for (boolean initialized : new boolean[]{false, true}) {
            for (int a : INT_VALUES) {
                for (int b : INT_VALUES) {
                    JSModuloNode node = initialized ? initializedNode() : JSModuloNode.create();
                    assertSameNumber((double) a % b, node.execute(a, b));
                }
            }
        }
    }

    @Test
    public void testPowerOfTwoRemainders() {
        for (int shift = 0; shift < 31; shift++) {
            int b = 1 << shift;
            for (int a : new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -b - 1, -b, -1, 0, 1, b, b + 1, Integer.MAX_VALUE}) {
                JSModuloNode node = initializedNode();
                assertSameNumber((double) a % b, node.execute(a, b));
            }
        }
    }

    @Test
    public void testDoubleRemainders() {
        double[] values = {0.0, -0.0, 1.0, -1.0, 3.5, -3.5, Double.MIN_VALUE, -Double.MIN_VALUE,
                        Double.MAX_VALUE, -Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (double a : values) {
            for (double b : values) {
                JSModuloNode node = initializedNode();
                assertSameNumber(a % b, node.execute(a, b));
            }
        }
    }

    private static JSModuloNode initializedNode() {
        JSModuloNode node = JSModuloNode.create();
        assertNonNegativeSpecializations(node);
        assertEquals(Integer.valueOf(-1), node.execute(-5, 4));
        assertEquals(Integer.valueOf(-2), node.execute(-5, 3));
        return node;
    }

    private static void assertNonNegativeSpecializations(JSModuloNode node) {
        assertEquals(Integer.valueOf(1), node.execute(5, 4));
        assertEquals(Integer.valueOf(2), node.execute(5, 3));
        assertEquals(Integer.valueOf(0), node.execute(0, 4));
        assertEquals(Integer.valueOf(0), node.execute(0, -3));
    }

    private static void assertSameNumber(double expected, Object actual) {
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(((Number) actual).doubleValue()));
    }
}

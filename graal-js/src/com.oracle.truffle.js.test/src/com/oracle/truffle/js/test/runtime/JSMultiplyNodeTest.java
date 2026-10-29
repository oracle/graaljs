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

import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.binary.JSMultiplyNode;

public class JSMultiplyNodeTest {

    private static final int[] INT_VALUES = {
                    Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -46341, -46340, -2, -1,
                    0, 1, 2, 46340, 46341, 65536, Integer.MAX_VALUE
    };

    @Test
    public void testSameSignIntegers() {
        JSMultiplyNode node = JSMultiplyNode.create();
        assertEquals(Integer.valueOf(0), node.execute(0, 0));
        assertEquals(Integer.valueOf(0), node.execute(0, 7));
        assertEquals(Integer.valueOf(0), node.execute(7, 0));
        assertEquals(Integer.valueOf(21), node.execute(3, 7));
        assertEquals(Integer.valueOf(21), node.execute(-3, -7));
        assertEquals(Integer.valueOf(2147395600), node.execute(46340, 46340));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), node.execute(Integer.MAX_VALUE, 1));
    }

    @Test
    public void testOppositeSigns() {
        JSMultiplyNode node = initializedNode(false);
        assertEquals(Integer.valueOf(-21), node.execute(3, -7));
        assertEquals(Integer.valueOf(-21), node.execute(-3, 7));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), node.execute(Integer.MIN_VALUE, 1));
        assertEquals(Integer.valueOf(21), node.execute(-3, -7));
        assertEquals(Integer.valueOf(0), node.execute(0, 7));
        assertEquals(Integer.valueOf(0), node.execute(7, 0));
        assertEquals(Integer.valueOf(0), node.execute(0, 0));
    }

    @Test
    public void testNegativeZeroRewrite() {
        int[][] inputs = {{0, -7}, {-7, 0}, {0, Integer.MIN_VALUE}, {Integer.MIN_VALUE, 0}};
        for (boolean mixedSigns : new boolean[]{false, true}) {
            for (int[] pair : inputs) {
                JSMultiplyNode node = initializedNode(mixedSigns);
                assertEquals(Double.valueOf(-0.0), node.execute(pair[0], pair[1]));
                assertDoubleSpecialization(node);
            }
        }
        JSMultiplyNode node = JSMultiplyNode.create();
        assertEquals(Double.valueOf(-0.0), node.execute(0, -7));
        assertDoubleSpecialization(node);
    }

    @Test
    public void testOverflowRewrite() {
        int[][] inputs = {
                        {-65536, -65536}, {65536, -65536}, {-65536, 65536},
                        {46341, 46341}, {Integer.MIN_VALUE, -1}, {Integer.MIN_VALUE, Integer.MIN_VALUE},
                        {Integer.MAX_VALUE, 2}
        };
        for (boolean mixedSigns : new boolean[]{false, true}) {
            for (int[] pair : inputs) {
                JSMultiplyNode node = initializedNode(mixedSigns);
                assertEquals(Double.valueOf((double) pair[0] * pair[1]), node.execute(pair[0], pair[1]));
                assertDoubleSpecialization(node);
            }
        }
    }

    @Test
    public void testIntegerProducts() {
        for (boolean mixedSigns : new boolean[]{false, true}) {
            for (int a : INT_VALUES) {
                for (int b : INT_VALUES) {
                    JSMultiplyNode node = initializedNode(mixedSigns);
                    assertSameNumber((double) a * b, node.execute(a, b));
                }
            }
        }
    }

    @Test
    public void testDoubleProducts() {
        double[] values = {0.0, -0.0, 1.0, -1.0, 0.5, -0.5, Double.MIN_VALUE, -Double.MIN_VALUE,
                        Double.MAX_VALUE, -Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (double a : values) {
            for (double b : values) {
                JSMultiplyNode node = initializedNode(true);
                assertSameNumber(a * b, node.execute(a, b));
            }
        }
    }

    @Test
    public void testConstantFolding() {
        for (int a : INT_VALUES) {
            for (int b : INT_VALUES) {
                JavaScriptNode node = JSMultiplyNode.create(JSConstantNode.createInt(a), JSConstantNode.createInt(b));
                assertSameNumber((double) a * b, node.execute(null));
            }
        }
    }

    private static JSMultiplyNode initializedNode(boolean mixedSigns) {
        JSMultiplyNode node = JSMultiplyNode.create();
        assertEquals(Integer.valueOf(21), node.execute(3, 7));
        assertEquals(Integer.valueOf(21), node.execute(-3, -7));
        if (mixedSigns) {
            assertEquals(Integer.valueOf(-21), node.execute(3, -7));
        }
        return node;
    }

    private static void assertDoubleSpecialization(JSMultiplyNode node) {
        assertEquals(Double.valueOf(21.0), node.execute(3, 7));
        assertEquals(Double.valueOf(21.0), node.execute(-3, -7));
        assertEquals(Double.valueOf(-21.0), node.execute(3, -7));
        assertEquals(Double.valueOf(0.0), node.execute(0, 0));
        assertEquals(Double.valueOf(-0.0), node.execute(0, -7));
    }

    private static void assertSameNumber(double expected, Object actual) {
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(((Number) actual).doubleValue()));
    }
}

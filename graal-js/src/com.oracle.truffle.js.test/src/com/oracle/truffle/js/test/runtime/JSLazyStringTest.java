/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.test.JSTest;

public class JSLazyStringTest extends JSTest {

    @Override
    public void setup() {
        super.setup();
        testHelper.getPolyglotContext().initialize(JavaScriptLanguage.ID);
        testHelper.enterContext();
    }

    @Override
    public void close() {
        testHelper.leaveContext();
        super.close();
    }

    @Test
    public void testLazyIntLength() {
        int[] values = new int[]{
                        0,
                        1,
                        9,
                        10,
                        99,
                        100,
                        999,
                        1000,
                        9999,
                        10000,
                        99999,
                        100000,
                        999999,
                        1000000,
                        9999999,
                        10000000,
                        99999999,
                        100000000,
                        999999999,
                        1000000000,
                        Integer.MAX_VALUE
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < JSConfig.MinLazyStringLength; i++) {
            sb.append('x');
        }
        String other = sb.toString();

        for (int value : values) {
            checkLazyIntLength(other, value);
            checkLazyIntLength(other, -value);
            checkLazyIntLength(value, other);
            checkLazyIntLength(-value, other);
        }
        checkLazyIntLength(other, Integer.MIN_VALUE);
        checkLazyIntLength(Integer.MIN_VALUE, other);
    }

    private static void checkLazyIntLength(String left, int right) {
        int actual = JSLazyString.createLazyInt(left, right, JavaScriptLanguage.getCurrentLanguage().getJSContext().getStringLengthLimit()).length();
        int expected = left.length() + Integer.toString(right).length();
        assertSame(expected, actual);
    }

    private static void checkLazyIntLength(int left, String right) {
        int actual = JSLazyString.createLazyInt(left, right, JavaScriptLanguage.getCurrentLanguage().getJSContext().getStringLengthLimit()).length();
        int expected = Integer.toString(left).length() + right.length();
        assertSame(expected, actual);
    }

}

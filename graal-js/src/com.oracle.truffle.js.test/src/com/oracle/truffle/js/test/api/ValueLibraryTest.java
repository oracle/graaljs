/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.api.ValueLibrary;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

@RunWith(Parameterized.class)
public class ValueLibraryTest extends JSTest {

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return List.of(Boolean.TRUE, Boolean.FALSE);
    }

    @Parameter(value = 0) public boolean cached;

    private ValueLibrary getLibrary(Object receiver) {
        if (cached) {
            return TestHelper.adopt(ValueLibrary.getFactory().create(receiver));
        } else {
            return ValueLibrary.getUncached();
        }
    }

    @Test
    public void testIsProxy() {
        testIsProxy("42", false);
        testIsProxy("undefined", false);
        testIsProxy("new Object()", false);
        testIsProxy("new Proxy({}, {})", true);
    }

    private void testIsProxy(String expression, boolean expected) {
        Object value = testHelper.runNoPolyglot(expression);
        boolean actual = getLibrary(value).isProxy(value);
        assertSame(expected, actual);
    }

    @Test
    public void testIsPromise() {
        testIsPromise("42", false);
        testIsPromise("undefined", false);
        testIsPromise("new Object()", false);
        testIsPromise("Promise.resolve()", true);
        testIsPromise("(async () => {})()", true);
    }

    private void testIsPromise(String expression, boolean expected) {
        Object value = testHelper.runNoPolyglot(expression);
        boolean actual = getLibrary(value).isPromise(value);
        assertSame(expected, actual);
    }

    @Test
    public void testIsArrayBuffer() {
        testIsArrayBuffer("42", false);
        testIsArrayBuffer("undefined", false);
        testIsArrayBuffer("new Object()", false);
        testIsArrayBuffer("new ArrayBuffer()", true);
        testIsArrayBuffer("new Uint8Array()", false);
        testIsArrayBuffer("new Uint8Array().buffer", true);
    }

    private void testIsArrayBuffer(String expression, boolean expected) {
        Object value = testHelper.runNoPolyglot(expression);
        boolean actual = getLibrary(value).isArrayBuffer(value);
        assertSame(expected, actual);
    }

    @Test
    public void testToString() {
        testToString("42", "42");
        testToString("undefined", "undefined");
        testToString("null", "null");
        testToString("42n", "42");
        testToString("new Object()", "[object Object]");
        testToString("[42, 211]", "42,211");
        testToString("({ toString() { return 'foo' } })", "foo");
    }

    public void testToString(String expression, String expected) {
        testHelper.enterContext();
        try {
            Object value = testHelper.runNoPolyglot(expression);
            String actual = getLibrary(value).toString(value).toJavaStringUncached();
            assertEquals(expected, actual);
        } finally {
            testHelper.leaveContext();
        }
    }

    @Test
    public void testToNumber() {
        testToNumber("42", 42);
        testToNumber("'123'", 123);
        testToNumber("undefined", Double.NaN);
        testToNumber("null", 0);
        testToNumber("Object(211)", 211);
        testToNumber("({ valueOf() { return Math.PI } })", Math.PI);
    }

    public void testToNumber(String expression, double expected) {
        try {
            testHelper.enterContext();
            Object value = testHelper.runNoPolyglot(expression);
            double actual = getLibrary(value).toNumber(value).doubleValue();
            assertEquals(expected, actual, 0);
        } finally {
            testHelper.leaveContext();
        }
    }

    @Test
    public void testToBoolean() {
        testToBoolean("false", false);
        testToBoolean("true", true);
        testToBoolean("0", false);
        testToBoolean("-0", false);
        testToBoolean("42", true);
        testToBoolean("NaN", false);
        testToBoolean("undefined", false);
        testToBoolean("null", false);
        testToBoolean("''", false);
        testToBoolean("'foo'", true);
        testToBoolean("[]", true);
        testToBoolean("new Object()", true);
        testToBoolean("new Boolean(false)", true);
    }

    public void testToBoolean(String expression, boolean expected) {
        try {
            testHelper.enterContext();
            Object value = testHelper.runNoPolyglot(expression);
            boolean actual = getLibrary(value).toBoolean(value);
            assertEquals(expected, actual);
        } finally {
            testHelper.leaveContext();
        }
    }

}

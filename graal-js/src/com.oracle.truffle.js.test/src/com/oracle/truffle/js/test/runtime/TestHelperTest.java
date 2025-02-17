/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper.ParsedFunction;

/**
 * Testing TestHelper itself, so we see it is covered.
 */
public class TestHelperTest extends JSTest {

    @Test
    public void testTestHelper() {
        assertTrue(testHelper.getGlobalObject() != Undefined.instance);
        assertEquals(1.0, testHelper.runDouble("1.0"), 0.00001);
        assertTrue(testHelper.runBoolean("true"));
        assertTrue(testHelper.runExpectUndefined("undefined"));
        assertEquals("bar\n", testHelper.runToString("print('bar')"));
        assertEquals("test", testHelper.runSilent("print('NOT VISIBLE ON CONSOLE'); 'test';"));
    }

    @Test
    public void testBinding() {
        // actually calls set, not define, thus failing!
        testHelper.putBinding(Strings.constant("__foo__"), Strings.constant("bar"));
        assertNull(testHelper.getBinding(Strings.constant("__foo__")));

        testHelper.run("var __foo__ = 'set';");

        testHelper.putBinding(Strings.constant("__foo__"), Strings.constant("bar"));
        testHelper.runVoid("__foo__+=42");
        assertEquals(Strings.fromJavaString("bar42"), testHelper.getBinding(Strings.constant("__foo__")));
    }

    @Test
    public void testTestHelperArray() {
        JSArrayObject arr = testHelper.runJSArray("[1,2,3]");
        Object[] javaArr = arr.getArrayType().toArray(arr);
        assertEquals(3, javaArr.length);
        assertEquals(1, javaArr[0]);
        assertEquals(2, javaArr[1]);
        assertEquals(3, javaArr[2]);
    }

    @Test
    public void testParseFirstFunction() {
        ParsedFunction fn = testHelper.parseFirstFunction("function fn(a) { return a; }");
        assertEquals("fn", fn.getRootNode().getName());
    }

    @Test
    public void testAssumeES6() {
        testHelper.assumeES6OrLater();
    }
}

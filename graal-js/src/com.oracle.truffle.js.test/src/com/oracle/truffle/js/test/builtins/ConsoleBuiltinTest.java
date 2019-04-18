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
package com.oracle.truffle.js.test.builtins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * Tests for the console builtin.
 */
public class ConsoleBuiltinTest extends JSTest {

    private String runInteractive(String sourceCode) {
        return testHelper.runToString(sourceCode, true).trim();
    }

    @Test
    public void testLogInfoDebugDir() {
        String result = runInteractive("console.log({a:'foo'});");
        assertEquals("[object Object]", result);
        result = runInteractive("console.info([1,2,3]);");
        assertEquals("1,2,3", result);
        result = runInteractive("console.debug(Object);");
        assertEquals("function Object() { [native code] }", result);
        result = runInteractive("console.dir(42);");
        assertEquals("42", result);
    }

    @Test
    public void testErrorWarn() {
        String result = runInteractive("console.error({a:'foo'});");
        assertEquals("[object Object]", result);
        result = runInteractive("console.warn([1,2,3]);");
        assertEquals("1,2,3", result);
    }

    @Test
    public void testAssert() {
        String result = runInteractive("console.assert(true);");
        assertEquals("", result);
        result = runInteractive("console.assert();");
        assertEquals("Assertion failed", result);
        result = runInteractive("console.assert(false);");
        assertEquals("Assertion failed", result);
        result = runInteractive("console.assert(false, 3, 4);");
        assertEquals("Assertion failed: 3 4", result);
    }

    @Test
    public void testClear() {
        runInteractive("console.clear();");
    }

    @Test
    public void testCount() {
        String result = runInteractive("console.count('xyz'); console.count('xyz'); console.count('xyz');");
        assertEquals("xyz: 1\nxyz: 2\nxyz: 3", result);

        result = runInteractive("console.count('xyz'); console.countReset('xyz'); console.count('xyz');");
        assertEquals("xyz: 1\nxyz: 1", result);

        result = runInteractive("console.count(); console.countReset(); console.count();");
        assertEquals("default: 1\ndefault: 1", result);
    }

    @Test
    public void testGroup() {
        String result = runInteractive("console.group('xyz'); console.log('test'); console.groupEnd('xyz'); console.log('test2');");
        assertEquals("xyz\n  test\ntest2", result);
    }

    @Test
    public void testTime() {
        String result = runInteractive("console.time('xyz'); console.timeLog('xyz', 'msg'); console.timeEnd('xyz');");
        assertTrue(result.contains("xyz: "));
        assertTrue(result.contains("ms msg"));

        result = runInteractive("console.time(); console.timeLog(); console.timeLog(undefined); console.timeEnd();");
        assertTrue(result.contains("default:"));
    }
}

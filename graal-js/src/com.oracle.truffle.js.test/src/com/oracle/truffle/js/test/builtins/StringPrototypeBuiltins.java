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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

/**
 * String.prototype.* behavior not sufficiently tested by test262.
 */
public class StringPrototypeBuiltins {

    private static boolean testIntl(String sourceText) {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, "string-prototype-test").buildLiteral());
            return result.asBoolean();
        }
    }

    private static Value evalWithLocale(String code, String locale) {
        try (Context context = JSTest.newContextBuilder().option("js.locale", locale).build()) {
            return context.eval(JavaScriptLanguage.ID, code);
        }
    }

    @Test
    public void testLocaleCompare() {
        assertTrue(testIntl("'abc'.localeCompare('abc') === 0;"));
        assertFalse(testIntl("'abc'.localeCompare('def') === 0;"));
    }

    @Test
    public void testToLocaleLowerCase() {
        String code = "'I'.toLocaleLowerCase()";
        assertEquals("i", evalWithLocale(code, "de").asString());
        assertEquals("\u0131", evalWithLocale(code, "tr").asString());
    }

    @Test
    public void testToLocaleUpperCase() {
        String code = "'i'.toLocaleUpperCase()";
        assertEquals("I", evalWithLocale(code, "de").asString());
        assertEquals("\u0130", evalWithLocale(code, "tr").asString());
    }

    @Test
    public void testReplaceAllRedefinedFlags() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME,
                        Integer.toString(JSConfig.ECMAScript2020)).build()) {
            String code = "var re = /a/; Object.defineProperty(re, 'flags', {value: 'g'}); 'a'.replaceAll(re, 'b');";
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isString());
            assertEquals("b", result.asString());
        }
    }

    @Test
    public void testReplaceAllCustomNonGlobalRegExp() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME,
                        Integer.toString(JSConfig.ECMAScript2020)).build()) {
            String code = "var searchValue = { [Symbol.match]: true, flags: '' }; var expectedError = false; try { 'a'.replaceAll(searchValue, 'b') } catch (e) { expectedError = e instanceof TypeError }";
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertEquals(true, result.asBoolean());
        }
    }

    @Test
    public void testReplaceAllCustomGlobalRegExp() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME,
                        Integer.toString(JSConfig.ECMAScript2020)).build()) {
            String code = "var searchValue = { [Symbol.match]: true, flags: 'g', [Symbol.replace]: () => 42 }; 'a'.replaceAll(searchValue, 'b');";
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isNumber());
            assertEquals(42, result.asInt());
        }
    }

    @Test
    public void testReplaceAllReplaceValueToString() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME,
                        Integer.toString(JSConfig.ECMAScript2020)).build()) {
            String code = "var toStringCount = 0; var replaceValue = { toString() { toStringCount++; return 'b'; } }; 'aa'.replaceAll('a', replaceValue); toStringCount;";
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isNumber());
            assertEquals(1, result.asInt());
        }
    }

}

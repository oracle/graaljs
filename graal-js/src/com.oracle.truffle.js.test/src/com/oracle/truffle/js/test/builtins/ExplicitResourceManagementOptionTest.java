/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class ExplicitResourceManagementOptionTest {

    @Test
    public void testDisabledByDefault() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(JavaScriptLanguage.ID, "{ using x = null; }");
            Assert.fail("Should fail with js.explicit-resource-management=false");
        } catch (PolyglotException e) {
            assertTrue(e.isSyntaxError());
        }
        try (Context context = JSTest.newContextBuilder().build()) {
            Value value = context.eval(JavaScriptLanguage.ID, "[typeof DisposableStack, typeof Symbol.dispose]");
            assertEquals("undefined", value.getArrayElement(0).asString());
            assertEquals("undefined", value.getArrayElement(1).asString());
        }
    }

    @Test
    public void testEnabledByOption() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.EXPLICIT_RESOURCE_MANAGEMENT_NAME, "true").build()) {
            checkEnabled(context);
        }
    }

    private static void checkEnabled(Context context) {
        Value value = context.eval(JavaScriptLanguage.ID, "" +
                        "let disposed = 0;" +
                        "{" +
                        "  using x = { [Symbol.dispose]() { disposed++; } };" +
                        "}" +
                        "[disposed, typeof DisposableStack, typeof Symbol.dispose];");
        assertEquals(1, value.getArrayElement(0).asInt());
        assertEquals("function", value.getArrayElement(1).asString());
        assertEquals("symbol", value.getArrayElement(2).asString());
    }

    @Test
    public void testUsingScopePreservesNullAndUndefinedErrors() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.EXPLICIT_RESOURCE_MANAGEMENT_NAME, "true").build()) {
            Value value = context.eval(JavaScriptLanguage.ID, "" +
                            "function throwInBody(v) {" +
                            "  try {" +
                            "    using x = { [Symbol.dispose]() {} };" +
                            "    throw v;" +
                            "  } catch (e) {" +
                            "    return e;" +
                            "  }" +
                            "}" +
                            "function throwInDispose(v) {" +
                            "  try {" +
                            "    using x = { [Symbol.dispose]() { throw v; } };" +
                            "  } catch (e) {" +
                            "    return e;" +
                            "  }" +
                            "}" +
                            "[" +
                            "  throwInBody(null) === null," +
                            "  throwInBody(undefined) === undefined," +
                            "  throwInDispose(null) === null," +
                            "  throwInDispose(undefined) === undefined" +
                            "];");
            assertTrue(value.getArrayElement(0).asBoolean());
            assertTrue(value.getArrayElement(1).asBoolean());
            assertTrue(value.getArrayElement(2).asBoolean());
            assertTrue(value.getArrayElement(3).asBoolean());
        }
    }

    @Test
    public void testEnabledByV8CompatibilityModeDefault() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_COMPATIBILITY_MODE_NAME, "true").build()) {
            checkEnabled(context);
        }
    }

    @Test
    public void testForUsingOfRemainsValid() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.EXPLICIT_RESOURCE_MANAGEMENT_NAME, "true").build()) {
            Value value = context.eval(JavaScriptLanguage.ID, "" +
                            "var using = 0;" +
                            "for (using of [1]) {}" +
                            "using;");
            assertEquals(1, value.asInt());
        }
    }

    @Test
    public void testForUsingOfOfRemainsIdentifierBased() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.EXPLICIT_RESOURCE_MANAGEMENT_NAME, "true").build()) {
            Value value = context.eval(JavaScriptLanguage.ID, "" +
                            "var using, of = [[9], [8], [7]], result = [];" +
                            "for (using of of [0, 1, 2]) {" +
                            "  result.push(using);" +
                            "}" +
                            "[result.length, result[0]];");
            assertEquals(1, value.getArrayElement(0).asInt());
            assertEquals(7, value.getArrayElement(1).asInt());
        }
    }

    @Test
    public void testExplicitOptionOverridesV8CompatibilityModeDefault() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_COMPATIBILITY_MODE_NAME, "true").option(JSContextOptions.EXPLICIT_RESOURCE_MANAGEMENT_NAME, "false").build()) {
            context.eval(JavaScriptLanguage.ID, "{ using x = null; }");
            Assert.fail("Should fail with js.explicit-resource-management=false");
        } catch (PolyglotException e) {
            assertTrue(e.isSyntaxError());
        }
    }
}

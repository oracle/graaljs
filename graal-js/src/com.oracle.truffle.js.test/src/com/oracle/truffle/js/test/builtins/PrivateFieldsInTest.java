/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class PrivateFieldsInTest {

    private static void runTestBinary(String operator) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.PRIVATE_FIELDS_IN_NAME, "true").build()) {
            context.eval(JavaScriptLanguage.ID, "class C {\n" +
                            "    #field;\n" +
                            "    static test(obj) {\n" +
                            "        return #field" + operator + " obj;\n" +
                            "    }\n" +
                            "}");
            Assert.fail("Should have thrown Parser error");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Expected an operand but found private_ident"));
        }
    }

    private static void runTestUnary(String operator) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.PRIVATE_FIELDS_IN_NAME, "true").build()) {
            context.eval(JavaScriptLanguage.ID, "class C {\n" +
                            "    #field;\n" +
                            "    static test(obj) {\n" +
                            "        return " + operator + "#field;\n" +
                            "    }\n" +
                            "}");
            Assert.fail("Should have thrown Parser error");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Expected an operand but found private_ident"));
        }
    }

    @Test
    public void testOtherOperators() {
        runTestBinary("==");
        runTestBinary("!=");
        runTestBinary(">");
        runTestBinary("<");
        runTestBinary(">=");
        runTestBinary("<=");
        runTestBinary("&&");
        runTestBinary("||");
        runTestBinary("&");
        runTestBinary("|");
        runTestBinary("<<");
        runTestBinary("+");
        runTestBinary("-");
        runTestBinary("*");
        runTestBinary("/");
        runTestBinary("%");

        runTestUnary("-");
        runTestUnary("!");
        runTestUnary("~");
    }

    @Test
    public void testStatic() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.PRIVATE_FIELDS_IN_NAME, "true").build()) {
            Value value = context.eval(JavaScriptLanguage.ID, "class C { static #foo() {}; static isC(o) { return #foo in o; } } C.isC(C);");
            Assert.assertTrue(value.isBoolean());
            Assert.assertTrue(value.asBoolean());
        }
    }

    @Test
    public void testLinked() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.PRIVATE_FIELDS_IN_NAME, "true").build()) {
            Value value = context.eval(JavaScriptLanguage.ID, "class C { static #foo; static isC(o) { return true && #foo in o; } } C.isC(C);");
            Assert.assertTrue(value.isBoolean());
            Assert.assertTrue(value.asBoolean());
        }
    }

    @Test
    public void testForLoop() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.PRIVATE_FIELDS_IN_NAME, "true").build()) {
            context.eval(JavaScriptLanguage.ID, "class C { #foo; static m(o) { for (#foo in o); } }");
            Assert.fail("Should have thrown Parser error");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Expected an operand but found private_ident"));
        }
    }

    @Test
    public void testConstructor() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.PRIVATE_FIELDS_IN_NAME, "true").build()) {
            Value value = context.eval(JavaScriptLanguage.ID, "class C { static f = C in C; } C.f");
            Assert.assertTrue(value.isBoolean());
            Assert.assertFalse(value.asBoolean());
        }
    }

    @Test
    public void testPrecedence() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.PRIVATE_FIELDS_IN_NAME, "true").build()) {
            context.eval(JavaScriptLanguage.ID, "class C { static #foo; static isC(o) { 1 * #foo in o; } }");
            Assert.fail("Should have thrown Parser error");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Expected an operand but found private_ident"));
        }
    }
}

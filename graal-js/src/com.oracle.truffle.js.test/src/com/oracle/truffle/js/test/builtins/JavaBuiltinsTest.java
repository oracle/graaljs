/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

/**
 * Tests for the Java builtin.
 */
public class JavaBuiltinsTest extends JSTest {

    private static String test(String sourceCode) {
        return test(sourceCode, null, true, null);
    }

    private static String test(String sourceCode, String failedMessage) {
        return test(sourceCode, failedMessage, true, null);
    }

    private static String test(String sourceCode, String failedMessage, boolean allowAllAccess) {
        return test(sourceCode, failedMessage, allowAllAccess, null);
    }

    private static String test(String sourceCode, String failedMessage, boolean allowAllAccess, Object arg) {
        return test(sourceCode, failedMessage, allowAllAccess, arg, false);
    }

    private static String test(String sourceCode, String failedMessage, boolean allowAllAccess, Object arg, boolean nashornCompat) {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(allowAllAccess).option(JSContextOptions.NASHORN_COMPATIBILITY_MODE_NAME,
                        String.valueOf(nashornCompat)).build()) {
            if (arg != null) {
                context.getBindings("js").putMember("arg", arg);
            }
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceCode, "java-test").buildLiteral());
            assertTrue(failedMessage == null);
            return result.asString();
        } catch (Exception ex) {
            assertTrue(failedMessage != null);
            assertTrue(ex.getMessage(), ex.getMessage().contains(failedMessage));
            return "FAILED_AS_EXPECTED";
        }
    }

    @Test
    public void testJavaType() {
        String result = test("''+(Java.type('java.lang.String'));");
        assertEquals("class java.lang.String", result);

        test("Java.type('does.not.exist');", "does not exist");
        test("Java.type(1);", "expects one string argument");
        test("Java.type('java.lang.String');", "Java is not defined", false);
    }

    @Test
    public void testJavaTypeName() {
        String result = test("var t = Java.type('java.lang.String'); Java.typeName(t);");
        assertEquals("java.lang.String", result);

        result = test("''+Java.typeName();");
        assertEquals("undefined", result);
    }

    @Test
    public void testJavaExtend() {
        // String result = test("var t = Java.type('java.lang.Object'); var e = Java.extend(t);
        // ''+e;");
        // assertEquals("class com.oracle.truffle.js.javaadapters.java.lang.Object", result);

        // result = test("var t = Java.type('java.lang.Object'); var e = Java.extend(t, {a:'foo'});
        // ''+e;");
        // assertEquals("class com.oracle.truffle.js.javaadapters.java.lang.Object", result);

        test("Java.extend();", "needs at least one argument");
        test("Java.extend({});", "needs at least one type argument");
        test("Java.extend(1);", "needs Java types");
    }

    @Test
    public void testJavaFrom() {
        String result = test("var t = Java.from(arg); ''+t;", null, true, new Object[]{1, 2, 3});
        assertEquals("1,2,3", result);

        List<Object> list = new ArrayList<>();
        list.add(true);
        list.add(42);
        result = test("var t = Java.from(arg); ''+t;", null, true, list);
        assertEquals("true,42", result);

        test("Java.from(1);", "Cannot convert to JavaScript");
        test("Java.from({a:'foo'});", "Cannot convert to JavaScript");
    }

    @Test
    public void testJavaTo() {
        String result = test("var t = Java.to({a:'foo'}); ''+t;");
        assertEquals("[]", result);

        result = test("var t = Java.to({a:'foo'},arg); ''+t;", null, true, (new Object[0]).getClass());
        assertEquals("[]", result);

        test("var t = Java.to({a:'foo'}, 'int[]'); ''+t;");
        assertEquals("[]", result);

        test("var t = Java.to(1, 'int[]'); ''+t;", "is not an Object");
    }

    @Test
    public void testJavaSuper() {
        test("var t = Java.super({a:'foo'}); ''+t;");
    }

    @Test
    public void testJavaIsType() {
        String result = test("var t = Java.isType(Java.type('java.lang.String')); ''+t;");
        assertEquals("true", result);
    }

    @Test
    public void testIsJavaObject() {
        String result = test("var t = Java.isJavaObject(Java.type('java.lang.String')); ''+t;");
        assertEquals("true", result);

        result = test("var t = Java.isJavaObject(1); ''+t;");
        assertEquals("false", result);
        result = test("var t = Java.isJavaObject({}); ''+t;");
        assertEquals("false", result);
    }

    // only in nashorn-compat mode
    @Test
    public void testIsJavaMethod() {
        String result = test("var t = Java.isJavaMethod(Java.type('java.lang.System').nanoTime); ''+t;", null, true, null, true);
        assertEquals("true", result);

        result = test("var t = Java.isJavaMethod( x => x+1 ); ''+t;", null, true, null, true);
        assertEquals("false", result);
    }

    // only in nashorn-compat mode
    @Test
    public void testIsJavaFunction() {
        String result = test("var t = Java.isJavaFunction(Java.type('java.lang.System').nanoTime); ''+t;", null, true, null, true);
        assertEquals("true", result);

        result = test("var t = Java.isJavaFunction( x => x+1 ); ''+t;", null, true, null, true);
        assertEquals("false", result);
    }

    // only in nashorn-compat mode
    @Test
    public void testIsScriptFunction() {
        String result = test("var t = Java.isScriptFunction(Java.type('java.lang.System').nanoTime); ''+t;", null, true, null, true);
        assertEquals("false", result);

        result = test("var t = Java.isScriptFunction( x => x+1 ); ''+t;", null, true, null, true);
        assertEquals("true", result);
    }

    // only in nashorn-compat mode
    @Test
    public void testIsScriptObject() {
        String result = test("var t = Java.isScriptObject(Java.type('java.lang.String')); ''+t;", null, true, null, true);
        assertEquals("false", result);

        result = test("var t = Java.isScriptObject({}); ''+t;", null, true, null, true);
        assertEquals("true", result);
    }

    // only in nashorn-compat mode
    @Test
    public void testSynchronize() {
        String result = test("var t = Java.synchronized(x=>x+1); ''+t;", null, true, null, true);
        assertEquals("function synchronizedWrapper() { [native code] }", result);

        result = test("var t = Java.synchronized(x=>x+1, {}); t(1); ''+t;", null, true, null, true);
        assertEquals("function bound() { [native code] }", result);

        test("var t = Java.synchronized(false, {}); ''+t;", "is not a function", true, null, true);
        test("var t = Java.synchronized(x=>x+1, 1); f(1); ''+t;", "Locking not supported on", true, null, true);
    }

    // only in nashorn-compat mode
    @Test
    public void testAddToClasspath() {
        String result = test("var t = Java.addToClasspath('.'); ''+t;", null, true, null, true);
        assertEquals("undefined", result);

        test("var t = Java.addToClasspath(true); ''+t;", null, true, null, true);
        assertEquals("undefined", result);
    }

}

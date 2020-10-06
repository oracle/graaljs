/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.scriptengine.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.util.Map;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

public class TestNashornTypeConversion {

    public static class ValueHolder {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static void testToString(Object value, String expectedResult) throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = TestUtil.getEngineNashornCompat(manager);
        ValueHolder holder = new ValueHolder();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("holder", holder);
        bindings.put("value", value);
        engine.eval("holder.setValue(value)");
        assertEquals(expectedResult, holder.getValue());
    }

    @Test
    public void testNumberToString() throws ScriptException {
        testToString(42, "42");
    }

    @Test
    public void testBooleanToString() throws ScriptException {
        testToString(true, "true");
    }

    @Test
    public void testObjectToString() throws ScriptException {
        Point p = new java.awt.Point(42, 211);
        testToString(p, p.toString());
    }

    @Test
    public void testNewLong() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = TestUtil.getEngineNashornCompat(manager);
        engine.eval("var Long = Java.type('java.lang.Long');");
        Object result;
        result = engine.eval("new Long(33);");
        assertTrue(String.valueOf(result), result instanceof Number && ((Number) result).intValue() == 33);
        result = engine.eval("Long.valueOf(33);");
        assertTrue(String.valueOf(result), result instanceof Number && ((Number) result).intValue() == 33);
    }

    @Test
    public void testUser1() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = TestUtil.getEngineNashornCompat(manager);
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("user", new User1());
        Object result;
        result = engine.eval("user.test(1, 3);");
        assertTrue(String.valueOf(result), "(int,Integer)".equals(result));
        result = engine.eval("user.test(1, 'test');");
        assertTrue(String.valueOf(result), "(int,String)".equals(result));
    }

    @Test
    public void testUser2() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = TestUtil.getEngineNashornCompat(manager);
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("user", new User2());
        bindings.put("throwable", new Throwable());
        Object result;
        result = engine.eval("user.test('msg');");
        assertTrue(String.valueOf(result), "(String)".equals(result));
        result = engine.eval("user.test('msg', throwable);");
        assertTrue(String.valueOf(result), "(String,Throwable)".equals(result));
        result = engine.eval("user.test('msg', {});");
        assertTrue(String.valueOf(result), "(String,Object)".equals(result));
        result = engine.eval("user.test('msg', 'str');");
        assertTrue(String.valueOf(result), "(String,String)".equals(result));
        result = engine.eval("user.test('msg', {}, {});");
        assertTrue(String.valueOf(result), "(String,Object,Object)".equals(result));
        result = engine.eval("user.test('msg', 'str', {}, 3);");
        assertTrue(String.valueOf(result), "(String,Object[])".equals(result));
    }

    @Test
    public void testUser3() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = TestUtil.getEngineNashornCompat(manager);
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("user", new User3());
        bindings.put("throwable", new Throwable());
        Object result;
        result = engine.eval("user.test(33);");
        assertTrue(String.valueOf(result), "(String)".equals(result));
        result = engine.eval("user.test({});");
        assertTrue(String.valueOf(result), "(Map)".equals(result));
    }

    @Test
    public void testUser4() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = TestUtil.getEngineNashornCompat(manager);
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("user", new User4());
        bindings.put("throwable", new Throwable());
        Object result;
        result = engine.eval("user.test(33);");
        assertTrue(String.valueOf(result), "(String)".equals(result));
        result = engine.eval("user.test({get: () => 42});");
        assertTrue(String.valueOf(result), "(Supplier)".equals(result));
    }

    @SuppressWarnings("unused")
    public static class User1 {
        public String test(int n1, Integer n2) {
            return "(int,Integer)";
        }

        public String test(int n1, String n2) {
            return "(int,String)";
        }
    }

    @SuppressWarnings("unused")
    public static class User2 {
        public String test(String msg) {
            return "(String)";
        }

        public String test(String format, Object arg) {
            return "(String,Object)";
        }

        public String test(String format, Object arg1, Object arg2) {
            return "(String,Object,Object)";
        }

        public String test(String msg, Throwable t) {
            return "(String,Throwable)";
        }

        public String test(String format, Object... args) {
            return "(String,Object[])";
        }

        public String test(String format, String arg) {
            return "(String,String)";
        }

        public String test(String msg, Map<?, ?> t) {
            return "(String,Map)";
        }
    }

    @SuppressWarnings("unused")
    public static class User3 {
        public String test(String arg) {
            return "(String)";
        }

        public String test(Map<?, ?> t) {
            return "(Map)";
        }
    }

    @SuppressWarnings("unused")
    public static class User4 {
        public String test(String arg) {
            return "(String)";
        }

        public String test(Supplier<Object> t) {
            assertEquals(42, t.get());
            return "(Supplier)";
        }
    }
}

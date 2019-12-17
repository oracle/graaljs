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
package com.oracle.truffle.js.scriptengine.test;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.junit.Test;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Various tests for accessing JavaScript array in Java and accessing appropriate Java objects as
 * JavaScript arrays in JavaScript.
 */
public class ScriptEngineInteropArrayTest {
    private static final int[] JAVA_ARRAY = new int[]{3, 4, 1, 5};
    private static final List<Integer> JAVA_LIST = Arrays.stream(JAVA_ARRAY).boxed().collect(Collectors.toList());
    private static final String JS_ARRAY_STRING = Arrays.toString(JAVA_ARRAY);

    public static class ToBePassedToJS {
        private List<?> list;

        @HostAccess.Export
        public void methodWithListArgument(List<?> argList) {
            this.list = argList;
        }

        @HostAccess.Export
        public void methodWithArrayArgument(int[] argArray) {
            this.list = Arrays.stream(argArray).boxed().collect(Collectors.toList());
        }

        @HostAccess.Export
        public int[] methodThatReturnsArray() {
            return JAVA_ARRAY;
        }

        @HostAccess.Export
        public List<Integer> methodThatReturnsList() {
            return JAVA_LIST;
        }
    }

    /**
     * Test that a JavaScript array can be evaluated via ScriptEngine and read as List from Java. To
     * ensure that JavaScript array is converted to a Java List when converting it to a generic Java
     * Object, appropriate target type mapping must be specified. See
     * {@link HostAccess.Builder#targetTypeMapping}.
     */
    @Test
    public void testArrayBasic() throws ScriptException {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(List.class, Object.class, null, (v) -> v).build();
        final Engine graalEngine = Engine.newBuilder().build();
        try (GraalJSScriptEngine graalJSScriptEngine = GraalJSScriptEngine.create(
                        graalEngine,
                        Context.newBuilder(ID).allowHostAccess(hostAccess).engine(graalEngine))) {
            Object o = graalJSScriptEngine.eval(JS_ARRAY_STRING);
            assertTrue(o instanceof List);
            List<?> list = (List<?>) o;
            assertEquals(JAVA_ARRAY.length, list.size());
            assertEquals(JS_ARRAY_STRING, Arrays.toString(list.toArray()));
        }
    }

    @Test
    public void testArrayAsListParameter() throws ScriptException {
        testArrayAsParameter("methodWithListArgument");
    }

    @Test
    public void testArrayAsArrayParameter() throws ScriptException {
        testArrayAsParameter("methodWithArrayArgument");
    }

    /**
     * Test that a JavaScript array can be passed as argument of Java function and read as List or
     * Java array from Java. Please note that {@link HostAccess.Builder#targetTypeMapping} is not
     * necessary, because the appropriate class is inferred from the method arguments. However, to
     * be able to call a Java method, access to it must be allowed, e.g. by a method annotation and
     * passing the class of that annotation to {@link HostAccess.Builder#allowAccessAnnotatedBy}.
     */
    private static void testArrayAsParameter(String methodName) throws ScriptException {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        final Engine graalEngine = Engine.newBuilder().build();
        try (GraalJSScriptEngine graalJSScriptEngine = GraalJSScriptEngine.create(
                        graalEngine,
                        Context.newBuilder(ID).allowHostAccess(hostAccess).engine(graalEngine))) {
            Bindings bindings = graalJSScriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            bindings.put("objectFromJava", objectFromJava);
            graalJSScriptEngine.eval("objectFromJava." + methodName + "(" + JS_ARRAY_STRING + ")");
            assertEquals(JAVA_ARRAY.length, objectFromJava.list.size());
            assertEquals(JS_ARRAY_STRING, Arrays.toString(objectFromJava.list.toArray()));
        }
    }

    /**
     * Test that a Java array can be passed to JavaScript scope and accessed as JavaScript array in
     * JavaScript. Please note that array access must be enabled for this.
     */
    @Test
    public void testJavaArrayAsJSArray() throws ScriptException {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(List.class, Object.class, null, (v) -> v).allowArrayAccess(true).build();
        final Engine graalEngine = Engine.newBuilder().build();
        try (GraalJSScriptEngine graalJSScriptEngine = GraalJSScriptEngine.create(
                        graalEngine,
                        Context.newBuilder(ID).allowHostAccess(hostAccess).engine(graalEngine))) {
            Bindings bindings = graalJSScriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("arrayFromJava", JAVA_ARRAY);
            Object o = graalJSScriptEngine.eval("var recreatedArray = [];" +
                            "for (var i = 0; i < arrayFromJava.length; i++)" +
                            "recreatedArray.push(arrayFromJava[i]);" +
                            "recreatedArray");
            assertTrue(o instanceof List);
            List<?> list = (List<?>) o;
            assertEquals(JAVA_ARRAY.length, list.size());
            assertEquals(JAVA_LIST, list);
        }
    }

    /**
     * Test that a Java List can be passed to JavaScript scope and accessed as JavaScript array in
     * JavaScript. Please note that List access must be enabled for this.
     */
    @Test
    public void testJavaListAsJSArray() throws ScriptException {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(List.class, Object.class, null, (v) -> v).allowListAccess(true).build();
        final Engine graalEngine = Engine.newBuilder().build();
        try (GraalJSScriptEngine graalJSScriptEngine = GraalJSScriptEngine.create(
                        graalEngine,
                        Context.newBuilder(ID).allowHostAccess(hostAccess).engine(graalEngine))) {
            Bindings bindings = graalJSScriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("arrayFromJava", JAVA_LIST);
            Object o = graalJSScriptEngine.eval("var recreatedArray = [];" +
                            "for (var i = 0; i < arrayFromJava.length; i++)" +
                            "recreatedArray.push(arrayFromJava[i]);" +
                            "recreatedArray");
            assertTrue(o instanceof List);
            List<?> list = (List<?>) o;
            assertEquals(JAVA_LIST.size(), list.size());
            assertEquals(JAVA_LIST, list);
        }
    }

    /**
     * Test that a Java array can be returned from a Java method called in JavaScript and accessed
     * as JavaScript array.
     */
    @Test
    public void testJavaReturnArrayAsJSArray() throws ScriptException {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(List.class, Object.class, null, (v) -> v).allowAccessAnnotatedBy(HostAccess.Export.class).allowArrayAccess(
                        true).build();
        final Engine graalEngine = Engine.newBuilder().build();
        try (GraalJSScriptEngine graalJSScriptEngine = GraalJSScriptEngine.create(
                        graalEngine,
                        Context.newBuilder(ID).allowHostAccess(hostAccess).engine(graalEngine))) {
            Bindings bindings = graalJSScriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            bindings.put("objectFromJava", objectFromJava);
            Object o = graalJSScriptEngine.eval("var arrayFromJava = objectFromJava.methodThatReturnsArray();" +
                            "var recreatedArray = [];" +
                            "for (var i = 0; i < arrayFromJava.length; i++)" +
                            "recreatedArray.push(arrayFromJava[i]);" +
                            "recreatedArray");
            assertTrue(o instanceof List);
            List<?> list = (List<?>) o;
            assertEquals(JAVA_ARRAY.length, list.size());
            assertEquals(JAVA_LIST, list);
        }
    }

    /**
     * Test that a Java List can be returned from a Java method called in JavaScript and accessed as
     * JavaScript array.
     */
    @Test
    public void testJavaReturnListAsJSArray() throws ScriptException {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(List.class, Object.class, null, (v) -> v).allowAccessAnnotatedBy(HostAccess.Export.class).allowListAccess(
                        true).build();
        final Engine graalEngine = Engine.newBuilder().build();
        try (GraalJSScriptEngine graalJSScriptEngine = GraalJSScriptEngine.create(
                        graalEngine,
                        Context.newBuilder(ID).allowHostAccess(hostAccess).engine(graalEngine))) {
            Bindings bindings = graalJSScriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            bindings.put("objectFromJava", objectFromJava);
            Object o = graalJSScriptEngine.eval("var arrayFromJava = objectFromJava.methodThatReturnsList();" +
                            "var recreatedArray = [];" +
                            "for (var i = 0; i < arrayFromJava.length; i++)" +
                            "recreatedArray.push(arrayFromJava[i]);" +
                            "recreatedArray");
            assertTrue(o instanceof List);
            List<?> list = (List<?>) o;
            assertEquals(JAVA_LIST.size(), list.size());
            assertEquals(JAVA_LIST, list);
        }
    }
}

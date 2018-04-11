/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.js.scriptengine.GraalJSEngineFactory;

/**
 * Tests for JSR-223 script engine from the Nashorn test suite.
 */
public class TestEngineNashorn {

    private final ScriptEngineManager manager = new ScriptEngineManager();

    private ScriptEngine getEngine() {
        return manager.getEngineByName(TestEngine.TESTED_ENGINE_NAME);
    }

    private static void invertedAssertEquals(Object actual, Object expected) {
        org.junit.Assert.assertEquals(expected, actual);
    }

    @Test
    public void argumentsTest() {
        final ScriptEngine e = getEngine();

        String[] args = new String[]{"hello", "world"};
        try {
            e.put("arguments", args);
            Object arg0 = e.eval("arguments[0]");
            Object arg1 = e.eval("arguments[1]");
            invertedAssertEquals(args[0], arg0);
            invertedAssertEquals(args[1], arg1);
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    /**
     * We do not yet support JavaImporter.
     */
    @Ignore
    @Test
    public void argumentsWithTest() {
        final ScriptEngine e = getEngine();

        String[] args = new String[]{"hello", "world"};
        try {
            e.put("arguments", args);
            Object arg0 = e.eval("var imports = new JavaImporter(java.io); " + " with(imports) { arguments[0] }");
            Object arg1 = e.eval("var imports = new JavaImporter(java.util, java.io); " + " with(imports) { arguments[1] }");
            invertedAssertEquals(args[0], arg0);
            invertedAssertEquals(args[1], arg1);
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void argumentsEmptyTest() {
        final ScriptEngine e = getEngine();

        try {
            invertedAssertEquals(e.eval("arguments instanceof Array"), true);
            invertedAssertEquals(e.eval("arguments.length == 0"), true);
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void factoryTests() {
        final ScriptEngine e = getEngine();
        assertNotNull(e);

        final ScriptEngineFactory fac = e.getFactory();

        invertedAssertEquals(fac.getLanguageName(), "ECMAScript");
        invertedAssertEquals(fac.getParameter(ScriptEngine.NAME), "javascript");
        invertedAssertEquals(fac.getLanguageVersion(), "ECMA - 262 Edition 6");
        invertedAssertEquals(fac.getEngineName(), "Graal.js");
        invertedAssertEquals(fac.getOutputStatement("context"), "print(context)");
        invertedAssertEquals(fac.getProgram("print('hello')", "print('world')"), "print('hello');print('world');");
        invertedAssertEquals(fac.getParameter(ScriptEngine.NAME), "javascript");

        if (GraalJSEngineFactory.RegisterAsNashornScriptEngineFactory) {
            boolean seenJS = false;
            for (String ext : fac.getExtensions()) {
                if (ext.equals("js")) {
                    seenJS = true;
                }
            }

            invertedAssertEquals(seenJS, true);

            String str = fac.getMethodCallSyntax("obj", "foo", "x");
            invertedAssertEquals(str, "obj.foo(x)");
            boolean seenJavaScript = false;
            boolean seenECMAScript = false;
            for (String name : fac.getNames()) {
                switch (name) {
                    case "javascript":
                        seenJavaScript = true;
                        break;
                    case "ECMAScript":
                        seenECMAScript = true;
                        break;
                }
            }

            assertTrue(seenJavaScript);
            assertTrue(seenECMAScript);

            boolean seenAppJS = false;
            boolean seenAppECMA = false;
            boolean seenTextJS = false;
            boolean seenTextECMA = false;
            for (String mime : fac.getMimeTypes()) {
                switch (mime) {
                    case "application/javascript":
                        seenAppJS = true;
                        break;
                    case "application/ecmascript":
                        seenAppECMA = true;
                        break;
                    case "text/javascript":
                        seenTextJS = true;
                        break;
                    case "text/ecmascript":
                        seenTextECMA = true;
                        break;
                }
            }

            assertTrue(seenAppJS);
            assertTrue(seenAppECMA);
            assertTrue(seenTextJS);
            assertTrue(seenTextECMA);
        }
    }

    @Test
    public void evalTests() {
        final ScriptEngine e = getEngine();
        e.put(ScriptEngine.FILENAME, "myfile.js");

        try {
            e.eval("print('hello')");
        } catch (final ScriptException se) {
            fail(se.getMessage());
        }
        try {
            e.eval("print('hello)");
            fail("script exception expected");
        } catch (final ScriptException se) {
            /**
             * TODO: provide info about line number, column number, and file name.
             */
            // assertEquals(se.getLineNumber(), 1);
            // assertEquals(se.getColumnNumber(), 13);
            // assertEquals(se.getFileName(), "myfile.js");
        }

        try {
            Object obj = e.eval("34 + 41");
            assertTrue(34.0 + 41.0 == ((Number) obj).doubleValue());
            obj = e.eval("x = 5");
            assertTrue(5.0 == ((Number) obj).doubleValue());
        } catch (final ScriptException se) {
            se.printStackTrace();
            fail(se.getMessage());
        }
    }

    @Test
    public void compileTests() {
        final ScriptEngine e = getEngine();
        CompiledScript script = null;

        try {
            script = ((Compilable) e).compile("print('hello')");
        } catch (final ScriptException se) {
            fail(se.getMessage());
        }

        try {
            script.eval();
        } catch (final ScriptException | NullPointerException se) {
            se.printStackTrace();
            fail(se.getMessage());
        }

        // try to compile from a Reader
        try {
            script = ((Compilable) e).compile(new StringReader("print('world')"));
        } catch (final ScriptException se) {
            fail(se.getMessage());
        }

        try {
            script.eval();
        } catch (final ScriptException | NullPointerException se) {
            se.printStackTrace();
            fail(se.getMessage());
        }
    }

    @Test
    public void compileAndEvalInDiffContextTest() throws ScriptException {
        final ScriptEngine engine = getEngine();
        final Compilable compilable = (Compilable) engine;
        final CompiledScript compiledScript = compilable.compile("foo");
        final ScriptContext ctxt = new SimpleScriptContext();
        ctxt.setAttribute("foo", "hello", ScriptContext.ENGINE_SCOPE);
        invertedAssertEquals(compiledScript.eval(ctxt), "hello");
    }

    @Test
    public void accessGlobalTest() {
        final ScriptEngine e = getEngine();

        try {
            e.eval("var x = 'hello'");
            invertedAssertEquals(e.get("x"), "hello");
        } catch (final ScriptException exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void exposeGlobalTest() {
        final ScriptEngine e = getEngine();

        try {
            e.put("y", "foo");
            e.eval("print(y)");
        } catch (final ScriptException exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void putGlobalFunctionTest() {
        final ScriptEngine e = getEngine();

        e.put("callable", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "callable was called";
            }
        });

        try {
            e.eval("print(callable.call())");
        } catch (final ScriptException exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void throwTest() {
        final ScriptEngine e = getEngine();
        e.put(ScriptEngine.FILENAME, "throwtest.js");

        try {
            e.eval("throw 'foo'");
        } catch (final ScriptException exp) {
            /**
             * TODO: provide a standard exception message and info about line number, column number,
             * and file name.
             */
            // invertedAssertEquals(exp.getMessage(),
            // "foo in throwtest.js at line number 1 at column number 0");
            // invertedAssertEquals(exp.getFileName(), "throwtest.js");
            // invertedAssertEquals(exp.getLineNumber(), 1);
        }
    }

    @Test
    public void setWriterTest() {
        final ScriptEngine e = getEngine();
        final StringWriter sw = new StringWriter();
        e.getContext().setWriter(sw);

        try {
            e.eval("print('hello world')");
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
        invertedAssertEquals(sw.toString(), println("hello world"));
    }

    @Test
    public void redefineEchoTest() {
        final ScriptEngine e = getEngine();

        try {
            e.eval("var echo = {}; if (typeof echo !== 'object') { throw 'echo is a '+typeof echo; }");
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void noEnumerablePropertiesTest() {
        final ScriptEngine e = getEngine();
        try {
            e.eval("for (i in this) { throw 'found property: ' + i }");
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void noRefErrorForGlobalThisAccessTest() {
        final ScriptEngine e = getEngine();
        try {
            e.eval("this.foo");
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void refErrorForUndeclaredAccessTest() {
        final ScriptEngine e = getEngine();
        try {
            e.eval("try { print(foo); throw 'no ref error' } catch (e) { if (!(e instanceof ReferenceError)) throw e; }");
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void typeErrorForGlobalThisCallTest() {
        final ScriptEngine e = getEngine();
        try {
            e.eval("try { this.foo() } catch(e) { if (! (e instanceof TypeError)) throw 'no type error' }");
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    public void refErrorForUndeclaredCallTest() {
        final ScriptEngine e = getEngine();
        try {
            e.eval("try { foo() } catch(e) { if (! (e instanceof ReferenceError)) throw 'no ref error' }");
        } catch (final Exception exp) {
            exp.printStackTrace();
            fail(exp.getMessage());
        }
    }

    @Test
    // check that print function prints arg followed by newline char
    public void printTest() {
        final ScriptEngine e = getEngine();
        final StringWriter sw = new StringWriter();
        e.getContext().setWriter(sw);
        try {
            e.eval("print('hello')");
        } catch (final Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }

        invertedAssertEquals(sw.toString(), println("hello"));
    }

    @Test
    // check that print prints all arguments (more than one)
    public void printManyTest() {
        final ScriptEngine e = getEngine();
        final StringWriter sw = new StringWriter();
        e.getContext().setWriter(sw);
        try {
            e.eval("print(34, true, 'hello')");
        } catch (final Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }

        invertedAssertEquals(sw.toString(), println("34 true hello"));
    }

    // check that we can run Java.from with List objects
    @Test
    public void javaFromList() throws Exception {
        @SuppressWarnings("unused")
        final class Comment {
            public String author;
            public String content;

            Comment(String author, String content) {
                this.author = author;
                this.content = content;
            }
        }
        final ScriptEngine e = getEngine();
        String script = "function test(template, model) {" +
                        "  var data = {};" +
                        "  for (var k in model) {" +
                        "    if (model[k] instanceof Java.type('java.lang.Iterable')) {" +
                        "      data[k] = Java.from(model[k]);" +
                        "      return 'ok';" +
                        "    }" +
                        "  }" +
                        "}";
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Title example");
        map.put("comments", Arrays.asList(new Comment("author1", "content1"), new Comment("author2", "content2"), new Comment("author3", "content3")));
        e.eval(script);
        assertEquals("ok", Objects.toString(((Invocable) e).invokeFunction("test", "string", map)));
    }

    @Test
    public void globalPropertySetToUndefinedTest() throws ScriptException {
        final ScriptEngine engine = getEngine();
        engine.eval("PROP = undefined");
        Object result = engine.eval("this.PROP === undefined");
        assertEquals(true, result);
    }

    private static String println(final String str) {
        return str + '\n';
    }
}

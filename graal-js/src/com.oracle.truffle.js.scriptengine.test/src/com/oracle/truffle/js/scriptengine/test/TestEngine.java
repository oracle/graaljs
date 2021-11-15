/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

public class TestEngine {

    static final String TESTED_ENGINE_NAME = "JavaScript";

    private final ScriptEngineManager manager = new ScriptEngineManager();

    @Rule public ExpectedException expectedException = ExpectedException.none();

    private ScriptEngine getEngine() {
        return manager.getEngineByName(TESTED_ENGINE_NAME);
    }

    @Test
    public void checkName() {
        assertEquals(getEngine().getClass().getName(), GraalJSScriptEngine.class.getName());
    }

    @Test
    public void compileAndEval1() throws ScriptException {
        assertEquals(true, getEngine().eval("true"));
    }

    @Test
    public void compileAndEval2() throws ScriptException {
        assertEquals(true, ((Compilable) getEngine()).compile("true").eval());
    }

    @Test
    public void compileSyntaxError() throws ScriptException {
        expectedException.expect(ScriptException.class);
        ((Compilable) getEngine()).compile(":-(");
    }

    @Test
    public void declareVar() throws ScriptException {
        // @formatter:off
        ScriptEngine engine = getEngine();
        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("polyglot.js.allowHostAccess", true);
        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("polyglot.js.allowHostClassLookup", true);
        assertEquals(true, engine.eval(
                        "var m = new javax.script.ScriptEngineManager();" +
                        "var engine = m.getEngineByName('Graal.js');" +
                        "var x;" +
                        "engine.eval('var x = \"ENGINE\"');" +
                        "typeof x == 'undefined'"
        ));
        // @formatter:on
    }

    @Test
    @Ignore("We do not support `engine.class.static.getProperty`")
    public void getProperty() throws ScriptException {
        // @formatter:off
        assertEquals(true, getEngine().eval(
                        "var m = new javax.script.ScriptEngineManager();" +
                        "var engine = m.getEngineByName('Graal.js');" +
                        "var obj = {prop: 'value'};" +
                        "engine.class.static.getProperty(obj, 'prop') == 'value';"
        ));
        // @formatter:on
    }

    @Test
    public void evalWithGlobal() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("x", 42);
        boolean result = (boolean) engine.eval("x === 42;");
        assertTrue(result);
    }

    @Test
    public void getPolyglotContextEvalWithGlobal() throws IOException, ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("x", 42);
        engine.eval("true"); // calls importScriptEngineGlobalBindings
        boolean result = ((GraalJSScriptEngine) engine).getPolyglotContext().eval(Source.newBuilder("js", "x === 42;", "src").build()).asBoolean();
        assertTrue(result);
    }

    @Test
    public void getPolyglotContextEvalWithGlobalFail() throws IOException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("x", 42);
        boolean result = ((GraalJSScriptEngine) engine).getPolyglotContext().eval(Source.newBuilder("js", "typeof x === \"undefined\";", "src").build()).asBoolean();
        assertTrue(result);
    }

    @Test
    public void getPolyglotContextEvalWithGlobalManualCall() throws IOException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("x", 42);
        // manually call importScriptEngineGlobalBindings
        ((GraalJSScriptEngine) engine).getPolyglotContext().getBindings("js").getMember("importScriptEngineGlobalBindings").execute(bindings);
        boolean result = ((GraalJSScriptEngine) engine).getPolyglotContext().eval(Source.newBuilder("js", "x === 42;", "src").build()).asBoolean();
        assertTrue(result);
    }

    @Test
    public void invokeFunctionWithGlobal() throws ScriptException, NoSuchMethodException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("x", 42);
        engine.eval("function foo() { return x === 42; }");
        boolean result = (boolean) ((Invocable) engine).invokeFunction("foo");
        assertTrue(result);
    }

    @Test
    public void invokeMethodWithGlobal() throws ScriptException, NoSuchMethodException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("x", 42);
        engine.eval("var obj = {f: () => { return x === 42; }};");
        boolean result = (boolean) ((Invocable) engine).invokeMethod(engine.eval("obj"), "f");
        assertTrue(result);
    }

    @Test
    public void factoryGetParameterTest() {
        ScriptEngineFactory factory = getEngine().getFactory();

        // Verify requirements from the JavaDoc of ScriptEngineFactory.getParameter()
        assertEquals(factory.getEngineName(), factory.getParameter(ScriptEngine.ENGINE));
        assertEquals(factory.getEngineVersion(), factory.getParameter(ScriptEngine.ENGINE_VERSION));
        assertEquals(factory.getLanguageName(), factory.getParameter(ScriptEngine.LANGUAGE));
        assertEquals(factory.getLanguageVersion(), factory.getParameter(ScriptEngine.LANGUAGE_VERSION));
        assertTrue(factory.getNames().contains(factory.getParameter(ScriptEngine.NAME)));

        // concurrent execution of scripts is not supported
        assertEquals(null, factory.getParameter("THREADING"));

        // null is returned for unknown parameters (i.e. no exception is thrown)
        assertEquals(null, factory.getParameter("noValueIsAssignedToThisKey"));
    }

    @Test
    public void twoNashornEngines() throws ScriptException {
        // Checks for a regression that causes the initialization of the second
        // ScriptEngine to fail.
        assertEquals("foo", TestUtil.getEngineNashornCompat(manager).eval("'foo'"));
        assertEquals("bar", TestUtil.getEngineNashornCompat(manager).eval("'bar'"));
    }

    @Test
    public void moduleWithDependency() throws IOException, ScriptException {
        ScriptEngine engine = getEngine();
        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("polyglot.js.allowIO", true);

        File dependency = File.createTempFile("dependency", ".mjs");
        dependency.deleteOnExit();
        try (FileWriter writer = new FileWriter(dependency)) {
            writer.append("export let answer = 42;");
        }

        String mainModuleName = new File(dependency.getParent(), "main.mjs").getAbsolutePath();
        String mainModule = "import { answer } from './" + dependency.getName() + "'; answer;";
        engine.getContext().setAttribute(ScriptEngine.FILENAME, mainModuleName, ScriptContext.ENGINE_SCOPE);

        Object result = engine.eval(mainModule);

        assertSame(42, ((Number) result).intValue());
    }

    @Test
    public void unicodeOutput() throws ScriptException {
        String text = "Tu\u010d\u0148\u00e1\u010d\u010d\u00ed \ud83d\udca9!";
        ScriptEngine engine = getEngine();
        StringWriter output = new StringWriter();
        engine.getContext().setWriter(output);
        engine.eval("print('" + text + "');");
        assertEquals(text + '\n', output.toString());
    }

    @Test
    public void unicodeInput() throws ScriptException {
        String text = "Tu\u010d\u0148\u00e1\u010d\u010d\u00ed \ud83d\udca9!";
        ScriptEngine engine = GraalJSScriptEngine.create(
                        Engine.newBuilder().build(),
                        Context.newBuilder("js").allowExperimentalOptions(true).option("js.shell", "true"));
        StringReader input = new StringReader(text);
        engine.getContext().setReader(input);
        Object result = engine.eval("readline()");
        assertEquals(text, result);
    }

    @Test
    public void exceptionInCauseChain() {
        try {
            TestUtil.getEngineNashornCompat(manager).eval("new java.util.ArrayList().iterator().next()");
            fail("ScriptException/NoSuchElementException expected");
        } catch (Exception ex) {
            boolean foundNoSuchElementException = false;
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof NoSuchElementException) {
                    foundNoSuchElementException = true;
                    break;
                }
                cause = cause.getCause();
            }
            assertTrue(foundNoSuchElementException);
        }
    }

    @Test
    public void noNullInNashornEngine() {
        ScriptEngine engine = manager.getEngineByName("nashorn");
        if (engine != null) {
            ScriptEngineFactory factory = engine.getFactory();
            assertEquals(-1, factory.getNames().indexOf(null));
            assertEquals(-1, factory.getMimeTypes().indexOf(null));
            assertEquals(-1, factory.getExtensions().indexOf(null));
        } // else no nashorn engine anymore
    }

}

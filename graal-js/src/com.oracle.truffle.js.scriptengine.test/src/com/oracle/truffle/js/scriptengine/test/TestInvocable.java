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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Ignore;
import org.junit.Test;

public class TestInvocable {

    private final ScriptEngineManager manager = new ScriptEngineManager();

    private ScriptEngine getEngine() {
        return manager.getEngineByName(TestEngine.TESTED_ENGINE_NAME);
    }

    @Test
    public void invokeMethod() throws ScriptException, NoSuchMethodException {
        ScriptEngine engine = getEngine();
        Invocable inv = (Invocable) engine;

        String functionName = "fun";
        String arg = "arg";
        Object obj = engine.eval("var obj = {" + functionName + ": function(arg) { return arg; }}; obj;");

        assertSame(arg, inv.invokeMethod(obj, functionName, arg));
    }

    @Test
    public void invokeFunction() throws ScriptException, NoSuchMethodException {
        ScriptEngine engine = getEngine();
        Invocable inv = (Invocable) engine;

        String functionName = "fun";
        String arg = "arg";
        engine.eval("var " + functionName + " = function(arg) { return arg; };");

        assertSame(arg, inv.invokeFunction(functionName, arg));
    }

    @Test(expected = NoSuchMethodException.class)
    public void invokeGlobal() throws ScriptException, NoSuchMethodException {
        ScriptEngine engine = getEngine();
        Invocable inv = (Invocable) engine;

        String functionName = "fun";
        Object obj = engine.eval(functionName + " = function() { }; var obj = { }; obj;");
        // Should throw NoSuchMethodException.
        inv.invokeMethod(obj, functionName, (Object[]) null);

        fail();
    }

    @Test
    @Ignore("Not supported by the polyglot API.")
    public void getInterface1() throws Exception {
        ScriptEngine engine = getEngine();
        Invocable inv = (Invocable) engine;

        String arg = "arg";
        engine.eval("function call() { return '" + arg + "'; }");
        Callable<?> c = inv.getInterface(Callable.class);

        assertEquals(arg, c.call());
    }

    @Test
    @Ignore("Not supported by the polyglot API.")
    public void getInterface2() throws Exception {
        ScriptEngine engine = getEngine();
        Invocable inv = (Invocable) engine;

        String arg = "arg";
        Object obj = engine.eval("var obj = { call: function() { return '" + arg + "'; } }; obj;");
        Callable<?> c = inv.getInterface(obj, Callable.class);

        assertEquals(arg, c.call());
    }
}

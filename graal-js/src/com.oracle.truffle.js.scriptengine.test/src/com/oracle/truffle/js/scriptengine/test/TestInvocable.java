/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.scriptengine.test;

import static org.junit.Assert.*;

import javax.script.*;

import org.junit.*;

public class TestBindings {
    private static final String argsName = "arguments";
    private static final String[] defaultArgs = new String[]{"arg0", "arg1"};
    private static final String varName = "boundVarName";
    private static final String defaultVarValue = "default";
    private static final String updatedVarValue = "updated";

    private final ScriptEngineManager manager = new ScriptEngineManager();

    private ScriptEngine getEngine() {
        return manager.getEngineByName(TestEngine.TESTED_ENGINE_NAME);
    }

    @Test
    public void enginePut() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.put(varName, defaultVarValue);
        boolean result = (boolean) engine.eval(varName + " === '" + defaultVarValue + "';");
        assertTrue(result);
    }

    @Test
    public void bindingsPut() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(varName, defaultVarValue);
        boolean result = (boolean) engine.eval(varName + " === '" + defaultVarValue + "';");
        assertTrue(result);
    }

    @Test
    public void updateEngineBindings() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(varName, defaultVarValue);
        engine.eval(varName + " = '" + updatedVarValue + "';");
        assertEquals(updatedVarValue, bindings.get(varName));
        assertEquals(updatedVarValue, engine.get(varName));
    }

    @Test
    public void updateGlobalBindings() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put(varName, defaultVarValue);
        engine.eval(varName + " = '" + updatedVarValue + "';");
        assertEquals(defaultVarValue, bindings.get(varName));
        assertEquals(updatedVarValue, engine.get(varName));
    }

    @Test
    public void overrideEngineBindings1() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.eval("var " + varName + " = '" + defaultVarValue + "';");
        engine.put(varName, updatedVarValue);
        assertEquals(updatedVarValue, engine.eval(varName));
    }

    @Test
    public void overrideEngineBindings2() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.eval("var " + varName + " = '" + defaultVarValue + "';");
        Bindings bindings = new SimpleBindings();
        bindings.put(varName, updatedVarValue);
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        assertEquals(updatedVarValue, engine.eval(varName));
    }

    @Test
    public void overrideGlobalBindings() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.eval("var " + varName + " = '" + defaultVarValue + "';");
        engine.getBindings(ScriptContext.GLOBAL_SCOPE).put(varName, updatedVarValue);
        assertEquals(defaultVarValue, engine.eval(varName));
    }

    @Test
    public void setBindings1() throws ScriptException {
        Bindings bindings = new SimpleBindings();
        bindings.put(varName, defaultVarValue);
        ScriptEngine engine = getEngine();
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        assertEquals(true, engine.eval(varName + " === '" + defaultVarValue + "';"));
    }

    @Test
    public void setBindings2() throws ScriptException {
        Bindings bindings = new SimpleBindings();
        bindings.put(varName, defaultVarValue);
        ScriptEngine engine = getEngine();
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        engine.eval(varName + " = '" + updatedVarValue + "';");
        assertEquals(bindings.get(varName), defaultVarValue);
        assertEquals(engine.get(varName), defaultVarValue);
    }

    @Test
    public void setBindings3() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.put(varName, null);
        System.out.println(engine.eval(varName));
        assertEquals(true, engine.eval(varName + " === null;"));
    }

    @Test
    public void engineGetDeclared() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.eval("var " + varName + " = '" + defaultVarValue + "';");
        assertEquals(defaultVarValue, engine.get(varName));
        assertEquals(1, engine.getBindings(ScriptContext.ENGINE_SCOPE).size());
    }

    @Test
    public void clearBindings1() {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(varName, defaultVarValue);
        assertEquals(1, bindings.size());
        assertEquals(defaultVarValue, bindings.get(varName));
        bindings.clear();
        assertEquals(0, bindings.size());
        assertEquals(null, bindings.get(varName));
    }

    @Test
    public void clearBindings2() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(varName, defaultVarValue);
        engine.eval("var " + varName + " = '" + updatedVarValue + "';");
        assertEquals(1, bindings.size());
        assertEquals(updatedVarValue, bindings.get(varName));
        bindings.clear();
        assertEquals(0, bindings.size());
        assertEquals(null, bindings.get(varName));
    }

    @Test
    public void clearNoBindings() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.eval("var " + varName + " = '" + defaultVarValue + "';");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.clear();
        assertEquals(1, bindings.size());
        assertEquals(defaultVarValue, bindings.get(varName));
    }

    @Test
    public void engineOverrideGlobal() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings engineB = engine.createBindings();
        Bindings globalB = engine.createBindings();
        engineB.put(varName, updatedVarValue);
        globalB.put(varName, defaultVarValue);
        engine.setBindings(engineB, ScriptContext.ENGINE_SCOPE);
        engine.setBindings(globalB, ScriptContext.GLOBAL_SCOPE);
        assertEquals(updatedVarValue, engine.eval(varName));
    }

    @Test
    public void setArgs1() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.put(argsName, defaultArgs);
        assertEquals(defaultArgs[0], engine.eval(argsName + "[0];"));
        assertEquals(defaultArgs[1], engine.eval(argsName + "[1];"));
        assertEquals(true, engine.eval("(function() { for (var idx in this) { if (idx == '" + argsName + "') { return false; } }; return true; })();"));
    }

    @Test
    public void setArgs2() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = new SimpleBindings();
        bindings.put(argsName, defaultArgs);
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        assertEquals(defaultArgs[0], engine.eval(argsName + "[0];"));
        assertEquals(defaultArgs[1], engine.eval(argsName + "[1];"));
        assertEquals(true, engine.eval("(function() { for (var idx in this) { if (idx == '" + argsName + "') { return false; } }; return true; })();"));
    }
}

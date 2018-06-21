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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.Test;

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
        assertNull(engine.get(varName));
        assertNull(engine.eval(varName));
        assertEquals(true, engine.eval(varName + " === null;"));
    }

    @Test
    public void engineGetDeclared() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        int sizeBefore = bindings.size();
        engine.eval("var " + varName + " = '" + defaultVarValue + "';");
        assertEquals(defaultVarValue, engine.get(varName));
        int sizeAfter = bindings.size();
        assertEquals(1, sizeAfter - sizeBefore);
    }

    @Test
    public void clearBindings1() {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        int sizeBeforePut = bindings.size();
        bindings.put(varName, defaultVarValue);
        int sizeAfterPut = bindings.size();
        assertEquals(1, sizeAfterPut - sizeBeforePut);
        assertEquals(defaultVarValue, bindings.get(varName));
        int sizeBeforeClear = bindings.size();
        bindings.clear();
        int sizeAfterClear = bindings.size();
        assertEquals("One binding cleared", 1, sizeBeforeClear - sizeAfterClear);
        assertEquals(null, bindings.get(varName));
    }

    @Test
    public void clearBindings2() throws ScriptException {
        ScriptEngine engine = getEngine();
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        int sizeBeforePut = bindings.size();
        bindings.put(varName, defaultVarValue);
        int sizeAfterPut = bindings.size();
        assertEquals(1, sizeAfterPut - sizeBeforePut);
        engine.eval("var " + varName + " = '" + updatedVarValue + "';");
        assertEquals(sizeAfterPut, bindings.size());
        assertEquals(updatedVarValue, bindings.get(varName));
        int sizeBeforeClear = bindings.size();
        bindings.clear();
        int sizeAfterClear = bindings.size();
        assertEquals("One binding cleared", 1, sizeBeforeClear - sizeAfterClear);
        assertEquals(null, bindings.get(varName));
    }

    @Test
    public void clearNoBindings() throws ScriptException {
        ScriptEngine engine = getEngine();
        engine.eval("var " + varName + " = '" + defaultVarValue + "';");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        int sizeBeforeClear = bindings.size();
        bindings.clear();
        int sizeAfterClear = bindings.size();
        assertEquals("No bindings cleared", sizeBeforeClear, sizeAfterClear);
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
    }
}

/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.scriptengine.test;

import static org.junit.Assert.assertEquals;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

public class TestEngine {

    static final String TESTED_ENGINE_NAME = "JavaScript";

    private final ScriptEngineManager manager = new ScriptEngineManager();

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
    public void declareVar() throws ScriptException {
        // @formatter:off
        assertEquals(true, getEngine().eval(
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
}

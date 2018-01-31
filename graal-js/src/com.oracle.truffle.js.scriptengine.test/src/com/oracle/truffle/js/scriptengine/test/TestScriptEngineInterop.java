/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.scriptengine.test;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

public class TestScriptEngineInterop {

    @Test
    public void testInterop() throws ScriptException {
        GraalJSScriptEngine engine = GraalJSScriptEngine.create(null, Context.newBuilder("js"));
        Object result = engine.eval("a = 42");
        Assert.assertEquals(42, result);
        Assert.assertEquals(42, engine.get("a"));
        engine.getContext().setAttribute("a", 43, ScriptContext.ENGINE_SCOPE);
        Assert.assertEquals(43, engine.getPolyglotContext().lookup("js", "a").asInt());

        SimpleScriptContext context = new SimpleScriptContext();

        Assert.assertEquals(43, engine.eval("this['a']"));
        // creates a new context
        Assert.assertNull(engine.eval("this['a']", context));
        Assert.assertEquals(43, engine.eval("this['a']"));

        Assert.assertNotSame(engine.getPolyglotContext(), engine.getPolyglotContext(context));
        Assert.assertSame(engine.getPolyglotContext().getEngine(), engine.getPolyglotContext(context).getEngine());
        Assert.assertSame(engine.getPolyglotEngine(), engine.getPolyglotContext().getEngine());
    }

    @Test
    public void testDirectPolyglotContextAccess() {
        GraalJSScriptEngine engine = GraalJSScriptEngine.create();
        SimpleScriptContext context = new SimpleScriptContext();
        Assert.assertNotNull(engine.getPolyglotContext(context));
    }

}

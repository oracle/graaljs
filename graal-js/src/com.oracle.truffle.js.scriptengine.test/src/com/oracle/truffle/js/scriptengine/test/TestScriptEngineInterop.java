/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.Callable;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.graalvm.polyglot.Engine;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

public class TestScriptEngineInterop {

    private static final String ID = "js";

    @Test
    public void testCustomBuilder() throws ScriptException {
        Engine graalEngine = Engine.newBuilder().build();
        GraalJSScriptEngine graalJSScriptEngine = GraalJSScriptEngine.create(graalEngine,
                        TestUtil.newContextBuilder().engine(graalEngine));
        Assert.assertEquals(42, graalJSScriptEngine.eval("42"));

        graalEngine.close();
    }

    @Test
    public void testInterop() throws ScriptException {
        GraalJSScriptEngine engine = GraalJSScriptEngine.create(null, TestUtil.newContextBuilder());
        Object result = engine.eval("a = 42");
        Assert.assertEquals(42, result);
        Assert.assertEquals(42, engine.get("a"));
        engine.getContext().setAttribute("a", 43, ScriptContext.ENGINE_SCOPE);
        Assert.assertEquals(43, engine.getPolyglotContext().getBindings(ID).getMember("a").asInt());

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

    interface TestInterface {

    }

    @Test
    public void testClose() throws ScriptException {
        GraalJSScriptEngine engine = GraalJSScriptEngine.create();
        Object obj = engine.eval("({})");
        engine.close();

        expectIllegalState(() -> engine.eval(""));
        expectIllegalState(() -> engine.compile(""));
        expectIllegalState(() -> engine.get("key"));
        expectIllegalState(() -> {
            engine.put("key", "");
            return null;
        });
        expectIllegalState(() -> {
            engine.invokeFunction("");
            return null;
        });
        expectIllegalState(() -> {
            engine.invokeMethod(obj, "");
            return null;
        });
        expectIllegalState(() -> {
            engine.getInterface(TestInterface.class);
            return null;
        });
    }

    private static void expectIllegalState(Callable<?> r) {
        try {
            r.call();
            Assert.fail("expected illegal state");
        } catch (IllegalStateException e) {
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}

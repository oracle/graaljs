/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.scriptengine.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

public class JSONCompatibleTest {

    private final ScriptEngineManager manager = new ScriptEngineManager();

    private ScriptEngine getEngine() {
        return manager.getEngineByName(TestEngine.TESTED_ENGINE_NAME);
    }

    /**
     * Wrap a top-level array as a list.
     */
    @Test
    public void testWrapArray() throws ScriptException {
        final ScriptEngine engine = getEngine();
        final Object val = engine.eval("Java.asJSONCompatible([1, 2, 3])");
        assertEquals(asList(val), Arrays.asList(1, 2, 3));
    }

    /**
     * Wrap an embedded array as a list.
     */
    @Test
    public void testWrapObjectWithArray() throws ScriptException {
        final ScriptEngine engine = getEngine();
        final Object val = engine.eval("Java.asJSONCompatible({x: [1, 2, 3]})");
        assertEquals(asList(asMap(val).get("x")), Arrays.asList(1, 2, 3));
    }

    /**
     * Check it all works transitively several more levels down.
     */
    @Test
    public void testDeepWrapping() throws ScriptException {
        final ScriptEngine engine = getEngine();
        final Object val = engine.eval("Java.asJSONCompatible({x: [1, {y: [2, {z: [3]}]}, [4, 5]]})");
        final Map<String, Object> root = asMap(val);
        final List<Object> x = asList(root.get("x"));
        assertEquals(x.get(0), 1);
        final Map<String, Object> x1 = asMap(x.get(1));
        final List<Object> y = asList(x1.get("y"));
        assertEquals(y.get(0), 2);
        final Map<String, Object> y1 = asMap(y.get(1));
        assertEquals(asList(y1.get("z")), Arrays.asList(3));
        assertEquals(asList(x.get(2)), Arrays.asList(4, 5));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(final Object obj) {
        assertThat(obj, instanceOf(List.class));
        return (List<Object>) obj;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final Object obj) {
        assertThat(obj, instanceOf(Map.class));
        return (Map<String, Object>) obj;
    }

}

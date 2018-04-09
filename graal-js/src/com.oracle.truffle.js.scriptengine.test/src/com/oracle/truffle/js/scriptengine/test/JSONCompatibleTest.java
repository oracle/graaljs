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

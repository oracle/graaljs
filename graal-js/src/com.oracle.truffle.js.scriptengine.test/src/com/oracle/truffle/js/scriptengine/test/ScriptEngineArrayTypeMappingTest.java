/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.scriptengine.GraalJSEngineFactory;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

public class ScriptEngineArrayTypeMappingTest {

    /**
     * Regression test for https://github.com/oracle/graaljs/issues/214.
     *
     * A JavaScript array, when passed to Java via ScriptEngine is converted to a {@link List}.
     */
    @Test
    public void testJavaScriptArrayViaScriptEngine() throws ScriptException {
        try (GraalJSScriptEngine engine = new GraalJSEngineFactory().getScriptEngine()) {
            Object result = engine.eval("['a', 'b', 'c']");

            Assert.assertTrue(result instanceof List);
            Assert.assertFalse(result instanceof Map);
            Assert.assertEquals(3, ((List<?>) result).size());
        }
    }

    /**
     * {@link HostAccess.Builder#targetTypeMapping} can be used to explicitly convert arrays (lists)
     * to {@link List} when the target type is {@link Object}.
     */
    @Test
    public void testJavaScriptArrayViaScriptEngineExplicit() throws ScriptException {
        HostAccess hostAccess = HostAccess.newBuilder(HostAccess.ALL).targetTypeMapping(List.class, Object.class, null, v -> v).build();
        try (GraalJSScriptEngine engine = GraalJSScriptEngine.create(null, Context.newBuilder("js").allowHostAccess(hostAccess))) {
            Object result = engine.eval("['a', 'b', 'c']");

            Assert.assertTrue(result instanceof List);
            Assert.assertFalse(result instanceof Map);
            Assert.assertEquals(3, ((List<?>) result).size());
        }
    }

    /**
     * {@link HostAccess.Builder#targetTypeMapping} can be used to represent arrays as {@link Map}
     * (this was the default behavior before GraalVM 22.1).
     */
    @Test
    public void testJavaScriptArrayViaScriptEngineAsMap() throws ScriptException {
        HostAccess hostAccess = HostAccess.newBuilder(HostAccess.ALL).targetTypeMapping(Value.class, Object.class,
                        v -> v.hasArrayElements(),
                        v -> v.as(Map.class)).build();
        try (GraalJSScriptEngine engine = GraalJSScriptEngine.create(null, Context.newBuilder("js").allowHostAccess(hostAccess))) {
            Object result = engine.eval("['a', 'b', 'c']");

            Assert.assertFalse(result instanceof List);
            Assert.assertTrue(result instanceof Map);
            Assert.assertEquals(0, ((Map<?, ?>) result).size());
        }
    }

    /**
     * {@link HostAccess.Builder#targetTypeMapping} can be used to represent arrays as {@link Map}
     * with numeric keys.
     */
    @Test
    public void testJavaScriptArrayViaScriptEngineAsMapLong() throws ScriptException {
        HostAccess hostAccess = HostAccess.newBuilder(HostAccess.ALL).targetTypeMapping(Value.class, Object.class,
                        v -> v.hasArrayElements(),
                        v -> v.as(new TypeLiteral<Map<Long, Object>>() {
                        })).build();
        try (GraalJSScriptEngine engine = GraalJSScriptEngine.create(null, Context.newBuilder("js").allowHostAccess(hostAccess))) {
            Object result = engine.eval("['a', 'b', 'c']");

            Assert.assertFalse(result instanceof List);
            Assert.assertTrue(result instanceof Map);
            Assert.assertEquals(3, ((Map<?, ?>) result).size());
        }
    }
}

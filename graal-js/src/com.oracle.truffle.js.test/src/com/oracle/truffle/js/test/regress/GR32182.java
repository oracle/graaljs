/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.regress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class GR32182 {

    @Test
    public void testJavaMapDelete() {
        try (Context ctx = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder(HostAccess.ALL).allowMapAccess(true).build()).build()) {
            Map<Object, Object> myMap = new HashMap<>();
            myMap.put("A", 1);
            myMap.put("B", 2);
            ctx.getBindings("js").putMember("myMap", myMap);

            assertTrue(myMap.keySet().toString(), myMap.keySet().contains("A"));
            Value result = ctx.eval("js", "delete myMap.A; myMap.A");
            assertTrue(result.toString(), result.isNull());
            assertTrue(myMap.keySet().toString(), !myMap.keySet().contains("A"));
            assertEquals(myMap.keySet().toString(), 1, myMap.keySet().size());
        }
    }

    @Test
    public void testJavaMapDeleteNumber() {
        try (Context ctx = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder(HostAccess.ALL).allowMapAccess(true).build()).build()) {
            Map<String, Object> map = new HashMap<>();
            ctx.getBindings("js").putMember("map", map);

            assertEquals(0, ctx.eval("js", "map['foo'] = 'someValue'; delete map['foo']; map.size();").asInt());
            assertEquals(0, ctx.eval("js", "map[42] = 'someValue'; delete map[42]; map.size()\n").asInt());

            map.clear();

            ctx.getBindings("js").putMember("map", ProxyObject.fromMap(map));
            ctx.eval("js", "map['foo'] = 'someValue';");
            assertEquals(1, map.size());
            ctx.eval("js", "delete map['foo'];");
            assertEquals(0, map.size());
            ctx.eval("js", "map[42] = 'someValue';");
            assertEquals(1, map.size());
            ctx.eval("js", "delete map[42];");
            assertEquals(0, map.size());
        }
    }

    @Test
    public void testJavaListDeleteNumber() {
        try (Context ctx = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder(HostAccess.ALL).allowMapAccess(true).build()).build()) {
            ArrayList<Object> array = new ArrayList<>();
            ctx.getBindings("js").putMember("array", array);
            array.add("element");

            assertEquals("element", ctx.eval("js", "array[0];").asString());
            assertEquals("element", ctx.eval("js", "array['0'];").asString());

            assertFalse(ctx.eval("js", "delete array[0];").asBoolean());
            assertFalse(ctx.eval("js", "delete array['0'];").asBoolean());

            assertEquals(1, array.size());
            assertEquals("element", array.get(0));

            ctx.eval("js", "array[0] = 'element0';");
            assertEquals("element0", array.get(0));

            ctx.eval("js", "array['0'] = 'element1';");
            assertEquals("element1", array.get(0));
        }
    }

    @Test
    public void testJavaMapReadWriteNumericIndexString() {
        try (Context ctx = JSTest.newContextBuilder().allowHostAccess(HostAccess.newBuilder(HostAccess.ALL).allowMapAccess(true).build()).build()) {
            Map<String, Object> map = new HashMap<>();
            ctx.getBindings("js").putMember("map", map);

            Value result;
            ctx.eval("js", "map['42'] = 'someValue';");
            assertEquals("someValue", ctx.eval("js", "map['42'];").asString());
            result = ctx.eval("js", "map[42];");
            assertTrue(result.toString(), result.isNull());

            map.clear();

            ctx.eval("js", "map[42] = 'someValue';");
            assertEquals("someValue", ctx.eval("js", "map[42];").asString());
            result = ctx.eval("js", "map['42'];");
            assertTrue(result.toString(), result.isNull());
        }
    }
}

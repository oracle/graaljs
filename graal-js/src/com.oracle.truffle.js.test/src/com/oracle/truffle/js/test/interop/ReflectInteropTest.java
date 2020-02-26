/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class ReflectInteropTest {
    private Context context;

    @Before
    public void setUp() {
        context = JSTest.newContextBuilder().build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void reflectApply() {
        ProxyExecutable function = (args) -> 40 + args.length;
        Value result = context.eval(ID, "(f, ...args) => Reflect.apply(f, null, args)").execute(function, "et", "al");
        assertTrue(result.fitsInInt());
        assertEquals(42, result.asInt());
    }

    @Test
    public void reflectConstruct() {
        ProxyInstantiable constructor = (args) -> ProxyArray.fromArray((Object[]) args);
        Value result = context.eval(ID, "(f, ...args) => Reflect.construct(f, args)").execute(constructor, "et", "al");
        assertTrue(result.hasArrayElements());
        assertEquals(2, result.getArraySize());
        assertEquals("et", result.getArrayElement(0).asString());
        assertEquals("al", result.getArrayElement(1).asString());
    }

    @Test
    public void reflectGet() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);

        Value result = context.eval(ID, "Reflect.get").execute(proxyObject, "p2");
        assertTrue(result.fitsInInt());
        assertEquals(42, result.asInt());
    }

    @Test
    public void reflectSet() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);

        context.eval(ID, "Reflect.set").execute(proxyObject, "p4", 44);
        assertTrue(map.containsKey("p4"));
        assertEquals(44, context.asValue(map.get("p4")).asInt());
    }

    @Test
    public void reflectHas() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);

        Value reflectHas = context.eval(ID, "Reflect.has");
        assertTrue(reflectHas.execute(proxyObject, "p2").asBoolean());
        assertTrue(reflectHas.execute(proxyObject, "p3").asBoolean());
        assertFalse(reflectHas.execute(proxyObject, "p4").asBoolean());
    }

    @Test
    public void reflectOwnKeys() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);

        Value reflectOwnKeys = context.eval(ID, "Reflect.ownKeys");
        assertEquals(Arrays.asList("p1", "p2", "p3"), reflectOwnKeys.execute(proxyObject).as(List.class));
    }

    @Test
    public void reflectDeleteProperty() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);

        Value reflectDelete = context.eval(ID, "Reflect.deleteProperty");
        assertTrue(reflectDelete.execute(proxyObject, "p1").asBoolean());
        assertFalse(reflectDelete.execute(proxyObject, "p1").asBoolean());
        assertEquals(2, map.size());
        assertTrue(map.containsKey("p2"));
        assertTrue(map.containsKey("p3"));
        assertFalse(reflectDelete.execute(proxyObject, 42).asBoolean());
    }
}

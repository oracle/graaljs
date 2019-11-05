/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProxyObjectTest {
    private Context context;

    @Before
    public void setUp() {
        context = Context.create(ID);
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void objectKeysValuesEntries() {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        ProxyObject proxyObject = ProxyObject.fromMap(map);

        context.getBindings(ID).putMember("obj", proxyObject);

        List<?> keys = context.eval(ID, "Object.keys(obj)").as(List.class);
        assertEquals(1, keys.size());
        assertTrue(keys.contains("foo"));

        List<?> values = context.eval(ID, "Object.values(obj)").as(List.class);
        assertEquals(1, values.size());
        assertTrue(values.contains("bar"));

        Value entries = context.eval(ID, "Object.entries(obj)");
        assertEquals(1, entries.getArraySize());
        assertEquals(2, entries.getArrayElement(0).getArraySize());
        assertEquals("foo", entries.getArrayElement(0).getArrayElement(0).asString());
        assertEquals("bar", entries.getArrayElement(0).getArrayElement(1).asString());
    }

    @Test
    public void copyElementsFromProxyArray() {
        ProxyArray proxyArr = ProxyArray.fromArray(41, 42, 43);
        context.getBindings(ID).putMember("proxyArr", proxyArr);
        context.eval(ID, "obj = [...proxyArr];");
        assertEquals(41, context.eval(ID, "obj[0]").asInt());
        assertEquals(42, context.eval(ID, "obj[1]").asInt());
        assertEquals(43, context.eval(ID, "obj[2]").asInt());
    }

    @Test
    public void copyPropertiesFromProxyObject() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);
        context.getBindings(ID).putMember("proxyObj", proxyObject);
        context.eval(ID, "obj = {...proxyObj};");
        assertEquals(41, context.eval(ID, "obj.p1").asInt());
        assertEquals(42, context.eval(ID, "obj.p2").asInt());
        assertEquals(43, context.eval(ID, "obj.p3").asInt());
    }

    @Test
    public void objectAssignFromProxyObject() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);
        context.getBindings(ID).putMember("proxyObj", proxyObject);
        context.eval(ID, "obj = Object.assign({}, proxyObj);");
        assertEquals(41, context.eval(ID, "obj.p1").asInt());
        assertEquals(42, context.eval(ID, "obj.p2").asInt());
        assertEquals(43, context.eval(ID, "obj.p3").asInt());
    }

    @Test
    public void objectAssignToProxyObject() {
        HashMap<String, Object> map = new HashMap<>();
        ProxyObject proxyObject = ProxyObject.fromMap(map);
        context.getBindings(ID).putMember("proxyObj", proxyObject);
        context.eval(ID, "obj = Object.assign(proxyObj, {p1: 41, p2: 42, p3: 43});");
        assertEquals(41, context.eval(ID, "obj.p1").asInt());
        assertEquals(42, context.eval(ID, "obj.p2").asInt());
        assertEquals(43, context.eval(ID, "obj.p3").asInt());
    }

    @Test
    public void definePropertyViaJSProxy() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("p1", 41);
        map.put("p2", 42);
        map.put("p3", 43);
        ProxyObject proxyObject = ProxyObject.fromMap(map);
        Value proxyObjWrapper = context.eval(ID, "(proxyObj) => new Proxy({}, {\n" +
                        "get: function(target, key) {\n" +
                        "  if (key in target) {\n" +
                        "    return target[key];\n" +
                        "  } else {\n" +
                        "    return proxyObj[key];\n" +
                        "  }\n" +
                        "}});");
        context.getBindings(ID).putMember("obj", proxyObjWrapper.execute(proxyObject));
        context.eval(ID, "" +
                        "Object.defineProperty(obj, 'foo', {\n" +
                        "  enumerable: true, configurable : false,\n" +
                        "  get: function() {\n" +
                        "    return 668;\n" +
                        "  }\n" +
                        "});");
        context.eval(ID, "" +
                        "Object.defineProperty(obj, 'p3', {\n" +
                        "  enumerable: true, configurable : false,\n" +
                        "  get: function() {\n" +
                        "    return 333;\n" +
                        "  }\n" +
                        "});");
        assertEquals(42, context.eval(ID, "obj.p2").asInt());
        assertEquals(668, context.eval(ID, "obj.foo").asInt());
        assertEquals(333, context.eval(ID, "obj.p3").asInt());
    }
}

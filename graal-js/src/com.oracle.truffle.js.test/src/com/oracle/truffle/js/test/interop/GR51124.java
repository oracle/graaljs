/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * Regression test of the consistency of delete, get and set operations on foreign objects.
 */
public class GR51124 {

    @Test
    public void testArrayDelete() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyArray array = ProxyArray.fromArray();
            context.getBindings(ID).putMember("array", array);
            context.eval(ID, "var proxyArray = new Proxy(array, {});");
            assertTrue(context.eval(ID, "delete array['foo']").asBoolean());
            assertTrue(context.eval(ID, "Reflect.deleteProperty(array, 'foo')").asBoolean());
            assertTrue(context.eval(ID, "delete proxyArray['foo']").asBoolean());
        }
    }

    @Test
    public void testMapDelete() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyHashMap map = ProxyHashMap.from(Map.of());
            context.getBindings(ID).putMember("map", map);
            context.eval(ID, "var proxyMap = new Proxy(map, {});");
            assertTrue(context.eval(ID, "delete map['foo']").asBoolean());
            assertTrue(context.eval(ID, "Reflect.deleteProperty(map, 'foo')").asBoolean());
            assertTrue(context.eval(ID, "delete proxyMap['foo']").asBoolean());
        }
    }

    @Test
    public void testArrayGet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String arrayItem = "someItem";
            ProxyArray array = ProxyArray.fromArray(arrayItem);
            context.getBindings(ID).putMember("array", array);
            context.eval(ID, "var proxyArray = new Proxy(array, {});");
            assertEquals(arrayItem, context.eval(ID, "array[0]").asString());
            assertEquals(arrayItem, context.eval(ID, "Reflect.get(array, 0)").asString());
            assertEquals(arrayItem, context.eval(ID, "proxyArray[0]").asString());
        }
    }

    @Test
    public void testMapGet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String mapKey = "klic";
            String mapValue = "hodnota";
            ProxyHashMap map = ProxyHashMap.from(Map.of(mapKey, mapValue));
            Value bindings = context.getBindings(ID);
            bindings.putMember("map", map);
            bindings.putMember("mapKey", mapKey);
            context.eval(ID, "var proxyMap = new Proxy(map, {});");
            assertEquals(mapValue, context.eval(ID, "map[mapKey]").asString());
            assertEquals(mapValue, context.eval(ID, "Reflect.get(map, mapKey)").asString());
            assertEquals(mapValue, context.eval(ID, "proxyMap[mapKey]").asString());
        }
    }

    @Test
    public void testArraySet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String oldItem = "someItem";
            String newItem = "anotherItem";
            ProxyArray array = ProxyArray.fromArray(oldItem);
            Value bindings = context.getBindings(ID);
            bindings.putMember("array", array);
            bindings.putMember("oldItem", oldItem);
            bindings.putMember("newItem", newItem);
            context.eval(ID, "var proxyArray = new Proxy(array, {});");

            context.eval(ID, "array[0] = newItem;");
            assertEquals(newItem, context.eval(ID, "array[0]").asString());
            context.eval(ID, "array[0] = oldItem;");

            context.eval(ID, "Reflect.set(array, 0, newItem);");
            assertEquals(newItem, context.eval(ID, "array[0]").asString());
            context.eval(ID, "array[0] = oldItem;");

            context.eval(ID, "proxyArray[0] = newItem;");
            assertEquals(newItem, context.eval(ID, "array[0]").asString());
            context.eval(ID, "array[0] = oldItem;");
        }
    }

    @Test
    public void testMapSet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String mapKey = "someKey";
            String oldValue = "someValue";
            String newValue = "anotherValue";
            Map<Object, Object> javaMap = new HashMap<>();
            javaMap.put(mapKey, oldValue);
            ProxyHashMap map = ProxyHashMap.from(javaMap);
            Value bindings = context.getBindings(ID);
            bindings.putMember("map", map);
            bindings.putMember("mapKey", mapKey);
            bindings.putMember("oldValue", oldValue);
            bindings.putMember("newValue", newValue);
            context.eval(ID, "var proxyMap = new Proxy(map, {});");

            context.eval(ID, "map[mapKey] = newValue;");
            assertEquals(newValue, context.eval(ID, "map[mapKey]").asString());
            context.eval(ID, "map[mapKey] = oldValue;");

            context.eval(ID, "Reflect.set(map, mapKey, newValue);");
            assertEquals(newValue, context.eval(ID, "map[mapKey]").asString());
            context.eval(ID, "map[mapKey] = oldValue;");

            context.eval(ID, "proxyMap[mapKey] = newValue;");
            assertEquals(newValue, context.eval(ID, "map[mapKey]").asString());
            context.eval(ID, "map[mapKey] = oldValue;");
        }
    }

}

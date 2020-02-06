/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * Various tests for accessing JavaScript object in Java and accessing Java Map as JavaScript object
 * in JavaScript.
 */
public class JavaScriptObjectInteropTest {
    private static final Map<String, Object> JAVA_MAP = new HashMap<>();

    static {
        JAVA_MAP.put("a", 5);
        JAVA_MAP.put("b", 5.6d);
        JAVA_MAP.put("c", "hello");
    }

    private static String convertMapEntry(Map.Entry<String, Object> entry) {
        StringBuilder sb = new StringBuilder(entry.getKey()).append(':');
        if (entry.getValue() instanceof Double) {
            sb.append(String.format(Locale.US, "%.1f", entry.getValue()));
        } else if (entry.getValue() instanceof String) {
            sb.append('"').append(entry.getValue()).append('"');
        } else {
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    private static final String JS_OBJECT_STRING = JAVA_MAP.entrySet().stream().map(JavaScriptObjectInteropTest::convertMapEntry).collect(Collectors.joining(", ", "{", "}"));

    public static class ToBePassedToJS {
        private Map<?, ?> map;

        @HostAccess.Export
        public void methodWithMapArgument(Map<?, ?> argMap) {
            this.map = argMap;
        }

        @HostAccess.Export
        public ProxyObject methodThatReturnsMap() {
            return ProxyObject.fromMap(JAVA_MAP);
        }
    }

    private static void mapsEqual(Map<?, ?> map1, Map<?, ?> map2) {
        assertEquals(map1.size(), map2.size());
        for (Map.Entry<?, ?> map1Entry : map1.entrySet()) {
            if (map1Entry.getValue() instanceof Double) {
                assertEquals((Double) map1Entry.getValue(), (Double) map2.get(map1Entry.getKey()), 0.0000000001d);
            } else {
                assertEquals(map1Entry.getValue(), map2.get(map1Entry.getKey()));
            }
        }
    }

    /**
     * Test that a JavaScript object can be evaluated via polyglot Context and read as Map from
     * Java.
     */
    @Test
    public void testBasic() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value v = context.eval(ID, "var o = " + JS_OBJECT_STRING + "; o;");
            Object o = v.as(Object.class);
            assertTrue(o instanceof Map<?, ?>);
            mapsEqual(JAVA_MAP, (Map<?, ?>) o);
        }
    }

    /**
     * Test that a JavaScript object can be passed as argument of Java function and read as Map from
     * Java. To be able to call a Java method, access to it must be allowed, e.g. by a method
     * annotation and passing the class of that annotation to
     * {@link HostAccess.Builder#allowAccessAnnotatedBy}.
     */
    @Test
    public void testMapAsParameter() {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            bindings.putMember("objectFromJava", objectFromJava);
            context.eval(ID, "objectFromJava.methodWithMapArgument(" + JS_OBJECT_STRING + ")");
            mapsEqual(JAVA_MAP, objectFromJava.map);
        }
    }

    /**
     * Test that a Java Map can be passed to JavaScript scope and accessed as JavaScript object in
     * JavaScript. Please note that {@link ProxyObject} must be used for this.
     */
    @Test
    public void testJavaMapInJS() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value bindings = context.getBindings(ID);
            bindings.putMember("javaMap", ProxyObject.fromMap(JAVA_MAP));
            Value v = context.eval(ID, "var recreatedObject = {};" +
                            "for(var key in javaMap) recreatedObject[key] = javaMap[key];" +
                            "recreatedObject");
            Object o = v.as(Object.class);
            assertTrue(o instanceof Map<?, ?>);
            mapsEqual(JAVA_MAP, (Map<?, ?>) o);
        }
    }

    /**
     * Test that a Java Map can be returned from a Java method called in JavaScript and accessed as
     * JavaScript object. Please note that {@link ProxyObject} must be used for this.
     */
    @Test
    public void testJavaReturnMapAsJSObject() {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).allowListAccess(
                        true).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            bindings.putMember("objectFromJava", objectFromJava);
            Value v = context.eval(ID, "var mapFromJava = objectFromJava.methodThatReturnsMap();" +
                            "var recreatedObject = {};" +
                            "for(var key in mapFromJava) recreatedObject[key] = mapFromJava[key];" +
                            "recreatedObject");
            Object o = v.as(Object.class);
            assertTrue(o instanceof Map<?, ?>);
            mapsEqual(JAVA_MAP, (Map<?, ?>) o);
        }
    }
}

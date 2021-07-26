/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * Testing behavior around Maps, Arrays, Lists and the conversion between them.
 *
 * Similar tests for the ScriptEngine are in com.oracle.truffle.js.scriptengine.test.MapTest.
 */
public class MapTest {

    @Test
    /**
     * Regression test for github.com/graalvm/graaljs/issues/214 is actually working when a Context
     * is used.
     */
    public void testJavaScriptArrayViaContext() {
        try (Context context = JSTest.newContextBuilder("js").build()) {
            Value result = context.eval("js", "['a', 'b', 'c']");

            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(3, result.getArraySize());
            Assert.assertEquals("a", result.getArrayElement(0).asString());
            Assert.assertEquals("b", result.getArrayElement(1).asString());
            Assert.assertEquals("c", result.getArrayElement(2).asString());
        }
    }

    @Test
    /**
     * Regression test for github.com/graalvm/graaljs/issues/211.
     *
     * JavaScript's Map type is no supported type in interop, so it does not automatically translate
     * to a Java Map.
     */
    public void testJavaScriptMapNoInterop() {
        String sourceJSMap = "var map = new Map(); map.set('mapkey', 'mapvalue'); map;";
        String sourceObject = "var obj = {}; obj.objkey = 'objvalue'; obj;";

        try (Context context = JSTest.newContextBuilder("js").build()) {
            Value jsMap = context.eval("js", sourceJSMap);

            // JSMap is no interop type, so getMember fails
            Assert.assertEquals(null, jsMap.getMember("mapkey"));
            // instead, you need to invoke `get`
            Assert.assertEquals("mapvalue", jsMap.invokeMember("get", "mapkey").asString());

            Value jsObj = context.eval("js", sourceObject);
            // works as expected on plain Object
            Assert.assertEquals("objvalue", jsObj.getMember("objkey").asString());
        }
    }

    /**
     * Regression test for https://github.com/oracle/graaljs/issues/203.
     *
     * According to issue, iteration over a Java HashMap returns wrong value. Actually, the problem
     * was that the property access was not mapped to Map.get().
     */
    @Test
    public void testMapIterationContext() {
        String source = "var HashMap = Java.type('java.util.HashMap'); \n" +
                        "var map = new HashMap();\n" +
                        "map.put(1, 'A');\n" +
                        "map.get(1);\n" +
                        "var str='';\n" +
                        "for (var key in map) {\n" +
                        "    str += key;\n" +
                        "    str += map[key];\n" +
                        "    str += map.get(key);\n" +
                        "}; str;";
        try (Context context = JSTest.newContextBuilder("js").allowHostAccess(HostAccess.ALL).allowHostClassLookup(c -> true).build()) {
            Value result = context.eval("js", source);
            Assert.assertEquals("1undefinedA", result.asString());
        }
    }

    @Test
    public void testListIterationContext() {
        String source = "var ArrayList = Java.type('java.util.ArrayList');\n" +
                        "var list = new ArrayList();\n" +
                        "list.add(42);\n" +
                        "list.add(\"ab\");\n" +
                        "list.add({});\n" +
                        "var str='';\n" +
                        "for (var idx in list) {\n" +
                        "    str += '|'+idx;\n" +
                        "    str += ''+list[idx];\n" +
                        "}; str;";
        try (Context context = JSTest.newContextBuilder("js").allowHostAccess(HostAccess.ALL).allowHostClassLookup(c -> true).build()) {
            Value result = context.eval("js", source);
            Assert.assertEquals("|042|1ab|2[object Object]", result.asString());
        }
    }

}

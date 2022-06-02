/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.test.JSTest;

@RunWith(Parameterized.class)
public class ForeignObjectToPrimitiveTest {

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return List.of(Boolean.FALSE, Boolean.TRUE);
    }

    @Parameter(value = 0) public boolean foreignObjectPrototype;

    private Context newContext() {
        return JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, Boolean.toString(foreignObjectPrototype)).allowHostAccess(HostAccess.ALL).build();
    }

    @Test
    public void testProxyObjectVsString() {
        try (Context context = newContext()) {
            Value isLooselyEqual = makeIsLooselyEqual(context);

            Map<String, Object> members = Map.of(
                            "toString", (ProxyExecutable) (args) -> "toString()",
                            "valueOf", (ProxyExecutable) (args) -> "valueOf()");
            Object object = ProxyObject.fromMap(members);
            assertEquals("toString()", toString(context, object));
            assertEquals("valueOf()", valueOf(context, object));
            assertTrue("string == object", isLooselyEqual.execute("valueOf()", object).asBoolean());
            assertTrue("object == string", isLooselyEqual.execute(object, "valueOf()").asBoolean());

            // isIdentical
            assertTrue("object == object", isLooselyEqual.execute(object, object).asBoolean());
            assertFalse("object == object", isLooselyEqual.execute(object, ProxyObject.fromMap(members)).asBoolean());
            assertFalse("object == object", isLooselyEqual.execute(ProxyObject.fromMap(members), object).asBoolean());
        }
    }

    @Test
    public void testProxyObjectVsNull() {
        try (Context context = newContext()) {
            Value isLooselyEqual = makeIsLooselyEqual(context);

            Object object = ProxyObject.fromMap(Map.of(
                            "toString", (ProxyExecutable) (args) -> {
                                throw new AssertionError("should not be called");
                            },
                            "valueOf", (ProxyExecutable) (args) -> {
                                throw new AssertionError("should not be called");
                            }));
            assertFalse("null == object", isLooselyEqual.execute(null, object).asBoolean());
            assertFalse("object == null", isLooselyEqual.execute(object, null).asBoolean());
        }
    }

    @Test
    public void testProxyArrayVsString() {
        try (Context context = newContext()) {
            Value isLooselyEqual = makeIsLooselyEqual(context);

            Object[] elements = {"fun", "with", "proxy", "array"};
            ProxyArray array = ProxyArray.fromArray(elements);
            assertEquals("fun,with,proxy,array", toString(context, array));
            assertEquals("fun,with,proxy,array", valueOf(context, array));
            assertTrue("string == array", isLooselyEqual.execute("fun,with,proxy,array", array).asBoolean());
            assertTrue("array == string", isLooselyEqual.execute(array, "fun,with,proxy,array").asBoolean());

            // isIdentical
            assertTrue("array == array", isLooselyEqual.execute(array, array).asBoolean());
            assertFalse("array == array", isLooselyEqual.execute(array, ProxyArray.fromArray(elements)).asBoolean());
            assertFalse("array == array", isLooselyEqual.execute(ProxyArray.fromArray(elements), array).asBoolean());
        }
    }

    @Test
    public void testHostArray() {
        try (Context context = newContext()) {
            Value isLooselyEqual = makeIsLooselyEqual(context);

            Object[] array = List.of("fun", "with", "proxy", "array").toArray();
            String expectedString = "fun,with,proxy,array";
            assertEquals(expectedString, toString(context, array));
            assertEquals(expectedString, valueOf(context, array));
            assertTrue("string == array", isLooselyEqual.execute(expectedString, array).asBoolean());
            assertTrue("array == string", isLooselyEqual.execute(array, expectedString).asBoolean());

            // isIdentical
            assertTrue("array == array", isLooselyEqual.execute(array, array).asBoolean());
            assertFalse("array == array", isLooselyEqual.execute(array, array.clone()).asBoolean());
            assertFalse("array == array", isLooselyEqual.execute(array.clone(), array).asBoolean());
        }
    }

    @Test
    public void testHostList() {
        try (Context context = newContext()) {
            Value isLooselyEqual = makeIsLooselyEqual(context);

            List<String> list = List.of("fun", "with", "proxy", "array");
            String expectedString = "[fun, with, proxy, array]";
            assertEquals(expectedString, toString(context, list));
            assertEquals(expectedString, valueOf(context, list));
            assertTrue("string == array", isLooselyEqual.execute(expectedString, list).asBoolean());
            assertTrue("array == string", isLooselyEqual.execute(list, expectedString).asBoolean());

            // isIdentical
            assertTrue("array == array", isLooselyEqual.execute(list, list).asBoolean());
            assertFalse("array == array", isLooselyEqual.execute(list, List.of(list.toArray())).asBoolean());
            assertFalse("array == array", isLooselyEqual.execute(List.of(list.toArray()), list).asBoolean());
        }
    }

    @Test
    public void testHostObjectVsString() {
        try (Context context = newContext()) {
            Value isLooselyEqual = makeIsLooselyEqual(context);

            Object object = new ValueOfTestObject();
            assertEquals("toString()", toString(context, object));
            assertEquals("valueOf()", valueOf(context, object));
            assertTrue("string == object", isLooselyEqual.execute("valueOf()", object).asBoolean());
            assertTrue("object == string", isLooselyEqual.execute(object, "valueOf()").asBoolean());

            // isIdentical
            assertTrue("object == object", isLooselyEqual.execute(object, object).asBoolean());
            assertFalse("object == object", isLooselyEqual.execute(object, new ValueOfTestObject()).asBoolean());
            assertFalse("object == object", isLooselyEqual.execute(new ValueOfTestObject(), object).asBoolean());
        }
    }

    public static class ValueOfTestObject {
        public String valueOf() {
            return "valueOf()";
        }

        @Override
        public String toString() {
            return "toString()";
        }
    }

    private static String toString(Context context, Object value) {
        return context.eval(ID, "String").execute(value).asString();
    }

    private static String valueOf(Context context, Object value) {
        return context.eval(ID, "x => x + []").execute(value).asString();
    }

    private static Value makeIsLooselyEqual(Context context) {
        return context.eval(ID, "(function(a, b){return a == b;})");
    }
}

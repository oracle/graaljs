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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test conversion or objects in JavaScript scope to Java objects using
 * {@link HostAccess.Builder#targetTypeMapping}.
 */
public class TargetTypeMappingTest {
    @Rule public ExpectedException expectedException = ExpectedException.none();

    public static class ToBePassedToJS {
        private JsonObject jsonObject;

        @HostAccess.Export
        public void json(JsonObject c) {
            this.jsonObject = c;
        }

    }

    public static class JsonObject {
        private final Value value;

        JsonObject(Value v) {
            this.value = v;
        }

        String stringify() {
            Value convertor = value.getContext().eval(ID, "(function () { return {getJson: function() { return JSON" +
                            ".stringify(this.jsObject); } }; })()");
            convertor.putMember("jsObject", value);
            return convertor.getMember("getJson").execute().asString();
        }
    }

    /**
     * Test conversion of JavaScript object or array to a custom Java wrapper that is able to
     * stringify the JavaScript object or array.
     */
    @Test
    public void testJSObjectWrapping() {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(Value.class, JsonObject.class,
                        (v) -> v.hasMembers() || v.hasArrayElements(),
                        JsonObject::new).allowAccessAnnotatedBy(HostAccess.Export.class).build();
        try (Context context = Context.newBuilder(ID).allowHostAccess(hostAccess).build()) {
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            context.getBindings(ID).putMember("objectFromJava", objectFromJava);

            String jsObjectString = "{b: true, x: 'abc', y: 1, z: 1.034, xyz: {a: 'one', b: 'two'}}";
            Value serializedValue = context.eval(ID, "JSON.stringify(" + jsObjectString + ")");
            Value convertedValue = context.eval(ID, "(function () { return " + jsObjectString + "; })()");
            context.eval(ID, "objectFromJava.json(" + jsObjectString + ")");
            assertEquals(serializedValue.asString(), convertedValue.as(JsonObject.class).stringify());
            assertEquals(serializedValue.asString(), objectFromJava.jsonObject.stringify());

            String jsArrayString = "[true, 'abc', 1, 1.034, {a: 'one', b: 'two'}]";
            serializedValue = context.eval(ID, "JSON.stringify(" + jsArrayString + ")");
            convertedValue = context.eval(ID, "(function () { return " + jsArrayString + "; })()");
            context.eval(ID, "objectFromJava.json(" + jsArrayString + ")");
            assertEquals(serializedValue.asString(), convertedValue.as(JsonObject.class).stringify());
            assertEquals(serializedValue.asString(), objectFromJava.jsonObject.stringify());
        }
    }

    /**
     * Test built-in ability to convert JavaScript array to a Java List and Java array.
     */
    @Test
    public void testJSArrayToList() {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(List.class, Collection.class, null, (v) -> v).targetTypeMapping(List.class, Object.class, null, (v) -> v).build();
        try (Context context = Context.newBuilder(ID).allowHostAccess(hostAccess).build()) {
            String jsArrayString = "[true, 'abc', 1, 1.034]";
            Value val = context.eval(ID, jsArrayString);
            // The following does not need any target type mapping, JS array converts to Java List
            // naturally
            List<?> list = val.as(List.class);
            assertEquals(jsArrayString,
                            list.stream().map(e -> e instanceof String ? "'" + e + "'" : "" + e).collect(Collectors.joining(
                                            ", ", "[", "]")));
            // The following does not need any target type mapping, JS array converts to Java array
            // naturally
            Object[] array = val.as(Object[].class);
            assertEquals(jsArrayString,
                            Arrays.stream(array).map(e -> e instanceof String ? "'" + e + "'" : "" + e).collect(Collectors.joining(
                                            ", ", "[", "]")));
            // The target type mapping ensures that things convertible to Java List will convert to
            // List even when
            // conversion to generic Java Collection is requested
            assertEquals(list, val.as(Collection.class));
            // The target type mapping ensures that things convertible to Java List will convert to
            // List even when
            // conversion to generic Java Object is requested
            assertEquals(list, val.as(Object.class));
        }
    }

    /**
     * Test that Java objects are kept as they are including when a conversion to a super class is
     * requested.
     */
    @Test
    public void testJavaListToJavaList() {
        try (Context context = Context.create(ID)) {
            Object[] javaArray = new Object[]{true, "abc", 1, 1.034};
            List<Object> javaList = Arrays.asList(javaArray);
            Value bindings = context.getBindings(ID);
            bindings.putMember("javaList", javaList);
            Value val = context.eval(ID, "javaList");
            assertSame(javaList, val.as(List.class));
            assertSame(javaList, val.as(Collection.class));
            assertSame(javaList, val.as(Object.class));
        }
    }

    /**
     * Test that Java List is not automatically converted to a Java array.
     */
    @Test
    public void testJavaListToJavaArrayNegative() {
        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage("Value must have array elements");
        try (Context context = Context.create(ID)) {
            Object[] javaArray = new Object[]{true, "abc", 1, 1.034};
            List<Object> javaList = Arrays.asList(javaArray);
            Value bindings = context.getBindings(ID);
            bindings.putMember("javaList", javaList);
            Value val = context.eval(ID, "javaList");
            val.as(Object[].class);
        }
    }

    /**
     * Test that Java List to Java array conversion can be acomplished by
     * {@link HostAccess.Builder#targetTypeMapping}.
     */
    @Test
    public void testJavaListToJavaArray() {
        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(List.class, Object[].class, null, List::toArray).build();
        try (Context context = Context.newBuilder(ID).allowHostAccess(hostAccess).build()) {
            Object[] javaArray = new Object[]{true, "abc", 1, 1.034};
            List<Object> javaList = Arrays.asList(javaArray);
            Value bindings = context.getBindings(ID);
            bindings.putMember("javaList", javaList);
            Value val = context.eval(ID, "javaList");
            Object[] array = val.as(Object[].class);
            assertArrayEquals(javaArray, array);
        }
    }

    /**
     * Test that integers are not automatically converted to strings via {@link Value#as(Class)}
     * method.
     */
    @Test
    public void testNoDefaultToStringConversion1() {
        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage("Invalid or lossy primitive coercion.");

        try (Context context = Context.create(ID)) {
            Value val = context.eval(ID, "1");
            assertEquals("1", val.as(String.class));
        }
    }

    /**
     * Test that integers are not automatically converted to strings via {@link Value#asString()}
     * method.
     */
    @Test
    public void testNoDefaultToStringConversion2() {
        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage("Invalid coercion. You can ensure that the value can be converted using Value" +
                        ".isString().");

        try (Context context = Context.create(ID)) {
            Value val = context.eval(ID, "1");
            assertEquals("1", val.asString());
        }
    }

    /**
     * Test that target type mapping has no effect on {@link Value#asString()} method, but
     * {@link Value#as(Class)} uses the mapping.
     */
    @Test
    public void testAsStringAndAsStringClassDifference() {
        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage("Invalid coercion. You can ensure that the value can be converted using Value" +
                        ".isString().");

        final HostAccess hostAccess = HostAccess.newBuilder().targetTypeMapping(Integer.class, String.class, null, String::valueOf).build();
        try (Context context = Context.newBuilder(ID).allowHostAccess(hostAccess).build()) {
            Value val = context.eval(ID, "1");
            assertEquals("1", val.as(String.class)); // works because of the target type mapping
            assertEquals("1", val.asString()); // doesn't work, because the value is not string
        }
    }
}

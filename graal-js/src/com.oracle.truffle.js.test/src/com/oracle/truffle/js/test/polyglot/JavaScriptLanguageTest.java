/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class JavaScriptLanguageTest {

    @Test
    public void testToStringNestingDepth() {
        // this tests that JavaScriptLanguage.toString() does not nest too deeply
        var out = new ByteArrayOutputStream();
        try (Context context = JSTest.newContextBuilder().out(out).build()) {
            context.getBindings("js").putMember("array", new ProxyArray() {
                @Override
                public Object get(long index) {
                    return this;
                }

                @Override
                public void set(long index, Value arg1) {
                }

                @Override
                public long getSize() {
                    return 10;
                }
            });
            Source source = Source.newBuilder("js", "''+array; array", "noname").interactive(true).buildLiteral();
            context.eval(source);
        }
        String output = out.toString().trim();
        String innermostArray = "[" + "Array(10), ".repeat(9) + "Array(10)" + "]";
        assertThat(output, allOf(startsWith("(10)[[" + innermostArray), endsWith(innermostArray + "]]")));
    }

    @Test
    public void testToStringForeignNull() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.getBindings("js").putMember("NULL", ForeignTestMap.newNull());
            Value result = context.eval(JavaScriptLanguage.ID, "[NULL, NULL];");
            assertTrue(result.toString(), result.toString().contains("null,"));
            result = context.eval(JavaScriptLanguage.ID, "String([NULL, NULL]);");
            assertEquals(",", result.asString());
        }
    }

    @Test
    public void testToStringForeignArray() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.getBindings("js").putMember("array", new ProxyArray() {
                @Override
                public Object get(long index) {
                    return index;
                }

                @Override
                public void set(long index, Value value) {
                }

                @Override
                public long getSize() {
                    return 5;
                }
            });
            Value result = context.eval(JavaScriptLanguage.ID, "[array];");
            assertTrue(result.toString(), result.toString().contains("0, 1, 2, 3, 4"));
            result = context.eval(JavaScriptLanguage.ID, "String([array]);");
            assertTrue(result.toString(), result.asString().contains("0,1,2,3,4"));
            result = context.eval(JavaScriptLanguage.ID, "'' + array;");
            assertTrue(result.toString(), result.asString().contains("0,1,2,3,4"));
        }
    }

    @Test
    public void testToStringForeignObject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.getBindings("js").putMember("obj", ProxyObject.fromMap(Collections.singletonMap("answer", 42)));
            Value result = context.eval(JavaScriptLanguage.ID, "[obj];");
            assertTrue(result.toString(), result.toString().contains("{answer: 42}"));
            result = context.eval(JavaScriptLanguage.ID, "String([obj]);");
            assertTrue(result.toString(), result.asString().contains("[object Object]"));
            result = context.eval(JavaScriptLanguage.ID, "'' + obj;");
            assertTrue(result.toString(), result.asString().contains("[object Object]"));
        }
    }

    @Test
    public void testToStringNestedArray() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1,[2,[3,[4,[5]]]]];");
            assertEquals("(2)[1, [2, [3, Array(2)]]]", result.toString());
        }
    }

    @Test
    public void testToPrimitiveHostObject() {
        try (Context context = Context.newBuilder(JavaScriptLanguage.ID).allowAllAccess(true).build()) {
            context.getBindings(JavaScriptLanguage.ID).putMember("obj", new TestHostObject());
            Value res = context.eval(JavaScriptLanguage.ID, "obj + obj");
            assertEquals(84, res.asInt());
        }
    }

    public static final class TestHostObject {
        @SuppressWarnings("static-method")
        public int valueOf() {
            return 42;
        }

        @Override
        public String toString() {
            return "string";
        }
    }

    @Test
    public void testRemoveBindings() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value bindings = context.getBindings("js");
            assertTrue(bindings.removeMember("eval"));

            assertFalse(bindings.hasMember("foo"));
            context.eval(JavaScriptLanguage.ID, "foo=42;");
            assertTrue(bindings.hasMember("foo"));
            assertTrue(bindings.removeMember("foo"));
            assertFalse(bindings.hasMember("foo"));
        }
    }
}

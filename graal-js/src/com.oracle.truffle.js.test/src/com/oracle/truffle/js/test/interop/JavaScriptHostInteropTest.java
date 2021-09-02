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
package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class JavaScriptHostInteropTest {

    public interface MyHostIntf {
        int foo(int value);

        Map<?, ?>[] bounce(Map<?, ?>[] mapList);

        void write(String a, String b, String c);
    }

    public static class MyHostObj implements MyHostIntf {
        @Override
        public int foo(int value) {
            return value;
        }

        @Override
        public Map<?, ?>[] bounce(Map<?, ?>[] mapList) {
            return mapList;
        }

        @Override
        public void write(@SuppressWarnings("unused") String a, @SuppressWarnings("unused") String b, @SuppressWarnings("unused") String c) {
        }
    }

    @Test
    public void testArityError() {
        Consumer<PolyglotException> expectedException;
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).build()) {
            for (int i = 0; i < 2; i++) {
                Object hostObj;
                if (i == 0) {
                    hostObj = new MyHostObj();
                } else {
                    Object delegate = new MyHostObj();
                    hostObj = Proxy.newProxyInstance(MyHostIntf.class.getClassLoader(),
                                    new Class[]{MyHostIntf.class}, (proxy, method, args) -> method.invoke(delegate, args));
                }
                context.getBindings(ID).putMember("hostobj", hostObj);
                String expectedClassName = hostObj.getClass().getSimpleName();

                expectedException = e -> {
                    assertThat(e.getMessage(), e.getMessage(), containsString(expectedClassName));
                    assertThat(e.getMessage(), e.getMessage(), containsString("foo"));
                    assertThat(e.getMessage(), e.getMessage(), containsString("Arity error"));
                };
                assertThrows(() -> context.eval(ID, "hostobj.foo();"), expectedException);
                assertThrows(() -> context.eval(ID, "" +
                                "var foo = hostobj.foo;\n" +
                                "foo();"),
                                expectedException);

                expectedException = e -> {
                    assertThat(e.getMessage(), e.getMessage(), containsString(expectedClassName));
                    assertThat(e.getMessage(), e.getMessage(), containsString("write"));
                    assertThat(e.getMessage(), e.getMessage(), containsString("Arity error"));
                };
                assertThrows(() -> context.eval(ID, "hostobj.write('a', 'b')"), expectedException);
                assertThrows(() -> context.eval(ID, "hostobj['write']('a', 'b')"), expectedException);
                assertThrows(() -> context.eval(ID, "hostobj.write('a', 'b', 'c', 'd')"), expectedException);
                assertThrows(() -> context.eval(ID, "hostobj['write']('a', 'b', 'c', 'd')"), expectedException);
            }
        }
    }

    @Test
    public void testArgumentTypeError() {
        Consumer<PolyglotException> expectedException;
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).build()) {
            for (int i = 0; i < 2; i++) {
                Object hostObj;
                if (i == 0) {
                    hostObj = new MyHostObj();
                } else {
                    Object delegate = new MyHostObj();
                    hostObj = Proxy.newProxyInstance(MyHostIntf.class.getClassLoader(),
                                    new Class[]{MyHostIntf.class}, (proxy, method, args) -> method.invoke(delegate, args));
                }
                context.getBindings(ID).putMember("hostobj", hostObj);
                String expectedClassName = hostObj.getClass().getSimpleName();

                expectedException = e -> {
                    assertThat(e.getMessage(), e.getMessage(), containsString(expectedClassName));
                    assertThat(e.getMessage(), e.getMessage(), containsString("foo"));
                    assertThat(e.getMessage(), e.getMessage(), containsString("Cannot convert '{}'(language: JavaScript, type: Object) to Java type 'int'"));
                };
                assertThrows(() -> context.eval(ID, "hostobj.foo({});"), expectedException);
                assertThrows(() -> context.eval(ID, "" +
                                "var foo = hostobj.foo;\n" +
                                "foo({});"), expectedException);

                context.eval(ID, "hostobj.foo(42);");
                context.eval(ID, "" +
                                "var foo = hostobj.foo;\n" +
                                "foo(42);");

                expectedException = e -> {
                    assertThat(e.getMessage(), e.getMessage(), containsString(expectedClassName));
                    assertThat(e.getMessage(), e.getMessage(), containsString("bounce"));
                    assertThat(e.getMessage(), e.getMessage(), containsString("Cannot convert 'abc'(language: Java, type: java.lang.String) to Java type 'java.util.Map[]'"));
                };
                assertThrows(() -> context.eval(ID, "hostobj.bounce('abc')"), expectedException);
                assertThrows(() -> context.eval(ID, "hostobj['bounce']('abc')"), expectedException);

                expectedException = e -> {
                    assertThat(e.getMessage(), e.getMessage(), containsString(expectedClassName));
                    assertThat(e.getMessage(), e.getMessage(), containsString("write"));
                    assertThat(e.getMessage(), e.getMessage(),
                                    containsString("Cannot convert '6'(language: Java, type: java.lang.Integer) to Java type 'java.lang.String'"));
                };
                assertThrows(() -> context.eval(ID, "hostobj.write(6, 'eight', null)"), expectedException);
                assertThrows(() -> context.eval(ID, "hostobj['write'](6, 'eight', null)"), expectedException);

                context.eval(ID, "hostobj.write(JSON.stringify(6), 'eight', null)");
                context.eval(ID, "hostobj['write'](JSON.stringify(6), 'eight', null)");
            }
        }
    }

    static final String EXPECTED_EXCEPTION_MESSAGE = "This will be swallowed :(";

    public static class Hello {
        public void thisIsFine(Value human) {
            assertEquals("timmy", human.invokeMember("hello").asString());
        }

        public void thisWillBreak(@SuppressWarnings("unused") Object human) {
            throw new ClassCastException(EXPECTED_EXCEPTION_MESSAGE);
        }
    }

    @Test
    public void testHostClassCastException() {
        try (Context context = Context.newBuilder().allowHostAccess(HostAccess.ALL).build()) {
            context.getBindings(ID).putMember("hello", new Hello());
            try {
                context.eval(ID, "class Human {\n" +
                                "  constructor(name) {\n" +
                                "    this.name = name;\n" +
                                "  }\n" +
                                "\n" +
                                "  hello() {\n" +
                                "    return this.name;\n" +
                                "  }\n" +
                                "}\n" +
                                "\n" +
                                "const human = new Human(\"timmy\");\n" +
                                "\n" +
                                "hello.thisIsFine(human);\n" +
                                "hello.thisWillBreak(human);");
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertTrue(e.isHostException());
                assertEquals(ClassCastException.class, e.asHostException().getClass());
                assertEquals(EXPECTED_EXCEPTION_MESSAGE, e.asHostException().getMessage());
            }
        }
    }

    static void assertThrows(Runnable test, Consumer<PolyglotException> exceptionVerifier) {
        try {
            test.run();
            fail("should have thrown");
        } catch (PolyglotException e) {
            assertTrue(e.isGuestException());
            exceptionVerifier.accept(e);
        }
    }

    @Test
    public void hostObjectIdentity() {
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).build()) {
            Object proxy1 = new Object();
            Object proxy2 = new Object();

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            assertTrue(equals.execute(proxy1, proxy1).asBoolean());
            assertTrue(identical.execute(proxy1, proxy1).asBoolean());
            assertFalse(equals.execute(proxy1, proxy2).asBoolean());
            assertFalse(identical.execute(proxy1, proxy2).asBoolean());
        }
    }

    public static class AmbiguousFunctionalInterfaces {
        public Object sort(List<Object> array, Comparator<Object> comparator) {
            array.sort(comparator);
            return array;
        }

        public Object sort(List<Integer> array, IntBinaryOperator comparator) {
            array.sort((a, b) -> comparator.applyAsInt(a, b));
            return array;
        }

        public Object consumeArray(@SuppressWarnings("unused") List<Object> array) {
            return "List";
        }

        public Object consumeArray(@SuppressWarnings("unused") Object[] array) {
            return "Object[]";
        }
    }

    @Test
    public void explicitMethodOverloadTest() {
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).build()) {
            Object api = new AmbiguousFunctionalInterfaces();
            context.getBindings(JavaScriptLanguage.ID).putMember("api", api);

            context.eval(ID, "var array = [3,13,3,7];");

            // TypeError: Multiple applicable overloads found
            assertThrows(() -> context.eval(ID, "api.sort(array, (a, b) => a - b);"),
                            e -> assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError")));

            Value result;
            result = context.eval(ID, "api['sort(java.util.List,java.util.Comparator)'](array, (a, b) => a - b);");
            assertArrayEquals(new int[]{3, 3, 7, 13}, result.as(int[].class));

            result = context.eval(ID, "api['sort(java.util.List,java.util.function.IntBinaryOperator)'](array, (a, b) => b - a);");
            assertArrayEquals(new int[]{13, 7, 3, 3}, result.as(int[].class));

            result = context.eval(ID, "api.consumeArray(array);");
            assertEquals("List", result.asString());

            result = context.eval(ID, "api['consumeArray(java.util.List)'](array);");
            assertEquals("List", result.asString());

            result = context.eval(ID, "api['consumeArray(java.lang.Object[])'](array);");
            assertEquals("Object[]", result.asString());
        }
    }
}

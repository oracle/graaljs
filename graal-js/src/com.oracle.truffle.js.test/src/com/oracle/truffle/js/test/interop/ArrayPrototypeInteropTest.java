/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class ArrayPrototypeInteropTest {

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
    public void testSplice() {
        testWithArray("Array.prototype.splice.call(a, 1, 2)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 40, 50),
                        Arrays.asList(20, 30));
        testWithArray("Array.prototype.splice.call(a, 1, 2, 70, 80, 90)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 70, 80, 90, 40, 50),
                        Arrays.asList(20, 30));
        testWithArray("Array.prototype.splice.call(a, 1, 3, 70, 80)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 70, 80, 50),
                        Arrays.asList(20, 30, 40));
        testWithArray("Array.prototype.splice.call(a, 3, 10)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 20, 30),
                        Arrays.asList(40, 50));
        testWithArray("Array.prototype.splice.call(a, -1, 0, 70)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 20, 30, 40, 70, 50),
                        Arrays.asList());
        testWithArray("Array.prototype.splice.call(a, 0, 0, 70)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(70, 10, 20, 30, 40, 50),
                        Arrays.asList());
        testWithArray("Array.prototype.splice.call(a, 0, 1, 70)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(70, 20, 30, 40, 50),
                        Arrays.asList(10));
    }

    @Test
    public void testPop() {
        testWithArray("Array.prototype.pop.call(a)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 20, 30, 40),
                        50);
        testWithArray("Array.prototype.pop.call(a)",
                        Arrays.asList(10),
                        Arrays.asList(),
                        10);
        testWithArray("Array.prototype.pop.call(a)",
                        Arrays.asList(),
                        Arrays.asList(),
                        result -> assertTrue(result.isNull()));
    }

    @Test
    public void testShift() {
        testWithArray("Array.prototype.shift.call(a)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(20, 30, 40, 50),
                        10);
        testWithArray("Array.prototype.shift.call(a)",
                        Arrays.asList(10),
                        Arrays.asList(),
                        10);
        testWithArray("Array.prototype.shift.call(a)",
                        Arrays.asList(),
                        Arrays.asList(),
                        result -> assertTrue(result.isNull()));
    }

    @Test
    public void testPush() {
        testWithArray("Array.prototype.push.call(a, 80)",
                        Arrays.asList(10, 20, 30, 40),
                        Arrays.asList(10, 20, 30, 40, 80),
                        5);
        testWithArray("Array.prototype.push.call(a, 10)",
                        Arrays.asList(),
                        Arrays.asList(10),
                        1);
        testWithArray("Array.prototype.push.call(a)",
                        Arrays.asList(10, 20, 30, 40),
                        Arrays.asList(10, 20, 30, 40),
                        4);
        testWithArray("Array.prototype.push.call(a, 80, 90)",
                        Arrays.asList(10, 20),
                        Arrays.asList(10, 20, 80, 90),
                        4);
    }

    @Test
    public void testReverse() {
        testWithArray("Array.prototype.reverse.call(a)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(50, 40, 30, 20, 10),
                        Arrays.asList(50, 40, 30, 20, 10));
    }

    @Test
    public void testSort() {
        testWithArray("Array.prototype.sort.call(a)",
                        Arrays.asList(50, 40, 30, 20, 10),
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 20, 30, 40, 50));
        testWithArray("Array.prototype.sort.call(a, (x, y) => y - x)",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(50, 40, 30, 20, 10),
                        Arrays.asList(50, 40, 30, 20, 10));
    }

    @Test
    public void testUnshift() {
        testWithArray("Array.prototype.unshift.call(a, 80)",
                        Arrays.asList(10, 20, 30, 40),
                        Arrays.asList(80, 10, 20, 30, 40),
                        5);
        testWithArray("Array.prototype.unshift.call(a, 10)",
                        Arrays.asList(),
                        Arrays.asList(10),
                        1);
        testWithArray("Array.prototype.unshift.call(a)",
                        Arrays.asList(10, 20, 30, 40),
                        Arrays.asList(10, 20, 30, 40),
                        4);
        testWithArray("Array.prototype.unshift.call(a, 80, 90)",
                        Arrays.asList(10, 20),
                        Arrays.asList(80, 90, 10, 20),
                        4);
    }

    @Test
    public void testCopyWithin() {
        testWithArray("Array.prototype.copyWithin.call(a, 4, 1, 3)",
                        Arrays.asList(10, 20, 30, 40, 50, 60),
                        Arrays.asList(10, 20, 30, 40, 20, 30),
                        Arrays.asList(10, 20, 30, 40, 20, 30));
        testWithArray("Array.prototype.copyWithin.call(a, 1, 4)",
                        Arrays.asList(10, 20, 30, 40, 50, 60),
                        Arrays.asList(10, 50, 60, 40, 50, 60),
                        Arrays.asList(10, 50, 60, 40, 50, 60));
    }

    @Test
    public void testFill() {
        testWithArray("Array.prototype.fill.call(a, 69)",
                        Arrays.asList(10, 20, 30, 40, 50, 60),
                        Arrays.asList(69, 69, 69, 69, 69, 69),
                        Arrays.asList(69, 69, 69, 69, 69, 69));
        testWithArray("Array.prototype.fill.call(a, 69)",
                        Arrays.asList(),
                        Arrays.asList(),
                        Arrays.asList());
    }

    @Test
    public void testDelete() {
        testWithArray("delete a[0]",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 20, 30, 40, 50),
                        result -> assertFalse(result.asBoolean()));
        testWithArray("delete a[5]",
                        Arrays.asList(10, 20, 30, 40, 50),
                        Arrays.asList(10, 20, 30, 40, 50),
                        result -> assertTrue(result.asBoolean()));
    }

    @Test
    public void testToStringWithoutJoin() {
        List<Object> values = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        context.getBindings(ID).putMember("a", new MyProxyArray(values));
        Value resultValue = context.eval(ID, "Array.prototype.toString.call(a);");
        assertTrue("toString should return a string", resultValue.isString());
        assertEquals("10,20,30,40,50", resultValue.asString());
    }

    @Test
    public void testToStringWithThrowingJoin() {
        List<Object> values = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        context.getBindings(ID).putMember("a", new MyProxyArrayWithThrowingJoin(values));
        Value resultValue = context.eval(ID, "Array.prototype.toString.call(a);");
        assertTrue("toString should return a string", resultValue.isString());
        assertEquals("10,20,30,40,50", resultValue.asString());
    }

    @Test
    public void testToStringWithCustomJoin() {
        List<Object> values = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        context.getBindings(ID).putMember("a", new MyProxyArrayWithCustomJoin(values));
        Value resultValue = context.eval(ID, "Array.prototype.toString.call(a);");
        assertTrue("toString should return a string", resultValue.isString());
        assertEquals("1020304050", resultValue.asString());
    }

    private void testWithArray(String test, List<Integer> before, List<Integer> afterExpected, List<Integer> expectedResult) {
        testWithArray(test, before, afterExpected, actualResult -> assertEquals("result", expectedResult, actualResult.as(LIST_OF_INTEGER)));
    }

    private void testWithArray(String test, List<Integer> before, List<Integer> afterExpected, int expectedResult) {
        testWithArray(test, before, afterExpected, actualResult -> assertEquals("result", expectedResult, actualResult.asInt()));
    }

    private void testWithArray(String test, List<Integer> before, List<Integer> afterExpected, Consumer<Value> resultTest) {
        List<Object> values = new ArrayList<>(before);
        context.getBindings(ID).putMember("a", new MyProxyArray(values));
        Value resultValue = context.eval(ID, test);
        List<Integer> afterValue = new ArrayList<>(context.getBindings(ID).getMember("a").as(LIST_OF_INTEGER));
        assertEquals("array", afterExpected, afterValue);
        resultTest.accept(resultValue);
    }

    private static final TypeLiteral<List<Integer>> LIST_OF_INTEGER = new TypeLiteral<>() {
    };

    private static class MyProxyArray implements ProxyArray {
        protected final List<Object> values;

        protected MyProxyArray(List<Object> values) {
            this.values = values;
        }

        @Override
        public Object get(long index) {
            return values.get(checkIndex(index));
        }

        @Override
        public void set(long index, Value value) {
            int idx = checkIndex(index);
            Object val = devalue(value);
            if (idx < getSize()) {
                values.set(idx, val);
            } else {
                while (idx > getSize()) {
                    values.add(null);
                }
                assert idx == getSize();
                values.add(val);
            }
        }

        @Override
        public boolean remove(long index) {
            values.remove(checkIndex(index));
            return true;
        }

        @Override
        public long getSize() {
            return values.size();
        }

        private static int checkIndex(long index) {
            if (index > Integer.MAX_VALUE || index < 0) {
                throw new ArrayIndexOutOfBoundsException("invalid index.");
            }
            return (int) index;
        }

        private static Object devalue(Value value) {
            if (value.isHostObject()) {
                return value.asHostObject();
            } else if (value.fitsInInt()) {
                return value.asInt();
            } else {
                return value;
            }
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }

    private static class MyProxyArrayWithThrowingJoin extends MyProxyArray implements ProxyObject {

        protected MyProxyArrayWithThrowingJoin(List<Object> values) {
            super(values);
        }

        @Override
        public Object getMember(String key) {
            if (key.equals("join")) {
                return (ProxyExecutable) arguments -> {
                    throw new IllegalArgumentException();
                };
            }
            return null;
        }

        @Override
        public Object getMemberKeys() {
            return ProxyArray.fromArray("join");
        }

        @Override
        public boolean hasMember(String key) {
            return key.equals("join");
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException();
        }
    }

    private static class MyProxyArrayWithCustomJoin extends MyProxyArray implements ProxyObject {

        protected MyProxyArrayWithCustomJoin(List<Object> values) {
            super(values);
        }

        @Override
        public Object getMember(String key) {
            if (key.equals("join")) {
                return (ProxyExecutable) arguments -> {
                    StringBuilder sb = new StringBuilder();
                    for (Object arg : values) {
                        sb.append(arg);
                    }
                    return sb.toString();
                };
            }
            return null;
        }

        @Override
        public Object getMemberKeys() {
            return ProxyArray.fromArray("join");
        }

        @Override
        public boolean hasMember(String key) {
            return key.equals("join");
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException();
        }
    }

}

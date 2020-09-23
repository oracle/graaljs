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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignTestMap;

/**
 * Various tests for accessing JavaScript array in Java and accessing appropriate Java objects as
 * JavaScript arrays in JavaScript.
 */
public class InteropArrayTest {
    /**
     * Test that fast JS array indices are not in member keys (which contains only enumerable
     * properties) and that both getMember and getArrayElement works on them.
     */
    @Test
    public void testArrayGetMembers() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "[3, 4, 1, 5]");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getMember("0").asInt());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(1, array.getMember("2").asInt());
            assertEquals(1, array.getArrayElement(2).asInt());
            assertTrue(array.getMemberKeys().toString(), array.getMemberKeys().isEmpty());
        }
    }

    /**
     * Test that slow JS array indices are not in member keys and also overridden index is not in
     * member keys.
     */
    @Test
    public void testSlowArrayGetMembers() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "var a = [3, 4, 1, 5]; Object.defineProperty(a, 2, {get: function(){return" +
                            " 42;}}); a;");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getMember("0").asInt());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(42, array.getMember("2").asInt());
            assertEquals(42, array.getArrayElement(2).asInt());
            assertTrue(array.getMemberKeys().toString(), array.getMemberKeys().isEmpty());
        }
    }

    /**
     * Test that JS array's enumerable property with integer key specified as integer is not in
     * member keys.
     */
    @Test
    public void testSlowArrayWithIntKeyEnumerablePropertyGetMembers1() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "var a = [3, 4, 1, 5]; Object.defineProperty(a, 2, {get: function(){return" +
                            " 42;}, enumerable: true}); a;");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getMember("0").asInt());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(42, array.getMember("2").asInt());
            assertEquals(42, array.getArrayElement(2).asInt());
            assertTrue(array.getMemberKeys().toString(), array.getMemberKeys().isEmpty());
        }
    }

    /**
     * Test that JS array's enumerable property with integer key specified as string is not in
     * member keys.
     */
    @Test
    public void testSlowArrayWithIntKeyEnumerablePropertyGetMembers2() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "var a = [3, 4, 1, 5]; Object.defineProperty(a, '2', {get: function()" +
                            "{return 42;}, enumerable: true}); a;");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getMember("0").asInt());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(42, array.getMember("2").asInt());
            assertEquals(42, array.getArrayElement(2).asInt());
            assertTrue(array.getMemberKeys().toString(), array.getMemberKeys().isEmpty());
        }
    }

    /**
     * Test that JS array's enumerable property with string key is the only element of member keys.
     */
    @Test
    public void testSlowArrayWithStringKeyEnumerablePropertyGetMembers() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "var a = [3, 4, 1, 5]; Object.defineProperty(a, 'x', {get: function()" +
                            "{return 42;}, enumerable: true}); a;");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getMember("0").asInt());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(1, array.getMember("2").asInt());
            assertEquals(1, array.getArrayElement(2).asInt());
            assertEquals(42, array.getMember("x").asInt());
            assertEquals(array.getMemberKeys().toString(), Collections.singleton("x"), array.getMemberKeys());
        }
    }

    /**
     * Test that typed JS array indices are not in member keys (enumberable properties).
     */
    @Test
    public void testTypedArrayGetMembers() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "Int8Array.from([3, 4, 1, 5]);");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getMember("0").asInt());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(1, array.getMember("2").asInt());
            assertEquals(1, array.getArrayElement(2).asInt());
            assertTrue(array.getMemberKeys().toString(), array.getMemberKeys().isEmpty());
        }
    }

    /**
     * Test that arguments JS array indices are not in member keys (enumerable properties).
     */
    @Test
    public void testArgumentsObjectGetMembers() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "(function(){return arguments;})(3, 4, 1, 5);");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getMember("0").asInt());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(1, array.getMember("2").asInt());
            assertEquals(1, array.getArrayElement(2).asInt());
            assertTrue(array.getMemberKeys().toString(), array.getMemberKeys().isEmpty());
        }
    }

    /**
     * Test that holes in JS arrays are readable and writable (i.e. getArrayElement and
     * setArrayElement work on them, respectively).
     */
    @Test
    public void testArrayHoles() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "[3,,,5]");
            assertEquals(4, array.getArraySize());
            assertEquals(3, array.getArrayElement(0).asInt());
            assertEquals(5, array.getArrayElement(3).asInt());
            assertTrue(array.getArrayElement(1).isNull());
            assertTrue(array.getArrayElement(2).isNull());
            array.setArrayElement(1, 4);
            array.setArrayElement(2, 1);
            assertEquals(4, array.getArrayElement(1).asInt());
            assertEquals(1, array.getArrayElement(2).asInt());
        }
    }

    /**
     * Test reading out of bounds.
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayIndexOutOfBounds() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value array = context.eval(ID, "[3, 4, 1, 5]");
            assertEquals(4, array.getArraySize());
            array.getArrayElement(4);
        }
    }

    private static final int[] JAVA_ARRAY = new int[]{3, 4, 1, 5};
    private static final List<Integer> JAVA_LIST = Arrays.stream(JAVA_ARRAY).boxed().collect(Collectors.toList());
    private static final String JS_ARRAY_STRING = Arrays.toString(JAVA_ARRAY);

    public static class ToBePassedToJS {
        private List<?> list;

        @HostAccess.Export
        public void methodWithListArgument(List<?> argList) {
            this.list = argList;
        }

        @HostAccess.Export
        public void methodWithArrayArgument(int[] argArray) {
            this.list = Arrays.stream(argArray).boxed().collect(Collectors.toList());
        }

        @HostAccess.Export
        public int[] methodThatReturnsArray() {
            return JAVA_ARRAY;
        }

        @HostAccess.Export
        public Value methodThatReturnsArrayAsValue() {
            return Value.asValue(JAVA_ARRAY);
        }

        @HostAccess.Export
        public List<Integer> methodThatReturnsList() {
            return JAVA_LIST;
        }

        @HostAccess.Export
        public Object createForeignMap() {
            final ForeignTestMap map = new ForeignTestMap();
            map.getContainer().put("x", 42);
            map.getContainer().put("y", "foo");
            return map;
        }

        @HostAccess.Export
        public Object[] methodThatReturnsArrayWithJSObject(Object jsObject) {
            return new Object[]{41, jsObject, "string", createForeignMap()};
        }
    }

    /**
     * Test that a JavaScript array can be evaluated via polyglot Context and read as List from
     * Java.
     */
    @Test
    public void testArrayBasic() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value v = context.eval(ID, JS_ARRAY_STRING);
            commonCheck(v);
        }
    }

    private static void commonCheck(Value v) {
        assertEquals(JAVA_ARRAY.length, v.getArraySize());
        assertArrayEquals(JAVA_ARRAY,
                        IntStream.range(0, (int) v.getArraySize()).map(i -> v.getArrayElement(i).asInt()).toArray());
    }

    @Test
    public void testArrayAsListParameter() {
        testArrayAsParameter("methodWithListArgument");
    }

    @Test
    public void testArrayAsArrayParameter() {
        testArrayAsParameter("methodWithArrayArgument");
    }

    /**
     * Test that a JavaScript array can be passed as argument of Java function and read as List or
     * Java array from Java. To be able to call a Java method, access to it must be allowed, e.g. by
     * a method annotation and passing the class of that annotation to
     * {@link HostAccess.Builder#allowAccessAnnotatedBy}.
     */
    private static void testArrayAsParameter(String methodName) {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            bindings.putMember("objectFromJava", objectFromJava);
            context.eval(ID, "objectFromJava." + methodName + "(" + JS_ARRAY_STRING + ")");
            assertEquals(JAVA_ARRAY.length, objectFromJava.list.size());
            assertEquals(JS_ARRAY_STRING, Arrays.toString(objectFromJava.list.toArray()));
        }
    }

    /**
     * Test that a Java array can be passed to JavaScript scope and accessed as JavaScript array in
     * JavaScript. Please note that array access must be enabled for this.
     */
    @Test
    public void testJavaArrayAsJSArray() {
        final HostAccess hostAccess = HostAccess.newBuilder().allowArrayAccess(true).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            bindings.putMember("arrayFromJava", JAVA_ARRAY);
            Value v = context.eval(ID, "var recreatedArray = [];" +
                            "for (var i = 0; i < arrayFromJava.length; i++)" +
                            "recreatedArray.push(arrayFromJava[i]);" +
                            "recreatedArray");
            commonCheck(v);
        }
    }

    /**
     * Test that a Java List can be passed to JavaScript scope and accessed as JavaScript array in
     * JavaScript. Please note that List access must be enabled for this.
     */
    @Test
    public void testJavaListAsJSArray() {
        final HostAccess hostAccess = HostAccess.newBuilder().allowListAccess(true).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            bindings.putMember("arrayFromJava", JAVA_LIST);
            Value v = context.eval(ID, "var recreatedArray = [];" +
                            "for (var i = 0; i < arrayFromJava.length; i++)" +
                            "recreatedArray.push(arrayFromJava[i]);" +
                            "recreatedArray");
            commonCheck(v);
        }
    }

    @Test
    public void testJavaReturnArrayAsJSArray() {
        testJavaReturnArrayOrListAsJSArray(true);
    }

    @Test
    public void testJavaReturnListAsJSArray() {
        testJavaReturnArrayOrListAsJSArray(false);
    }

    /**
     * Test that a Java array or Java List can be returned from a Java method called in JavaScript
     * and accessed as JavaScript array.
     */
    private static void testJavaReturnArrayOrListAsJSArray(boolean isArray) {
        final HostAccess.Builder hostAccessBuilder = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class);
        final HostAccess hostAccess = (isArray ? hostAccessBuilder.allowArrayAccess(true) : hostAccessBuilder.allowListAccess(true)).build();
        String methodName = isArray ? "methodThatReturnsArray" : "methodThatReturnsList";
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS();
            bindings.putMember("objectFromJava", objectFromJava);
            Value v = context.eval(ID, "var arrayFromJava = objectFromJava." + methodName + "();" +
                            "var recreatedArray = [];" +
                            "for (var i = 0; i < arrayFromJava.length; i++)" +
                            "recreatedArray.push(arrayFromJava[i]);" +
                            "recreatedArray");
            commonCheck(v);
        }
    }

    /**
     * Test that a Java array returned from a Java method is printed as a JavaScript array.
     */
    @Test
    public void testPrintJavaArrayInJS() {
        HostAccess accessWithArrays = HostAccess.newBuilder(HostAccess.EXPLICIT).allowArrayAccess(true).build();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(accessWithArrays).out(baos).err(baos).build()) {
            context.getBindings(ID).putMember("javaArray", new ToBePassedToJS());
            context.eval(ID, "var arrayFromJava = javaArray.methodThatReturnsArrayWithJSObject(" +
                            "{foo: 'bar', number: 42, f: function() { return 'yes';}, array: [2, 4, 8]});" +
                            "console.log(arrayFromJava);");
            assertEquals("[41, {foo: \"bar\", number: 42, f: function() { return 'yes';}, array: [2, 4, 8]}, \"string\", {x: 42, y: \"foo\"}]", baos.toString().trim());
            baos.reset();
            context.eval(ID, "var arrayFromJava = javaArray.methodThatReturnsArray();" +
                            "console.log(arrayFromJava);");
            assertEquals("[3, 4, 1, 5]", baos.toString().trim());
            baos.reset();
            context.eval(ID, "var arrayFromJavaAsValue = javaArray.methodThatReturnsArrayAsValue();" +
                            "console.log(arrayFromJavaAsValue);");
            assertEquals("[3, 4, 1, 5]", baos.toString().trim());
        }
    }

    @Test
    public void testForLetItemOfLazyArray() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value collect = context.eval(ID, "" + //
                            "(function (arr) {\n" + //
                            "  let collect = [];\n" + //
                            "  for (let item of arr) {\n" + //
                            "    collect.push(item);\n" + //
                            "    collect.push(item);\n" + //
                            "    collect.push(item);\n" + //
                            "  }\n" + //
                            "  return collect;\n" + //
                            "})\n" //
            );

            final List<Integer> list = Arrays.asList(5, 7, 11, 13, 17, 23);

            Value tripples = collect.execute(new LazyArray(list.iterator()));
            assertTrue("Array returned", tripples.hasArrayElements());
            assertEquals(list.size() * 3, tripples.getArraySize());
        }
    }

    @Test
    public void testManyTypes() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value arrays = context.eval(ID, "var arrays = [];" +
                            "var a;" +
                            "a = [1,2,3,4];" +
                            "arrays.push(a);" +
                            "a = [1000,2000,3000];" +
                            "arrays.push(a);" +
                            "a = [1.1,2.2,3.3];" +
                            "arrays.push(a);" +
                            "a = ['a','b','c'];" +
                            "arrays.push(a);" +
                            "a = [1,2,3,4]; a.push(5);" +
                            "arrays.push(a);" +
                            "a = [1000,2000,3000]; a.push(4000);" +
                            "arrays.push(a);" +
                            "a = [1.1,2.2,3.3]; a.push(4.4);" +
                            "arrays.push(a);" +
                            "a = ['a','b','c']; a.push('d');" +
                            "arrays.push(a);" +
                            "a = [,,1,2,3,4]; delete a[4];" +
                            "arrays.push(a);" +
                            "a = [1,,2,,3]; a.push(4);" +
                            "arrays.push(a);" +
                            "a = ['a',,'b',,'c']; a.push('d');" +
                            "arrays.push(a);" +
                            "a = [1]; a[2**31] = 2;" +
                            "arrays.push(a);" +
                            "arrays;");
            for (int i = 0; i < arrays.getArraySize(); i++) {
                Value array = arrays.getArrayElement(i);
                assertTrue(array.hasArrayElements());
                array.setArrayElement(0, i);
            }
        }
    }

    private static final class LazyArray implements ProxyArray {

        private final Iterator<?> it;
        private long at;

        LazyArray(Iterator<?> it) {
            this.it = it;
            this.at = 0;
        }

        @Override
        public Object get(long index) {
            if (index == at) {
                at++;
                return it.next();
            }
            return null;
        }

        @Override
        public void set(long index, Value value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return it.hasNext() ? at + 1 : at;
        }
    }
}

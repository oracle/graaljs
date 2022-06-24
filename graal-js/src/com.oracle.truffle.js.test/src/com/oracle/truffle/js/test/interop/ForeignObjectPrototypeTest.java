/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.test.JSTest;

public class ForeignObjectPrototypeTest {

    @Test
    public void testProxyArray() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").build()) {
            ProxyArray array = ProxyArray.fromArray("fun", "with", "proxy", "array");
            Value result = context.eval(ID, "(array) => array.sort()").execute(array);
            assertEquals(Arrays.asList("array", "fun", "proxy", "with"), result.as(List.class));

            array = ProxyArray.fromArray(4, 5, 6, 1, 2, 3, 7, 8, 9);
            result = context.eval(ID, "(array) => array.reduce((a,b) => a + b)").execute(array);
            assertEquals(45, result.asInt());
        }
    }

    @Test
    public void testHostArray() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").allowHostAccess(HostAccess.ALL).build()) {
            Value array = context.asValue(new String[]{"fun", "with", "proxy", "array"});
            Value result = context.eval(ID, "(array) => array.sort()").execute(array);
            assertEquals(Arrays.asList("array", "fun", "proxy", "with"), result.as(List.class));

            array = context.asValue(new int[]{4, 5, 6, 1, 2, 3, 7, 8, 9});
            result = context.eval(ID, "(array) => array.reduce((a,b) => a + b)").execute(array);
            assertEquals(45, result.asInt());
            result = context.eval(ID, "(array) => array.sort()").execute(array);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result.as(List.class));
        }
    }

    @Test
    public void testProxyExecutable() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").build()) {
            context.getBindings(ID).putMember("moo", (ProxyExecutable) args -> {
                assertEquals(3, args.length);
                assertTrue(args[0].isString());
                assertTrue(args[1].isNumber());
                assertTrue(args[2].isBoolean());
                assertEquals("foo", args[0].asString());
                assertEquals(123, args[1].asInt());
                assertTrue(args[2].asBoolean());
                return "hi";
            });
            Value value = context.eval("js", "(function(...args) { return moo.apply(null, args); })('foo', 123, true)");
            assertTrue(value.isString());
            assertEquals("hi", value.asString());
        }
    }

    @Test
    public void testHostMethodStatic() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").allowHostAccess(HostAccess.ALL).allowHostClassLookup(s -> true).build()) {
            Value method = context.eval(ID, "Java.type('java.util.Arrays').asList");
            assertTrue(method.canExecute());
            assertFalse(method.hasMembers());
            Value result = context.eval(ID, "m => m.call(null, 0, 8, 1, 5)").execute(method);
            assertEquals(Arrays.asList(0, 8, 1, 5), result.as(List.class));
            result = context.eval(ID, "m => m.apply(null, [0, 8, 1, 5])").execute(method);
            assertEquals(Arrays.asList(0, 8, 1, 5), result.as(List.class));
        }
    }

    @Test
    public void testPrototype() {
        testPrototypeIntl("Array", ProxyArray.fromArray("fun", "with", "proxy", "array"));
        testPrototypeIntl("Date", Instant.now());
        testPrototypeIntl("Map", new TestTruffleHash());
        testPrototypeIntl("String", new TestTruffleString());
        testPrototypeIntl("Boolean", new TestTruffleBoolean());
        testPrototypeIntl("Number", new TestTruffleNumber());
        testPrototypeIntl("Function", (ProxyExecutable) v -> true);
        testPrototypeIntl("Object", new Object());
    }

    private static void testPrototypeIntl(String prototype, Object obj) {
        String code = "(obj) => { var proto = Object.getPrototypeOf(obj); \n" +
                        "  var protoProto = Object.getPrototypeOf(proto); \n" +
                        "  return protoProto === " + prototype + ".prototype; \n" +
                        "}";
        try (Context context = JSTest.newContextBuilder(ID).build()) {
            Value result = context.eval(ID, code).execute(obj);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testPrototypeDisabled() {
        testDisabled(ProxyArray.fromArray("fun", "with", "proxy", "array"));
        testDisabled((ProxyExecutable) args -> "hi");
        testDisabled(Instant.now());
    }

    private static void testDisabled(Object array) {
        String code = "(obj) => { return Object.getPrototypeOf(obj) === null; }";
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "false").build()) {
            Value result = context.eval(ID, code).execute(array);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testHostMethodHasPrecedence() {
        String codeGetEpochSecond = "(obj) => { return obj.getEpochSecond(); }";
        try (Context context = JSTest.newContextBuilder(ID).allowHostAccess(HostAccess.ALL).build()) {
            Instant inst = Instant.now();

            // check getter from Java can be called
            long second = context.eval(ID, codeGetEpochSecond).execute(inst).asLong();
            Assert.assertEquals(inst.getEpochSecond(), second);

            // provide your own getter
            context.eval(ID, "(obj) => { Object.getPrototypeOf(obj).getEpochSecond = () => { return 666; }; };").execute(inst);

            // verify that still the host getter is called
            Instant inst2 = Instant.now();
            Value result = context.eval(ID, codeGetEpochSecond).execute(inst2);
            Assert.assertEquals(inst2.getEpochSecond(), result.asLong());
        }
    }

    @Test
    public void testJSBuiltinCanBeOverwritten() {
        String codeGetUTCString = "(obj) => { return obj.toUTCString(); }";
        try (Context context = JSTest.newContextBuilder(ID).allowHostAccess(HostAccess.ALL).build()) {
            Instant inst = Instant.now();

            // check JS Prototype method can be called
            String utcString = context.eval(ID, codeGetUTCString).execute(inst).asString();
            Assert.assertTrue(utcString.length() > 10);

            // provide your own getter
            context.eval(ID, "(obj) => { Object.getPrototypeOf(obj).toUTCString = () => { return 'special'; }; };").execute(inst);

            // verify that on another object this overwritten method is called
            Instant inst2 = Instant.now();
            Value result = context.eval(ID, codeGetUTCString).execute(inst2);
            Assert.assertEquals("special", result.asString());
        }
    }

    @Test
    public void testForeignInstanceof() {
        // test expected Instance
        Assert.assertTrue(testInstanceofIntl("Array", ProxyArray.fromArray("fun", "with", "proxy", "array")));
        // Assert.assertTrue(testInstanceofIntl("Date", Instant.now())); //see GR-39319
        Assert.assertTrue(testInstanceofIntl("Map", new TestTruffleHash()));
        Assert.assertTrue(testInstanceofIntl("String", new TestTruffleString()));
        Assert.assertTrue(testInstanceofIntl("Boolean", new TestTruffleBoolean()));
        Assert.assertTrue(testInstanceofIntl("Number", new TestTruffleNumber()));
        Assert.assertTrue(testInstanceofIntl("Function", (ProxyExecutable) v -> true));
        Assert.assertTrue(testInstanceofIntl("Object", new Object()));

        // test non-matching instance
        Assert.assertFalse(testInstanceofIntl("RegExp", ProxyArray.fromArray("fun", "with", "proxy", "array")));
        Assert.assertFalse(testInstanceofIntl("RegExp", Instant.now()));
        Assert.assertFalse(testInstanceofIntl("RegExp", new TestTruffleHash()));
        Assert.assertFalse(testInstanceofIntl("RegExp", new TestTruffleString()));
        Assert.assertFalse(testInstanceofIntl("RegExp", new TestTruffleBoolean()));
        Assert.assertFalse(testInstanceofIntl("RegExp", new TestTruffleNumber()));
        Assert.assertFalse(testInstanceofIntl("RegExp", (ProxyExecutable) v -> true));
        Assert.assertFalse(testInstanceofIntl("RegExp", new Object()));
    }

    private static boolean testInstanceofIntl(String prototype, Object obj) {
        String code = "(obj) => { return (obj instanceof " + prototype + ") && (obj instanceof Object); }";
        try (Context context = JSTest.newContextBuilder(ID).build()) {
            Value result = context.eval(ID, code).execute(obj);
            return result.asBoolean();
        }
    }

    @Test
    public void testForeignRightPrototype() {
        String code = "ForeignObjectPrototype = Object.getPrototypeOf(new java.lang.Object());\n" +
                        "function f() {}; \n" +
                        "f.prototype = ForeignObjectPrototype;\n" +
                        "new java.lang.Object() instanceof f;";
        testTrue(code);
    }

    private static void testTrue(String code) {
        Assert.assertTrue(testIntl(code));
    }

    private static void testFalse(String code) {
        Assert.assertFalse(testIntl(code));
    }

    private static boolean testIntl(String code) {
        try (Context context = JSTest.newContextBuilder(ID).allowAllAccess(true).allowHostAccess(HostAccess.ALL).build()) {
            Value result = context.eval(ID, code);
            return result.asBoolean();
        }
    }

    @Test
    public void testCallableProxies() {
        String code = "new java.lang.Object instanceof new Proxy(Object, {});";
        testTrue(code);

        code = "var handler = { get(target, prop, recv) { return (prop === 'prototype') ? Object.prototype : Reflect.get(target, prop, recv); } };\n" +
                        "var proxy = new Proxy(function() {}, handler);\n" +
                        "new java.lang.Object() instanceof proxy";
        testTrue(code);

        code = "var handler = { get(target, prop, recv) { if (prop === 'prototype') { throw new Error() } else { return Reflect.get(target, prop, recv); } } };\n" +
                        "var proxy = new Proxy(function() {}, handler);\n" +
                        "42 instanceof proxy";
        testFalse(code);
    }

    @ExportLibrary(InteropLibrary.class)
    public static class TestTruffleHash implements TruffleObject {

        @ExportMessage
        @SuppressWarnings("static-method")
        boolean hasHashEntries() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        long getHashSize() {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        Object getHashEntriesIterator() {
            return null;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class TestTruffleString implements TruffleObject {
        @ExportMessage
        @SuppressWarnings("static-method")
        boolean isString() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        String asString() {
            return "";
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class TestTruffleBoolean implements TruffleObject {
        @ExportMessage
        @SuppressWarnings("static-method")
        boolean isBoolean() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        boolean asBoolean() {
            return true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class TestTruffleNumber implements TruffleObject {
        @ExportMessage
        @SuppressWarnings("static-method")
        boolean isNumber() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final boolean fitsInByte() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final boolean fitsInShort() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final boolean fitsInInt() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final boolean fitsInLong() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final boolean fitsInFloat() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final boolean fitsInDouble() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final byte asByte() {
            return (byte) 0;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final short asShort() {
            return (short) 0;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final int asInt() {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final long asLong() {
            return 0L;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final float asFloat() {
            return 0.0F;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        final double asDouble() {
            return 0.0D;
        }

    }
}

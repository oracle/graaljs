/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.interop.AsyncInteropTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class IteratorPrototypeBuiltins {
    @Test
    public void testObject() {
        String src = "var parent = Object.getPrototypeOf(Iterator.prototype) === Object.prototype; var proto = typeof Iterator.prototype";
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, src);
            var parent = context.getBindings(JavaScriptLanguage.ID).getMember("parent");
            var proto = context.getBindings(JavaScriptLanguage.ID).getMember("proto");

            Assert.assertTrue(parent.asBoolean());
            Assert.assertEquals("object", proto.asString());
        }
    }

    @Test
    public void testConstructor() {
        String src = "Iterator.prototype.constructor === Iterator";
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Assert.assertTrue(context.eval(JavaScriptLanguage.ID, src).asBoolean());
        }
    }

    @Test
    public void testMap() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().map(x => 2*x).toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(3, result.getArraySize());
            Assert.assertEquals(2, result.getArrayElement(0).asInt());
            Assert.assertEquals(4, result.getArrayElement(1).asInt());
            Assert.assertEquals(6, result.getArrayElement(2).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.map.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().map(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.map.call({next: () => ({value: 1, done: true})}, x => x).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.map.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).next()");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testFilter() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3, 4].values().filter(x => x%2===0).toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(2, result.getArraySize());
            Assert.assertEquals(2, result.getArrayElement(0).asInt());
            Assert.assertEquals(4, result.getArrayElement(1).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.filter.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().filter(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.filter.call({next: () => ({value: 1, done: true})}, () => true).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.filter.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).next()");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testTake() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3, 4].values().take(2).toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(2, result.getArraySize());
            Assert.assertEquals(1, result.getArrayElement(0).asInt());
            Assert.assertEquals(2, result.getArrayElement(1).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.take.call({}, 2)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().take(NaN)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().take(-1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            try {
                context.eval(JavaScriptLanguage.ID, "[].values().take(() => 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.take.call({next: () => ({value: 1, done: true})}, 1).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.take.call({next: () => ({value: 1, done: false})}, 0).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());
        }
    }

    @Test
    public void testDrop() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3, 4].values().drop(2).toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(2, result.getArraySize());
            Assert.assertEquals(3, result.getArrayElement(0).asInt());
            Assert.assertEquals(4, result.getArrayElement(1).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.drop.call({}, 2)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().drop(NaN)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().drop(-1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            try {
                context.eval(JavaScriptLanguage.ID, "[].values().drop(() => 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.drop.call({next: () => ({value: 1, done: true})}, 1).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.drop.call({next: () => ({value: 1, done: false})}, 10).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertFalse(result.getMember("done").asBoolean());
            Assert.assertEquals(1, result.getMember("value").asInt());
        }
    }

    @Test
    public void testIndexed() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2].values().indexed().toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(2, result.getArraySize());
            Assert.assertTrue(result.getArrayElement(0).hasMembers());
            Assert.assertEquals(2, result.getArrayElement(0).getArraySize());
            Assert.assertEquals(0, result.getArrayElement(0).getArrayElement(0).asInt());
            Assert.assertEquals(1, result.getArrayElement(0).getArrayElement(1).asInt());
            Assert.assertTrue(result.getArrayElement(1).hasMembers());
            Assert.assertEquals(2, result.getArrayElement(1).getArraySize());
            Assert.assertEquals(1, result.getArrayElement(1).getArrayElement(0).asInt());
            Assert.assertEquals(2, result.getArrayElement(1).getArrayElement(1).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.drop.indexed({})");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.indexed.call({next: () => ({value: 1, done: true})}, 1).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());
        }
    }

    @Test
    public void testFlatMap() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2].values().flatMap(x => [0, x]).toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(4, result.getArraySize());
            Assert.assertEquals(0, result.getArrayElement(0).asInt());
            Assert.assertEquals(1, result.getArrayElement(1).asInt());
            Assert.assertEquals(0, result.getArrayElement(2).asInt());
            Assert.assertEquals(2, result.getArrayElement(3).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.flatMap.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().flatMap(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.flatMap.call({next: () => ({value: 1, done: true})}, x => x).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.flatMap.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).next()");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testReduce() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2].values().reduce((a, b) => a + b, 1)");
            Assert.assertEquals(4, result.asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.reduce.call({}, x => x, 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[1].values().reduce(1, 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().reduce(x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.reduce.call({next: () => ({value: 1, done: true})}, x => x, 0)");
            Assert.assertEquals(0, result.asInt());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.reduce.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')})");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testToArray() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2].values().toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(2, result.getArraySize());
            Assert.assertEquals(1, result.getArrayElement(0).asInt());
            Assert.assertEquals(2, result.getArrayElement(1).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.toArray.call({})");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.toArray.call({next: () => ({value: 1, done: true})})");
            Assert.assertEquals(0, result.getArraySize());
        }
    }

    @Test
    public void testToAsync() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "[1, 2].values().toAsync().next().then(x => console.log(x.done, x.value))");
            Assert.assertEquals("false 1\n", out.toString());
        }
    }

    @Test
    public void testForEach() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "var sum = 1; [1, 2].values().forEach(x => sum+=x); sum");
            Assert.assertEquals(4, result.asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.forEach.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[1].values().forEach(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.forEach.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')})");
            Assert.assertTrue(result.isNull());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.forEach.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')})");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testSome() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().some(x => x > 2)");
            Assert.assertTrue(result.isBoolean());
            Assert.assertTrue(result.asBoolean());

            result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().some(x => x > 3)");
            Assert.assertTrue(result.isBoolean());
            Assert.assertFalse(result.asBoolean());


            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.some.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[1].values().some(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.some.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')})");
            Assert.assertTrue(result.isBoolean());
            Assert.assertFalse(result.asBoolean());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.some.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')})");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testEvery() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().every(x => x < 4)");
            Assert.assertTrue(result.isBoolean());
            Assert.assertTrue(result.asBoolean());

            result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().every(x => x < 3)");
            Assert.assertTrue(result.isBoolean());
            Assert.assertFalse(result.asBoolean());


            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.every.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[1].values().every(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.every.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')})");
            Assert.assertTrue(result.isBoolean());
            Assert.assertTrue(result.asBoolean());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.every.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')})");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testFind() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().find(x => x > 3)");
            Assert.assertTrue(result.isNull());

            result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().find(x => x > 2)");
            Assert.assertTrue(result.isNumber());
            Assert.assertEquals(3, result.asInt());


            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.find.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[1].values().find(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.find.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')})");
            Assert.assertTrue(result.isNull());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.find.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')})");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }

    @Test
    public void testToString() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype[Symbol.toStringTag]");
            Assert.assertEquals("Iterator", result.asString());
        }
    }
}

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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.interop.AsyncInteropTest;

public class AsyncIteratorPrototypeBuiltinsTest {
    @Test
    public void testObject() {
        String src = "var parent = Object.getPrototypeOf(AsyncIterator.prototype) === Object.prototype; var proto = typeof AsyncIterator.prototype";
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
        String src = "AsyncIterator.prototype.constructor === AsyncIterator";
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Assert.assertTrue(context.eval(JavaScriptLanguage.ID, src).asBoolean());
        }
    }

    @Test
    public void testMap() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3]).map(x => 2*x).toArray().then(x => console.log(x))");
            Assert.assertEquals("2,4,6\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.map.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).map(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.map.call({next: () => ({value: 1, done: true})}, x => x).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("undefined undefined boolean true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.map.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).next().catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testFilter() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3, 4]).filter(x => x%2===0).toArray().then(x => console.log(x))");
            Assert.assertEquals("2,4\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.filter.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).filter(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.filter.call({next: () => ({value: 1, done: true})}, () => true).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("undefined undefined boolean true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.filter.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).next().catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testTake() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3, 4]).take(2).toArray().then(x => console.log(x))");
            Assert.assertEquals("1,2\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.take.call({}, 2)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).take(NaN)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).take(-1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).take(() => 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.take.call({next: () => ({value: 1, done: true})}, 1).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("undefined undefined boolean true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.take.call({next: () => ({value: 1, done: false})}, 0).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("undefined undefined boolean true\n", out.toString());
        }
    }

    @Test
    public void testDrop() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3, 4]).drop(2).toArray().then(x => console.log(x))");
            Assert.assertEquals("3,4\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.drop.call({}, 2)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).drop(NaN)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).drop(-1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).drop(() => 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("RangeError: ", e.getMessage().substring(0, "RangeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.drop.call({next: () => ({value: 1, done: true})}, 1).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("undefined undefined boolean true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.drop.call({next: () => ({value: 1, done: false})}, 10).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("number 1 boolean false\n", out.toString());
        }
    }

    @Test
    public void testIndexed() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2]).indexed().toArray().then(x => {for (const y of x) console.log(y)})");
            Assert.assertEquals("0,1\n1,2\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.drop.indexed({})");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.indexed.call({next: () => ({value: 1, done: true})}, 1).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("undefined undefined boolean true\n", out.toString());
        }
    }

    @Test
    public void testFlatMap() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2]).flatMap(x => [0, x]).toArray().then(x => console.log(x))");
            Assert.assertEquals("0,1,0,2\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.flatMap.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).flatMap(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.flatMap.call({next: () => ({value: 1, done: true})}, x => x).next().then(x => console.log(typeof x.value, x.value, typeof x.done, x.done))");
            Assert.assertEquals("undefined undefined boolean true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.flatMap.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).next().catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testReduce() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2]).reduce((a, b) => a + b, 1).then(x => console.log(x))");
            Assert.assertEquals("4\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.reduce.call({}, x => x, 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1]).reduce(1, 0)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([]).reduce(x => x).catch(err => console.log(err))");
            Assert.assertTrue(out.toString().startsWith("TypeError: "));
            out.reset();

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.reduce.call({next: () => ({value: 1, done: true})}, x => x, 0).then(x => console.log(x))");
            Assert.assertEquals("0\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.reduce.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testToArray() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2]).toArray().then(x => console.log(x))");
            Assert.assertEquals("1,2\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.toArray.call({})");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.toArray.call({next: () => ({value: 1, done: true})}).then(x => console.log(x))");
            Assert.assertEquals("\n", out.toString());
        }
    }

    @Test
    public void testForEach() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2]).forEach(x => console.log(x)).then(() => console.log())");
            Assert.assertEquals("1\n2\n\n", out.toString());
            out.reset();

            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.forEach.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1]).forEach(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.forEach.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')}).then(() => console.log())");
            Assert.assertEquals("\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.forEach.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testSome() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3]).some(x => x > 2).then(x => console.log(x))");
            Assert.assertEquals("true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3]).some(x => x > 3).then(x => console.log(x))");
            Assert.assertEquals("false\n", out.toString());
            out.reset();


            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.some.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1]).some(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.some.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')}).then(x => console.log(x))");
            Assert.assertEquals("false\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.some.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testEvery() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3]).every(x => x < 4).then(x => console.log(x))");
            Assert.assertEquals("true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3]).every(x => x < 3).then(x => console.log(x))");
            Assert.assertEquals("false\n", out.toString());
            out.reset();


            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.every.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1]).every(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.every.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')}).then(x => console.log(x))");
            Assert.assertEquals("true\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.every.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testFind() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3]).find(x => x > 3).then(x => console.log(x))");
            Assert.assertEquals("undefined\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1, 2, 3]).find(x => x > 2).then(x => console.log(x))");
            Assert.assertEquals("3\n", out.toString());
            out.reset();


            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.find.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([1]).find(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype.find.call({next: () => ({value: 1, done: true})}, x => {throw new Error('test')}).then(x => console.log(x))");
            Assert.assertEquals("undefined\n", out.toString());
            out.reset();

            context.eval(JavaScriptLanguage.ID, "var called = false; AsyncIterator.prototype.find.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).catch(err => console.log(called, err))");
            Assert.assertEquals("true Error: test\n", out.toString());
        }
    }

    @Test
    public void testToString() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "AsyncIterator.prototype[Symbol.toStringTag]");
            Assert.assertEquals("Async Iterator", result.asString());
        }
    }


    @Test
    public void testCombined() {
        AsyncInteropTest.TestOutput out = new AsyncInteropTest.TestOutput();
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        builder.out(out);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, "AsyncIterator.from([4,5,6,7]).indexed().flatMap(x => x).filter(x=>x>1).map(x => x*2).drop(3).take(1).reduce((a, b) => a + b, -1).then(x => console.log(x))");
            Assert.assertEquals("11\n", out.toString());
        }
    }
}

/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.regress;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import java.util.Map;
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Assert;
import org.junit.Test;

public class GR45823 {

    @Test
    public void testSyncPolyglot() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object iterable = createSyncIterable();
            context.getBindings("js").putMember("iterable", iterable);
            setUpForward(context, true);
            String code = "Array.from(iterable).toString()";
            Value result = context.eval("js", code);
            Assert.assertEquals("5,4,3,2,1", result.asString());
        }
    }

    private static Object createSyncIterable() {
        ProxyExecutable nextMethod = new ProxyExecutable() {

            private int remains = 5;

            @Override
            public Object execute(Value... arguments) {
                return ProxyObject.fromMap((remains == 0) ? Map.of("done", true) : Map.of("value", remains--));
            }
        };
        ProxyExecutable iteratorMethod = (ProxyExecutable) (Value... args) -> ProxyObject.fromMap(Map.of("next", nextMethod));
        return ProxyObject.fromMap(Map.of("foreignIterator", iteratorMethod));
    }

    @Test
    public void testSyncJava() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).build()) {
            context.getBindings("js").putMember("iterable", new SyncIterable());
            setUpForward(context, true);
            String code = "Array.from(iterable).toString()";
            Value result = context.eval("js", code);
            Assert.assertEquals("5,4,3,2,1", result.asString());
        }
    }

    @Test
    public void testIteratorHelpers() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).option(JSContextOptions.ITERATOR_HELPERS_NAME, "true").build()) {
            context.getBindings("js").putMember("iterable", new SyncIterable());
            String code = "Iterator.prototype.toArray.call(iterable.foreignIterator()).toString()";
            Value result = context.eval("js", code);
            Assert.assertEquals("5,4,3,2,1", result.asString());

            code = "Iterator.from(iterable.foreignIterator()).toArray().toString()";
            result = context.eval("js", code);
            Assert.assertEquals("5,4,3,2,1", result.asString());
        }
    }

    @Test
    public void testAsyncPolyglot() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw").build()) {
            Object iterable = createAsyncIterable();
            context.getBindings("js").putMember("iterable", iterable);
            setUpForward(context, false);
            asyncTest(context);
        }
    }

    @Test
    public void testAsyncFromSyncPolyglot() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw").build()) {
            Object iterable = createSyncIterable();
            context.getBindings("js").putMember("iterable", iterable);
            setUpForward(context, true);
            asyncTest(context);
        }
    }

    private static Object createAsyncIterable() {
        ProxyExecutable nextMethod = new ProxyExecutable() {

            private int remains = 5;

            @Override
            public Object execute(Value... arguments) {
                ProxyExecutable thenMethod = (ProxyExecutable) (Value... args) -> args[0].execute(ProxyObject.fromMap((remains == 0) ? Map.of("done", true) : Map.of("value", remains--)));
                return ProxyObject.fromMap(Map.of("then", thenMethod));
            }
        };
        ProxyExecutable iteratorMethod = (ProxyExecutable) (Value... args) -> ProxyObject.fromMap(Map.of("next", nextMethod));
        return ProxyObject.fromMap(Map.of("foreignAsyncIterator", iteratorMethod));
    }

    @Test
    public void testAsyncJava() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw").build()) {
            context.getBindings("js").putMember("iterable", new AsyncIterable());
            setUpForward(context, false);
            asyncTest(context);
        }
    }

    @Test
    public void testAsyncFromSyncJava() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw").build()) {
            context.getBindings("js").putMember("iterable", new SyncIterable());
            setUpForward(context, true);
            asyncTest(context);
        }
    }

    private static void setUpForward(Context context, boolean sync) {
        String code = "var FOP = Object.getPrototypeOf(iterable);";
        if (sync) {
            code += "Object.defineProperty(FOP, Symbol.iterator, { get() { return this.foreignIterator; } });";
        } else {
            code += "Object.defineProperty(FOP, Symbol.asyncIterator, { get() { return this.foreignAsyncIterator; } });";
        }
        context.eval("js", code);
    }

    private static void asyncTest(Context context) {
        String code = """
                            (async function() {
                                let array = [];
                                for await (item of iterable) array.push(item);
                                let result = String(array);
                                if ("5,4,3,2,1" !== result) {
                                    throw new Error("Unexpected result: " + result);
                                }
                            })();
                        """;
        context.eval("js", code);
    }

    public static class SyncIterable {

        public Object foreignIterator() {
            return new SyncIterator();
        }

    }

    public static class SyncIterator {
        private int remains = 5;

        public Object next() {
            boolean done = (remains == 0);
            return new IteratorResult(done, done ? null : remains--);
        }

    }

    public static class IteratorResult {
        public final boolean done;
        public final Object value;

        public IteratorResult(boolean done, Object value) {
            this.done = done;
            this.value = value;
        }

    }

    public static class AsyncIterable {

        public Object foreignAsyncIterator() {
            return new AsyncIterator();
        }

    }

    public static class AsyncIterator {
        private int remains = 5;

        public Object next() {
            boolean done = (remains == 0);
            return new Thenable(new IteratorResult(done, done ? null : remains--));
        }

    }

    public static class Thenable {
        private final Object result;

        public Thenable(Object result) {
            this.result = result;
        }

        public void then(Function<Object, Object> resolve, @SuppressWarnings("unused") Function<Object, Object> reject) {
            resolve.apply(result);
        }

    }

}

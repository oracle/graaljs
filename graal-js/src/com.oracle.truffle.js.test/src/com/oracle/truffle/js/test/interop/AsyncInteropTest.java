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
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class AsyncInteropTest {

    /**
     * A Java object with a method called 'then' can be used as thenable.
     */
    @Test
    public void testJavaThenable() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            Thenable then2 = (resolve, reject) -> resolve.executeVoid(42);
            Thenable then1 = (resolve, reject) -> resolve.executeVoid(then2);
            context.getBindings(ID).putMember("myJavaThenable", then1);
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            "  let x = await myJavaThenable;" +
                            "  console.log(x);" +
                            "})");
            asyncFn.executeVoid();
        }
        assertEquals("42\n", out.toString());
    }

    /**
     * A Java object with a method called 'then' can be used as the `executor` function for a
     * JavaScript promise.
     */
    @Test
    public void testJavaExecutor() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            Executable javaExecutable = (resolve, reject) -> resolve.execute(42);
            context.getBindings(ID).putMember("javaExecutable", javaExecutable);
            Value asyncFn = context.eval(ID, "new Promise(javaExecutable).then(x => console.log(x));");
            Consumer<Object> javaThen = (v) -> out.write("All done :)");
            asyncFn.invokeMember("then", javaThen);
        }
        assertEquals("42\nAll done :)", out.toString());
    }

    /**
     * Java functions can be used as functions in `then`.
     */
    @Test
    public void testPromiseJavaThen() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            Value jsPromise = context.eval(ID, "Promise.resolve(42);");
            Consumer<Object> javaThen = (value) -> out.write("Resolved from JavaScript: " + value);
            jsPromise.invokeMember("then", javaThen);
        }
        assertEquals("Resolved from JavaScript: 42", out.toString());
    }

    /**
     * Java functions can be used as functions in `then`.
     */
    @Test
    public void testPromiseJavaThenAsync() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            "  return await 42;" +
                            "})");
            Value jsPromise = asyncFn.execute();
            Consumer<Object> then = (value) -> out.write("Resolved from Java: " + value);
            Consumer<Object> catchy = (value) -> out.write("Promise failed!" + value);
            jsPromise.invokeMember("then", then).invokeMember("catch", catchy);
        }
        assertEquals("Resolved from Java: 42", out.toString());
    }

    /**
     * Java functions can be used as functions in `catch`.
     */
    @Test
    public void testPromiseJavaCatch() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            "  throw 42;" +
                            "})");
            Value promise = asyncFn.execute();
            Consumer<Object> then = (value) -> out.write("Resolved from Java: " + value);
            Consumer<Object> catchy = (value) -> out.write("Promise failed: " + value);
            promise.invokeMember("then", then).invokeMember("catch", catchy);
        }
        assertEquals("Promise failed: 42", out.toString());
    }

    /**
     * Wait on a Java Completable future and resume JS execution when it completes.
     */
    @Test
    public void testJavaCompletableFutureToPromise() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            CompletableFuture<String> javaFuture = new CompletableFuture<>();
            // Wrap Java future in a JS Promise
            Value jsPromise = wrapPromise(context, javaFuture);
            context.getBindings(ID).putMember("myJsPromise", jsPromise);
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            "  console.log('pausing...');" +
                            "  var foo = await myJsPromise;" +
                            "  console.log('resumed with value ' + foo);" +
                            "})");
            assertEquals("", out.toString());
            asyncFn.execute();
            assertEquals("pausing...\n", out.toString());
            javaFuture.complete("from Java");
            assertEquals("pausing...\nresumed with value from Java\n", out.toString());
        }
    }

    /**
     * Chain JS and Java reactions.
     */
    @Test
    public void testChainReactions() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            Function<Integer, Integer> incJava = i -> i + 1;
            Consumer<Object> print = (value) -> out.write("Final result: " + value);
            context.getBindings(ID).putMember("incJava", incJava);
            Value jsPromise = context.eval(ID, "var incJs = (x) => x + 1;" +
                            "async function foo(val) {" +
                            "  return val + 1;" +
                            "};" +
                            "foo(41).then(incJava).then(incJs);");
            assertEquals("", out.toString());
            jsPromise.invokeMember("then", incJava).invokeMember("then", print);
            assertEquals("Final result: 45", out.toString());
        }
    }

    /**
     * Chain CompletableFutures and JS functions.
     */
    @Test
    public void testChainCompletableFuturePromises() throws ExecutionException, InterruptedException {
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            CompletableFuture<String> javaFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    assert false;
                }
                return "Java";
            });
            Value jsFunction = context.eval("js", "(function jsFunction(v) { return v + 'JS'; })");
            String result = javaFuture.thenCompose(asChainable(jsFunction)).get();
            assertEquals("JavaJS", result);
        }
    }

    private static Function<String, CompletionStage<String>> asChainable(Value jsFunction) {
        assert jsFunction.canExecute();
        return v -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            try {
                future.complete(jsFunction.execute(v).asString());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
            return future;
        };
    }

    private static Value wrapPromise(Context context, CompletableFuture<String> javaFuture) {
        Value global = context.getBindings("js");
        Value promiseConstructor = global.getMember("Promise");
        return promiseConstructor.newInstance((ProxyExecutable) arguments -> {
            Value resolve = arguments[0];
            Value reject = arguments[1];
            javaFuture.whenComplete((result, ex) -> {
                if (result != null) {
                    resolve.execute(result);
                } else {
                    reject.execute(ex);
                }
            });
            // return value of function(resolve,reject) is ignored by `new Promise()`.
            return null;
        });
    }

    public interface Thenable {
        void then(Value onResolve, Value onReject);
    }

    @FunctionalInterface
    public interface Executable {
        void onPromiseCreation(Value onResolve, Value onReject);
    }

    private static class TestOutput extends ByteArrayOutputStream {

        void write(String text) {
            try {
                this.write(text.getBytes());
            } catch (IOException e) {
                assert false;
            }
        }

        @Override
        public synchronized String toString() {
            return new String(this.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}

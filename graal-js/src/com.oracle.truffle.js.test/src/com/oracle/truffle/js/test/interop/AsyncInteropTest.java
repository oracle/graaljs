/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;

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
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class AsyncInteropTest {

    private static final String LF = System.getProperty("line.separator");

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"none"</code> (or not
     * provided), no warnings are printed when rejected promises are not handled using `catch()`.
     */
    @Test
    public void testJavaUnhandledRejectionNone() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).err(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "none").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            "  throw 'failed!!';" +
                            "  console.log(x);" +
                            "})");
            asyncFn.executeVoid();
        }
        assertTrue(out.toString().isEmpty());
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"warn"</code>, a warning
     * is printed to the stderr when a promise is rejected in JS land and not handled in Java or JS.
     */
    @Test
    public void testJavaUnhandledRejectionWarn() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).err(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "warn").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            "  throw 'failed!!';" +
                            "  console.log(x);" +
                            "})");
            asyncFn.executeVoid();
            assertEquals("[GraalVM JavaScript Warning] Unhandled promise rejection: failed!!" + LF, out.toString());
        }
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"warn"</code>, a warning
     * is printed when a promise rejection is not immediately handled. Another warning is printed
     * when a reaction is registered later on (e.g., in Java).
     */
    @Test
    public void testJavaHandledRejection() {
        TestOutput out = new TestOutput();
        TestOutput err = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).err(err).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "warn").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            "  throw 'failed!!';" +
                            "  console.log(x);" +
                            "})");
            Value asyncPromise = asyncFn.execute();
            Consumer<Object> javaThen = (v) -> out.write("Got exception: " + v.toString());
            asyncPromise.invokeMember("catch", javaThen);
        }
        assertEquals("[GraalVM JavaScript Warning] Unhandled promise rejection: failed!!" + LF +
                        "[GraalVM JavaScript Warning] Promise rejection was handled asynchronously: failed!!" + LF, err.toString());
        assertEquals("Got exception: failed!!", out.toString());
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"warn"</code>, a warning
     * is printed when a promise rejection is not immediately handled. Another warning is printed
     * when a reaction is registered later on (e.g., in Java).
     */
    @Test
    public void testJavaHandledRejectionStep() {
        TestOutput out = new TestOutput();
        TestOutput err = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).err(err).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "warn").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            Value promise = context.eval(ID, "Promise.reject(42);");
            assertEquals("[GraalVM JavaScript Warning] Unhandled promise rejection: 42" + LF, err.toString());
            err.reset();
            Consumer<Object> javaThen = (v) -> out.write("Promise rejected: " + v.toString());
            promise.invokeMember("catch", javaThen);
        }
        assertEquals("[GraalVM JavaScript Warning] Promise rejection was handled asynchronously: 42" + LF, err.toString());
        assertEquals("Promise rejected: 42", out.toString());
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"warn"</code>, no warning
     * is printed when a promise rejection is immediately handled.
     */
    @Test
    public void testJSHandledRejection() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).err(out).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "warn").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            context.eval(ID, "" +
                            "(async function foo() {" +
                            "  throw 'failed!!';" +
                            "  console.log(x);" +
                            "})().catch(x => console.log('Got exception: ' + x));");
        }
        assertEquals("Got exception: failed!!\n", out.toString());
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"warn"</code>, no warning
     * is printed when a promise rejection is handled before leaving a Polyglot context.
     */
    @Test
    public void testJSHandledRejectionLater() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).err(out).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "warn").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            context.eval(ID, "" +
                            "const promise = (async function foo() {" +
                            "  throw 'failed!!';" +
                            "  console.log(x);" +
                            "})();" +
                            "for (var i = 0; i < 42; i++);" +
                            "promise.catch(x => console.log('Got exception: ' + x + ' --- ' + i));");
        }
        assertEquals("Got exception: failed!! --- 42\n", out.toString());
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"throw"</code> a polyglot
     * exception is thrown as soon as an unhandled promise rejection is detected.
     */
    @Test
    public void testJavaUnhandledRejectionThrow() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).err(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "throw").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            try {
                context.eval(ID, "Promise.reject(42);");
                assert false;
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertEquals("Error: Unhandled promise rejection: 42", e.getMessage());
            }
        }
        assertTrue(out.toString().isEmpty());
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"throw"</code>, no
     * exception is raised when a promise rejection is immediately handled.
     */
    @Test
    public void testJSHandledRejectionThrow() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).err(out).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "throw").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            context.eval(ID, "" +
                            "(async function foo() {" +
                            "  throw 'failed!!';" +
                            "  console.log(x);" +
                            "})().catch(x => console.log('Got exception: ' + x));");
        }
        assertEquals("Got exception: failed!!\n", out.toString());
    }

    /**
     * When {@link JSContextOptions#UNHANDLED_REJECTIONS} is set to <code>"throw"</code>, an
     * exception is raised when a promise rejection is not handled. A further warning is printed
     * when a reaction is registered later on.
     */
    @Test
    public void testJSHandledRejectionThrowAsyncRegisterThen() {
        TestOutput out = new TestOutput();
        TestOutput err = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).err(err).option(JSContextOptions.CONSOLE_NAME, "true").option(
                        JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "throw").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            try {
                context.eval(ID, "const rejectedPromise = Promise.reject(42);");
                assert false;
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertEquals("Error: Unhandled promise rejection: 42", e.getMessage());
            }
            context.eval("js", "rejectedPromise.catch(x => console.log(`Async handled: ${x}`));");
        }
        assertEquals("Async handled: 42\n", out.toString());
        assertEquals("[GraalVM JavaScript Warning] Promise rejection was handled asynchronously: 42" + LF, err.toString());
    }

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

    @Test
    public void orderedSwitchAsyncJava() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            Value func = context.eval("js", "async function doStuff(value, callback) {" //
                            + "  async function somethingAsync(v) {" //
                            + "    console.log(`From JS ${value} ${v}`);" //
                            + "    return v;" //
                            + "  };" //
                            + "  var result = 0;" //
                            + "  switch(value) {" //
                            + "    case 'foo':" //
                            + "      result += callback(1);" //
                            + "      result += await somethingAsync(1);" //
                            + "      result += callback(2);" //
                            + "      result += await somethingAsync(2);" //
                            + "      result += callback(3);" //
                            + "  };" //
                            + "  return result;" //
                            + "};" //
                            + "doStuff;");

            func.execute("foo", (ProxyExecutable) arguments -> {
                int arg = arguments[0].asInt();
                out.write("From Java " + arg + "\n");
                return arg;
            }).invokeMember("then", (Consumer<Object>) result -> {
                out.write("result: " + result.toString() + "\n");
            });

            assertEquals("From Java 1\n" //
                            + "From JS foo 1\n" //
                            + "From Java 2\n" //
                            + "From JS foo 2\n" //
                            + "From Java 3\n" //
                            + "result: 9\n", out.toString());
        }
    }

    @Test
    public void orderedSwitchAsyncJS() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            context.eval("js", "async function doStuff(value, callback) {" //
                            + "  async function somethingAsync(x) {" //
                            + "    console.log(`From JS ${value} ${x}`);" //
                            + "    return x;" //
                            + "  };" //
                            + "  async function caseExp(x) {" //
                            + "    return 'oo';" //
                            + "  };" //
                            + "  var result = 0;" //
                            + "  switch(value) {" //
                            + "    case 'f' + await caseExp():" //
                            + "      result += callback(1);" //
                            + "      result += await somethingAsync(1);" //
                            + "      result += callback(2);" //
                            + "      result += await somethingAsync(2);" //
                            + "      result += callback(3);" //
                            + "  };" //
                            + "  return result;" //
                            + "};" //
                            + "function cb(x) {" //
                            + "  console.log(`From Callback ${x}`);" //
                            + "  return x;" //
                            + "};" //
                            + "doStuff('foo', cb).then(x => console.log(`result: ${x}`));");

            assertEquals("From Callback 1\n" //
                            + "From JS foo 1\n" //
                            + "From Callback 2\n" //
                            + "From JS foo 2\n" //
                            + "From Callback 3\n" //
                            + "result: 9\n", out.toString());
        }
    }

    @Test
    public void unOrderedSwitchAsyncJS() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            context.eval("js", "async function doStuff(value, callback) {" //
                            + "  async function somethingAsync(x) {" //
                            + "    console.log(`From JS ${value} ${x}`);" //
                            + "    return x;" //
                            + "  };" //
                            + "  var res = 0;" //
                            + "  switch(value) {" //
                            + "    case 'nope':" //
                            + "      res += callback(42);" //
                            + "      return res;" //
                            + "    case 42:" //
                            + "    case 33:" //
                            + "      res += callback(1);" //
                            + "      res += await somethingAsync(1);" //
                            + "      res += callback(2);" //
                            + "      res += await somethingAsync(2);" //
                            + "      res += callback(3);" //
                            + "    default:" //
                            + "      return 3;" //
                            + "    case 3:" //
                            + "  };" //
                            + "  return res;" //
                            + "};" //
                            + "function cb(x) {" //
                            + "  console.log(`From Callback ${x}`);" //
                            + "  return x;" //
                            + "};" //
                            + "doStuff(42, cb).then(x => console.log(`result: ${x}`));");

            assertEquals("From Callback 1\n" //
                            + "From JS 42 1\n" //
                            + "From Callback 2\n" //
                            + "From JS 42 2\n" //
                            + "From Callback 3\n" //
                            + "result: 3\n", out.toString());
        }
    }

    @Test
    public void unOrderedSwitchAsyncJSDefault() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            context.eval("js", "async function doStuff(value, callback) {" //
                            + "  async function somethingAsync(x) {" //
                            + "    console.log(`From JS ${value} ${x}`);" //
                            + "    return x;" //
                            + "  };" //
                            + "  var res = 0;" //
                            + "  switch(value) {" //
                            + "    case 'nope':" //
                            + "      res += callback(32);" //
                            + "      return res + 10;" //
                            + "    case 42:" //
                            + "    case 33:" //
                            + "      res += callback(1);" //
                            + "    default:" //
                            + "      return 42;" //
                            + "    case 3:" //
                            + "  };" //
                            + "  return res;" //
                            + "};" //
                            + "function cb(x) {" //
                            + "  console.log(`From Callback ${x}`);" //
                            + "  return x;" //
                            + "};" //
                            + "doStuff('nope', cb).then(x => console.log(`result: ${x}`));" //
                            + "doStuff('nein', cb).then(x => console.log(`result: ${x}`));");

            assertEquals("From Callback 32\n" //
                            + "result: 42\n" //
                            + "result: 42\n", out.toString());
        }
    }

    @Test
    public void asyncCaseSwitch() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME,
                        "false").build()) {
            context.eval("js", "async function f(x) {" //
                            + "  switch (x) {" //
                            + "    case 0:" //
                            + "      console.log('not 0');" //
                            + "      break;" //
                            + "    case await 42:" //
                            + "      console.log('was 42');" //
                            + "      break;" //
                            + "    case await 84:" //
                            + "      var x = 41 + await 43;" //
                            + "      console.log('was ' + x);" //
                            + "      break;" //
                            + "  }" //
                            + "};" //
                            + "f(42);" //
                            + "f(84);");

            assertEquals("was 42\nwas 84\n", out.toString());
        }
    }

    @Test
    public void testAwaitInSwitchInLoop() {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().out(out).build()) {
            context.eval("js", "" //
                            + "(async function () {" //
                            + "  for (o of ['a', 'b']) {" //
                            + "    switch (o) {" //
                            + "      case 'a':" //
                            + "        await 42;" //
                            + "        console.log('seen a');" //
                            + "        break;" //
                            + "      case 'b':" //
                            + "        console.log('seen b');" //
                            + "        break;" //
                            + "    }" //
                            + "  }" //
                            + "})();");
            assertEquals("seen a\nseen b\n", out.toString());
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

    public static class TestOutput extends ByteArrayOutputStream {

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

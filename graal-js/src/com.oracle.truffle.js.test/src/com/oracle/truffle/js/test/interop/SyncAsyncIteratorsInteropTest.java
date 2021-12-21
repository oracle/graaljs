/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.interop.AsyncInteropTest.Executable;
import com.oracle.truffle.js.test.interop.AsyncInteropTest.TestOutput;
import com.oracle.truffle.js.test.interop.AsyncInteropTest.Thenable;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyIterable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SyncAsyncIteratorsInteropTest {

    /**
     * Any Java object can be used to implement JavaScript's Sync/Async iterable protocol. To do so,
     * the object should expose two fields called <code>value</code> and <code>done</code>. They
     * will be used by Graal.js to advance Async iterations.
     */
    public static class IterableProtocolObject<T> {

        public T value;
        public boolean done;

        IterableProtocolObject(T initialValue, boolean done) {
            this.value = initialValue;
            this.done = done;
        }
    }

    /**
     * A Java object can be used to control a JS iterator.
     */
    @Test
    public void basicSyncIterator() {
        final String code = "const syncIterable = {" //
                        + "  [Symbol.iterator]() {" //
                        + "    return {" //
                        + "      next() {" //
                        + "        return nextFromJava();" //
                        + "      }" //
                        + "    };" //
                        + "  }" //
                        + "};" //
                        + "(function() {" //
                        + "   for (let val of syncIterable) {" //
                        + "     console.log(`from Java: ${val}`);" //
                        + "   }" //
                        + "})();";

        TestOutput out = new TestOutput();
        try (Context context = createTestContext(out)) {

            final IterableProtocolObject<Integer> iteratorStep = new IterableProtocolObject<>(3, false);

            Supplier<IterableProtocolObject<Integer>> nextFromJava = () -> {
                if (iteratorStep.value > 0) {
                    // Keep iterating as long as `value > 0`.
                    assert !iteratorStep.done;
                    iteratorStep.value--;
                } else {
                    // Signal that iteration is done.
                    iteratorStep.done = true;
                }
                return iteratorStep;
            };

            context.getBindings("js").putMember("nextFromJava", nextFromJava);

            context.eval("js", code);

            Assert.assertEquals("from Java: 2\n" //
                            + "from Java: 1\n" //
                            + "from Java: 0\n", out.toString());
        }
    }

    /**
     * Java objects can be used to create JavaScript Promise objects, to control the async
     * iteration.
     */
    @Test
    public void basicAsyncIterationPromise() {
        final String code = "const asyncIterable = {" //
                        + "  [Symbol.asyncIterator]() {" //
                        + "    return {" //
                        + "      next() {" //
                        + "        if (hasNextFromJava()) {" //
                        + "            return new Promise(nextFromJava);" //
                        + "        } else {" //
                        + "            return new Promise(doneFromJava);" //
                        + "        }" //
                        + "      }" //
                        + "    };" //
                        + "  }" //
                        + "};" //
                        + "(async function() {" //
                        + "   for await (let val of asyncIterable) {" //
                        + "     console.log(`from Java: ${val}`);" //
                        + "   }" //
                        + "})();";

        TestOutput out = new TestOutput();
        try (Context context = createTestContext(out)) {

            final IterableProtocolObject<Integer> iteratorStep = new IterableProtocolObject<>(3, false);

            Supplier<Boolean> hasNextFromJava = () -> iteratorStep.value > 0;

            Executable nextFromJava = (resolve, reject) -> {
                iteratorStep.value--;
                resolve.execute(iteratorStep);
            };

            Executable doneFromJava = (resolve, reject) -> {
                iteratorStep.done = true;
                resolve.execute(iteratorStep);
            };

            context.getBindings("js").putMember("hasNextFromJava", hasNextFromJava);
            context.getBindings("js").putMember("doneFromJava", doneFromJava);
            context.getBindings("js").putMember("nextFromJava", nextFromJava);

            context.eval("js", code);

            Assert.assertEquals("from Java: 2\n" //
                            + "from Java: 1\n" //
                            + "from Java: 0\n", out.toString());
        }
    }

    /**
     * Java thenable objects can be used as JavaScript promises, to control the async iteration.
     */
    @Test
    public void basicAsyncIterationThenable() {
        final String code = "const asyncIterable = {" //
                        + "  [Symbol.asyncIterator]() {" //
                        + "    return {" //
                        + "      next() {" //
                        + "        return hasNextFromJava() ? nextFromJava : doneFromJava;" //
                        + "      }" //
                        + "    };" //
                        + "  }" //
                        + "};" //
                        + "(async function() {" //
                        + "   for await (let val of asyncIterable) {" //
                        + "     console.log(`from Java thenable: ${val}`);" //
                        + "   }" //
                        + "})();";

        TestOutput out = new TestOutput();
        try (Context context = createTestContext(out)) {

            final IterableProtocolObject<String> iteratorStep = new IterableProtocolObject<>("", false);

            Supplier<Boolean> hasNextFromJava = () -> iteratorStep.value.length() < 3;

            Thenable nextFromJava = (resolve, reject) -> {
                iteratorStep.value += "x";
                resolve.execute(iteratorStep);
            };

            Thenable doneFromJava = (resolve, reject) -> {
                iteratorStep.done = true;
                resolve.execute(iteratorStep);
            };

            context.getBindings("js").putMember("hasNextFromJava", hasNextFromJava);
            context.getBindings("js").putMember("doneFromJava", doneFromJava);
            context.getBindings("js").putMember("nextFromJava", nextFromJava);

            context.eval("js", code);

            Assert.assertEquals("from Java thenable: x\n" //
                            + "from Java thenable: xx\n" //
                            + "from Java thenable: xxx\n", out.toString());
        }
    }

    /**
     * The <code>next()</code> method of the Async iteration protocol can be implemented in Java. It
     * should return a Promise-like object.
     */
    @Test
    public void javaAsyncIteration() {
        final String code = "(async function() {" //
                        + "   let text = '';" //
                        + "   for await (let val of asyncIterable) {" //
                        + "       text += val;" +
                        "         console.log(`in JS, from Java: '${val}'`);" //
                        + "   };" +
                        "     return `'${text}'`;" //
                        + "})();";

        TestOutput out = new TestOutput();
        try (Context context = createTestContext(out)) {

            context.eval("js", "var asyncIterable = { [Symbol.asyncIterator]() {return this} };");

            Value asyncIterableJsObject = context.getBindings("js").getMember("asyncIterable");

            final Iterator<String> javaIterator = Arrays.asList("f", "o", "o").iterator();
            asyncIterableJsObject.putMember("next", (Supplier<Thenable>) () -> {
                // For Async iterators, the `next` function returns a Promise-like object.
                return (onResolve, onReject) -> {
                    if (javaIterator.hasNext()) {
                        onResolve.execute(new IterableProtocolObject<>(javaIterator.next(), false));
                    } else {
                        onResolve.execute(new IterableProtocolObject<String>(null, true));
                    }
                };
            });

            context.eval("js", code).invokeMember("then", (Consumer<String>) (v) -> {
                out.write("in Java, from JS: " + v);
                Assert.assertEquals("'foo'", v);
            });

            Assert.assertEquals("in JS, from Java: 'f'\n" //
                            + "in JS, from Java: 'o'\n" //
                            + "in JS, from Java: 'o'\n" //
                            + "in Java, from JS: 'foo'", out.toString());
        }
    }

    /**
     * The <code>ProxyIterable</code> interface can be used to map a Java <code>Iterable</code>
     * object to a JavaScript one. When the <code>next()</code> method of the Java iterator returns
     * a Promise-like object (e.g., <code>Thenable</code> in this example testcase), Graal.js can
     * iterate over it using <code>for-await-of</code>.
     */
    @Test
    public void proxyAsyncIteration() {
        final String code = "(async function() {" +
                        "  var total = 0; " +
                        "  for await (let x of proxyIterable) {" +
                        "    console.log(`in JS, from ProxyIterable: ${x}`);" +
                        "    total += x;" +
                        "  };" +
                        "  return 36 + total;" +
                        "})();";

        TestOutput out = new TestOutput();
        try (Context context = createTestContext(out)) {

            Object proxyIterable = ProxyIterable.from(() -> new Iterator<>() {
                private int count = 0;

                @Override
                public boolean hasNext() {
                    return count < 4;
                }

                @Override
                public Thenable next() {
                    return (resolve, reject) -> resolve.execute(count++);
                }
            });

            context.getBindings("js").putMember("proxyIterable", proxyIterable);

            context.eval("js", code).invokeMember("then", (Consumer<Integer>) (v) -> {
                out.write("in Java, from JS: " + v);
                Assert.assertEquals(42, (int) v);
            });

            Assert.assertEquals("in JS, from ProxyIterable: 0\n" //
                            + "in JS, from ProxyIterable: 1\n" //
                            + "in JS, from ProxyIterable: 2\n" //
                            + "in JS, from ProxyIterable: 3\n" //
                            + "in Java, from JS: 42", out.toString());
        }
    }

    private static Context createTestContext(TestOutput out) {
        return JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.UNHANDLED_REJECTIONS_NAME,
                        "warn").option("engine.WarnInterpreterOnly", "false").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").out(out).err(out).build();
    }

}

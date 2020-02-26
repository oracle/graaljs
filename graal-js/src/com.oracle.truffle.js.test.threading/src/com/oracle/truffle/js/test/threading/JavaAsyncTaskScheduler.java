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
package com.oracle.truffle.js.test.threading;

import static org.junit.Assert.assertTrue;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class JavaAsyncTaskScheduler {

    /**
     * Utility class simulating an asynchronous event from Java.
     */
    public static class Example {
        // a concurrent queue shared with Node
        private final Queue<Object> queue;

        public Example(Queue<Object> queue) {
            this.queue = queue;
        }

        public void doAsynchronousWork() {
            // simulate asynchronous events, e.g., an HTTP request, and notify back once received.
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new AssertionError();
                    }
                    double data = Math.random() + 42;
                    // awake the Node.js worker, which is waiting on this queue
                    queue.offer(data);
                }
            });
            thread.start();
        }

        public static Object evalJsCode() {
            Context context = TestUtil.newContextBuilder().build();
            return context.eval("js", "42;");
        }
    }

    /**
     * Java synchronization (e.g., a blocking queue) can be used to "awake" JavaScript code.
     */
    @Test
    public void testJavaWakeup() {
        Context cx = TestUtil.newContextBuilder().allowHostAccess(HostAccess.ALL).build();
        Queue<Object> sharedQueue = new LinkedBlockingDeque<>();
        Example async = new Example(sharedQueue);

        cx.getBindings("js").putMember("queue", sharedQueue);
        cx.getBindings("js").putMember("javaAsync", async);
        try {
            Value data = cx.eval("js", "javaAsync.doAsynchronousWork();" +
                            // Block until the queue has work to offer
                            "var data = queue.take();" +
                            "data;");
            assertTrue(data.fitsInDouble());
            assertTrue(data.asDouble() >= 42);
        } finally {
            cx.close();
        }
    }
}

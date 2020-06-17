/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class ConcurrentAccess {

    /**
     * Concurrent execution of code belonging to two distinct JS contexts is allowed.
     */
    @Test
    public void concurrentEvalsTwoContexts() throws InterruptedException {

        final String jsonCode = "(function(x,y) { return JSON.stringify({x:x,y:y}); })";
        try (Context cx1 = TestUtil.newContextBuilder().build(); Context cx2 = TestUtil.newContextBuilder().build()) {
            Value json1 = cx1.eval("js", jsonCode);
            Value json2 = cx2.eval("js", jsonCode);

            Thread thread = new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    String encoded = json1.execute(42, 43).asString();
                    assertEquals("{\"x\":42,\"y\":43}", encoded);
                }
            });

            thread.start();
            for (int i = 0; i < 1000; i++) {
                String encoded = json2.execute(42, 43).asString();
                assertEquals("{\"x\":42,\"y\":43}", encoded);
            }
            thread.join();
        }
    }

    /**
     * Host threads cannot be executed using Graal.js' Java interop.
     */
    @Test(timeout = 30000)
    public void javaInteropThread() throws InterruptedException {
        final CountDownLatch endGate = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);
        final Context context = TestUtil.newContextBuilder().allowHostAccess(HostAccess.ALL).allowHostClassLookup(s -> true).build();

        context.getBindings("js").putMember("onThreadException", (Thread.UncaughtExceptionHandler) (t, e) -> {
            exception.set(e);
            endGate.countDown();
        });

        final String threadCode = "var Thread = Java.type('java.lang.Thread');" +
                        "var t = new Thread(function() {" +
                        "    console.log('hello from another thread');" +
                        "});" +
                        "t.setUncaughtExceptionHandler(onThreadException);" +
                        "t.start();" +
                        "t.join();";
        try {
            context.eval("js", threadCode);

            endGate.await();
            Throwable throwable = exception.get();
            assertNotNull(throwable);
            assertTrue(throwable instanceof IllegalStateException);
        } finally {
            context.close();
        }
    }

    /**
     * Concurrent execution of code belonging to the same context is prevented without
     * entering/leaving the Graal.js context.
     */
    @Test(timeout = 30000)
    public void concurrentEvalsNoEnter() {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(1);
        final AtomicBoolean hadException = new AtomicBoolean(false);

        final Context cx = TestUtil.newContextBuilder().build();

        Value json = cx.eval("js", "(function(x,y) { return JSON.stringify({x:x,y:y}); })");

        Thread t = new Thread(() -> {
            try {
                startGate.await();
                try {
                    while (!hadException.get()) {
                        try {
                            String encoded = json.execute(42, 43).asString();
                            assertEquals("{\"x\":42,\"y\":43}", encoded);
                        } catch (IllegalStateException e) {
                            hadException.set(true);
                        }
                    }
                } finally {
                    endGate.countDown();
                }
            } catch (InterruptedException ignored) {
            }
        });

        try {
            t.start();
            startGate.countDown();
            while (!hadException.get()) {
                try {
                    String encoded = json.execute(42, 43).asString();
                    assertEquals("{\"x\":42,\"y\":43}", encoded);
                } catch (IllegalStateException e) {
                    hadException.set(true);
                }
            }
            endGate.await();
            t.join(10000);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            cx.close();
        }
    }

    /**
     * Concurrent execution of code belonging to the same context is enabled when proper
     * synchronization is used and the shared JS context is accessed using enter/leave.
     */
    @Test(timeout = 30000)
    public void concurrentEvalsWithEnter() {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(1);
        final AtomicBoolean hadException = new AtomicBoolean(false);

        final Context cx = TestUtil.newContextBuilder().build();
        final ReentrantLock contextLock = new ReentrantLock();

        cx.enter();
        Value json = cx.eval("js", "(function(x,y) { return JSON.stringify({x:x,y:y}); })");
        cx.leave();

        Thread t = new Thread(() -> {
            try {
                startGate.await();
                try {
                    for (int it = 0; it < 100000; it++) {
                        try {
                            try {
                                contextLock.lock();
                                cx.enter();
                                String encoded = json.execute(42, 43).asString();
                                assertEquals("{\"x\":42,\"y\":43}", encoded);
                            } finally {
                                cx.leave();
                                contextLock.unlock();
                            }
                        } catch (IllegalStateException e) {
                            hadException.set(true);
                        }
                    }
                } finally {
                    endGate.countDown();
                }
            } catch (InterruptedException ignored) {
            }
        });

        try {
            t.start();
            startGate.countDown();
            for (int it = 0; it < 100000; it++) {
                try {
                    contextLock.lock();
                    cx.enter();
                    String encoded = json.execute(42, 43).asString();
                    assertEquals("{\"x\":42,\"y\":43}", encoded);
                    cx.leave();
                } finally {
                    contextLock.unlock();
                }
            }
            endGate.await();
            t.join(10000);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            cx.close();
        }
        assertFalse(hadException.get());
    }
}

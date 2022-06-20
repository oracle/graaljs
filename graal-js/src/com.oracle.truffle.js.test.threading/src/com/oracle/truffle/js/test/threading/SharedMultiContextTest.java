/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SharedMultiContextTest {

    final String someFunction = "(function (x) { x.inc(); });";
    final String someKlass = "class SharedKlass {" +
                    "    #foo = 0;" +
                    "    inc() {" +
                    "        this.#foo++;" +
                    "    };" +
                    "    value() {" +
                    "        return this.#foo;" +
                    "    };" +
                    "};";

    /**
     * A JavaScript object created in one context can be accessed from another context when proper
     * synchronization is used.
     */
    @Test
    public void transferObjectsWithSync() {
        Context mainContext = Context.create("js");
        mainContext.eval("js", someKlass);
        Value sharedValue = mainContext.eval("js", "new SharedKlass();");

        Lock lock = new ReentrantLock();

        Runnable runnable = () -> {
            int it = 21000;
            Context c1 = Context.newBuilder().allowAllAccess(true).build();
            Value fun = c1.eval("js", someFunction);
            while (it-- > 0) {
                try {
                    lock.lock();
                    fun.execute(sharedValue);
                } catch (Throwable t) {
                    assert false : t.getMessage();
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);

        t1.start();
        t2.start();
        try {
            t1.join(10000);
            t2.join(10000);
        } catch (InterruptedException e) {
            assert false : e.getMessage();
        }
        Value finalValue = sharedValue.invokeMember("value");
        Assert.assertTrue(finalValue.fitsInInt());
        Assert.assertEquals(42000, finalValue.asInt());
    }

    /**
     * A JS object created in one context can _NOT_ be accessed from another context without
     * enforcing proper synchronization.
     */
    @Test
    public void transferObjectsWithoutSync() {
        Context mainContext = Context.create("js");
        mainContext.eval("js", someKlass);
        Value sharedValue = mainContext.eval("js", "new SharedKlass();");

        AtomicBoolean seenException = new AtomicBoolean(false);

        Runnable runnable = () -> {
            Context c1 = Context.newBuilder().allowAllAccess(true).build();
            Value fun = c1.eval("js", someFunction);
            try {
                while (true) {
                    fun.execute(sharedValue);
                    if (seenException.get()) {
                        break;
                    }
                }
            } catch (Throwable t) {
                seenException.set(true);
                Assert.assertTrue(t.getMessage().contains("Multi threaded access requested by thread"));
            }
        };

        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);

        t1.start();
        t2.start();
        try {
            t1.join(10000);
            t2.join(10000);
        } catch (InterruptedException e) {
            assert false : e.getMessage();
        }
        Assert.assertTrue(seenException.get());
    }
}

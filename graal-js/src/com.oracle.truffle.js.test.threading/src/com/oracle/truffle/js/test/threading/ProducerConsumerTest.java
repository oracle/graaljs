/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class ProducerConsumerTest {

    /**
     * Data can be exchanged between different threads running isolated, share-nothing, Graal.js
     * contexts. Java synchronization can be used to exchange data between them.
     */
    @Test(timeout = 10000)
    public void pingPong() {
        final BlockingQueue<Value> queue = new ArrayBlockingQueue<>(1024);

        Producer p = new Producer(queue);
        Consumer c = new Consumer(queue);

        c.start();
        p.start();
        try {
            p.join();
            c.join();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        assertEquals(p.sent, 128);
        assertEquals(c.received, 128);
    }

    static class Producer extends Thread {

        private int sent;
        private final BlockingQueue<Value> queue;

        Producer(BlockingQueue<Value> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            Context cx = Context.create("js");
            cx.getBindings("js").putMember("queue", queue);
            sent = cx.eval("js", " var sent = 0;" +
                            "for(var i = 0; i < 127; i++) {" +
                            "   queue.put(JSON.stringify({message:i}));" +
                            "   sent++;" +
                            "};" +
                            "queue.put(JSON.stringify({message:'byebye'}));" +
                            "++sent;").asInt();
        }

    }

    class Consumer extends Thread {

        private int received;
        private final BlockingQueue<Value> queue;

        Consumer(BlockingQueue<Value> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            Context cx = Context.create("js");
            cx.getBindings("js").putMember("queue", queue);
            received = cx.eval("js", "var received = 0;" +
                            "do {" +
                            "   var str = queue.take();" +
                            "   received++;" +
                            "   var m = JSON.parse(str);" +
                            "} while (m.message != 'byebye');" +
                            "received;").asInt();
        }
    }
}

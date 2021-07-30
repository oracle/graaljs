package com.oracle.truffle.js.test.threading;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class ModuleBlockSerializationAPITest {
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
            Context cx = TestUtil.newContextBuilder().allowHostAccess(HostAccess.ALL).build();
            cx.getBindings("js").putMember("queue", queue);
            try {
                sent = cx.eval("js", " var sent = 0;" +
                                "for(var i = 0; i < 127; i++) {" +
                                "   queue.put(JSON.stringify({message:i}));" +
                                "   sent++;" +
                                "};" +
                                "queue.put(JSON.stringify({message:'byebye'}));" +
                                "++sent;").asInt();
            } finally {
                cx.close();
            }
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
            Context cx = TestUtil.newContextBuilder().allowHostAccess(HostAccess.ALL).build();
            cx.getBindings("js").putMember("queue", queue);
            try {
                received = cx.eval("js", "var received = 0;" +
                                "do {" +
                                "   var str = queue.take();" +
                                "   received++;" +
                                "   var m = JSON.parse(str);" +
                                "} while (m.message != 'byebye');" +
                                "received;").asInt();
            } finally {
                cx.close();
            }
        }
    }
}

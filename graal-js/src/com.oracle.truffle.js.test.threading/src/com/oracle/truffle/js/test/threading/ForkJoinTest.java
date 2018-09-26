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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.Supplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class ForkJoinTest {

    /**
     * Language bindings and Java interop can be used to expose synchronization primitives to
     * Graal.js.
     */
    @Test
    public void forkJoinPool() {
        final ForkJoinPool pool = new ForkJoinPool();
        int result = pool.invoke(new GraaljsFibTask(13, tl));
        assertEquals(result, 233);
    }

    private final Engine engine = Engine.create();
    private final ThreadLocal<Value> tl = ThreadLocal.withInitial(new Supplier<Value>() {

        @Override
        public Value get() {
            String src = "function(n) {" +
                            "   if (n <= 1) " +
                            "      return n;" +
                            "   var f1 = fib.task(n - 1);" +
                            "   f1.fork();" +
                            "   var f2 = fib.task(n - 2);" +
                            "   return f2.compute() + f1.join();" +
                            "}";

            Context cx = Context.newBuilder("js").engine(engine).build();
            cx.getBindings("js").putMember("fib", new FibTaskCreator(tl));
            return cx.eval("js", src);
        }
    });

    public static class GraaljsFibTask extends RecursiveTask<Integer> {

        private static final long serialVersionUID = 1L;

        private final int num;
        private final ThreadLocal<Value> fib;

        public GraaljsFibTask(int num, ThreadLocal<Value> tl) {
            this.fib = tl;
            this.num = num;
        }

        @Override
        public Integer compute() {
            return fib.get().execute(num).asInt();
        }
    }

    public static class FibTaskCreator {
        private final ThreadLocal<Value> tl;

        public FibTaskCreator(ThreadLocal<Value> tl) {
            this.tl = tl;
        }

        public GraaljsFibTask task(int num) {
            return new GraaljsFibTask(num, tl);
        }
    }

}

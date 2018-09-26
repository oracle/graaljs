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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class ExecutorsTest {

    /**
     * Graal.js can be used in Executors to evaluate share-nothing JavaScript code in parallel.
     *
     * ThreadLocals can be used to limit the number of Graal.js contexts. A single PolyglotEngine
     * can be shared among Graal.js contexts to benefit from compilation cache sharing and other
     * GraalVM optimizations.
     */
    @Test
    public void fixedThreadPool() {
        final Engine engine = Engine.create();
        try {
            final ExecutorService pool = Executors.newFixedThreadPool(4);
            final ThreadLocal<Context> tl = ThreadLocal.withInitial(new Supplier<Context>() {

                @Override
                public Context get() {
                    return Context.newBuilder("js").engine(engine).build();
                }
            });

            Set<Callable<Value>> tasks = new HashSet<>();
            for (int i = 0; i < 42; i++) {
                tasks.add(new Callable<Value>() {

                    @Override
                    public Value call() throws Exception {
                        Context cx = tl.get();
                        cx.enter();
                        try {
                            return cx.eval("js", "42;");
                        } finally {
                            cx.leave();
                        }
                    }
                });
            }

            try {
                for (Future<Value> v : pool.invokeAll(tasks)) {
                    assertEquals(v.get().asInt(), 42);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new AssertionError(e);
            }
        } finally {
            engine.close();
        }
    }

}

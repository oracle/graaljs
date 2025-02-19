/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.regress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class GR62390 {

    @FunctionalInterface
    public interface TriConsumer {
        void accept(Value val, Value key, Value set);
    }

    /**
     * Set.prototype.forEach should work with foreign (e.g. host proxy) callback.
     */
    @Test
    public void testSetForEachForeignCallback() {
        try (Context ctx = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).build()) {
            Value testForEach = ctx.eval(Source.create("js", """
                            (function(callback, ...elements) {
                                const set = new Set(elements);
                                set.forEach(callback);
                            })
                            """));

            AtomicInteger counter = new AtomicInteger();
            TriConsumer consumer = (val, key, set) -> {
                switch (counter.getAndIncrement()) {
                    case 0 -> {
                        assertEquals("a", val.asString());
                        assertEquals("a", key.asString());
                    }
                    case 1 -> {
                        assertEquals("b", val.asString());
                        assertEquals("b", key.asString());
                    }
                    case 2 -> fail("too many callback invocations");
                }
                assertEquals("Set", set.getMetaObject().getMetaSimpleName());
                assertEquals(2, set.getMember("size").asInt());
            };
            ProxyExecutable callback = (args) -> {
                assertEquals(3, args.length);
                final Value val = args[0];
                final Value key = args[1];
                final Value set = args[2];
                consumer.accept(val, key, set);
                return null;
            };

            testForEach.execute(consumer, "a", "b");
            assertEquals("callback invocations", 2, counter.get());

            counter.set(0);
            testForEach.execute(callback, "a", "b");
            assertEquals("callback invocations", 2, counter.get());
        }
    }
}

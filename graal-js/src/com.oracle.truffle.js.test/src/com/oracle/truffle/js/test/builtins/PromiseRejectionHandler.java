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
package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PromiseRejectionHandler {

    @Test
    public void testExists() {
        try (Context context = newUnhandledRejectionContext()) {
            assertTrue(registerHandlerFunctionExists(context));
        }
        try (Context context = JSTest.newContextBuilder().build()) {
            assertFalse(registerHandlerFunctionExists(context));
        }
    }

    @Test
    public void testInvokeRegister() {
        try (Context context = newUnhandledRejectionContext()) {
            context.eval("js", "Graal.setUnhandledPromiseRejectionHandler(() => {}); Promise.reject('test')");
            context.eval("js", "Graal.setUnhandledPromiseRejectionHandler(); Promise.reject('test')");
            context.eval("js", "Graal.setUnhandledPromiseRejectionHandler(null); Promise.reject('test')");
            context.eval("js", "Graal.setUnhandledPromiseRejectionHandler(undefined); Promise.reject('test')");
        }
    }

    @Test
    public void testMultipleContexts() {
        // Make sure we're accessing the right realm when using a shared engine.
        Engine engine = Engine.newBuilder("js").allowExperimentalOptions(true).build();
        try (Context context1 = newUnhandledRejectionContext(engine)) {
            try (Context context2 = newUnhandledRejectionContext(engine)) {
                context1.eval("js", "count = 0; Graal.setUnhandledPromiseRejectionHandler(() => count++)");
                context2.eval("js", "Graal.setUnhandledPromiseRejectionHandler(null);");
                context1.eval("js", "Promise.reject('test')");
                context2.eval("js", "Promise.reject('test')");
                Value count = context1.eval("js", "count");
                assertEquals(1, count.asInt());
            }
        }
    }

    @Test(expected = PolyglotException.class)
    public void testUncallableHandler() {
        try (Context context = newUnhandledRejectionContext()) {
            context.eval("js", "Graal.setUnhandledPromiseRejectionHandler('not callable');");
        }
    }

    @Test
    public void testSingleUnhandledRejection() {
        try (Context context = newUnhandledRejectionContext()) {
            context.eval("js", "count = 0; Graal.setUnhandledPromiseRejectionHandler(() => count++); Promise.reject('test')");
            Value count = context.eval("js", "count");
            assertEquals(1, count.asInt());
        }
    }

    @Test
    public void testUnhandledRejectionInHandler() {
        try (Context context = newUnhandledRejectionContext()) {
            context.eval("js", "" +
                            "count = 0; " +
                            "Graal.setUnhandledPromiseRejectionHandler(() => {" +
                            "   if (count == 0) Promise.reject('testInner');" +
                            "   count++;" +
                            "}); " +
                            "Promise.reject('test1')");
            context.eval("js", "Promise.reject('test2')");
            Value count = context.eval("js", "count");
            assertEquals(3, count.asInt());
        }
    }

    @Test
    public void testFireArguments() {
        try (Context context = newUnhandledRejectionContext()) {
            final AtomicBoolean executed = new AtomicBoolean(false);
            BiConsumer<Object, Object> testHandler = (rejection, promise) -> {
                assertEquals("test", rejection);
                assertNotNull(promise);
                assertEquals("Promise", context.asValue(promise).getMetaObject().getMetaSimpleName());
                executed.set(true);
            };
            context.getBindings("js").putMember("handler", testHandler);
            context.eval("js", "Graal.setUnhandledPromiseRejectionHandler((r, p) => handler(r, p)); Promise.reject('test')");
            assertTrue(executed.get());
        }
    }

    @Test
    public void testFireHostFunction() {
        try (Context context = newUnhandledRejectionContext()) {
            final AtomicBoolean executed = new AtomicBoolean(false);
            BiConsumer<Object, Object> testHandler = (rejection, promise) -> {
                executed.set(true);
            };
            context.getBindings("js").putMember("handler", testHandler);
            context.eval("js", "Graal.setUnhandledPromiseRejectionHandler(handler); Promise.reject('test')");
            assertTrue(executed.get());
        }
    }

    Context newUnhandledRejectionContext(Engine engine) {
        Context.Builder builder = JSTest.newContextBuilder().option("js.unhandled-rejections", "handler").allowAllAccess(true);
        if (engine != null) {
            builder.engine(engine);
        }
        return builder.build();
    }

    Context newUnhandledRejectionContext() {
        return newUnhandledRejectionContext(null);
    }

    boolean registerHandlerFunctionExists(Context context) {
        return context.eval("js", "'setUnhandledPromiseRejectionHandler' in Graal").asBoolean();
    }
}

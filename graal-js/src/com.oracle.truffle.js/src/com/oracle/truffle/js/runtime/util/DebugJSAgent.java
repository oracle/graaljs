/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.util;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Testing and debug JSAgent used by test262.
 */
public class DebugJSAgent extends JSAgent {

    private OptionValues optionValues;

    private final Deque<Object> reportValues;
    private final List<AgentExecutor> spawnedAgent;

    private boolean quit;
    private Object debugReceiveBroadcast;

    @TruffleBoundary
    public DebugJSAgent(boolean canBlock, OptionValues optionValues) {
        super(canBlock);
        this.optionValues = optionValues;
        this.reportValues = new ConcurrentLinkedDeque<>();
        this.spawnedAgent = new LinkedList<>();
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "DebugJSAgent{signifier=" + getSignifier() + "}";
    }

    @TruffleBoundary
    public Object startNewAgent(String source) {
        final AtomicReference<Object> result = new AtomicReference<>(null);
        final CountDownLatch barrier = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Context.Builder contextBuilder = Context.newBuilder(JavaScriptLanguage.ID).allowExperimentalOptions(true);
                contextBuilder.option("engine.WarnInterpreterOnly", Boolean.toString(false));
                if (optionValues.hasSetOptions()) {
                    for (OptionDescriptor optionDescriptor : optionValues.getDescriptors()) {
                        if (optionDescriptor.getKey().hasBeenSet(optionValues)) {
                            contextBuilder.option(optionDescriptor.getName(), getOptionValueAsString(optionDescriptor));
                        }
                    }
                }

                Context polyglotContext = contextBuilder.build();
                polyglotContext.enter();
                try {
                    polyglotContext.initialize(JavaScriptLanguage.ID);

                    DebugJSAgent debugJSAgent = (DebugJSAgent) JavaScriptLanguage.getCurrentJSRealm().getAgent();
                    AgentExecutor executor = registerChildAgent(Thread.currentThread(), debugJSAgent);

                    polyglotContext.eval(JavaScriptLanguage.ID, source);

                    barrier.countDown();

                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            executor.executeBroadcastCallback();
                        }
                        if (executor.jsAgent.quit) {
                            return;
                        }
                        executor.jsAgent.processAllPromises(false);
                    }
                } finally {
                    polyglotContext.leave();
                    polyglotContext.close();
                }
            }

            private String getOptionValueAsString(OptionDescriptor optionDescriptor) {
                Object optionValue = optionDescriptor.getKey().getValue(optionValues);
                if (optionDescriptor.getKey() == JSContextOptions.ECMASCRIPT_VERSION && Integer.valueOf(JSConfig.StagingECMAScriptVersion).equals(optionValue)) {
                    optionValue = JSContextOptions.ECMASCRIPT_VERSION_STAGING;
                }
                return String.valueOf(optionValue);
            }
        });
        thread.setDaemon(true);
        thread.setName("Debug-JSAgent-Worker-Thread");
        thread.start();
        try {
            barrier.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        return result.get();
    }

    @TruffleBoundary
    public void setDebugReceiveBroadcast(Object lambda) {
        this.debugReceiveBroadcast = lambda;
    }

    @TruffleBoundary
    public AgentExecutor registerChildAgent(Thread thread, DebugJSAgent jsAgent) {
        AgentExecutor spawned = new AgentExecutor(thread, jsAgent);
        spawnedAgent.add(spawned);
        return spawned;
    }

    @TruffleBoundary
    public void broadcast(Object sab) {
        for (AgentExecutor e : spawnedAgent) {
            e.pushMessage(sab);
        }
    }

    @TruffleBoundary
    public Object getReport() {
        for (AgentExecutor e : spawnedAgent) {
            if (e.jsAgent.reportValues.size() > 0) {
                return e.jsAgent.reportValues.pollLast();
            }
        }
        return Null.instance;
    }

    @TruffleBoundary
    public void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @TruffleBoundary
    public void report(Object value) {
        this.reportValues.push(value);
    }

    @TruffleBoundary
    public void leaving() {
        quit = true;
    }

    @Override
    @TruffleBoundary
    public void wakeAgent(int w) {
        for (AgentExecutor e : spawnedAgent) {
            if (e.jsAgent.getSignifier() == w) {
                e.thread.interrupt();
            }
        }
    }

    private static final class AgentExecutor {

        private final DebugJSAgent jsAgent;
        private final Thread thread;

        private ConcurrentLinkedDeque<Object> incoming;

        @TruffleBoundary
        AgentExecutor(Thread thread, DebugJSAgent jsAgent) {
            this.thread = thread;
            this.jsAgent = jsAgent;
            this.incoming = new ConcurrentLinkedDeque<>();
        }

        @TruffleBoundary
        private void pushMessage(Object sab) {
            incoming.add(sab);
            thread.interrupt();
        }

        @TruffleBoundary
        public void executeBroadcastCallback() {
            assert jsAgent.debugReceiveBroadcast != null;
            while (incoming.size() > 0) {
                DynamicObject cb = (DynamicObject) jsAgent.debugReceiveBroadcast;
                JSFunction.call(cb, cb, new Object[]{incoming.pop()});
            }
        }
    }

    @TruffleBoundary
    @Override
    public boolean isTerminated() {
        throw new UnsupportedOperationException("Not supported in Debug agent");
    }

    @TruffleBoundary
    @Override
    public void terminate(int timeout) {
        throw new UnsupportedOperationException("Not supported in Debug agent");
    }

}

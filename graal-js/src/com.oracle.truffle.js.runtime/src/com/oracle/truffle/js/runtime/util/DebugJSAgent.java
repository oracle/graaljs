/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.EcmaAgent;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Testing and debug JSAgent used by test262.
 */
public class DebugJSAgent extends JSAgent {

    private final JSContext context;

    private final Deque<Object> reportValues;
    private final List<AgentExecutor> spawnedAgent;

    private boolean quit;
    private Object debugReceiveBroadcast;

    public DebugJSAgent(JSContext context) {
        super();
        this.context = context;
        this.reportValues = new ConcurrentLinkedDeque<>();
        this.spawnedAgent = new LinkedList<>();
    }

    public Object startNewAgent(String source) {
        final JSContext parentContext = context;
        final AtomicReference<Object> result = new AtomicReference<>(null);
        final CountDownLatch barrier = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Context.Builder contextBuilder = Context.newBuilder(AbstractJavaScriptLanguage.ID);
                OptionValues optionValues = parentContext.getOptionValues();
                for (OptionDescriptor optionDescriptor : optionValues.getDescriptors()) {
                    if (optionDescriptor.getKey().hasBeenSet(optionValues)) {
                        contextBuilder.option(optionDescriptor.getName(), String.valueOf(optionDescriptor.getKey().getValue(optionValues)));
                    }
                }
                Context polyglotContext = contextBuilder.build();

                String init = "var $262 = { agent : {} };" +
                                "$262.agent.receiveBroadcast = Test262.agentReceiveBroadcast;" +
                                "$262.agent.report = Test262.agentReport;" +
                                "$262.agent.sleep = Test262.agentSleep;" +
                                "$262.agent.leaving = Test262.agentLeaving;" +
                                "$262;";

                polyglotContext.eval(AbstractJavaScriptLanguage.ID, init);
                JSContext jsContext = AbstractJavaScriptLanguage.getJSContext(polyglotContext);
                AgentExecutor executor = ((DebugJSAgent) parentContext.getJSAgent()).registerChildAgent(Thread.currentThread(), (DebugJSAgent) jsContext.getJSAgent());

                polyglotContext.eval(AbstractJavaScriptLanguage.ID, Source.newBuilder(source).name("MainSource").mimeType("application/javascript").build().getCharacters());

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
                }

            }
        });
        thread.setDaemon(true);
        thread.setName("Debug-JSAgent-Worker");
        thread.start();
        try {
            barrier.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        return result.get();
    }

    public void setDebugReceiveBroadcast(Object lambda) {
        this.debugReceiveBroadcast = lambda;
    }

    public AgentExecutor registerChildAgent(Thread thread, DebugJSAgent jsAgent) {
        AgentExecutor spawned = new AgentExecutor(thread, jsAgent);
        spawnedAgent.add(spawned);
        return spawned;
    }

    public void broadcast(Object sab) {
        for (AgentExecutor e : spawnedAgent) {
            e.pushMessage(sab);
        }
    }

    public Object getReport() {
        for (AgentExecutor e : spawnedAgent) {
            if (e.jsAgent.reportValues.size() > 0) {
                return e.jsAgent.reportValues.pollLast();
            }
        }
        return Null.instance;
    }

    public void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public void report(Object value) {
        this.reportValues.push(value);
    }

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

        AgentExecutor(Thread thread, DebugJSAgent jsAgent) {
            this.thread = thread;
            this.jsAgent = jsAgent;
            this.incoming = new ConcurrentLinkedDeque<>();
        }

        private void pushMessage(Object sab) {
            incoming.add(sab);
            thread.interrupt();
        }

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
    public void execute(EcmaAgent owner, Runnable task) {
        throw new UnsupportedOperationException("Not supported in Debug agent");
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

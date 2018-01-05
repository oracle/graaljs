/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.interop;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.EcmaAgent;
import com.oracle.truffle.js.runtime.Errors;

/**
 * Default implementation of a Java interop worker used when Graal.js is not embedded in Node.js.
 **/
public class DefaultJavaInteropWorker implements EcmaAgent {

    private int submittedTasks;

    private final DefaultMainWorker parent;
    private final ExecutorService executor;

    public DefaultJavaInteropWorker(DefaultMainWorker main) {
        this.parent = main;
        this.submittedTasks = 0;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @TruffleBoundary
    @Override
    public void execute(EcmaAgent sender, Runnable task) {
        submittedTasks++;
        executor.submit(new Runnable() {

            @Override
            public void run() {
                // scheduled back on the main thread: no races here.
                task.run();
                submittedTasks--;
            }
        });
        executor.submit(task);
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated() || executor.isShutdown();
    }

    @TruffleBoundary
    @Override
    public void terminate(int timeout) {
        while (true) {
            try {
                executor.shutdown();
                if (executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                    parent.activeWorkers.remove(this);
                    return;
                }
            } catch (InterruptedException e) {
                parent.activeWorkers.remove(this);
                throw Errors.createError("Failed to kill Java worker within timeout: terminating");
            }
            Thread.yield();
        }
    }

    public static class Factory implements EcmaAgent.Factory {

        private DefaultMainWorker main;

        public Factory(DefaultMainWorker mainAgent) {
            main = mainAgent;
        }

        @Override
        public EcmaAgent createAgent(EcmaAgent parent) {
            EcmaAgent newAgent = new DefaultJavaInteropWorker(main);
            main.registerNewWorker(newAgent);
            return newAgent;
        }
    }

    public static class DefaultMainWorker implements EcmaAgent {

        private final Deque<EcmaAgent> activeWorkers = new LinkedList<>();
        private final Deque<Runnable> asyncTasks = new ConcurrentLinkedDeque<>();

        @TruffleBoundary
        @Override
        public void execute(EcmaAgent sender, Runnable task) {
            asyncTasks.push(task);
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public void terminate(int timeout) {
            // Main thread cannot be killed.
        }

        @TruffleBoundary
        public void registerNewWorker(EcmaAgent newAgent) {
            this.activeWorkers.push(newAgent);
        }

        @TruffleBoundary
        public boolean processPendingTasks() {
            boolean maybeNewTasks = false;

            while (asyncTasks.size() > 0) {
                asyncTasks.pollLast().run();
                maybeNewTasks = true;
            }

            int activeTasks = activeWorkers.size();
            while (activeTasks > 0) {
                activeTasks = 0;
                for (EcmaAgent a : activeWorkers) {
                    activeTasks += ((DefaultJavaInteropWorker) a).submittedTasks;
                }

                while (asyncTasks.size() > 0) {
                    asyncTasks.pollLast().run();
                    maybeNewTasks = true;
                }
            }
            return maybeNewTasks;
        }

    }
}

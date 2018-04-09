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

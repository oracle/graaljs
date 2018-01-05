/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.interop;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.EcmaAgent;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;

public class GraalJSJavaInteropMainWorker implements EcmaAgent, EcmaAgent.Factory {

    private final GraalJSAccess access;
    private final long loopAddress;
    private final Deque<Runnable> tasks = new ConcurrentLinkedDeque<>();

    public GraalJSJavaInteropMainWorker(GraalJSAccess access, long loopAddress) {
        this.access = access;
        this.loopAddress = loopAddress;
    }

    @Override
    public EcmaAgent createAgent(EcmaAgent parent) {
        return new GraalJSJavaInteropWorker(loopAddress, (GraalJSJavaInteropMainWorker) parent);
    }

    @Override
    public void execute(EcmaAgent sender, Runnable task) {
        tasks.push(task);
        NativeAccess.sendAsyncHandle(((GraalJSJavaInteropWorker) sender).asyncHandle);
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @TruffleBoundary
    @Override
    public void terminate(int timeout) {
        // main worker terminates when the node.js loop returns.
        throw new UnsupportedOperationException("Cannot terminate main node.js loop");
    }

    class GraalJSJavaInteropWorker implements EcmaAgent {

        private final long asyncHandle;
        private final ExecutorService executor;
        private final GraalJSJavaInteropMainWorker parent;

        public GraalJSJavaInteropWorker(long loop, GraalJSJavaInteropMainWorker parent) {
            this.parent = parent;
            this.executor = Executors.newSingleThreadExecutor();
            this.asyncHandle = NativeAccess.createAsyncHandle(loop, new AsyncHandleCallback(this));
        }

        @TruffleBoundary
        @Override
        public void execute(EcmaAgent sender, Runnable task) {
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    task.run();
                }
            });
        }

        @Override
        public boolean isTerminated() {
            return executor.isShutdown() || executor.isTerminated();
        }

        @TruffleBoundary
        @Override
        public void terminate(int timeout) {
            executor.shutdown();
            try {
                executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            // To flush the queue and remove the handle.
            NativeAccess.sendAsyncHandle(asyncHandle);
        }

    }

    private static class AsyncHandleCallback implements Runnable {

        private final GraalJSJavaInteropWorker worker;

        public AsyncHandleCallback(GraalJSJavaInteropWorker worker) {
            this.worker = worker;
        }

        @TruffleBoundary
        @Override
        public void run() {
            try {
                // scheduled back on the main event loop. No races here.
                while (worker.parent.tasks.size() > 0) {
                    worker.parent.tasks.pollFirst().run();
                }
            } finally {
                worker.parent.access.isolateRunMicrotasks();
            }
            if (worker.isTerminated()) {
                NativeAccess.closeAsyncHandle(worker.asyncHandle);
            }
        }
    }

}

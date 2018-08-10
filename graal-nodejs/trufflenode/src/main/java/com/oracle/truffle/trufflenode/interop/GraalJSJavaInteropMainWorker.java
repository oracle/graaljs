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

        GraalJSJavaInteropWorker(long loop, GraalJSJavaInteropMainWorker parent) {
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

        AsyncHandleCallback(GraalJSJavaInteropWorker worker) {
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

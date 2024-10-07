/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.WaiterRecord;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistryObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.AsyncContext;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Base class for ECMA2017 8.7 Agents.
 */
public abstract class JSAgent {

    private static final AtomicInteger signifierGenerator = new AtomicInteger(0);

    /* ECMA2017 Agent Record */
    private final int signifier;
    private boolean canBlock;

    /**
     * ECMA 8.4 "PromiseJobs" job queue.
     */
    private final Deque<JSFunctionObject> promiseJobsQueue;

    /**
     * According to ECMA2017 8.4 the queue of pending jobs (promises reactions) must be processed
     * when the current stack is empty. For Interop, we assume that the stack is empty when (1) we
     * are called from another foreign language, and (2) there are no other nested JS Interop calls.
     *
     * This flag is used to implement this semantics.
     */
    private int interopCallStackDepth;

    /**
     * Used to keep alive objects from weak references in the current job. Made for specification
     * compliance.
     */
    private EconomicSet<Object> weakRefTargets;

    private final Deque<WeakReference<JSFinalizationRegistryObject>> finalizationRegistryQueue;

    private final Deque<WaiterRecord> waitAsyncJobsQueue;

    private PromiseRejectionTracker promiseRejectionTracker;

    private AsyncContext asyncContextMapping = AsyncContext.empty();

    private final List<JSAgent> childAgents = new ArrayList<>();

    public JSAgent(boolean canBlock) {
        this.signifier = signifierGenerator.incrementAndGet();
        this.canBlock = canBlock;
        this.promiseJobsQueue = new ArrayDeque<>();
        this.waitAsyncJobsQueue = new ConcurrentLinkedDeque<>();
        this.finalizationRegistryQueue = new ArrayDeque<>(4);
    }

    public abstract void wake();

    public int getSignifier() {
        return signifier;
    }

    public boolean canBlock() {
        return canBlock;
    }

    public final JobCallback hostMakeJobCallback(Object callback) {
        return new JobCallback(callback, getAsyncContextMapping());
    }

    @TruffleBoundary
    public final void enqueuePromiseJob(JSFunctionObject job) {
        promiseJobsQueue.push(job);
    }

    @TruffleBoundary
    public void enqueueWaitAsyncPromiseJob(WaiterRecord waiter) {
        waitAsyncJobsQueue.push(waiter);
        // Wake up agent to process waitAsync and promise queue now.
        if (waiter.isReadyToResolve()) {
            waiter.getAgent().wake();
        }
    }

    @TruffleBoundary
    public final void processAllPromises(boolean processWeakRefs) {
        try {
            interopBoundaryEnter();
            boolean checkWaiterRecords = !waitAsyncJobsQueue.isEmpty();
            while (!promiseJobsQueue.isEmpty() || checkWaiterRecords) {
                if (checkWaiterRecords) {
                    checkWaiterRecords = processWaitAsyncJobs();
                }
                if (!promiseJobsQueue.isEmpty()) {
                    JSFunctionObject nextJob = promiseJobsQueue.pollLast();
                    JSFunction.call(nextJob, Undefined.instance, JSArguments.EMPTY_ARGUMENTS_ARRAY);
                    checkWaiterRecords = true;
                }
            }
        } catch (Throwable t) {
            // Ensure that there are no leftovers when the processing
            // is terminated by an exception (like ExitException).
            promiseJobsQueue.clear();
            waitAsyncJobsQueue.clear();
            throw t;
        } finally {
            interopBoundaryExit();
            if (processWeakRefs) {
                if (weakRefTargets != null) {
                    weakRefTargets.clear();
                }
                cleanupFinalizers();
            }
            if (promiseRejectionTracker != null) {
                promiseRejectionTracker.promiseReactionJobsProcessed();
            }
        }
    }

    private boolean processWaitAsyncJobs() {
        boolean checkWaiterRecords = false;
        Iterator<WaiterRecord> iter = waitAsyncJobsQueue.descendingIterator();
        while (iter.hasNext()) {
            WaiterRecord wr = iter.next();
            JSAgentWaiterListEntry wl = wr.getWaiterListEntry();
            wl.enterCriticalSection();
            boolean isReadyToResolve = wr.isReadyToResolve();
            try {
                if (isReadyToResolve) {
                    iter.remove();
                    checkWaiterRecords = true;
                    if (wl.contains(wr)) {
                        wr.setResult(Strings.TIMED_OUT);
                        wl.remove(wr);
                    }
                }
            } finally {
                wl.leaveCriticalSection();
            }
            if (isReadyToResolve) {
                JSDynamicObject resolve = (JSDynamicObject) wr.getPromiseCapability().getResolve();
                assert JSFunction.isJSFunction(resolve);
                Object result = wr.getResult();
                JSFunction.call(JSArguments.createOneArg(Undefined.instance, resolve, result));
            }
        }
        return checkWaiterRecords;
    }

    /**
     * Cleanup the finalizationRegistries that are unreferenced; cleanup referenced ones according
     * to 4.1.3 Execution and 4.1.4.1 HostCleanupFinalizatioRegistry.
     */
    private void cleanupFinalizers() {
        for (Iterator<WeakReference<JSFinalizationRegistryObject>> iter = finalizationRegistryQueue.iterator(); iter.hasNext();) {
            WeakReference<JSFinalizationRegistryObject> ref = iter.next();
            JSFinalizationRegistryObject fr = ref.get();
            if (fr == null) {
                iter.remove();
            } else {
                JSFinalizationRegistry.hostCleanupFinalizationRegistry(fr);
            }
        }
    }

    public final void interopBoundaryEnter() {
        interopCallStackDepth++;
    }

    public final boolean interopBoundaryExit() {
        return --interopCallStackDepth == 0;
    }

    @TruffleBoundary
    public boolean addWeakRefTargetToSet(Object target) {
        if (weakRefTargets == null) {
            weakRefTargets = EconomicSet.create(Equivalence.IDENTITY);
        }
        return weakRefTargets.add(target);
    }

    @TruffleBoundary
    public void registerFinalizationRegistry(JSFinalizationRegistryObject finalizationRegistry) {
        finalizationRegistryQueue.add(new WeakReference<>(finalizationRegistry));
    }

    @TruffleBoundary
    public int getAsyncWaitersToBeResolved(JSAgentWaiterListEntry wl) {
        int result = 0;
        for (WaiterRecord wr : waitAsyncJobsQueue) {
            if (wr.getWaiterListEntry() == wl) {
                wl.enterCriticalSection();
                try {
                    if (wr.isReadyToResolve()) {
                        result++;
                    }
                } finally {
                    wl.leaveCriticalSection();
                }
            }
        }
        return result;
    }

    // Used by TestV8 only
    public void setCanBlock(boolean canBlock) {
        this.canBlock = canBlock;
    }

    protected void registerChildAgent(JSAgent agent) {
        synchronized (childAgents) {
            childAgents.add(agent);
        }
    }

    /**
     * Terminate the agent.
     */
    public void terminate() {
        synchronized (childAgents) {
            for (JSAgent childAgent : childAgents) {
                childAgent.terminate();
            }
        }
    }

    public static JSAgent get(Node node) {
        return JSRealm.getMain(node).getAgent();
    }

    protected final boolean hasPromiseRejectionTracker() {
        return promiseRejectionTracker != null;
    }

    protected final void setPromiseRejectionTracker(PromiseRejectionTracker tracker) {
        this.promiseRejectionTracker = tracker;
    }

    @TruffleBoundary
    protected final void notifyPromiseRejectionTracker(JSPromiseObject promise, int operation, Object value) {
        if (!hasPromiseRejectionTracker()) {
            return;
        }
        switch (operation) {
            case JSPromise.REJECTION_TRACKER_OPERATION_REJECT:
                promiseRejectionTracker.promiseRejected(promise, value);
                break;
            case JSPromise.REJECTION_TRACKER_OPERATION_HANDLE:
                promiseRejectionTracker.promiseRejectionHandled(promise);
                break;
            case JSPromise.REJECTION_TRACKER_OPERATION_REJECT_AFTER_RESOLVED:
                promiseRejectionTracker.promiseRejectedAfterResolved(promise, value);
                break;
            case JSPromise.REJECTION_TRACKER_OPERATION_RESOLVE_AFTER_RESOLVED:
                promiseRejectionTracker.promiseResolvedAfterResolved(promise, value);
                break;
            default:
                assert false : "Unknown operation: " + operation;
        }
    }

    public AsyncContext getAsyncContextMapping() {
        return asyncContextMapping;
    }

    private void setAsyncContextMapping(AsyncContext asyncContextMapping) {
        assert asyncContextMapping != null;
        this.asyncContextMapping = asyncContextMapping;
    }

    public AsyncContext asyncContextSwap(AsyncContext snapshot) {
        var previous = getAsyncContextMapping();
        setAsyncContextMapping(snapshot);
        return previous;
    }

}

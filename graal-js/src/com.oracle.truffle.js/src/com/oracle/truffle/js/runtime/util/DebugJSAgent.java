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
package com.oracle.truffle.js.runtime.util;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSInterruptedExecutionException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Testing and debug JSAgent used by test262.
 */
public class DebugJSAgent extends JSAgent {

    private final Deque<Object> reportValues;

    private boolean quit;
    private JSFunctionObject debugReceiveBroadcast;
    private final Queue<JSArrayBufferObject.Shared> broadcasts;
    private Thread thread;

    private final Lock queueLock;
    private final Condition queueCondition;

    static final int POLL_TIMEOUT_MS = 100;

    public DebugJSAgent(boolean canBlock) {
        super(canBlock);
        this.reportValues = new ConcurrentLinkedDeque<>();
        this.broadcasts = new ConcurrentLinkedQueue<>();
        this.queueLock = new ReentrantLock();
        this.queueCondition = queueLock.newCondition();
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "DebugJSAgent{signifier=" + getSignifier() + "}";
    }

    @TruffleBoundary
    public void startNewAgent(String sourceText) {
        final Source agentSource = Source.newBuilder(JavaScriptLanguage.ID, sourceText, "agent").build();
        final TruffleLanguage.Env env = JavaScriptLanguage.getCurrentEnv();
        final TruffleContext agentContext = env.newInnerContextBuilder().inheritAllAccess(true).build();
        final CountDownLatch barrier = new CountDownLatch(1);

        agentContext.initializePublic(null, JavaScriptLanguage.ID);

        Thread newThread = env.newTruffleThreadBuilder(new Runnable() {
            @Override
            public void run() {
                JSRealm innerContext = JavaScriptLanguage.getCurrentJSRealm();
                DebugJSAgent childAgent = (DebugJSAgent) innerContext.getAgent();
                childAgent.thread = Thread.currentThread();
                DebugJSAgent parentAgent = DebugJSAgent.this;
                parentAgent.registerChildAgent(childAgent);

                CallTarget callTarget = innerContext.getEnv().parsePublic(agentSource);
                callTarget.call();

                barrier.countDown();

                try {
                    // Note: Evaluation of the agent source may have already called agent.leaving().
                    while (true) {
                        childAgent.queueLock.lock();
                        try {
                            if (childAgent.quit) {
                                return;
                            }
                            childAgent.queueCondition.await(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        } finally {
                            childAgent.queueLock.unlock();
                        }
                        // Signal received or timeout. Process all pending events.
                        do {
                            JSArrayBufferObject.Shared original = childAgent.broadcasts.poll();
                            if (original != null) {
                                // Create SharedArrayBuffer for this agent
                                // (sharing the ByteBuffer with the original)
                                JSArrayBufferObject.Shared current = (JSArrayBufferObject.Shared) JSSharedArrayBuffer.createSharedArrayBuffer(innerContext.getContext(), innerContext,
                                                original.getByteBuffer());
                                current.setWaiterList(original.getWaiterList());

                                childAgent.executeBroadcastCallback(current);
                                // broadcast callback may have called agent.leaving().
                                if (childAgent.quit) {
                                    return;
                                }
                            }
                            childAgent.processAllPromises(true);
                        } while (!childAgent.broadcasts.isEmpty());
                    }
                } catch (InterruptedException e) {
                    System.err.println("Interrupted " + Thread.currentThread());
                } catch (AbstractTruffleException e) {
                    System.err.println("Uncaught error from " + Thread.currentThread() + ": " + e.getMessage());
                }
            }
        }).context(agentContext).build();

        newThread.setName("Debug-JSAgent-Worker-Thread");
        newThread.start();
        try {
            barrier.await();
        } catch (InterruptedException e) {
            throw JSInterruptedExecutionException.wrap(e);
        }
    }

    @TruffleBoundary
    public void setDebugReceiveBroadcast(JSFunctionObject broadcast) {
        this.debugReceiveBroadcast = broadcast;
    }

    @TruffleBoundary
    public void broadcast(JSArrayBufferObject.Shared sab) {
        synchronized (childAgents) {
            for (JSAgent agent : childAgents) {
                if (agent instanceof DebugJSAgent debugAgent) {
                    debugAgent.pushMessage(sab);
                }
            }
        }
    }

    @TruffleBoundary
    public Object getReport() {
        synchronized (childAgents) {
            for (JSAgent agent : childAgents) {
                if (agent instanceof DebugJSAgent debugAgent) {
                    if (!debugAgent.reportValues.isEmpty()) {
                        return debugAgent.reportValues.pollLast();
                    }
                }
            }
        }
        return Null.instance;
    }

    @TruffleBoundary
    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw JSInterruptedExecutionException.wrap(e);
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
    public void wake() {
        CompilerAsserts.neverPartOfCompilation();
        queueLock.lock();
        try {
            queueCondition.signalAll();
        } finally {
            queueLock.unlock();
        }
    }

    private void pushMessage(JSArrayBufferObject.Shared sab) {
        CompilerAsserts.neverPartOfCompilation();
        broadcasts.add(sab);
        wake();
    }

    private void executeBroadcastCallback(JSArrayBufferObject.Shared sab) {
        CompilerAsserts.neverPartOfCompilation();
        JSFunction.call(debugReceiveBroadcast, Undefined.instance, new Object[]{sab});
    }

    @TruffleBoundary
    @Override
    public void terminate() {
        super.terminate();
        synchronized (childAgents) {
            try {
                for (JSAgent agent : childAgents) {
                    if (agent instanceof DebugJSAgent debugAgent) {
                        debugAgent.thread.join();
                    }
                }
            } catch (InterruptedException iex) {
                Thread.currentThread().interrupt();
            }
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

}

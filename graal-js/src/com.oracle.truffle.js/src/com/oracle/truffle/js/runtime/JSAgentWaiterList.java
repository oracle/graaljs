/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;

/**
 * Java-based implementation of ECMA2017 WaiterList (24.4.1.2).
 */
public class JSAgentWaiterList {

    private final Map<Integer, JSAgentWaiterListEntry> waiters;

    private final ReentrantLock atomicSection;

    @TruffleBoundary
    public JSAgentWaiterList() {
        this.waiters = new ConcurrentHashMap<>();
        this.atomicSection = new ReentrantLock();
    }

    public JSAgentWaiterListEntry getListForIndex(int indexPos) {
        JSAgentWaiterListEntry list = Boundaries.mapPutIfAbsent(waiters, indexPos, new JSAgentWaiterListEntry());
        if (list == null) {
            return Boundaries.mapGet(waiters, indexPos);
        } else {
            return list;
        }
    }

    @TruffleBoundary
    public void enterAtomicSection() {
        assert !inAtomicSection();
        atomicSection.lock();
    }

    @TruffleBoundary
    public void leaveAtomicSection() {
        assert inAtomicSection();
        atomicSection.unlock();
    }

    public boolean inAtomicSection() {
        return atomicSection.isHeldByCurrentThread();
    }

    public static final class JSAgentWaiterListEntry extends ConcurrentLinkedQueue<WaiterRecord> {

        private static final long serialVersionUID = 2655886588267252886L;

        private final ReentrantLock criticalSection;
        private final Condition waitCondition;

        @TruffleBoundary
        public JSAgentWaiterListEntry() {
            this.criticalSection = new ReentrantLock();
            this.waitCondition = criticalSection.newCondition();
        }

        public void enterCriticalSection() {
            assert !inCriticalSection();
            criticalSection.lock();
        }

        public void leaveCriticalSection() {
            assert inCriticalSection();
            criticalSection.unlock();
        }

        public Condition getCondition() {
            return waitCondition;
        }

        public boolean inCriticalSection() {
            return criticalSection.isHeldByCurrentThread();
        }
    }

    public static final class WaiterRecord {

        private final int agentSignifier;
        private final PromiseCapabilityRecord promiseCapability;
        private final double timeout;
        private TruffleString result;
        private final JSAgentWaiterListEntry wl;
        private final JSAgent agent;

        private long creationTimestamp;
        private boolean notified;

        private WaiterRecord(int agentSignifier, PromiseCapabilityRecord promiseCapability, double timeout, TruffleString result, JSAgentWaiterListEntry wl, JSAgent agent) {
            this.agentSignifier = agentSignifier;
            this.promiseCapability = promiseCapability;
            this.timeout = timeout;
            this.result = result;
            this.wl = wl;
            this.agent = agent;

            this.notified = false;
        }

        public static WaiterRecord create(int agentSignifier, PromiseCapabilityRecord promiseCapability, double timeout, TruffleString result, JSAgentWaiterListEntry wl, JSAgent agent) {
            return new WaiterRecord(agentSignifier, promiseCapability, timeout, result, wl, agent);
        }

        public int getAgentSignifier() {
            return agentSignifier;
        }

        public PromiseCapabilityRecord getPromiseCapability() {
            return promiseCapability;
        }

        public double getTimeout() {
            return timeout;
        }

        public TruffleString getResult() {
            return result;
        }

        public void setResult(TruffleString result) {
            this.result = result;
        }

        public JSAgentWaiterListEntry getWaiterListEntry() {
            return wl;
        }

        public void setCreationTime(long timeMillis) {
            creationTimestamp = timeMillis;
        }

        public long getCreationTime() {
            return creationTimestamp;
        }

        public void setNotified() {
            assert wl.inCriticalSection();
            assert !notified;
            notified = true;
        }

        public boolean isNotified() {
            assert wl.inCriticalSection();
            return notified;
        }

        public boolean isReadyToResolve() {
            assert wl.inCriticalSection();
            return notified || isTimedOut();
        }

        private boolean isTimedOut() {
            assert !notified;
            long current = System.nanoTime() / JSRealm.NANOSECONDS_PER_MILLISECOND;
            long elapsed = current - creationTimestamp;
            return elapsed >= timeout;
        }

        public void enqueueInAgent() {
            agent.enqueueWaitAsyncPromiseJob(this);
        }

        public JSAgent getAgent() {
            return agent;
        }
    }

}

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
package com.oracle.truffle.js.builtins.helper;

import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.WaiterRecord;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSInterruptedExecutionException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedBigIntArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedIntArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

/**
 * Implementation of the synchronization primitives of ECMA2017 Shared Memory model.
 */
public final class SharedMemorySync {

    private SharedMemorySync() {
        // should not be constructed
    }

    // ##### Getters and setters with ordering and memory barriers
    public static int doVolatileGet(JSTypedArrayObject target, int intArrayOffset, TypedIntArray typedArray) {
        int result = typedArray.getInt(target, intArrayOffset, InteropLibrary.getUncached());
        VarHandle.acquireFence();
        return result;
    }

    public static BigInt doVolatileGetBigInt(JSTypedArrayObject target, int intArrayOffset, TypedBigIntArray typedArray) {
        BigInt result = typedArray.getBigInt(target, intArrayOffset, InteropLibrary.getUncached());
        VarHandle.acquireFence();
        return result;
    }

    public static void doVolatilePut(JSTypedArrayObject target, int index, int value, TypedIntArray typedArray) {
        VarHandle.releaseFence();
        typedArray.setInt(target, index, value, InteropLibrary.getUncached());
        VarHandle.fullFence();
    }

    public static void doVolatilePutBigInt(JSTypedArrayObject target, int index, BigInt value, TypedBigIntArray typedArray) {
        VarHandle.releaseFence();
        typedArray.setBigInt(target, index, value, InteropLibrary.getUncached());
        VarHandle.fullFence();
    }

    // ##### Atomic CAS primitives
    public static boolean compareAndSetInt(JSTypedArrayObject target, int intArrayOffset, int expected, int replacement, TypedIntArray typedArray) {
        return typedArray.compareExchangeInt(target, intArrayOffset, expected, replacement) == expected;
    }

    public static boolean compareAndSetBigInt(JSTypedArrayObject target, int intArrayOffset, BigInt expected, BigInt replacement, TypedBigIntArray typedArray) {
        long expectedAsLong = expected.longValue();
        return typedArray.compareExchangeLong(target, intArrayOffset, expectedAsLong, replacement.longValue()) == expectedAsLong;
    }

    // ##### Thread Wake/Park primitives

    public static JSAgentWaiterListEntry getWaiterList(JSContext context, JSDynamicObject target, int indexPos) {
        JSDynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        JSAgentWaiterList waiterList = JSSharedArrayBuffer.getWaiterList(arrayBuffer);
        int offset = JSArrayBufferView.getByteOffset(target, context);
        int bytesPerElement = JSArrayBufferView.typedArrayGetArrayType(target).bytesPerElement();
        return waiterList.getListForIndex(indexPos * bytesPerElement + offset);
    }

    @TruffleBoundary
    public static void enterCriticalSection(JSAgentWaiterListEntry wl) {
        wl.enterCriticalSection();
    }

    @TruffleBoundary
    public static void leaveCriticalSection(JSAgentWaiterListEntry wl) {
        wl.leaveCriticalSection();
    }

    public static boolean agentCanSuspend(JSAgent agent) {
        return agent.canBlock();
    }

    @TruffleBoundary
    public static void addWaiter(JSAgent agent, JSAgentWaiterListEntry wl, WaiterRecord waiterRecord, boolean isAsync) {
        assert wl.inCriticalSection();
        assert !wl.contains(waiterRecord);
        wl.add(waiterRecord);
        if (isAsync && Double.isFinite(waiterRecord.getTimeout())) {
            waiterRecord.setCreationTime(System.nanoTime() / JSRealm.NANOSECONDS_PER_MILLISECOND);
            agent.enqueueWaitAsyncPromiseJob(waiterRecord);
        }
    }

    @TruffleBoundary
    public static void removeWaiter(JSAgentWaiterListEntry wl, WaiterRecord w) {
        assert wl.inCriticalSection();
        assert wl.contains(w);
        wl.remove(w);
    }

    /**
     * SuspendAgent (WL, W, timeout).
     *
     * Suspends (blocks) this agent, awaiting a notification via this WaiterList.
     *
     * @return true if agent W was notified by another agent; false if timed out.
     */
    @TruffleBoundary
    public static boolean suspendAgent(JSAgent agent, JSAgentWaiterListEntry wl, WaiterRecord waiterRecord) {
        assert wl.inCriticalSection();
        assert agent.getSignifier() == waiterRecord.getAgentSignifier();
        assert wl.contains(waiterRecord);
        assert agent.canBlock();
        boolean finiteTimeout = Double.isFinite(waiterRecord.getTimeout());
        long timeoutRemaining = finiteTimeout ? TimeUnit.MILLISECONDS.toNanos((long) waiterRecord.getTimeout()) : 0L;
        try {
            Condition condition = wl.getCondition();
            while (true) {
                if (waiterRecord.isNotified()) {
                    return true;
                }
                if (finiteTimeout) {
                    timeoutRemaining = condition.awaitNanos(timeoutRemaining);
                    if (timeoutRemaining <= 0) {
                        // timed out
                        return false;
                    }
                } else {
                    condition.await();
                }
            }
        } catch (InterruptedException e) {
            throw new JSInterruptedExecutionException(e.getMessage(), null);
        }
    }

    /**
     * NotifyWaiter (WL, W). Notifies (but does not wake) a waiting agent.
     */
    @TruffleBoundary
    public static void notifyWaiter(WaiterRecord waiterRecord) {
        waiterRecord.setNotified();
    }

    /**
     * Wakes waiting agents on the WaiterList, since at least one of them should be notified.
     */
    @TruffleBoundary
    public static void wakeWaiters(JSAgentWaiterListEntry wl) {
        assert wl.inCriticalSection();
        wl.getCondition().signalAll();
    }

    @TruffleBoundary
    public static WaiterRecord[] removeWaiters(JSAgentWaiterListEntry wl, int count) {
        assert wl.inCriticalSection();
        int c = 0;
        Iterator<WaiterRecord> iter = wl.iterator();
        List<WaiterRecord> list = new LinkedList<>();
        while (iter.hasNext() && c < count) {
            WaiterRecord wr = iter.next();
            if (wr.getPromiseCapability() == null || !wr.isReadyToResolve()) {
                list.add(wr);
                iter.remove();
                ++c;
            }
        }
        return list.toArray(new WaiterRecord[c]);
    }
}

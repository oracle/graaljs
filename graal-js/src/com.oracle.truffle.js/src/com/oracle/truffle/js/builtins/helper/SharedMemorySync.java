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

import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetArrayType;

import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.WaiterRecord;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;

/**
 * Implementation of the synchronization primitives of ECMA2017 Shared Memory model.
 */
public final class SharedMemorySync {

    private SharedMemorySync() {
        // should not be constructed
    }

    // ##### Getters and setters with ordering and memory barriers
    @TruffleBoundary
    public static int doVolatileGet(DynamicObject target, int intArrayOffset) {
        VarHandle.acquireFence();
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) array;
        return typedArray.getInt(target, intArrayOffset, InteropLibrary.getUncached());
    }

    // ##### Getters and setters with ordering and memory barriers
    @TruffleBoundary
    public static BigInt doVolatileGetBigInt(DynamicObject target, int intArrayOffset) {
        VarHandle.acquireFence();
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedBigIntArray typedArray = (TypedArray.TypedBigIntArray) array;
        return typedArray.getBigInt(target, intArrayOffset, InteropLibrary.getUncached());
    }

    @TruffleBoundary
    public static void doVolatilePut(DynamicObject target, int index, int value) {
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) array;
        typedArray.setInt(target, index, value, InteropLibrary.getUncached());
        VarHandle.releaseFence();
    }

    @TruffleBoundary
    public static void doVolatilePutBigInt(DynamicObject target, int index, BigInt value) {
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedBigIntArray typedArray = (TypedArray.TypedBigIntArray) array;
        typedArray.setBigInt(target, index, value, InteropLibrary.getUncached());
        VarHandle.releaseFence();
    }

    // ##### Atomic CAS primitives
    @TruffleBoundary
    public static boolean compareAndSwapInt(JSAgent agent, DynamicObject target, int intArrayOffset, int initial, int result) {
        agent.atomicSectionEnter(target);
        try {
            int value = doVolatileGet(target, intArrayOffset);
            if (value == initial) {
                doVolatilePut(target, intArrayOffset, result);
                return true;
            }
            return false;
        } finally {
            agent.atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static boolean compareAndSwapBigInt(JSAgent agent, DynamicObject target, int intArrayOffset, BigInt initial, BigInt result) {
        agent.atomicSectionEnter(target);
        try {
            BigInt value = doVolatileGetBigInt(target, intArrayOffset);
            if (value.compareTo(initial) == 0) {
                doVolatilePutBigInt(target, intArrayOffset, result);
                return true;
            }
            return false;
        } finally {
            agent.atomicSectionLeave(target);
        }
    }

    // ##### Atomic Fetch-or-Get primitives
    @TruffleBoundary
    public static long atomicFetchOrGetUnsigned(JSAgent agent, DynamicObject target, int intArrayOffset, Object expected, Object replacement) {
        agent.atomicSectionEnter(target);
        long read = JSRuntime.toUInt32(doVolatileGet(target, intArrayOffset));
        if (read == JSRuntime.toUInt32(expected)) {
            doVolatilePut(target, intArrayOffset, (int) JSRuntime.toUInt32(replacement));
        }
        agent.atomicSectionLeave(target);
        return read;
    }

    @TruffleBoundary
    public static long atomicFetchOrGetLong(JSAgent agent, DynamicObject target, int intArrayOffset, long expected, long replacement) {
        agent.atomicSectionEnter(target);
        try {
            int read = doVolatileGet(target, intArrayOffset);
            if (read == expected) {
                doVolatilePut(target, intArrayOffset, (int) replacement);
            }
            return read;
        } finally {
            agent.atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static int atomicFetchOrGetInt(JSAgent agent, DynamicObject target, int intArrayOffset, int expected, int replacement) {
        agent.atomicSectionEnter(target);
        try {
            int read = doVolatileGet(target, intArrayOffset);
            if (read == expected) {
                doVolatilePut(target, intArrayOffset, replacement);
            }
            return read;
        } finally {
            agent.atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static int atomicFetchOrGetShort(JSAgent agent, DynamicObject target, int intArrayOffset, int expected, int replacement, boolean sign) {
        agent.atomicSectionEnter(target);
        int read = doVolatileGet(target, intArrayOffset);
        read = sign ? read : read & 0xFFFF;
        int expectedChopped = sign ? (short) expected : expected & 0xFFFF;
        if (read == expectedChopped) {
            int signed = sign ? replacement : replacement & 0xFFFF;
            SharedMemorySync.doVolatilePut(target, intArrayOffset, (short) signed);
        }
        agent.atomicSectionLeave(target);
        return read;
    }

    @TruffleBoundary
    public static int atomicFetchOrGetByte(JSAgent agent, DynamicObject target, int intArrayOffset, int expected, int replacement, boolean sign) {
        agent.atomicSectionEnter(target);
        try {
            int read = doVolatileGet(target, intArrayOffset);
            read = sign ? read : read & 0xFF;
            int expectedChopped = sign ? (byte) expected : expected & 0xFF;
            if (read == expectedChopped) {
                int signed = sign ? replacement : replacement & 0xFF;
                SharedMemorySync.doVolatilePut(target, intArrayOffset, (byte) signed);
            }
            return read;
        } finally {
            agent.atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static BigInt atomicFetchOrGetBigInt(JSAgent agent, DynamicObject target, int intArrayOffset, BigInt expected, BigInt replacement) {
        agent.atomicSectionEnter(target);
        try {
            BigInt read = doVolatileGetBigInt(target, intArrayOffset);
            if (read.compareTo(expected) == 0) {
                doVolatilePutBigInt(target, intArrayOffset, replacement);
            }
            return read;
        } finally {
            agent.atomicSectionLeave(target);
        }
    }

    // ##### Thread Wake/Park primitives

    @SuppressWarnings("unused")
    public static JSAgentWaiterListEntry getWaiterList(JSContext context, JSAgent agent, DynamicObject target, int indexPos) {
        DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        JSAgentWaiterList waiterList = JSSharedArrayBuffer.getWaiterList(arrayBuffer);
        int offset = JSArrayBufferView.getByteOffset(target, context);
        int bytesPerElement = JSArrayBufferView.typedArrayGetArrayType(target).bytesPerElement();
        return waiterList.getListForIndex(indexPos * bytesPerElement + offset);
    }

    @TruffleBoundary
    public static void enterCriticalSection(JSAgent agent, JSAgentWaiterListEntry wl) {
        assert !agent.inCriticalSection();
        agent.criticalSectionEnter(wl);
    }

    @TruffleBoundary
    public static void leaveCriticalSection(JSAgent agent, JSAgentWaiterListEntry wl) {
        agent.criticalSectionLeave(wl);
    }

    public static boolean agentCanSuspend(JSAgent agent) {
        return agent.canBlock();
    }

    @TruffleBoundary
    public static void addWaiter(JSAgent agent, JSAgentWaiterListEntry wl, WaiterRecord waiterRecord, boolean isAsync) {
        assert agent.inCriticalSection();
        assert !wl.contains(waiterRecord);
        wl.add(waiterRecord);
        if (isAsync && Double.isFinite(waiterRecord.getTimeout())) {
            waiterRecord.setCreationTime(System.nanoTime() / JSRealm.NANOSECONDS_PER_MILLISECOND);
            agent.enqueueWaitAsyncPromiseJob(waiterRecord);
        }
    }

    @TruffleBoundary
    public static void removeWaiter(JSAgent agent, JSAgentWaiterListEntry wl, WaiterRecord w) {
        assert agent.inCriticalSection();
        assert wl.contains(w);
        wl.remove(w);
    }

    /* ECMA2022 25.4.1.9 - Suspend returns true if agent was woken by another agent */
    @TruffleBoundary
    public static boolean suspendAgent(JSAgent agent, JSAgentWaiterListEntry wl, WaiterRecord waiterRecord) {
        assert agent.inCriticalSection();
        assert agent.getSignifier() == waiterRecord.getAgentSignifier();
        assert wl.contains(waiterRecord);
        assert agent.canBlock();
        agent.criticalSectionLeave(wl);
        boolean interrupt = false;
        try {
            Thread.sleep((long) waiterRecord.getTimeout());
        } catch (InterruptedException e) {
            interrupt = true;
        }
        agent.criticalSectionEnter(wl);
        return interrupt;
    }

    /* ECMA2022 25.4.1.10 - Wake up another agent */
    @TruffleBoundary
    public static void notifyWaiter(JSAgent agent, WaiterRecord waiterRecord) {
        assert agent.inCriticalSection();
        assert waiterRecord.getPromiseCapability() == null;
        agent.wakeAgent(waiterRecord.getAgentSignifier());
    }

    @TruffleBoundary
    public static WaiterRecord[] removeWaiters(JSAgent agent, JSAgentWaiterListEntry wl, int count) {
        assert agent.inCriticalSection();
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

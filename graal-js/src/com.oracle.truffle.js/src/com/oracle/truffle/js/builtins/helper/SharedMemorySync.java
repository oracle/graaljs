/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.util.Fences;

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
        Fences.acquireFence();
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) array;
        return typedArray.getInt(target, intArrayOffset);
    }

    // ##### Getters and setters with ordering and memory barriers
    @TruffleBoundary
    public static BigInt doVolatileGetBigInt(DynamicObject target, int intArrayOffset) {
        Fences.acquireFence();
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedBigIntArray<?> typedArray = (TypedArray.TypedBigIntArray<?>) array;
        return typedArray.getBigInt(target, intArrayOffset);
    }

    @TruffleBoundary
    public static void doVolatilePut(DynamicObject target, int index, int value) {
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) array;
        typedArray.setInt(target, index, value);
        Fences.releaseFence();
    }

    @TruffleBoundary
    public static void doVolatilePutBigInt(DynamicObject target, int index, BigInt value) {
        TypedArray array = typedArrayGetArrayType(target);
        TypedArray.TypedBigIntArray<?> typedArray = (TypedArray.TypedBigIntArray<?>) array;
        typedArray.setBigInt(target, index, value);
        Fences.releaseFence();
    }

    // ##### Atomic CAS primitives
    @TruffleBoundary
    public static boolean compareAndSwapInt(JSContext cx, DynamicObject target, int intArrayOffset, int initial, int result) {
        cx.getJSAgent().atomicSectionEnter(target);
        try {
            int value = doVolatileGet(target, intArrayOffset);
            if (value == initial) {
                doVolatilePut(target, intArrayOffset, result);
                return true;
            }
            return false;
        } finally {
            cx.getJSAgent().atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static boolean compareAndSwapBigInt(JSContext cx, DynamicObject target, int intArrayOffset, BigInt initial, BigInt result) {
        cx.getJSAgent().atomicSectionEnter(target);
        try {
            BigInt value = doVolatileGetBigInt(target, intArrayOffset);
            if (value.compareTo(initial) == 0) {
                doVolatilePutBigInt(target, intArrayOffset, result);
                return true;
            }
            return false;
        } finally {
            cx.getJSAgent().atomicSectionLeave(target);
        }
    }

    // ##### Atomic Fetch-or-Get primitives
    @TruffleBoundary
    public static long atomicFetchOrGetUnsigned(JSContext cx, DynamicObject target, int intArrayOffset, Object expected, Object replacement) {
        cx.getJSAgent().atomicSectionEnter(target);
        long read = JSRuntime.toUInt32(doVolatileGet(target, intArrayOffset));
        if (read == JSRuntime.toUInt32(expected)) {
            doVolatilePut(target, intArrayOffset, (int) JSRuntime.toUInt32(replacement));
        }
        cx.getJSAgent().atomicSectionLeave(target);
        return read;
    }

    @TruffleBoundary
    public static long atomicFetchOrGetLong(JSContext cx, DynamicObject target, int intArrayOffset, long expected, long replacement) {
        cx.getJSAgent().atomicSectionEnter(target);
        try {
            int read = doVolatileGet(target, intArrayOffset);
            if (read == expected) {
                doVolatilePut(target, intArrayOffset, (int) replacement);
            }
            return read;
        } finally {
            cx.getJSAgent().atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static int atomicFetchOrGetInt(JSContext cx, DynamicObject target, int intArrayOffset, int expected, int replacement) {
        cx.getJSAgent().atomicSectionEnter(target);
        try {
            int read = doVolatileGet(target, intArrayOffset);
            if (read == expected) {
                doVolatilePut(target, intArrayOffset, replacement);
            }
            return read;
        } finally {
            cx.getJSAgent().atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static int atomicFetchOrGetShort(JSContext cx, DynamicObject target, int intArrayOffset, int expected, int replacement, boolean sign) {
        cx.getJSAgent().atomicSectionEnter(target);
        int read = doVolatileGet(target, intArrayOffset);
        read = sign ? read : read & 0xFFFF;
        int expectedChopped = sign ? (short) expected : expected & 0xFFFF;
        if (read == expectedChopped) {
            int signed = sign ? replacement : replacement & 0xFFFF;
            SharedMemorySync.doVolatilePut(target, intArrayOffset, (short) signed);
        }
        cx.getJSAgent().atomicSectionLeave(target);
        return read;
    }

    @TruffleBoundary
    public static int atomicFetchOrGetByte(JSContext cx, DynamicObject target, int intArrayOffset, int expected, int replacement, boolean sign) {
        cx.getJSAgent().atomicSectionEnter(target);
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
            cx.getJSAgent().atomicSectionLeave(target);
        }
    }

    @TruffleBoundary
    public static BigInt atomicFetchOrGetBigInt(JSContext cx, DynamicObject target, int intArrayOffset, BigInt expected, BigInt replacement) {
        cx.getJSAgent().atomicSectionEnter(target);
        try {
            BigInt read = doVolatileGetBigInt(target, intArrayOffset);
            if (read.compareTo(expected) == 0) {
                doVolatilePutBigInt(target, intArrayOffset, replacement);
            }
            return read;
        } finally {
            cx.getJSAgent().atomicSectionLeave(target);
        }
    }

    // ##### Thread Wake/Park primitives

    @SuppressWarnings("unused")
    public static JSAgentWaiterListEntry getWaiterList(JSContext cx, DynamicObject target, int indexPos) {
        DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        JSAgentWaiterList waiterList = JSSharedArrayBuffer.getWaiterList(arrayBuffer);
        return waiterList.getListForIndex(indexPos);
    }

    @TruffleBoundary
    public static void enterCriticalSection(JSContext cx, JSAgentWaiterListEntry wl) {
        assert !cx.getJSAgent().inCriticalSection();
        cx.getJSAgent().criticalSectionEnter(wl);
    }

    @TruffleBoundary
    public static void leaveCriticalSection(JSContext cx, JSAgentWaiterListEntry wl) {
        cx.getJSAgent().criticalSectionLeave(wl);
    }

    public static boolean agentCanSuspend(JSContext cx) {
        return cx.getJSAgent().canBlock();
    }

    @TruffleBoundary
    public static void addWaiter(JSContext cx, JSAgentWaiterListEntry wl, int id) {
        assert cx.getJSAgent().inCriticalSection();
        assert !wl.contains(id);
        wl.add(id);
    }

    @TruffleBoundary
    public static void removeWaiter(JSContext cx, JSAgentWaiterListEntry wl, int w) {
        assert cx.getJSAgent().inCriticalSection();
        assert wl.contains(w);
        wl.remove(w);
    }

    /* ECMA2017 24.4.1.9 - Suspend returns true if agent was woken by another agent */
    @TruffleBoundary
    public static boolean suspendAgent(JSContext cx, JSAgentWaiterListEntry wl, int w, int timeout) {
        assert cx.getJSAgent().inCriticalSection();
        assert wl.contains(w);
        assert cx.getJSAgent().getSignifier() == w;
        assert cx.getJSAgent().canBlock();
        cx.getJSAgent().criticalSectionLeave(wl);
        boolean interrupt = false;
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            interrupt = true;
        }
        cx.getJSAgent().criticalSectionEnter(wl);
        return interrupt;
    }

    /* ECMA2017 24.4.1.10 - Wake up another agent */
    @TruffleBoundary
    public static void wakeWaiter(JSContext cx, int w) {
        assert cx.getJSAgent().inCriticalSection();
        cx.getJSAgent().wakeAgent(w);
    }

    @TruffleBoundary
    public static int[] removeWaiters(JSContext cx, JSAgentWaiterListEntry wl, int count) {
        assert cx.getJSAgent().inCriticalSection();
        int c = Integer.min(wl.size(), count);
        int[] removed = new int[c];
        while (c-- > 0) {
            removed[c] = wl.poll();
        }
        return removed;
    }
}

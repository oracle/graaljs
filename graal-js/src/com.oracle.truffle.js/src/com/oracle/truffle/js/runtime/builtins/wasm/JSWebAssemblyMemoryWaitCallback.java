/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins.wasm;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.helper.SharedMemorySync;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;

import java.lang.invoke.VarHandle;

/**
 * Represents a callback that is invoked when the memory.atomic.waitN instruction executes in
 * WebAssembly. This allows us to reuse the JS implementation of Atomics.wait.
 */
@ExportLibrary(InteropLibrary.class)
public final class JSWebAssemblyMemoryWaitCallback implements TruffleObject {
    private static final int INT32_BYTES_PER_ELEMENT = 4;
    private static final int BIGINT64_BYTES_PER_ELEMENT = 8;
    private final JSRealm realm;
    private final JSContext context;

    public JSWebAssemblyMemoryWaitCallback(JSRealm realm, JSContext context) {
        this.realm = realm;
        this.context = context;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments) {
        assert arguments.length == 5;
        final JSWebAssemblyMemoryObject memoryObject = JSWebAssemblyMemory.create(context, realm, arguments[0], true);
        final long address = (long) arguments[1];
        final long expected = (long) arguments[2];
        final long timeout = (long) arguments[3];
        final double convertedTimeout = timeout >= 0 ? (timeout / 1e6) : Double.POSITIVE_INFINITY;
        final boolean is64 = (boolean) arguments[4];

        final JSArrayBufferObject buffer = memoryObject.getBufferObject(context, realm);
        final TruffleString result = atomicsWait(buffer, (int) address, expected, convertedTimeout, is64);

        if (Strings.equals(result, Strings.OK)) {
            return 0;
        } else if (Strings.equals(result, Strings.NOT_EQUAL)) {
            return 1;
        } else {
            assert Strings.equals(result, Strings.TIMED_OUT);
            return 2;
        }
    }

    private TruffleString atomicsWait(JSArrayBufferObject buffer, int address, long expected, double timeout, boolean is64) {
        final JSAgent agent = realm.getAgent();
        if (!agent.canBlock()) {
            throw Errors.createRuntimeError("wait instruction used by agent which cannot block", realm);
        }
        final JSAgentWaiterList waiterList = JSSharedArrayBuffer.getWaiterList(buffer);
        final JSAgentWaiterList.JSAgentWaiterListEntry wl;
        if (!is64) {
            wl = waiterList.getListForIndex(address * INT32_BYTES_PER_ELEMENT);
        } else {
            wl = waiterList.getListForIndex(address * BIGINT64_BYTES_PER_ELEMENT);
        }
        wl.enterCriticalSection();
        try {
            if (!is64) {
                final int val = doVolatileGetFromBuffer(buffer, address);
                if (val != expected) {
                    return Strings.NOT_EQUAL;
                }
            } else {
                final BigInt val = doVolatileGetBigIntFromBuffer(buffer, address);
                if (val.longValue() != expected) {
                    return Strings.NOT_EQUAL;
                }
            }
            final int id = agent.getSignifier();
            final JSAgentWaiterList.WaiterRecord waiterRecord = JSAgentWaiterList.WaiterRecord.create(id, null, timeout, Strings.OK, wl, agent);
            SharedMemorySync.addWaiter(agent, wl, waiterRecord, false);
            final boolean awoken = SharedMemorySync.suspendAgent(agent, wl, waiterRecord);
            if (awoken) {
                assert !wl.contains(waiterRecord);
                return Strings.OK;
            } else {
                SharedMemorySync.removeWaiter(wl, waiterRecord);
                return Strings.TIMED_OUT;
            }
        } finally {
            wl.leaveCriticalSection();
        }
    }

    private static int doVolatileGetFromBuffer(JSArrayBufferObject buffer, int intArrayOffset) {
        TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) TypedArrayFactory.Int32Array.createArrayType(TypedArray.BUFFER_TYPE_DIRECT, false, true);
        int result = typedArray.getBufferElementIntImpl(buffer, intArrayOffset, true, InteropLibrary.getUncached());
        VarHandle.acquireFence();
        return result;
    }

    private static BigInt doVolatileGetBigIntFromBuffer(JSArrayBufferObject buffer, int bigIntArrayOffset) {
        TypedArray.TypedBigIntArray typedArray = (TypedArray.TypedBigIntArray) TypedArrayFactory.BigInt64Array.createArrayType(TypedArray.BUFFER_TYPE_DIRECT, false, true);
        BigInt result = BigInt.valueOf(typedArray.getBufferElementLongImpl(buffer, bigIntArrayOffset, true, InteropLibrary.getUncached()));
        VarHandle.acquireFence();
        return result;
    }
}

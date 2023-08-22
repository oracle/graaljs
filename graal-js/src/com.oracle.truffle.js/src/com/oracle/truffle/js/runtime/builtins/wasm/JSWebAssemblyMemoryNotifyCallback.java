/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.helper.SharedMemorySync;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

/**
 * Represents a callback that is invoked when the memory.atomic.notify instruction executes in WebAssembly.
 * This allows us to reuse the JS implementation of Atomics.notify.
 */
@ExportLibrary(InteropLibrary.class)
public final class JSWebAssemblyMemoryNotifyCallback implements TruffleObject {
    private final JSRealm realm;
    private final JSContext context;
    private final Object memSetNotifyCallbackFunction;

    public JSWebAssemblyMemoryNotifyCallback(JSRealm realm, JSContext context, Object memSetNotifyCallbackFunction) {
        this.realm = realm;
        this.context = context;
        this.memSetNotifyCallbackFunction = memSetNotifyCallbackFunction;
    }

    public void attachToMemory(Object wasmMemory) {
        InteropLibrary lib = InteropLibrary.getUncached();
        try {
            lib.execute(memSetNotifyCallbackFunction, wasmMemory, this);
        } catch (InteropException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments) {
        assert arguments.length == 3;
        final Object embedderData = JSWebAssembly.getEmbedderData(realm, arguments[0]);
        assert embedderData instanceof JSWebAssemblyMemoryObject;
        final JSWebAssemblyMemoryObject memoryObject = (JSWebAssemblyMemoryObject) embedderData;
        final long address = (long) arguments[1];
        final int count = (int) arguments[2];

        // Let buffer be memory(Get(memory, "buffer")).
        final JSArrayBufferObject buffer = memoryObject.getBufferObject(context, realm);

        // Let int32array be Int32Array(buffer).
        final JSTypedArrayObject int32array = constructInt32Array(buffer);

        // Let result be Atomics.notify(int32array, address, count).
        return atomicsNotify(int32array, (int) address, count);
    }

    private JSTypedArrayObject constructInt32Array(JSArrayBufferObject buffer) {
        final ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(buffer);
        final TypedArrayFactory factory = findTypedArrayFactory(Strings.INT32_ARRAY);
        final int length = byteBuffer.limit() / factory.getBytesPerElement();
        final TypedArray typedArray = factory.createArrayType(true, false);
        final JSObjectFactory objectFactory = context.getArrayBufferViewFactory(factory);
        return JSArrayBufferView.createArrayBufferView(objectFactory, realm, buffer, typedArray, 0, length);
    }

    private static TypedArrayFactory findTypedArrayFactory(TruffleString name) {
        for (TypedArrayFactory typedArrayFactory : TypedArray.factories()) {
            if (Strings.equals(typedArrayFactory.getName(), name)) {
                return typedArrayFactory;
            }
        }
        throw new NoSuchElementException(Strings.toJavaString(name));
    }

    private int atomicsNotify(JSTypedArrayObject int32array, int address, int count) {
        final int convertedCount = Integer.max(count, 0);
        final JSAgentWaiterList.JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(context, int32array, address);
        wl.enterCriticalSection();
        try {
            boolean wake = false;
            final JSAgentWaiterList.WaiterRecord[] waiters = SharedMemorySync.removeWaiters(wl, convertedCount);
            int n;
            for (n = 0; n < waiters.length; n++) {
                final JSAgentWaiterList.WaiterRecord waiterRecord = waiters[n];
                waiterRecord.setNotified();
                if (waiterRecord.getPromiseCapability() == null) {
                    wake = true;
                } else {
                    if (Double.isInfinite(waiterRecord.getTimeout())) {
                        waiterRecord.enqueueInAgent();
                    }
                }
            }
            if (wake) {
                SharedMemorySync.wakeWaiters(wl);
            }
            return n;
        } finally {
            wl.leaveCriticalSection();
        }
    }
}

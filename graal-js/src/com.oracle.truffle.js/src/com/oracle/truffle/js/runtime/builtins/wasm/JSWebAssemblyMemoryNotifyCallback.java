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
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Represents a callback that is invoked when the memory.atomic.notify instruction executes in WebAssembly.
 * This allows us to reuse the JS implementation of Atomics.notify.
 */
@ExportLibrary(InteropLibrary.class)
public final class JSWebAssemblyMemoryNotifyCallback implements TruffleObject {
    private final JSRealm realm;
    private final JSContext context;
    private final TruffleContext truffleContext;
    private final Object memSetNotifyCallbackFunction;
    private final RootCallTarget constructInt32Array;
    private final RootCallTarget atomicsNotify;

    public JSWebAssemblyMemoryNotifyCallback(JSRealm realm, JSContext context, TruffleContext truffleContext, Object memSetNotifyCallbackFunction,
                    RootCallTarget constructInt32Array, RootCallTarget atomicsNotify) {
        this.realm = realm;
        this.context = context;
        this.truffleContext = truffleContext;
        this.memSetNotifyCallbackFunction = memSetNotifyCallbackFunction;
        this.constructInt32Array = constructInt32Array;
        this.atomicsNotify = atomicsNotify;
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
        final JSDynamicObject buffer = memoryObject.getBufferObject(context, realm);

        final Object prev = truffleContext.enter(null);
        int result;
        try {
            // Let int32array be Int32Array(buffer).
            JSDynamicObject newTarget = realm.getArrayBufferViewConstructor(TypedArrayFactory.Int32Array);
            JSTypedArrayObject int32array = (JSTypedArrayObject) constructInt32Array.call(newTarget, buffer, Undefined.instance, Undefined.instance);
            // Let result be Atomics.notify(int32array, address, count).
            result = (int) atomicsNotify.call(int32array, address, count);
        } finally {
            truffleContext.leave(null, prev);
        }

        return result;
    }
}

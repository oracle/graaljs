/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public final class JSWebAssemblyMemoryObject extends JSNonProxyObject {
    private final Object wasmMemory;
    private JSArrayBufferObject bufferObject;
    private final boolean shared;

    protected JSWebAssemblyMemoryObject(Shape shape, JSDynamicObject proto, Object wasmMemory, boolean shared) {
        super(shape, proto);
        this.wasmMemory = wasmMemory;
        this.shared = shared;
    }

    public Object getWASMMemory() {
        return wasmMemory;
    }

    public boolean isShared() {
        return shared;
    }

    @TruffleBoundary
    private JSArrayBufferObject createBufferObject(JSContext context, JSRealm realm) {
        InteropLibrary lib = InteropLibrary.getUncached();
        try {
            if (lib.getBufferSize(wasmMemory) > Integer.MAX_VALUE) {
                throw Errors.createRangeErrorInvalidBufferSize();
            }
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(wasmMemory, e, "WebAssembly.Memory underlying buffer object is not an interop buffer", null);
        }
        if (!shared) {
            return JSArrayBuffer.createInteropArrayBuffer(context, realm, wasmMemory);
        } else {
            synchronized (wasmMemory) {
                ByteBuffer buffer = JSInteropUtil.foreignInteropBufferAsByteBuffer(wasmMemory, lib, realm);
                if (buffer == null) {
                    throw Errors.createTypeError("No ByteBuffer exposed from WebAssembly memory");
                }
                JSArrayBufferObject bufferObj = JSSharedArrayBuffer.createSharedArrayBuffer(context, realm, buffer);
                boolean status = bufferObj.setIntegrityLevel(true, false);
                if (!status) {
                    throw Errors.createTypeError("Failed to set integrity level of buffer object");
                }
                return bufferObj;
            }
        }
    }

    public JSArrayBufferObject getBufferObject(JSContext context, JSRealm realm) {
        if (bufferObject == null) {
            bufferObject = createBufferObject(context, realm);
        }
        return bufferObject;
    }

    public void resetBufferObject() {
        if (bufferObject != null && !shared) {
            JSArrayBuffer.detachArrayBuffer(bufferObject);
        }
        bufferObject = null;
    }

    @Override
    public TruffleString getClassName() {
        return JSWebAssemblyMemory.WEB_ASSEMBLY_MEMORY;
    }
}

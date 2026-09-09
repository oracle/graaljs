/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.HiddenKey;
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
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSWebAssemblyMemoryObject extends JSNonProxyObject {
    private static final int MEMORY_PAGE_SIZE = 65536;
    public static final HiddenKey MEMORY_OBJECT_ID = new HiddenKey("WebAssembly.Memory");

    private final Object wasmMemory;
    private JSArrayBufferObject bufferObject;
    private final boolean shared;
    private final long maximum;

    protected JSWebAssemblyMemoryObject(Shape shape, JSDynamicObject proto, Object wasmMemory, boolean shared, long maximum) {
        super(shape, proto);
        this.wasmMemory = wasmMemory;
        this.shared = shared;
        this.maximum = maximum;
    }

    public Object getWASMMemory() {
        return wasmMemory;
    }

    public boolean isShared() {
        return shared;
    }

    public boolean hasMaximum() {
        return maximum != JSWebAssemblyMemory.NO_MAXIMUM;
    }

    @TruffleBoundary
    private JSArrayBufferObject createBufferObject(JSContext context, JSRealm realm, long maxByteLength) {
        assert Thread.holdsLock(wasmMemory);
        InteropLibrary lib = InteropLibrary.getUncached();
        final int byteLength;
        try {
            long bufferSize = lib.getBufferSize(wasmMemory);
            if (bufferSize > Integer.MAX_VALUE) {
                throw Errors.createRangeErrorInvalidBufferSize();
            }
            byteLength = (int) bufferSize;
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(wasmMemory, e, "WebAssembly.Memory underlying buffer object is not an interop buffer", null);
        }
        JSArrayBufferObject bufferObj;
        if (!shared) {
            bufferObj = JSArrayBuffer.createInteropArrayBuffer(context, realm, wasmMemory, maxByteLength);
            bufferObj.setDetachKey(JSWebAssemblyMemory.WEB_ASSEMBLY_MEMORY);
            setMemoryObject(bufferObj, this);
        } else {
            ByteBuffer buffer = JSInteropUtil.foreignInteropBufferAsByteBuffer(wasmMemory, lib, lib, realm);
            if (buffer == null) {
                throw Errors.createTypeError("No ByteBuffer exposed from WebAssembly memory");
            }
            AtomicInteger byteLengthObject = maxByteLength == JSArrayBuffer.FIXED_LENGTH ? new AtomicInteger(byteLength)
                            : JSWebAssemblyMemory.getSharedGrowableByteLength(realm, wasmMemory, byteLength);
            bufferObj = JSSharedArrayBuffer.createSharedArrayBuffer(context, realm, buffer, JSWebAssemblyMemory.getSharedWaiterList(realm, wasmMemory), byteLengthObject, maxByteLength);
            setMemoryObject(bufferObj, this);
            freezeBufferObject(bufferObj);
        }
        return bufferObj;
    }

    private static void setMemoryObject(JSArrayBufferObject buffer, JSWebAssemblyMemoryObject memory) {
        JSObjectUtil.putHiddenProperty(buffer, MEMORY_OBJECT_ID, memory);
    }

    private static void freezeBufferObject(JSArrayBufferObject buffer) {
        boolean status = buffer.setIntegrityLevel(true, false);
        if (!status) {
            throw Errors.createTypeError("Failed to set integrity level of buffer object");
        }
    }

    private JSArrayBufferObject getBufferObjectImpl(JSContext context, JSRealm realm) {
        assert Thread.holdsLock(wasmMemory);
        if (bufferObject == null) {
            bufferObject = createBufferObject(context, realm, JSArrayBuffer.FIXED_LENGTH);
        }
        return bufferObject;
    }

    public JSArrayBufferObject getBufferObject(JSContext context, JSRealm realm) {
        synchronized (wasmMemory) {
            return getBufferObjectImpl(context, realm);
        }
    }

    public JSArrayBufferObject createResizableBufferObject(JSContext context, JSRealm realm) {
        assert shared;
        synchronized (wasmMemory) {
            return createBufferObject(context, realm, getMaximumByteLength());
        }
    }

    public void setBufferObject(JSArrayBufferObject buffer) {
        assert shared;
        synchronized (wasmMemory) {
            setMemoryObject(buffer, this);
            freezeBufferObject(buffer);
            bufferObject = buffer;
        }
    }

    public JSArrayBufferObject toFixedLengthBuffer(JSContext context, JSRealm realm) {
        synchronized (wasmMemory) {
            JSArrayBufferObject buffer = getBufferObjectImpl(context, realm);
            if (buffer.isFixedLength()) {
                return buffer;
            }
            JSArrayBufferObject fixedBuffer = createBufferObject(context, realm, JSArrayBuffer.FIXED_LENGTH);
            if (!shared) {
                JSArrayBuffer.detachArrayBuffer(buffer, JSWebAssemblyMemory.WEB_ASSEMBLY_MEMORY);
            }
            bufferObject = fixedBuffer;
            return fixedBuffer;
        }
    }

    public JSArrayBufferObject toResizableBuffer(JSContext context, JSRealm realm) {
        long maxByteLength = getMaximumByteLength();
        synchronized (wasmMemory) {
            JSArrayBufferObject buffer = getBufferObjectImpl(context, realm);
            if (!buffer.isFixedLength()) {
                return buffer;
            }
            JSArrayBufferObject resizableBuffer = createBufferObject(context, realm, maxByteLength);
            if (!shared) {
                JSArrayBuffer.detachArrayBuffer(buffer, JSWebAssemblyMemory.WEB_ASSEMBLY_MEMORY);
            }
            bufferObject = resizableBuffer;
            return resizableBuffer;
        }
    }

    private long getMaximumByteLength() {
        if (!hasMaximum()) {
            throw Errors.createTypeError("WebAssembly.Memory.toResizableBuffer(): Memory has no maximum");
        }
        try {
            return Math.multiplyExact(maximum, MEMORY_PAGE_SIZE);
        } catch (ArithmeticException ex) {
            throw Errors.createRangeErrorInvalidBufferSize();
        }
    }

    @TruffleBoundary
    public void resizeBuffer(JSRealm realm, JSArrayBufferObject buffer, long newByteLength) {
        assert shared || buffer == bufferObject;
        if (newByteLength > Integer.MAX_VALUE) {
            throw Errors.createRangeErrorInvalidBufferSize();
        }
        synchronized (wasmMemory) {
            boolean refreshSharedAlias = shared && buffer != bufferObject;
            final long currentByteLength;
            try {
                currentByteLength = InteropLibrary.getUncached().getBufferSize(wasmMemory);
            } catch (UnsupportedMessageException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            long lengthDelta = newByteLength - currentByteLength;
            if (lengthDelta < 0 || lengthDelta % MEMORY_PAGE_SIZE != 0) {
                throw Errors.createRangeError("WebAssembly.Memory buffer can only grow by whole pages");
            }
            int delta = Math.toIntExact(lengthDelta / MEMORY_PAGE_SIZE);
            try {
                Object growFn = realm.getWASMMemGrow();
                InteropLibrary.getUncached().execute(growFn, wasmMemory, delta);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                throw Errors.createRangeError(ex, null);
            }
            if (refreshSharedAlias) {
                refreshSharedBufferObject(realm, (JSArrayBufferObject.Shared) buffer);
            }
        }
    }

    private void refreshSharedBufferObject(JSRealm realm, JSArrayBufferObject.Shared sharedBuffer) {
        assert Thread.holdsLock(wasmMemory);
        InteropLibrary lib = InteropLibrary.getUncached();
        ByteBuffer buffer = JSInteropUtil.foreignInteropBufferAsByteBuffer(wasmMemory, lib, lib, realm);
        if (buffer == null) {
            throw Errors.createTypeError("No ByteBuffer exposed from WebAssembly memory");
        }
        sharedBuffer.setByteBuffer(buffer);
    }

    @TruffleBoundary
    public void refreshBufferObject(JSRealm realm) {
        synchronized (wasmMemory) {
            if (bufferObject == null) {
                return;
            }
            if (bufferObject.isFixedLength()) {
                if (!shared) {
                    JSArrayBuffer.detachArrayBuffer(bufferObject, JSWebAssemblyMemory.WEB_ASSEMBLY_MEMORY);
                }
                bufferObject = null;
            } else if (shared) {
                refreshSharedBufferObject(realm, (JSArrayBufferObject.Shared) bufferObject);
            }
        }
    }

    @Override
    public TruffleString getClassName() {
        return JSWebAssemblyMemory.WEB_ASSEMBLY_MEMORY;
    }
}

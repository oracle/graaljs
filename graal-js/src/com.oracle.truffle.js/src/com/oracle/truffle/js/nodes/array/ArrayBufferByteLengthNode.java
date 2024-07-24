/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.array;

import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;

@ImportStatic({JSArray.class, JSConfig.class})
@GenerateInline(true)
@GenerateCached(false)
public abstract class ArrayBufferByteLengthNode extends JavaScriptBaseNode {

    public abstract int execute(Node node, JSArrayBufferObject arrayBufferObj, JSContext context);

    @Specialization
    protected static int heapArrayBuffer(JSArrayBufferObject.Heap thisObj, JSContext context) {
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && thisObj.getByteArray() == null) {
            return 0;
        }
        return thisObj.getByteLength();
    }

    @Specialization
    protected static int directArrayBuffer(JSArrayBufferObject.Direct thisObj, JSContext context) {
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && thisObj.getByteBuffer() == null) {
            return 0;
        }
        return thisObj.getByteLength();
    }

    @Specialization
    protected static int sharedArrayBuffer(JSArrayBufferObject.Shared thisObj, @SuppressWarnings("unused") JSContext context) {
        return thisObj.getByteLength();
    }

    @Specialization
    protected static int interopArrayBuffer(JSArrayBufferObject.Interop thisObj, JSContext context,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        Object buffer = thisObj.getInteropBuffer();
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && buffer == null) {
            return 0;
        }
        try {
            long bufferSize = interop.getBufferSize(buffer);
            // Buffer size was already checked in the ArrayBuffer constructor.
            assert JSRuntime.longIsRepresentableAsInt(bufferSize) : bufferSize;
            return (int) bufferSize;
        } catch (UnsupportedMessageException e) {
            return 0;
        }
    }
}

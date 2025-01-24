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
package com.oracle.truffle.js.nodes.wasm;

import org.graalvm.polyglot.io.ByteSequence;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.array.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.nodes.array.GetViewByteLengthNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDataViewObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;

/**
 * Exports byte source such that it can be read by WASM.
 */
public abstract class ExportByteSourceNode extends JavaScriptBaseNode {
    private final JSContext context;
    private final String nonByteSourceMessage;
    private final String emptyByteSourceMessage;
    @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

    protected ExportByteSourceNode(JSContext context, String nonByteSourceMessage, String emptyByteSourceMessage) {
        this.context = context;
        this.nonByteSourceMessage = nonByteSourceMessage;
        this.emptyByteSourceMessage = emptyByteSourceMessage;
    }

    public abstract ByteSequence execute(Object byteSource);

    public static ExportByteSourceNode create(JSContext context, String nonByteSourceMessage, String emptyByteSourceMessage) {
        return ExportByteSourceNodeGen.create(context, nonByteSourceMessage, emptyByteSourceMessage);
    }

    @Specialization
    protected ByteSequence exportBuffer(JSArrayBufferObject arrayBuffer,
                    @Cached @Shared InlinedBranchProfile errorBranch) {
        int length;
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
            length = 0;
        } else {
            length = arrayBuffer.getByteLength();
        }
        return exportBuffer(arrayBuffer, 0, length, errorBranch);
    }

    @Specialization
    protected ByteSequence exportTypedArray(JSTypedArrayObject typedArray,
                    @Cached ArrayBufferViewGetByteLengthNode getByteLengthNode,
                    @Cached @Shared InlinedBranchProfile errorBranch) {
        int offset = JSArrayBufferView.getByteOffset(typedArray, context);
        int length = getByteLengthNode.executeInt(this, typedArray, context);
        return exportBuffer(typedArray.getArrayBuffer(), offset, length, errorBranch);
    }

    @Specialization
    protected ByteSequence exportDataView(JSDataViewObject dataView,
                    @Cached GetViewByteLengthNode getByteLengthNode,
                    @Cached @Shared InlinedBranchProfile errorBranch) {
        int offset = JSDataView.dataViewGetByteOffset(dataView);
        int length = getByteLengthNode.execute(dataView, context);
        return exportBuffer(dataView.getArrayBuffer(), offset, length, errorBranch);
    }

    @Fallback
    protected ByteSequence exportOther(@SuppressWarnings("unused") Object other) {
        throw Errors.createTypeError(nonByteSourceMessage, this);
    }

    private ByteSequence exportBuffer(JSArrayBufferObject arrayBuffer, int offset, int length,
                    InlinedBranchProfile errorBranch) {
        JSArrayBufferObject buffer = arrayBuffer;
        if (emptyByteSourceMessage != null && length == 0) {
            errorBranch.enter(this);
            throw Errors.createCompileError(emptyByteSourceMessage, this);
        }
        JSRealm realm = getRealm();
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
            buffer = JSArrayBuffer.createArrayBuffer(context, realm, 0);
        }
        byte[] bytes = new byte[length];
        JSInteropUtil.copyFromBuffer(buffer, offset, bytes, 0, length, interop);
        reportLoopCount(this, length);
        return ByteSequence.create(bytes);
    }

}

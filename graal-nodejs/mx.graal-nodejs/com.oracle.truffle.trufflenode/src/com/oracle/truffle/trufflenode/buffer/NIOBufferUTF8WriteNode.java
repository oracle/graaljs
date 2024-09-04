/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.buffer;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.InternalByteArray;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.array.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.trufflenode.GraalJSAccess;

@ImportStatic(JSConfig.class)
public abstract class NIOBufferUTF8WriteNode extends NIOBufferAccessNode {

    protected final BranchProfile errorBranch = BranchProfile.create();

    public NIOBufferUTF8WriteNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private JSFunctionObject getNativeUtf8Write() {
        return GraalJSAccess.getRealmEmbedderData(getRealm()).getNativeUtf8Write();
    }

    @Specialization(guards = "isJSDirectOrSharedArrayBuffer(target.getArrayBuffer())")
    final Object writeDirect(JSTypedArrayObject target, TruffleString str, Object destOffset, Object bytes,
                    @Cached @Shared JSToIntegerAsIntNode toIntNode,
                    @Cached @Shared ArrayBufferViewGetByteLengthNode getLengthNode,
                    @Cached @Shared TruffleString.SwitchEncodingNode switchEncodingNode,
                    @Cached @Shared TruffleString.GetInternalByteArrayNode getInternalByteArrayNode,
                    @Cached @Shared TruffleString.ReadByteNode readByteNode) {
        JSArrayBufferObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        ByteBuffer rawBuffer = getDirectByteBuffer(arrayBuffer);

        return write(target, str, destOffset, bytes, rawBuffer,
                        toIntNode, getLengthNode, switchEncodingNode, getInternalByteArrayNode, readByteNode, null);
    }

    @Specialization(guards = "isJSHeapArrayBuffer(target.getArrayBuffer())")
    final Object writeHeap(JSTypedArrayObject target, TruffleString str, Object destOffset, Object bytes,
                    @Cached @Shared JSToIntegerAsIntNode toIntNode,
                    @Cached @Shared ArrayBufferViewGetByteLengthNode getLengthNode,
                    @Cached @Shared TruffleString.SwitchEncodingNode switchEncodingNode,
                    @Cached @Shared TruffleString.GetInternalByteArrayNode getInternalByteArrayNode,
                    @Cached @Shared TruffleString.ReadByteNode readByteNode) {
        ByteBuffer rawBuffer = Boundaries.byteBufferWrap(JSArrayBufferObject.getByteArray(target.getArrayBuffer()));
        return write(target, str, destOffset, bytes, rawBuffer,
                        toIntNode, getLengthNode, switchEncodingNode, getInternalByteArrayNode, readByteNode, null);
    }

    @Specialization(guards = "isJSInteropArrayBuffer(target.getArrayBuffer())")
    final Object writeInterop(JSTypedArrayObject target, TruffleString str, Object destOffset, Object bytes,
                    @Cached @Shared JSToIntegerAsIntNode toIntNode,
                    @Cached @Shared ArrayBufferViewGetByteLengthNode getLengthNode,
                    @Cached @Shared TruffleString.SwitchEncodingNode switchEncodingNode,
                    @Cached @Shared TruffleString.GetInternalByteArrayNode getInternalByteArrayNode,
                    @Cached @Shared TruffleString.ReadByteNode readByteNode,
                    @CachedLibrary(limit = "1") InteropLibrary asByteBufferInterop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bufferInterop) {
        ByteBuffer rawBuffer;
        ByteBuffer maybeRawBuffer = JSInteropUtil.foreignInteropBufferAsByteBuffer(target.getArrayBuffer(), asByteBufferInterop, getRealm());
        if (maybeRawBuffer != null && !maybeRawBuffer.isReadOnly()) {
            // We have direct write access to the backing ByteBuffer.
            rawBuffer = maybeRawBuffer;
        } else {
            // Write to the foreign buffer object using InteropLibrary.
            rawBuffer = null;
        }

        return write(target, str, destOffset, bytes, rawBuffer,
                        toIntNode, getLengthNode, switchEncodingNode, getInternalByteArrayNode, readByteNode, bufferInterop);
    }

    private int write(JSTypedArrayObject target, TruffleString str, Object destOffset0, Object bytes, ByteBuffer rawBuffer,
                    JSToIntegerAsIntNode toIntNode,
                    ArrayBufferViewGetByteLengthNode getLengthNode,
                    TruffleString.SwitchEncodingNode switchEncodingNode,
                    TruffleString.GetInternalByteArrayNode getInternalByteArrayNode,
                    TruffleString.ReadByteNode readByteNode,
                    InteropLibrary interop) {
        int bufferOffset = getOffset(target);
        int bufferLen = getLengthNode.executeInt(this, target, getContext());

        int destOffset;
        if (destOffset0 == Undefined.instance) {
            destOffset = 0;
        } else {
            destOffset = toIntNode.executeInt(destOffset0);
            if (destOffset < 0) {
                errorBranch.enter();
                throw indexOutOfRange();
            } else if (destOffset > bufferLen) {
                errorBranch.enter();
                throw offsetOutOfBounds();
            }
        }
        int maxLength;
        if (bytes == Undefined.instance) {
            maxLength = bufferLen - destOffset;
        } else {
            maxLength = toIntNode.executeInt(bytes);
            if (maxLength < 0) {
                errorBranch.enter();
                throw indexOutOfRange();
            }
            maxLength = Math.min(bufferLen - destOffset, maxLength);
        }
        if (maxLength == 0) {
            return 0;
        }

        TruffleString utf8Str = switchEncodingNode.execute(str, TruffleString.Encoding.UTF_8);
        int utf8Length = utf8Str.byteLength(TruffleString.Encoding.UTF_8);
        int copyLength = Math.min(utf8Length, maxLength);
        if (utf8Length > maxLength) {
            // Avoid writing an incomplete UTF-8 sequence.
            while (copyLength > 0 && isUTF8ContinuationByte(readByteNode.execute(utf8Str, copyLength, TruffleString.Encoding.UTF_8))) {
                copyLength--;
            }
        }

        InternalByteArray byteArray = getInternalByteArrayNode.execute(utf8Str, TruffleString.Encoding.UTF_8);
        assert copyLength <= byteArray.getLength();

        if (interop == null || rawBuffer != null) {
            // Bulk-copy the string contents directly to the backing ByteBuffer
            Boundaries.byteBufferPutArray(rawBuffer, bufferOffset + destOffset, byteArray.getArray(), byteArray.getOffset(), copyLength);
        } else {
            // Write the data byte by byte to the foreign buffer using InteropLibrary
            JSInteropUtil.writeBuffer(target.getArrayBuffer(), (long) bufferOffset + destOffset, byteArray.getArray(), byteArray.getOffset(), copyLength, interop);
        }
        LoopNode.reportLoopCount(this, copyLength);
        return copyLength;
    }

    private static boolean isUTF8ContinuationByte(int b) {
        return (b & 0xc0) == 0x80;
    }

    @Specialization(guards = "!isString(str)")
    final Object writeFallback(JSTypedArrayObject target, Object str, Object destOffset, Object bytes) {
        return JSFunction.call(getNativeUtf8Write(), target, new Object[]{str, destOffset, bytes});
    }

    @Specialization(guards = {"!isJSArrayBufferView(target)"})
    @SuppressWarnings("unused")
    static Object writeNotBuffer(Object target, Object str, Object destOffset, Object bytes) {
        throw notBuffer();
    }
}

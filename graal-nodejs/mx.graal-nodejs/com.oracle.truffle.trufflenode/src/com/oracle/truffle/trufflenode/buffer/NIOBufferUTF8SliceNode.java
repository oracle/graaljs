/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.node.ArrayBufferGetContentsNode;

public abstract class NIOBufferUTF8SliceNode extends NIOBufferAccessNode {
    protected final BranchProfile nativePath = BranchProfile.create();
    protected final BranchProfile errorBranch = BranchProfile.create();

    public NIOBufferUTF8SliceNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private JSFunctionObject getNativeUtf8Slice() {
        return GraalJSAccess.getRealmEmbedderData(getRealm()).getNativeUtf8Slice();
    }

    @Specialization(guards = "isJSDirectOrSharedArrayBuffer(target.getArrayBuffer())")
    final Object sliceDirect(JSTypedArrayObject target, Object start, Object end,
                    @Cached @Shared("toInt") JSToIntegerAsIntNode toIntNode,
                    @Cached @Shared("getLength") ArrayBufferViewGetByteLengthNode getLengthNode,
                    @Cached @Shared("fromByteArray") TruffleString.FromByteArrayNode fromByteArrayNode,
                    @Cached @Shared("switchEncoding") TruffleString.SwitchEncodingNode switchEncodingNode,
                    @Cached @Shared("isValid") TruffleString.IsValidNode isValidNode) {
        JSArrayBufferObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        ByteBuffer rawBuffer = getDirectByteBuffer(arrayBuffer);

        return slice(target, start, end, rawBuffer,
                        toIntNode, getLengthNode, fromByteArrayNode, switchEncodingNode, isValidNode);
    }

    @Specialization(guards = "!isJSDirectOrSharedArrayBuffer(target.getArrayBuffer())")
    final Object sliceInterop(JSTypedArrayObject target, Object start, Object end,
                    @Cached @Shared("toInt") JSToIntegerAsIntNode toIntNode,
                    @Cached @Shared("getLength") ArrayBufferViewGetByteLengthNode getLengthNode,
                    @Cached @Shared("fromByteArray") TruffleString.FromByteArrayNode fromByteArrayNode,
                    @Cached @Shared("switchEncoding") TruffleString.SwitchEncodingNode switchEncodingNode,
                    @Cached @Shared("isValid") TruffleString.IsValidNode isValidNode,
                    @Cached(inline = true) ArrayBufferGetContentsNode arrayBufferGetContentsNode) {
        JSArrayBufferObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        ByteBuffer rawBuffer = arrayBufferGetContentsNode.execute(this, arrayBuffer);

        return slice(target, start, end, rawBuffer,
                        toIntNode, getLengthNode, fromByteArrayNode, switchEncodingNode, isValidNode);
    }

    private Object slice(JSTypedArrayObject target, Object start, Object end, ByteBuffer rawBuffer,
                    JSToIntegerAsIntNode toIntNode,
                    ArrayBufferViewGetByteLengthNode getLengthNode,
                    TruffleString.FromByteArrayNode fromByteArrayNode,
                    TruffleString.SwitchEncodingNode switchEncodingNode,
                    TruffleString.IsValidNode isValidNode) {
        int bufferLength = getLengthNode.executeInt(this, target, getContext());
        if (bufferLength == 0) {
            // By default, an empty buffer returns an empty string
            return Strings.EMPTY_STRING;
        }
        int byteOffset = getOffset(target);

        int actualStart;
        if (start == Undefined.instance) {
            actualStart = 0;
        } else {
            actualStart = toIntNode.executeInt(start);
            if (actualStart < 0) {
                errorBranch.enter();
                throw indexOutOfRange();
            }
        }
        int actualEnd;
        if (end == Undefined.instance) {
            actualEnd = bufferLength;
        } else {
            actualEnd = toIntNode.executeInt(end);
            if (actualEnd < 0) {
                errorBranch.enter();
                throw indexOutOfRange();
            }
        }

        if (actualEnd < actualStart) {
            actualEnd = actualStart;
        }

        if (actualEnd > bufferLength) {
            errorBranch.enter();
            throw indexOutOfRange();
        }

        int length = actualEnd - actualStart;
        if (length > getContext().getStringLengthLimit()) {
            errorBranch.enter();
            throw stringTooLong();
        }

        byte[] data = copySliceToByteArray(rawBuffer, byteOffset, actualStart, actualEnd, length);
        LoopNode.reportLoopCount(this, length);

        TruffleString utf8String = fromByteArrayNode.execute(data, TruffleString.Encoding.UTF_8, false);
        if (isValidNode.execute(utf8String, TruffleString.Encoding.UTF_8)) {
            TruffleString utf16String = switchEncodingNode.execute(utf8String, TruffleString.Encoding.UTF_16);
            if (Strings.length(utf16String) > getContext().getStringLengthLimit()) {
                errorBranch.enter();
                throw stringTooLong();
            }
            return utf16String;
        }

        // TruffleString handles incomplete UTF-8 sequences wrongly, hence the fallback to native.
        // Note: Not passing the original start, end arguments to avoid repeating any side effects.
        return doNativeFallback(target, actualStart, actualEnd);
    }

    @TruffleBoundary
    private static byte[] copySliceToByteArray(ByteBuffer sourceBuffer, int byteOffset, int start, int end, int length) {
        assert start >= 0 && end >= 0 && length <= sourceBuffer.capacity() - byteOffset;
        ByteBuffer data = sourceBuffer.duplicate().position(byteOffset + start).limit(byteOffset + end).slice();
        assert data.remaining() == length;
        return ByteBuffer.allocate(length).put(data).array();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isJSArrayBufferView(target)"})
    static Object sliceNotBuffer(Object target, Object start, Object end) {
        throw notBuffer();
    }

    private Object doNativeFallback(JSTypedArrayObject target, Object start, Object end) {
        nativePath.enter();
        return JSFunction.call(getNativeUtf8Slice(), target, new Object[]{start, end});
    }
}

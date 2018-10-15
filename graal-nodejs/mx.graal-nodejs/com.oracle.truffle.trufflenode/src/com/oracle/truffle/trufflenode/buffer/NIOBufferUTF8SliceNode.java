/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.trufflenode.GraalJSAccess;

public abstract class NIOBufferUTF8SliceNode extends NIOBufferAccessNode {

    private static final int V8MaxStringLength = (1 << 30) - 1 - 24;

    protected final BranchProfile nativePath = BranchProfile.create();

    public NIOBufferUTF8SliceNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private DynamicObject getNativeUtf8Slice() {
        return GraalJSAccess.getRealmEmbedderData(getContext().getRealm()).getNativeUtf8Slice();
    }

    @Specialization(guards = {"accept(target)"})
    public Object slice(DynamicObject target, int start, int end) {
        try {
            return doSlice(target, start, end);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, start, end);
        }
    }

    @Specialization(guards = {"accept(target)"})
    public Object slice(DynamicObject target, double start, double end) {
        try {
            return doSlice(target, (int) start, (int) end);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, start, end);
        }
    }

    @Specialization
    public Object sliceDefault(DynamicObject target, Object start, Object end) {
        return JSFunction.call(getNativeUtf8Slice(), target, new Object[]{start, end});
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isJSArrayBufferView(target)"})
    public Object sliceAbort(Object target, Object start, Object end) {
        throw Errors.createTypeErrorArrayBufferViewExpected();
    }

    private Object doNativeFallback(DynamicObject target, Object start, Object end) {
        nativePath.enter();
        return JSFunction.call(getNativeUtf8Slice(), target, new Object[]{start, end});
    }

    private Object doSlice(DynamicObject target, int start, int end) throws CharacterCodingException {
        DynamicObject arrayBuffer = getArrayBuffer(target);
        ByteBuffer rawBuffer = getDirectByteBuffer(arrayBuffer);
        int byteOffset = getOffset(target);
        int actualEnd = end;
        if (end < start) {
            actualEnd = start;
        }
        if (rawBuffer.capacity() == 0) {
            // By default, an empty buffer returns an empty string
            return "";
        }
        if (actualEnd > rawBuffer.capacity() || !oobCheck(start, end)) {
            outOfBoundsFail();
        }
        int length = actualEnd - start;
        if (length > V8MaxStringLength) {
            return doNativeFallback(target, start, end);
        }
        int bufferLen = getLength(target);
        if (length > bufferLen) {
            outOfBoundsFail();
        }
        ByteBuffer data = sliceBuffer(rawBuffer, byteOffset);
        data.position(start);
        data.limit(end);
        return doDecode(data);
    }

    @TruffleBoundary
    private static Object doDecode(ByteBuffer data) throws CharacterCodingException {
        CharsetDecoder decoder = utf8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer decoded = decoder.decode(data);
        return decoded.toString();
    }

    private static boolean oobCheck(int start, int end) {
        return start <= end && start >= 0;
    }

}

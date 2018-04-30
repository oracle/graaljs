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
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.cast.JSToIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.trufflenode.GraalJSAccess;

public abstract class NIOBufferUTF8WriteNode extends NIOBufferAccessNode {

    @Child protected JSToIntegerNode toInt;

    protected final BranchProfile nativePath = BranchProfile.create();

    public NIOBufferUTF8WriteNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
        this.toInt = JSToIntegerNodeGen.create();
    }

    private DynamicObject getNativeUtf8Write() {
        return GraalJSAccess.getRealmEmbedderData(getContext().getRealm()).getNativeUtf8Write();
    }

    @Specialization(guards = "accept(target)")
    public Object write(DynamicObject target, String str, int destOffset, int bytes) {
        try {
            return doWrite(target, str, destOffset, bytes);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization(guards = {"accept(target)", "isUndefined(bytes)"})
    public Object writeDefaultOffset(DynamicObject target, String str, int destOffset, Object bytes) {
        try {
            return doWrite(target, str, destOffset, getBytes(str).length);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization(guards = {"accept(target)", "isUndefined(destOffset)", "isUndefined(bytes)"})
    public Object writeDefaultValues(DynamicObject target, String str, Object destOffset, Object bytes) {
        try {
            return doWrite(target, str, 0, getBytes(str).length);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization(guards = "accept(target)")
    public Object write(DynamicObject target, String str, double destOffset, double bytes) {
        try {
            return doWrite(target, str, toInt.executeInt(destOffset), toInt.executeInt(bytes));
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization
    public Object writeDefault(DynamicObject target, Object str, Object destOffset, Object bytes) {
        return JSFunction.call(getNativeUtf8Write(), target, new Object[]{str, destOffset, bytes});
    }

    @Specialization(guards = {"!isJSArrayBufferView(target)"})
    @SuppressWarnings("unused")
    public Object writeAbort(Object target, Object str, Object destOffset, Object bytes) {
        throw Errors.createTypeErrorArrayBufferViewExpected();
    }

    private Object doNativeFallback(DynamicObject target, String str, Object destOffset, Object bytes) {
        nativePath.enter();
        return JSFunction.call(getNativeUtf8Write(), target, new Object[]{str, destOffset, bytes});
    }

    private int doWrite(DynamicObject target, String str, int destOffset, int bytes) throws CharacterCodingException {
        DynamicObject arrayBuffer = getArrayBuffer(target);
        int bufferOffset = getOffset(target);
        int bufferLen = getLength(target);

        if (destOffset > bufferLen || bytes < 0 || destOffset < 0) {
            outOfBoundsFail();
        }
        ByteBuffer rawBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        ByteBuffer buffer = sliceBuffer(rawBuffer, bufferOffset);
        buffer.position(destOffset);
        buffer.limit(Math.min(bufferLen, destOffset + bytes));

        CoderResult res = doEncode(str, buffer);
        if (cannotEncode(res)) {
            throw new CharacterCodingException();
        }
        return buffer.position() - destOffset;
    }

    @TruffleBoundary
    private static CoderResult doEncode(String str, ByteBuffer buffer) {
        CharsetEncoder encoder = utf8.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer cb = CharBuffer.wrap(str);
        CoderResult res = encoder.encode(cb, buffer, true);

        if (res.isUnderflow()) {
            encoder.encode(cb, buffer, true);
            res = encoder.flush(buffer);
        }
        return res;
    }

    @TruffleBoundary
    private static byte[] getBytes(String str) {
        return str.getBytes(utf8);
    }

    private static boolean cannotEncode(CoderResult res) {
        return res.isMalformed() || res.isUnmappable() || res.isError();
    }

}

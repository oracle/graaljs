/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.trufflenode.GraalJSAccess;

public abstract class NIOBufferUTF8SliceNode extends NIOBufferAccessNode {

    private static final int V8MaxStringLength = (1 << 28) - 16;

    protected final DynamicObject nativeUtf8Slice;
    protected final BranchProfile nativePath = BranchProfile.create();

    public NIOBufferUTF8SliceNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
        this.nativeUtf8Slice = Objects.requireNonNull(GraalJSAccess.getContextData(context).getNativeUtf8Slice());
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
        return JSFunction.call(nativeUtf8Slice, target, new Object[]{start, end});
    }

    @SuppressWarnings("unused")
    @Specialization
    public Object sliceAbort(Object target, Object start, Object end) {
        throw Errors.createTypeError("Typed array expected");
    }

    private Object doNativeFallback(DynamicObject target, Object start, Object end) {
        nativePath.enter();
        return JSFunction.call(nativeUtf8Slice, target, new Object[]{start, end});
    }

    private Object doSlice(DynamicObject target, int start, int end) throws CharacterCodingException {
        DynamicObject arrayBuffer = getArrayBuffer(target);
        ByteBuffer rawBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
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
            throw Errors.createError("\"toString()\" failed");
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

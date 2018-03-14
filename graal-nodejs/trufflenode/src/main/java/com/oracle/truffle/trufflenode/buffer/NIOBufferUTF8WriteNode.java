/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

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

    protected final DynamicObject nativeUtf8Write;
    protected final BranchProfile nativePath = BranchProfile.create();

    public NIOBufferUTF8WriteNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
        this.nativeUtf8Write = Objects.requireNonNull(GraalJSAccess.getContextData(context).getNativeUtf8Write());
        this.toInt = JSToIntegerNodeGen.create();
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
        return JSFunction.call(nativeUtf8Write, target, new Object[]{str, destOffset, bytes});
    }

    @Specialization(guards = {"!isJSArrayBufferView(target)"})
    @SuppressWarnings("unused")
    public Object writeAbort(Object target, Object str, Object destOffset, Object bytes) {
        throw Errors.createTypeError("Typed array expected");
    }

    private Object doNativeFallback(DynamicObject target, String str, Object destOffset, Object bytes) {
        nativePath.enter();
        return JSFunction.call(nativeUtf8Write, target, new Object[]{str, destOffset, bytes});
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

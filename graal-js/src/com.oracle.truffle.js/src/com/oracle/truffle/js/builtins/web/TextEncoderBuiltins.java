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
package com.oracle.truffle.js.builtins.web;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.InternalByteArray;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.ConstructorBuiltins.ConstructWithNewTargetNode;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.array.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * TextEncoder built-in functions.
 */
public class TextEncoderBuiltins {

    public enum TextEncoderPrototype implements BuiltinEnum<TextEncoderPrototype> {

        TextEncoder(0) {
            @Override
            public boolean isConstructor() {
                return true;
            }

            @Override
            public boolean isNewTargetConstructor() {
                return true;
            }
        },
        encode(0),
        encodeInto(2),
        encoding(0);

        private final int length;

        TextEncoderPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return this == encoding;
        }

        @Override
        public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            return switch (this) {
                case TextEncoder -> TextEncoderBuiltinsFactory.ConstructorNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).createArgumentNodes(context));
                case encode -> TextEncoderBuiltinsFactory.EncodeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case encodeInto -> TextEncoderBuiltinsFactory.EncodeIntoNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                case encoding -> TextEncoderBuiltinsFactory.GetEncodingNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            };
        }
    }

    public static final TruffleString UTF_8 = Strings.constant("utf-8");
    public static final TruffleString TEXT_ENCODER_PROTOTYPE = Strings.constant("TextEncoder.prototype");
    public static final JSBuiltinsContainer BUILTINS = JSBuiltinsContainer.fromEnum(TEXT_ENCODER_PROTOTYPE, TextEncoderPrototype.class);

    public abstract static class ConstructorNode extends ConstructWithNewTargetNode {

        protected ConstructorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final JSObject construct(JSDynamicObject newTarget) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTextEncoder.create(getJSContext(), realm, proto);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return getRealm().getTextEncoderPrototype();
        }
    }

    public abstract static class GetEncodingNode extends JSBuiltinNode {

        protected GetEncodingNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static TruffleString encoding(JSTextEncoderObject thisObj) {
            return UTF_8;
        }

        @Fallback
        protected final JSObject incompatibleReceiver(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    public abstract static class EncodeNode extends JSBuiltinNode {

        @Child private ArraySpeciesConstructorNode constructTypedArrayNode;
        @Child private TruffleString.SwitchEncodingNode switchEncodingNode;

        protected EncodeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.constructTypedArrayNode = ArraySpeciesConstructorNode.create(context, true);
            this.switchEncodingNode = TruffleString.SwitchEncodingNode.create();
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final JSTypedArrayObject doString(JSTextEncoderObject thisObj, TruffleString string) {
            TruffleString utf8Str = switchEncodingNode.execute(string, TruffleString.Encoding.UTF_8);
            int utf8ByteLength = utf8Str.byteLength(TruffleString.Encoding.UTF_8);
            JSTypedArrayObject destination = constructTypedArrayNode.typedArrayCreate(getRealm().getArrayBufferViewConstructor(TypedArrayFactory.Uint8Array), utf8ByteLength);
            JSArrayBufferObject arrayBuffer = destination.getArrayBuffer();

            ByteBuffer rawBuffer;
            if (arrayBuffer instanceof JSArrayBufferObject.Heap heapBuffer) {
                rawBuffer = Boundaries.byteBufferWrap(heapBuffer.getByteArray());
            } else {
                rawBuffer = ((JSArrayBufferObject.Direct) arrayBuffer).getByteBuffer();
            }
            InternalByteArray byteArray = utf8Str.getInternalByteArrayUncached(TruffleString.Encoding.UTF_8);
            assert byteArray.getLength() == utf8ByteLength;
            Boundaries.byteBufferPutArray(rawBuffer, 0, byteArray.getArray(), byteArray.getOffset(), utf8ByteLength);
            reportLoopCount(this, utf8ByteLength);
            return destination;
        }

        @Specialization
        protected final JSTypedArrayObject doOther(JSTextEncoderObject thisObj, Object string,
                        @Cached("createUndefinedToEmpty()") JSToStringNode toStringNode) {
            return doString(thisObj, toStringNode.executeString(string));
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final Object incompatibleReceiver(Object thisObj, Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic({TypedArrayFactory.class, JSConfig.class})
    public abstract static class EncodeIntoNode extends JSBuiltinNode {

        @Child private CreateDataPropertyNode createReadDataPropertyNode;
        @Child private CreateDataPropertyNode createWrittenDataPropertyNode;

        protected EncodeIntoNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createReadDataPropertyNode = CreateDataPropertyNode.create(context, Strings.READ);
            this.createWrittenDataPropertyNode = CreateDataPropertyNode.create(context, Strings.WRITTEN);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"destination.getArrayType().getFactory() == Uint8Array"})
        protected final JSObject encodeInto(JSTextEncoderObject thisObj, TruffleString string, JSTypedArrayObject destination,
                        @Cached @Shared UTF8EncodeIntoNode utf8EncodeInto) {
            long result;
            if (JSArrayBufferView.isOutOfBounds(destination, getContext())) {
                result = 0;
            } else {
                result = utf8EncodeInto.execute(string, destination);
            }
            int readLength = (int) (result >>> 32);
            int writtenLength = (int) result;
            return createEncodeIntoResult(readLength, writtenLength);
        }

        private JSObject createEncodeIntoResult(int readLength, int writtenLength) {
            JSRealm realm = getRealm();
            JSObject result = JSOrdinary.create(getContext(), realm);
            createReadDataPropertyNode.executeVoid(result, readLength);
            createWrittenDataPropertyNode.executeVoid(result, writtenLength);
            return result;
        }

        @Specialization
        protected final JSObject doValidate(JSTextEncoderObject thisObj, Object source, Object destination,
                        @Cached JSToStringNode toStringNode,
                        @Cached @Shared UTF8EncodeIntoNode utf8EncodeInto) {
            TruffleString string = toStringNode.executeString(source);
            if (destination instanceof JSTypedArrayObject typedArray && typedArray.getArrayType().getFactory() == TypedArrayFactory.Uint8Array) {
                return encodeInto(thisObj, string, typedArray, utf8EncodeInto);
            } else {
                throw Errors.createTypeError("Not a Uint8Array");
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final Object incompatibleReceiver(Object thisObj, Object string, Object destination) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic({JSArrayBuffer.class, JSConfig.class})
    public abstract static class UTF8EncodeIntoNode extends JavaScriptBaseNode {

        @Child private TruffleString.SwitchEncodingNode switchEncodingNode;
        @Child private TruffleString.ReadByteNode readByteNode;
        @Child private TruffleString.GetCodeRangeImpreciseNode getCodeRangeImpreciseNode;
        @Child private TruffleString.GetInternalByteArrayNode getInternalByteArrayNode;

        protected UTF8EncodeIntoNode() {
            this.switchEncodingNode = TruffleString.SwitchEncodingNode.create();
            this.readByteNode = TruffleString.ReadByteNode.create();
            this.getCodeRangeImpreciseNode = TruffleString.GetCodeRangeImpreciseNode.create();
            this.getInternalByteArrayNode = TruffleString.GetInternalByteArrayNode.create();
        }

        public final long execute(TruffleString string, JSTypedArrayObject destination) {
            return execute(string, destination, 0, Integer.MAX_VALUE);
        }

        /**
         * UTF-8-encode a string into a typed array.
         *
         * @param string source string
         * @param destination destination typed array
         * @param destOffset destination byte offset
         * @param maxLength max output byte length
         * @return the read and written length in the upper and lower half, respectively
         */
        public abstract long execute(TruffleString string, JSTypedArrayObject destination, int destOffset, int maxLength);

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSHeapArrayBuffer(destination.getArrayBuffer())"})
        protected final long doHeapBuffer(TruffleString string, JSTypedArrayObject destination, int destOffset, int maxLength,
                        @Cached @Shared ArrayBufferViewGetByteLengthNode getTypedArrayByteLengthNode) {
            ByteBuffer rawBuffer = Boundaries.byteBufferWrap(JSArrayBufferObject.getByteArray(destination.getArrayBuffer()));
            return encodeInto(string, destination, destOffset, maxLength, rawBuffer, getTypedArrayByteLengthNode, null);
        }

        @Specialization(guards = {"isJSDirectOrSharedArrayBuffer(destination.getArrayBuffer())"})
        protected final long doDirectBuffer(TruffleString string, JSTypedArrayObject destination, int destOffset, int maxLength,
                        @Cached @Shared ArrayBufferViewGetByteLengthNode getTypedArrayByteLengthNode) {
            ByteBuffer rawBuffer = JSArrayBuffer.getDirectByteBuffer(destination.getArrayBuffer());
            return encodeInto(string, destination, destOffset, maxLength, rawBuffer, getTypedArrayByteLengthNode, null);
        }

        @Specialization(guards = {"isJSInteropArrayBuffer(destination.getArrayBuffer())"})
        protected final long doInteropBuffer(TruffleString string, JSTypedArrayObject destination, int destOffset, int maxLength,
                        @Cached @Shared ArrayBufferViewGetByteLengthNode getTypedArrayByteLengthNode,
                        @CachedLibrary(limit = "1") InteropLibrary asByteBufferInterop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bufferInterop) {
            ByteBuffer rawBuffer = JSInteropUtil.foreignInteropBufferAsByteBuffer(destination.getArrayBuffer(), asByteBufferInterop, getRealm());
            if (rawBuffer != null && rawBuffer.isReadOnly()) {
                // If we don't have direct write access to the backing ByteBuffer,
                // write to the foreign buffer object using InteropLibrary.
                rawBuffer = null;
            }
            return encodeInto(string, destination, destOffset, maxLength, rawBuffer, getTypedArrayByteLengthNode, bufferInterop);
        }

        protected final long encodeInto(TruffleString string, JSTypedArrayObject destination, int viewOffset, int maxLength, ByteBuffer rawBuffer,
                        ArrayBufferViewGetByteLengthNode getTypedArrayByteLengthNode, InteropLibrary interop) {
            int bufferByteOffset = destination.getByteOffset();
            int bufferByteLength = getTypedArrayByteLengthNode.executeInt(this, destination, getJSContext());
            int destinationOffset = bufferByteOffset + viewOffset;
            int destinationByteLength = Math.min(bufferByteLength - viewOffset, maxLength);
            if (destinationByteLength <= 0) {
                // Empty or detached buffer.
                return 0L;
            }

            TruffleString utf8Str = switchEncodingNode.execute(string, TruffleString.Encoding.UTF_8);
            int utf8ByteLength = utf8Str.byteLength(TruffleString.Encoding.UTF_8);
            JSArrayBufferObject arrayBuffer = destination.getArrayBuffer();

            int readLength;
            int copyLength;
            InternalByteArray byteArray = getInternalByteArrayNode.execute(utf8Str, TruffleString.Encoding.UTF_8);
            if (utf8ByteLength <= destinationByteLength) {
                copyLength = byteArray.getLength();
                readLength = Strings.length(string);
            } else {
                copyLength = destinationByteLength;
                // Avoid writing an incomplete UTF-8 sequence.
                while (copyLength > 0 && isUTF8ContinuationByte(readByteNode.execute(utf8Str, copyLength, TruffleString.Encoding.UTF_8))) {
                    copyLength--;
                }
                TruffleString.CodeRange codeRange = getCodeRangeImpreciseNode.execute(utf8Str, TruffleString.Encoding.UTF_8);
                if (codeRange.isSubsetOf(TruffleString.CodeRange.ASCII)) {
                    readLength = copyLength;
                } else {
                    // Calculate equivalent UTF-16 length from written UTF-8 substring bytes.
                    readLength = calculateUtf16Length(byteArray.getArray(), byteArray.getOffset(), copyLength);
                }
                assert copyLength <= byteArray.getLength();
            }

            if (interop == null || rawBuffer != null) {
                // Bulk-copy the string contents directly to the backing ByteBuffer
                Boundaries.byteBufferPutArray(rawBuffer, destinationOffset, byteArray.getArray(), byteArray.getOffset(), copyLength);
            } else {
                // Write the data byte by byte to the foreign buffer using InteropLibrary
                JSInteropUtil.writeBuffer(arrayBuffer, destinationOffset, byteArray.getArray(), byteArray.getOffset(), copyLength, interop);
            }
            reportLoopCount(this, copyLength);
            return ((long) readLength << 32) | Integer.toUnsignedLong(copyLength);
        }

        private static boolean isUTF8ContinuationByte(int b) {
            return (b & 0xc0) == 0x80;
        }

        /**
         * Calculate the read UTF-16 length (in characters) from the written UTF-8 bytes, which we
         * can assume contain only complete, valid UTF-8 sequences (invalid UTF-16 code units will
         * have been replaced with U+FFFD, a 3-byte UTF-8 sequence that counts as one UTF-16 unit).
         * So we can use a simplified algorithm that does not have to deal with any special cases.
         */
        private static int calculateUtf16Length(byte[] utf8Bytes, int offset, int length) {
            int utf16Length = 0;
            int utf8Index = offset;
            while (utf8Index < length) {
                int b = Byte.toUnsignedInt(utf8Bytes[utf8Index]);
                int utf8Width;
                int utf16Width;
                if (b < 0x80) {
                    // 1-byte UTF-8 sequence (0xxxxxxx)
                    utf8Width = 1;
                    utf16Width = 1;
                } else if (b < 0xE0) {
                    // 2-byte UTF-8 sequence (110xxxxx 10xxxxxx)
                    utf8Width = 2;
                    utf16Width = 1;
                } else if (b < 0xF0) {
                    // 3-byte UTF-8 sequence (1110xxxx 10xxxxxx 10xxxxxx)
                    utf8Width = 3;
                    utf16Width = 1;
                } else {
                    // 4-byte UTF-8 sequence (11110xxx 10xxxxxx 10xxxxxx 10xxxxxx)
                    // Corresponds to a surrogate pair in UTF-16
                    utf8Width = 4;
                    utf16Width = 2;
                }
                utf8Index += utf8Width;
                utf16Length += utf16Width;
            }
            assert utf8Index == length;
            return utf16Length;
        }
    }
}

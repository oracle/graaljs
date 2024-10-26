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

import java.nio.ByteOrder;
import java.util.Locale;

import com.oracle.truffle.api.ArrayUtils;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ConstructorBuiltins.ConstructWithNewTargetNode;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.array.ArrayBufferByteLengthNode;
import com.oracle.truffle.js.nodes.array.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.nodes.array.GetViewByteLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSTrimWhitespaceNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.GetBooleanOptionNode;
import com.oracle.truffle.js.nodes.unary.JSIsNullOrUndefinedNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.ByteArrayAccess;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSDataViewObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * TextDecoder built-in functions.
 */
public class TextDecoderBuiltins {

    public static final char UNICODE_REPLACEMENT_CHARACTER = 0xFFFD;
    public static final char BYTE_ORDER_MARK = 0xFEFF;
    public static final byte[] UTF_8_BOM_BYTES = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}; // U+FEFF
    public static final TruffleString UTF_8 = Strings.constant("utf-8");
    public static final TruffleString UTF_16LE = Strings.constant("utf-16le");
    public static final TruffleString UTF_16BE = Strings.constant("utf-16be");
    public static final TruffleString STREAM = Strings.constant("stream");
    public static final TruffleString FATAL = Strings.constant("fatal");
    public static final TruffleString IGNORE_BOM = Strings.constant("ignoreBOM");
    public static final TruffleString TEXT_DECODER_PROTOTYPE = Strings.constant("TextDecoder.prototype");

    public static final JSBuiltinsContainer BUILTINS = JSBuiltinsContainer.fromEnum(TEXT_DECODER_PROTOTYPE, TextDecoderPrototype.class);

    public enum TextDecoderPrototype implements BuiltinEnum<TextDecoderPrototype> {
        TextDecoder(0) {
            @Override
            public boolean isConstructor() {
                return true;
            }

            @Override
            public boolean isNewTargetConstructor() {
                return true;
            }
        },
        decode(0),
        encoding(0),
        fatal(0),
        ignoreBOM(0);

        private final int length;

        TextDecoderPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return this == encoding || this == fatal || this == ignoreBOM;
        }

        @Override
        public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            return switch (this) {
                case TextDecoder -> TextDecoderBuiltinsFactory.ConstructorNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
                case decode -> TextDecoderBuiltinsFactory.DecodeNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                case encoding, fatal, ignoreBOM -> TextDecoderBuiltinsFactory.GetterNodeGen.create(context, builtin, this, args().withThis().createArgumentNodes(context));
            };
        }
    }

    public abstract static class ConstructorNode extends ConstructWithNewTargetNode {

        @Child private GetBooleanOptionNode getFatalOption;
        @Child private GetBooleanOptionNode getIgnoreBOMOption;

        protected ConstructorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.getFatalOption = GetBooleanOptionNode.create(context, FATAL, false);
            this.getIgnoreBOMOption = GetBooleanOptionNode.create(context, IGNORE_BOM, false);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUndefined(label)", "isUndefined(options)"})
        protected final JSObject constructNoArgs(JSDynamicObject newTarget, Object label, Object options) {
            return construct(newTarget, UTF_8, TruffleString.Encoding.UTF_8, false, false);
        }

        @SuppressWarnings("hiding")
        @Specialization(replaces = {"constructNoArgs"})
        protected final JSObject construct(JSDynamicObject newTarget, Object label, Object options,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached JSTrimWhitespaceNode trimWhitespaceNode,
                        @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached JSIsNullOrUndefinedNode isNullOrUndefinedNode,
                        @Cached IsObjectNode isObjectNode) {
            TruffleString labelStr = label == Undefined.instance ? UTF_8 : toStringNode.executeString(label);
            TruffleString encodingName;
            TruffleString.Encoding truffleStringEncoding;
            if (Strings.equals(equalNode, labelStr, UTF_8)) {
                encodingName = UTF_8;
                truffleStringEncoding = TruffleString.Encoding.UTF_8;
            } else {
                // lower-case and canonicalize encoding label
                labelStr = trimWhitespaceNode.executeString(labelStr);
                String lowerCase = Strings.javaStringToLowerCase(toJavaStringNode.execute(labelStr), Locale.ROOT);
                switch (lowerCase) {
                    case "utf-8", "utf8", "unicode-1-1-utf-8", "unicode11utf8", "unicode20utf8", "x-unicode20utf8" -> {
                        encodingName = UTF_8;
                        truffleStringEncoding = TruffleString.Encoding.UTF_8;
                    }
                    case "utf-16le", "utf-16", "ucs-2", "iso-10646-ucs-2", "csunicode", "unicode", "unicodefeff" -> {
                        encodingName = UTF_16LE;
                        truffleStringEncoding = TruffleString.Encoding.UTF_16LE;
                    }
                    case "utf-16be", "unicodefffe" -> {
                        encodingName = UTF_16BE;
                        truffleStringEncoding = TruffleString.Encoding.UTF_16BE;
                    }
                    default -> {
                        throw Errors.createRangeErrorEncodingNotSupported(labelStr);
                    }
                }
            }
            boolean fatal;
            boolean ignoreBOM;
            if (isNullOrUndefinedNode.executeBoolean(options)) {
                fatal = false;
                ignoreBOM = false;
            } else if (isObjectNode.executeBoolean(options)) {
                fatal = getFatalOption.executeValue(options);
                ignoreBOM = getIgnoreBOMOption.executeValue(options);
            } else {
                throw Errors.createTypeErrorNotAnObject(options);
            }
            return construct(newTarget, encodingName, truffleStringEncoding, fatal, ignoreBOM);
        }

        @SuppressWarnings("hiding")
        private JSTextDecoderObject construct(JSDynamicObject newTarget, TruffleString encodingName, TruffleString.Encoding truffleStringEncoding, boolean fatal, boolean ignoreBOM) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTextDecoder.create(getJSContext(), realm, proto, encodingName, truffleStringEncoding, fatal, ignoreBOM);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return getRealm().getTextDecoderPrototype();
        }
    }

    public abstract static class GetterNode extends JSBuiltinNode {

        private final TextDecoderPrototype method;

        protected GetterNode(JSContext context, JSBuiltin builtin, TextDecoderPrototype method) {
            super(context, builtin);
            this.method = method;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object get(JSTextDecoderObject thisObj) {
            return switch (method) {
                case encoding -> thisObj.getEncoding();
                case fatal -> thisObj.isFatal();
                case ignoreBOM -> thisObj.isIgnoreBOM();
                default -> Errors.shouldNotReachHereUnexpectedValue(method);
            };
        }

        @Fallback
        protected final Object incompatibleReceiver(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    public abstract static class DecodeNode extends JSBuiltinNode {

        @Child private JSIsNullOrUndefinedNode isNullOrUndefinedNode;
        @Child private IsObjectNode isObjectNode;
        @Child private GetBooleanOptionNode getStreamOption;

        protected DecodeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isNullOrUndefinedNode = JSIsNullOrUndefinedNode.create();
            this.isObjectNode = IsObjectNode.create();
            this.getStreamOption = GetBooleanOptionNode.create(context, STREAM, false);
        }

        @Specialization
        protected final TruffleString doTypedArray(JSTextDecoderObject thisObj, JSTypedArrayObject source, Object options,
                        @Cached @Shared DecodeBufferSlice decodeBufferSlice,
                        @Cached ArrayBufferViewGetByteLengthNode getTypedArrayByteLengthNode) {
            boolean stream = getTextDecodeOptions(options);
            int byteOffset = source.getByteOffset();
            int byteLength = getTypedArrayByteLengthNode.executeInt(this, source, getContext());
            return decodeBufferSlice.execute(thisObj, source.getArrayBuffer(), byteOffset, byteLength, stream);
        }

        @Specialization
        protected final TruffleString doDataView(JSTextDecoderObject thisObj, JSDataViewObject source, Object options,
                        @Cached @Shared DecodeBufferSlice decodeBufferSlice,
                        @Cached GetViewByteLengthNode getViewByteLengthNode) {
            boolean stream = getTextDecodeOptions(options);
            int byteOffset = source.getByteOffset();
            int byteLength = getViewByteLengthNode.execute(source, getContext());
            return decodeBufferSlice.execute(thisObj, source.getArrayBuffer(), byteOffset, byteLength, stream);
        }

        @Specialization
        protected final TruffleString doArrayBuffer(JSTextDecoderObject thisObj, JSArrayBufferObject buffer, Object options,
                        @Cached @Shared DecodeBufferSlice decodeBufferSlice,
                        @Cached ArrayBufferByteLengthNode arrayBufferByteLengthNode) {
            boolean stream = getTextDecodeOptions(options);
            int byteLength = arrayBufferByteLengthNode.execute(this, buffer, getContext());
            return decodeBufferSlice.execute(thisObj, buffer, 0, byteLength, stream);
        }

        @Specialization(guards = "isUndefined(source)")
        protected final TruffleString doEmpty(JSTextDecoderObject thisObj, @SuppressWarnings("unused") Object source, Object options,
                        @Cached @Shared DecodeBufferSlice decodeBufferSlice) {
            boolean stream = getTextDecodeOptions(options);
            JSArrayBufferObject emptyBuffer = JSArrayBuffer.createArrayBuffer(getContext(), getRealm(), 0);
            return decodeBufferSlice.execute(thisObj, emptyBuffer, 0, 0, stream);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final TruffleString incompatibleReceiver(Object thisObj, Object source, Object options) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }

        @HostCompilerDirectives.InliningCutoff
        private boolean getTextDecodeOptions(Object options) {
            if (isNullOrUndefinedNode.executeBoolean(options)) {
                return false;
            } else if (isObjectNode.executeBoolean(options)) {
                boolean stream = getStreamOption.executeValue(options);
                return stream;
            } else {
                throw Errors.createTypeErrorNotAnObject(options);
            }
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class DecodeBufferSlice extends JavaScriptBaseNode {

        @Child private TruffleString.SwitchEncodingNode switchEncodingNode;
        @Child private TruffleString.FromByteArrayNode fromByteArrayNode;
        @Child private TruffleString.IsValidNode isValidNode;
        @Child private TruffleString.ToValidStringNode toValidNode;
        @Child private TruffleString.ByteLengthOfCodePointNode byteLengthOfCodePointNode;

        protected DecodeBufferSlice() {
            this.switchEncodingNode = TruffleString.SwitchEncodingNode.create();
            this.fromByteArrayNode = TruffleString.FromByteArrayNode.create();
            this.isValidNode = TruffleString.IsValidNode.create();
            this.toValidNode = TruffleString.ToValidStringNode.create();
            this.byteLengthOfCodePointNode = TruffleString.ByteLengthOfCodePointNode.create();
        }

        @HostCompilerDirectives.InliningCutoff
        public abstract TruffleString execute(JSTextDecoderObject thisObj, JSArrayBufferObject buffer, int byteOffset, int byteLength, boolean stream);

        @Specialization
        protected final TruffleString doArrayBufferHeap(JSTextDecoderObject thisObj, JSArrayBufferObject.Heap buffer, int byteOffset, int byteLength, boolean stream,
                        @Cached @Shared InlinedBranchProfile utf16Branch) {
            return decodeBufferSlice(thisObj, buffer, byteOffset, byteLength, stream, null, utf16Branch);
        }

        @Specialization
        protected final TruffleString doArrayBufferDirect(JSTextDecoderObject thisObj, JSArrayBufferObject.Direct buffer, int byteOffset, int byteLength, boolean stream,
                        @Cached @Shared InlinedBranchProfile utf16Branch) {
            return decodeBufferSlice(thisObj, buffer, byteOffset, byteLength, stream, null, utf16Branch);
        }

        @Specialization
        protected final TruffleString doArrayBufferShared(JSTextDecoderObject thisObj, JSArrayBufferObject.Shared buffer, int byteOffset, int byteLength, boolean stream,
                        @Cached @Shared InlinedBranchProfile utf16Branch) {
            return decodeBufferSlice(thisObj, buffer, byteOffset, byteLength, stream, null, utf16Branch);
        }

        @Specialization
        protected final TruffleString doArrayBufferInterop(JSTextDecoderObject thisObj, JSArrayBufferObject.Interop buffer, int byteOffset, int byteLength, boolean stream,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached @Shared InlinedBranchProfile utf16Branch) {
            return decodeBufferSlice(thisObj, buffer, byteOffset, byteLength, stream, interop, utf16Branch);
        }

        private TruffleString decodeBufferSlice(JSTextDecoderObject thisObj, JSArrayBufferObject buffer, int byteOffset, int byteLength, boolean stream,
                        InteropLibrary interop, InlinedBranchProfile utf16Branch) {
            boolean error = false;
            try {
                byte[] prefix = thisObj.getPendingBytes();
                int prefixLen = prefix == null ? 0 : prefix.length;
                int allocLength = byteLength + prefixLen;
                if (allocLength == 0) {
                    return Strings.EMPTY_STRING;
                }

                TruffleString.Encoding sourceEncoding = thisObj.getTruffleStringEncoding();
                // Handle UTF-16 byte length not being a multiple of 2: If not streaming,
                // we need to allocate one more byte to make space for replacement character.
                boolean appendReplacementCharacter = false;
                if (!stream && (sourceEncoding == TruffleString.Encoding.UTF_16LE || sourceEncoding == TruffleString.Encoding.UTF_16BE)) {
                    utf16Branch.enter(this);
                    if (allocLength % Character.BYTES != 0) {
                        if (thisObj.isFatal()) {
                            throw createTypeErrorInvalidData(thisObj.getEncoding());
                        }
                        allocLength += 1;
                        appendReplacementCharacter = true;
                    }
                }

                byte[] sourceBytes = new byte[allocLength];
                if (prefixLen != 0) {
                    System.arraycopy(prefix, 0, sourceBytes, 0, prefixLen);
                }
                // Note: If byteLength == 0, the buffer may be detached.
                if (byteLength != 0) {
                    copyFromBuffer(buffer, byteOffset, sourceBytes, prefixLen, byteLength, interop);
                }

                if (appendReplacementCharacter) {
                    putCharUTF16(sourceBytes, allocLength - Character.BYTES, UNICODE_REPLACEMENT_CHARACTER, sourceEncoding);
                }

                TruffleString result;
                if (sourceEncoding == TruffleString.Encoding.UTF_8) {
                    result = stringFromByteArrayUTF8(thisObj, sourceBytes, stream);
                } else {
                    utf16Branch.enter(this);
                    result = stringFromByteArrayUTF16(thisObj, sourceBytes, stream, sourceEncoding);
                }
                return result;
            } catch (JSException e) {
                error = true;
                throw e;
            } finally {
                thisObj.endDecode(stream, error);
            }
        }

        private void copyFromBuffer(JSArrayBufferObject buffer, int byteOffset, byte[] destination, int destinationOffset, int byteLength, InteropLibrary interop) {
            if (buffer instanceof JSArrayBufferObject.Heap heapBuffer) {
                System.arraycopy(heapBuffer.getByteArray(), byteOffset, destination, destinationOffset, byteLength);
            } else if (buffer instanceof JSArrayBufferObject.DirectBase directBuffer) {
                Boundaries.byteBufferGet(directBuffer.getByteBuffer(), byteOffset, destination, destinationOffset, byteLength);
            } else if (buffer instanceof JSArrayBufferObject.Interop interopBuffer) {
                JSInteropUtil.readBuffer(interopBuffer.getInteropBuffer(), byteOffset, destination, destinationOffset, byteLength, interop);
            } else {
                throw Errors.shouldNotReachHereUnexpectedValue(buffer);
            }
            reportLoopCount(this, byteLength);
        }

        private static boolean arrayStartsWith(byte[] bytes, byte[] prefix) {
            return ArrayUtils.regionEqualsWithOrMask(bytes, 0, prefix, 0, prefix.length, null);
        }

        private TruffleString stringFromByteArrayUTF8(JSTextDecoderObject thisObj, byte[] bytes, boolean stream) {
            final TruffleString.Encoding sourceEncoding = TruffleString.Encoding.UTF_8;
            int byteOffset = 0;
            int byteLength = bytes.length;
            if (!thisObj.isIgnoreBOM() && !thisObj.isBomSeen()) {
                // We only set BOM seen once we've seen a complete UTF-8 sequence (see below).
                if (arrayStartsWith(bytes, UTF_8_BOM_BYTES)) {
                    thisObj.setBomSeen();
                    byteOffset += UTF_8_BOM_BYTES.length;
                    byteLength -= UTF_8_BOM_BYTES.length;
                }
            }

            /*
             * If the decoded string is not valid, and we are streaming, it could be due to an
             * unfinished code point at the end (if not a fixed-width encoding). Try to trim the
             * unfinished byte sequence from the end.
             */
            TruffleString utf8Str = fromByteArrayNode.execute(bytes, byteOffset, byteLength, sourceEncoding, false);
            if (stream) {
                int newByteLength = byteLength;
                if (!isValidNode.execute(utf8Str, sourceEncoding)) {
                    /*
                     * In UTF-8 encoding, the maximum byte length of a code point is 4 bytes, so we
                     * check the last (up to) 4 bytes. ByteLengthOfCodePointNode returns -1 for
                     * invalid code points and (-1 - x) for unfinished code points missing x bytes.
                     * So we go backwards and search for a code point length less than -1 or greater
                     * than 1 (valid). Note that if an incomplete sequence is not at the end, it
                     * will be treated as invalid, too.
                     */
                    int numBytesToCheck = Math.min(byteLength, 4);
                    for (int i = byteLength - 1; i >= byteLength - numBytesToCheck; i--) {
                        int lastCodePointLength = byteLengthOfCodePointNode.execute(
                                        utf8Str, i, sourceEncoding, TruffleString.ErrorHandling.RETURN_NEGATIVE);
                        if (lastCodePointLength < -1) {
                            newByteLength = i;
                            break;
                        } else if (lastCodePointLength > 0) {
                            break;
                        }
                    }
                }
                if (newByteLength != 0) {
                    // Set BOM seen once we've seen a complete UTF-8 sequence.
                    thisObj.setBomSeen();
                }
                thisObj.setPendingBytes(bytes, newByteLength, byteLength);
                if (newByteLength != byteLength) {
                    // Re-create the string with bytes up until the last complete codepoint.
                    utf8Str = fromByteArrayNode.execute(bytes, byteOffset, newByteLength, sourceEncoding, false);
                }
            }
            return toValidUTF16String(thisObj, utf8Str, sourceEncoding);
        }

        private TruffleString toValidUTF16String(JSTextDecoderObject thisObj, TruffleString utf8Str, TruffleString.Encoding sourceEncoding) {
            if (thisObj.isFatal()) {
                if (!isValidNode.execute(utf8Str, sourceEncoding)) {
                    throw createTypeErrorInvalidData(thisObj.getEncoding());
                }
            }
            return switchEncodingNode.execute(utf8Str, TruffleString.Encoding.UTF_16);
        }

        private TruffleString stringFromByteArrayUTF16(JSTextDecoderObject thisObj, byte[] bytes, boolean stream, TruffleString.Encoding sourceEncoding) {
            assert sourceEncoding == TruffleString.Encoding.UTF_16LE || sourceEncoding == TruffleString.Encoding.UTF_16BE : sourceEncoding;
            int byteOffset = 0;
            int byteLength = bytes.length;

            if (!thisObj.isIgnoreBOM() && !thisObj.isBomSeen() && byteLength >= Character.BYTES) {
                // Only set BOM seen once we've seen at least 2 bytes.
                thisObj.setBomSeen();
                char firstChar = readCharUTF16(bytes, byteOffset, sourceEncoding);
                if (firstChar == BYTE_ORDER_MARK) {
                    byteOffset += Character.BYTES;
                    byteLength -= Character.BYTES;
                }
            }

            // Handle byte length not being a multiple of 2 by trimming any trailing single byte.
            int newByteLength = byteLength & -Character.BYTES;

            // If streaming, handle (trim) trailing incomplete surrogate pair.
            if (stream) {
                if (newByteLength >= Character.BYTES) {
                    char lastChar = readCharUTF16(bytes, byteOffset + (newByteLength - Character.BYTES), sourceEncoding);
                    if (Character.isHighSurrogate(lastChar)) {
                        newByteLength -= Character.BYTES;
                    }
                }
            }

            if (stream) {
                thisObj.setPendingBytes(bytes, newByteLength, byteLength);
            } else {
                assert newByteLength == byteLength;
            }

            // Handle non-native byte order by flipping the bytes in the array.
            toUTF16NativeByteOrder(bytes, byteOffset, newByteLength, sourceEncoding);

            TruffleString utf16Str = fromByteArrayNode.execute(bytes, byteOffset, newByteLength, TruffleString.Encoding.UTF_16, false);
            if (thisObj.isFatal()) {
                if (!isValidNode.execute(utf16Str, TruffleString.Encoding.UTF_16)) {
                    throw createTypeErrorInvalidData(thisObj.getEncoding());
                }
                return utf16Str;
            } else {
                return toValidNode.execute(utf16Str, TruffleString.Encoding.UTF_16);
            }
        }

        private static char readCharUTF16(byte[] bytes, int byteOffset, TruffleString.Encoding utf16Encoding) {
            return (char) (utf16Encoding == TruffleString.Encoding.UTF_16LE
                            ? ByteArrayAccess.littleEndian().getUint16(bytes, byteOffset)
                            : ByteArrayAccess.bigEndian().getUint16(bytes, byteOffset));
        }

        private static void putCharUTF16(byte[] utf8Bytes, int byteIndex, char value, TruffleString.Encoding utf16Encoding) {
            if (utf16Encoding == TruffleString.Encoding.UTF_16LE) {
                ByteArrayAccess.littleEndian().putInt16(utf8Bytes, byteIndex, value);
            } else {
                ByteArrayAccess.bigEndian().putInt16(utf8Bytes, byteIndex, value);
            }
        }

        private static void toUTF16NativeByteOrder(byte[] bytes, int byteOffset, int byteLength, TruffleString.Encoding sourceEncoding) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN && sourceEncoding == TruffleString.Encoding.UTF_16BE ||
                            ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN && sourceEncoding == TruffleString.Encoding.UTF_16LE) {
                assert byteLength % Character.BYTES == 0 : byteLength;
                for (int i = byteOffset; i < byteOffset + byteLength; i += Character.BYTES) {
                    ByteArrayAccess.littleEndian().putInt16(bytes, i, ByteArrayAccess.bigEndian().getInt16(bytes, i));
                }
            }
        }

        @TruffleBoundary
        private static JSException createTypeErrorInvalidData(TruffleString sourceEncoding) {
            return Errors.createTypeError("The encoded data was not valid for encoding " + sourceEncoding);
        }
    }
}

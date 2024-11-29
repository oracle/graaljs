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
package com.oracle.truffle.js.builtins;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.array.TypedArrayLengthNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.GetBooleanOptionNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains methods of the {@code Uint8Array} constructor and prototype.
 */
public final class Uint8ArrayBuiltins {

    public static final JSBuiltinsContainer PROTOTYPE_BUILTINS = JSBuiltinsContainer.fromEnum(JSArrayBufferView.UINT8ARRAY_PROTOTYPE_NAME, Uint8ArrayPrototype.class);
    public static final JSBuiltinsContainer CONSTRUCTOR_BUILTINS = JSBuiltinsContainer.fromEnum(JSArrayBufferView.UINT8ARRAY_CONSTRUCTOR_NAME, Uint8ArrayConstructor.class);

    private Uint8ArrayBuiltins() {
    }

    public enum Uint8ArrayPrototype implements BuiltinEnum<Uint8ArrayPrototype> {
        toBase64(0),
        toHex(0),
        setFromBase64(1),
        setFromHex(1);

        private final int functionLength;

        Uint8ArrayPrototype(int length) {
            this.functionLength = length;
        }

        @Override
        public int getLength() {
            return functionLength;
        }

        @Override
        public int getECMAScriptVersion() {
            return JSConfig.StagingECMAScriptVersion;
        }

        @Override
        public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            return switch (this) {
                case toBase64 -> Uint8ArrayBuiltinsFactory.ToBase64NodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case toHex -> Uint8ArrayBuiltinsFactory.ToHexNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case setFromBase64 -> Uint8ArrayBuiltinsFactory.SetFromBase64NodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                case setFromHex -> Uint8ArrayBuiltinsFactory.SetFromHexNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            };
        }
    }

    public enum Uint8ArrayConstructor implements BuiltinEnum<Uint8ArrayConstructor> {
        fromBase64(1),
        fromHex(1);

        private final int functionLength;

        Uint8ArrayConstructor(int length) {
            this.functionLength = length;
        }

        @Override
        public int getLength() {
            return functionLength;
        }

        @Override
        public int getECMAScriptVersion() {
            return JSConfig.StagingECMAScriptVersion;
        }

        @Override
        public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            return switch (this) {
                case fromBase64 -> Uint8ArrayBuiltinsFactory.Uint8ArrayFromBase64NodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
                case fromHex -> Uint8ArrayBuiltinsFactory.Uint8ArrayFromHexNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            };
        }
    }

    public abstract static class Uint8ArrayBaseNode extends JSBuiltinNode {

        protected static final TruffleString BASE64 = Strings.constant("base64");
        protected static final TruffleString BASE64URL = Strings.constant("base64url");
        @CompilationFinal(dimensions = 1) protected static final TruffleString[] ALPHABET_VALUES = new TruffleString[]{BASE64, BASE64URL};

        protected static final TruffleString LOOSE = Strings.constant("loose");
        protected static final TruffleString STRICT = Strings.constant("strict");
        protected static final TruffleString STOP_BEFORE_PARTIAL = Strings.constant("stop-before-partial");
        @CompilationFinal(dimensions = 1) protected static final TruffleString[] LAST_CHUNK_HANDLING_VALUES = new TruffleString[]{LOOSE, STRICT, STOP_BEFORE_PARTIAL};

        @Child private TypedArrayLengthNode typedArrayLengthNode = TypedArrayLengthNode.create();
        protected final BranchProfile errorBranch = BranchProfile.create();

        protected Uint8ArrayBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static boolean isUint8Array(JSTypedArrayObject typedArray) {
            return typedArray.getArrayType().getFactory() == TypedArrayFactory.Uint8Array;
        }

        protected final int getByteLengthOrThrow(JSTypedArrayObject view) {
            if (JSArrayBufferView.isOutOfBounds(view, getContext())) {
                errorBranch.enter();
                throw Errors.createTypeErrorOutOfBoundsTypedArray();
            }
            return typedArrayLengthNode.execute(null, view, getContext());
        }

        protected final byte[] getUint8ArrayBytes(JSTypedArrayObject ta, InteropLibrary interop) {
            int byteLength = getByteLengthOrThrow(ta);
            byte[] buffer = new byte[byteLength];
            JSInteropUtil.copyFromBuffer(ta.getArrayBuffer(), ta.getByteOffset(), buffer, 0, byteLength, interop);
            reportLoopCount(this, byteLength);
            return buffer;
        }

        protected void setUint8ArrayBytes(JSTypedArrayObject into, byte[] bytes, int resultLength) {
            JSArrayBufferObject arrayBuffer = into.getArrayBuffer();
            ByteBuffer rawBuffer;
            if (arrayBuffer instanceof JSArrayBufferObject.Heap heapBuffer) {
                rawBuffer = Boundaries.byteBufferWrap(heapBuffer.getByteArray());
            } else {
                rawBuffer = ((JSArrayBufferObject.DirectBase) arrayBuffer).getByteBuffer();
            }
            Boundaries.byteBufferPutArray(rawBuffer, into.getByteOffset(), bytes, 0, resultLength);
            reportLoopCount(this, resultLength);
        }

        @CompilerDirectives.ValueType
        protected record EncodeResult(int read, int written, byte[] bytes, JSException error) {
        }

        /** The Base64 Alphabet according to RFC 4648. */
        private static final String BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

        private static final int LOOKUP_TABLE_SIZE = 256;
        /**
         * Lookup table for decoding characters from the Base64 Alphabet (as specified in RFC 4648)
         * into their 6-bit positive integer equivalents. Characters that are not in the Base64
         * alphabet but fall within the bounds of the array are encoded as negative values.
         */
        private static final byte[] FROM_BASE64_TABLE;
        static {
            byte[] fromBase64Table = new byte[LOOKUP_TABLE_SIZE];
            Arrays.fill(fromBase64Table, (byte) -1);
            for (int i = 0; i < BASE64_ALPHABET.length(); i++) {
                fromBase64Table[BASE64_ALPHABET.charAt(i)] = (byte) i;
            }
            FROM_BASE64_TABLE = fromBase64Table;
        }

        private int decodeBase64Chunk(byte[] chunk, int chunkLength, byte[] bytes, int bytesBegin, boolean throwOnExtraBits) {
            int resultLength;
            switch (chunkLength) {
                case 2 -> {
                    chunk[2] = 'A';
                    chunk[3] = 'A';
                    resultLength = 1;
                }
                case 3 -> {
                    chunk[3] = 'A';
                    resultLength = 2;
                }
                default -> {
                    assert chunkLength == 4 : chunkLength;
                    resultLength = 3;
                }
            }

            byte sx0 = FROM_BASE64_TABLE[Byte.toUnsignedInt(chunk[0])];
            byte sx1 = FROM_BASE64_TABLE[Byte.toUnsignedInt(chunk[1])];
            byte sx2 = FROM_BASE64_TABLE[Byte.toUnsignedInt(chunk[2])];
            byte sx3 = FROM_BASE64_TABLE[Byte.toUnsignedInt(chunk[3])];
            // a negative value in any sextet (sx*) would indicate a non-base64 character
            assert !(sx0 < 0 || sx1 < 0 || sx2 < 0 || sx3 < 0) : "unexpected character";
            int triplet = ((sx0 << 18) | (sx1 << 12) | (sx2 << 6) | sx3);
            byte b0 = (byte) (triplet >>> 16);
            byte b1 = (byte) (triplet >>> 8);
            byte b2 = (byte) (triplet);

            if (throwOnExtraBits && ((chunkLength == 2 && b1 != 0) || (chunkLength == 3 && b2 != 0))) {
                throw syntaxError("extra bits");
            }

            bytes[bytesBegin] = b0;
            if (chunkLength > 2) {
                bytes[bytesBegin + 1] = b1;
            }
            if (chunkLength > 3) {
                bytes[bytesBegin + 2] = b2;
            }
            return resultLength;
        }

        protected final EncodeResult fromBase64(TruffleString string, boolean base64url, int maxLength, TruffleString lastChunkHandling,
                        TruffleString.ReadCharUTF16Node charAtNode) {
            assert lastChunkHandling == LOOSE || lastChunkHandling == STRICT || lastChunkHandling == STOP_BEFORE_PARTIAL;
            boolean throwOnExtraBits = lastChunkHandling == STRICT;
            int chunkLength = 0;
            int index = 0;
            int length = Strings.length(string);
            int read = 0;
            int written = 0;
            byte[] bytes = new byte[Math.min(maxLength, estimateDecodedByteLengthFromBase64(string, length, charAtNode))];

            JSException error = null;
            done: try {
                if (maxLength == 0) {
                    break done;
                }
                byte[] chunk = new byte[4];
                while (true) {
                    index = skipAsciiWhitespace(string, index, length, charAtNode);

                    if (index == length) {
                        if (chunkLength > 0) {
                            assert chunkLength < 4;
                            if (lastChunkHandling == STOP_BEFORE_PARTIAL) {
                                break;
                            }
                            if (chunkLength == 1) {
                                throw syntaxError("unexpected incomplete chunk: only one character");
                            }
                            if (lastChunkHandling == LOOSE) {
                                written += decodeBase64Chunk(chunk, chunkLength, bytes, written, false);
                            } else {
                                assert lastChunkHandling == STRICT : lastChunkHandling;
                                throw syntaxError("missing padding");
                            }
                        }
                        read = length;
                        break;
                    }

                    char ch = Strings.charAt(charAtNode, string, index);
                    index++;
                    if (ch == '=') {
                        if (chunkLength < 2) {
                            throw syntaxError("padding is too early");
                        }
                        index = skipAsciiWhitespace(string, index, length, charAtNode);
                        if (chunkLength == 2) {
                            if (index == length) {
                                if (lastChunkHandling == STOP_BEFORE_PARTIAL) {
                                    break;
                                } else {
                                    throw syntaxError("malformed padding: only one '='");
                                }
                            }
                            ch = Strings.charAt(charAtNode, string, index);
                            if (ch == '=') {
                                index = skipAsciiWhitespace(string, index + 1, length, charAtNode);
                            }
                        }
                        if (index < length) {
                            throw syntaxError("unexpected character after padding");
                        }
                        written += decodeBase64Chunk(chunk, chunkLength, bytes, written, throwOnExtraBits);
                        read = length;
                        break;
                    }
                    if (base64url) {
                        if (ch == '+' || ch == '/') {
                            throw syntaxError("unexpected character");
                        } else if (ch == '-') {
                            ch = '+';
                        } else if (ch == '_') {
                            ch = '/';
                        }
                    }
                    if (ch > FROM_BASE64_TABLE.length || FROM_BASE64_TABLE[ch] < 0) {
                        throw syntaxError("unexpected character");
                    }
                    int remaining = maxLength - written;
                    if ((remaining == 1 && chunkLength == 2) || (remaining == 2 && chunkLength == 3)) {
                        break;
                    }
                    chunk[chunkLength++] = (byte) ch;
                    if (chunkLength == 4) {
                        written += decodeBase64Chunk(chunk, chunkLength, bytes, written, false);
                        chunkLength = 0;
                        read = index;
                        if (written == maxLength) {
                            break;
                        }
                    }
                }
            } catch (JSException e) {
                error = e;
            }
            return new EncodeResult(read, written, bytes, error);
        }

        /**
         * Calculates the longest expectable decoded byte length from the given base64 input string.
         * The actual result length may be shorter in case of errors or skipped whitespace.
         */
        protected static int estimateDecodedByteLengthFromBase64(TruffleString src, int len, TruffleString.ReadCharUTF16Node charAtNode) {
            if (len < 2) {
                return 0;
            }
            int decodedLength = 3 * (len / 4);
            if (len % 4 == 0) {
                // account for 1 or 2 padding bytes at the end
                if (Strings.charAt(charAtNode, src, len - 1) == '=') {
                    decodedLength--;
                    if (Strings.charAt(charAtNode, src, len - 2) == '=') {
                        decodedLength--;
                    }
                }
            } else {
                // if last chunk length is 1, 2, or 3, add 0, 1, or 2 bytes, respectively.
                decodedLength += len % 4 - 1;
            }
            return decodedLength;
        }

        private static int skipAsciiWhitespace(TruffleString string, int start, int len, TruffleString.ReadCharUTF16Node charAtNode) {
            assert len == Strings.length(string);
            int idx = start;
            while (idx < len) {
                char ch = Strings.charAt(charAtNode, string, idx);
                if (!JSRuntime.isAsciiWhitespace(ch)) {
                    break;
                }
                idx++;
            }
            return idx;
        }

        private static final String HEX_ALPHABET = "0123456789abcdefABCDEF";

        private static final byte[] FROM_HEX_TABLE;
        static {
            byte[] fromHexTable = new byte[LOOKUP_TABLE_SIZE];
            Arrays.fill(fromHexTable, (byte) -1);
            for (int i = 0; i < HEX_ALPHABET.length(); i++) {
                fromHexTable[HEX_ALPHABET.charAt(i)] = (byte) JSRuntime.valueInHex(HEX_ALPHABET.charAt(i));
            }
            FROM_HEX_TABLE = fromHexTable;
        }

        protected final EncodeResult fromHex(TruffleString string, int maxLength,
                        TruffleString.ReadCharUTF16Node charAtNode) {
            int length = Strings.length(string);

            if (length % 2 != 0) {
                throw syntaxError("string length not modulo 2");
            }
            byte[] bytes = new byte[Math.min(maxLength, length / 2)];

            int read = 0;
            int written = 0;
            JSException error = null;
            done: try {
                if (maxLength == 0) {
                    break done;
                }

                while (read < length && written < maxLength) {
                    char c0 = Strings.charAt(charAtNode, string, read);
                    char c1 = Strings.charAt(charAtNode, string, read + 1);
                    byte b0;
                    byte b1;
                    if (c0 > FROM_HEX_TABLE.length || c1 > FROM_HEX_TABLE.length ||
                                    (b0 = FROM_HEX_TABLE[c0]) < 0 ||
                                    (b1 = FROM_HEX_TABLE[c1]) < 0) {
                        throw syntaxError("unexpected character");
                    }
                    read += 2;
                    bytes[written++] = (byte) ((b0 << 4) | b1);
                }
            } catch (JSException e) {
                error = e;
            }

            return new EncodeResult(read, written, bytes, error);
        }

        protected final JSException syntaxError(String message) {
            errorBranch.enter();
            throw Errors.createSyntaxError(message);
        }

        protected final TruffleString getStringOption(Object opts, TruffleString key, TruffleString[] allowedValues, TruffleString defaultValue,
                        PropertyGetNode getOptionValueNode, TruffleString.EqualNode equalNode) {
            CompilerAsserts.partialEvaluationConstant(allowedValues);
            Object value = getOptionValueNode.getValue(opts);
            if (value == Undefined.instance) {
                return defaultValue;
            } else if (value instanceof TruffleString string) {
                for (TruffleString choice : allowedValues) {
                    if (Strings.equals(equalNode, choice, string)) {
                        return choice;
                    }
                }
                errorBranch.enter();
                throw createTypeErrorUnsupportedOptionValue(key, allowedValues);
            }
            errorBranch.enter();
            throw Errors.createTypeErrorNotAString(value);
        }

        @TruffleBoundary
        private static JSException createTypeErrorUnsupportedOptionValue(TruffleString name, TruffleString[] allowedValues) {
            return Errors.createTypeError("Expected " + name + " option value to be one of " + Arrays.toString(allowedValues));
        }
    }

    public abstract static class SetFromBaseNode extends Uint8ArrayBaseNode {

        @Child private CreateDataPropertyNode createReadDataPropertyNode;
        @Child private CreateDataPropertyNode createWrittenDataPropertyNode;
        @Child private InteropLibrary interopLibrary;

        protected SetFromBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createReadDataPropertyNode = CreateDataPropertyNode.create(context, Strings.READ);
            this.createWrittenDataPropertyNode = CreateDataPropertyNode.create(context, Strings.WRITTEN);
        }

        protected final JSObject createResultObject(int readLength, int writtenLength) {
            JSRealm realm = getRealm();
            JSObject result = JSOrdinary.create(getContext(), realm);
            createReadDataPropertyNode.executeVoid(result, readLength);
            createWrittenDataPropertyNode.executeVoid(result, writtenLength);
            return result;
        }

        @Override
        protected void setUint8ArrayBytes(JSTypedArrayObject into, byte[] bytes, int resultLength) {
            if (into.getArrayBuffer() instanceof JSArrayBufferObject.Interop) {
                setUint8ArrayBytesInterop(into, bytes, resultLength);
            } else {
                super.setUint8ArrayBytes(into, bytes, resultLength);
            }
        }

        private void setUint8ArrayBytesInterop(JSTypedArrayObject into, byte[] bytes, int resultLength) {
            if (interopLibrary == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interopLibrary = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            JSInteropUtil.writeBuffer(into.getArrayBuffer(), into.getByteOffset(), bytes, 0, resultLength, interopLibrary);
            reportLoopCount(this, resultLength);
        }
    }

    @ImportStatic({Strings.class})
    public abstract static class SetFromBase64Node extends SetFromBaseNode {

        protected SetFromBase64Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isUint8Array(into)"})
        protected final JSObject doUint8Array(JSTypedArrayObject into, TruffleString string, Object options,
                        @Cached(parameters = {"getContext()"}) GetOptionsObjectNode getOptionsObjectNode,
                        @Cached(parameters = {"ALPHABET", "getContext()"}) PropertyGetNode getAlphabetNode,
                        @Cached(parameters = {"LAST_CHUNK_HANDLING", "getContext()"}) PropertyGetNode getLastChunkHandlingNode,
                        @Cached TruffleString.ReadCharUTF16Node charAtNode,
                        @Cached TruffleString.EqualNode equalNode) {
            Object opts = getOptionsObjectNode.execute(options);
            TruffleString alphabet = getStringOption(opts, Strings.ALPHABET, ALPHABET_VALUES, BASE64, getAlphabetNode, equalNode);
            boolean base64url = Strings.equals(equalNode, alphabet, BASE64URL);
            TruffleString lastChunkHandling = getStringOption(opts, Strings.LAST_CHUNK_HANDLING, LAST_CHUNK_HANDLING_VALUES, LOOSE, getLastChunkHandlingNode, equalNode);

            int byteLength = getByteLengthOrThrow(into);
            var result = fromBase64(string, base64url, byteLength, lastChunkHandling, charAtNode);

            assert !into.getArrayBuffer().isDetached();
            assert result.written() <= byteLength;
            setUint8ArrayBytes(into, result.bytes(), result.written());

            if (result.error() != null) {
                throw result.error();
            }
            return createResultObject(result.read(), result.written());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUint8Array(into)", "!isString(string)"})
        protected static Object doNotString(JSTypedArrayObject into, Object string, Object options) {
            throw Errors.createTypeErrorNotAString(string);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static Object doNotUint8Array(Object thisObj, Object string, Object options) {
            throw Errors.createTypeErrorUint8ArrayExpected();
        }
    }

    @ImportStatic({Strings.class})
    public abstract static class Uint8ArrayFromBase64Node extends Uint8ArrayBaseNode {

        protected Uint8ArrayFromBase64Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSObject doString(TruffleString string, Object options,
                        @Cached(parameters = {"getContext()"}) GetOptionsObjectNode getOptionsObjectNode,
                        @Cached(parameters = {"ALPHABET", "getContext()"}) PropertyGetNode getAlphabetNode,
                        @Cached(parameters = {"LAST_CHUNK_HANDLING", "getContext()"}) PropertyGetNode getLastChunkHandlingNode,
                        @Cached(parameters = {"getContext()", "true"}) ArraySpeciesConstructorNode constructTypedArrayNode,
                        @Cached TruffleString.ReadCharUTF16Node charAtNode,
                        @Cached TruffleString.EqualNode equalNode) {
            Object opts = getOptionsObjectNode.execute(options);
            TruffleString alphabet = getStringOption(opts, Strings.ALPHABET, ALPHABET_VALUES, BASE64, getAlphabetNode, equalNode);
            boolean base64url = Strings.equals(equalNode, alphabet, BASE64URL);
            TruffleString lastChunkHandling = getStringOption(opts, Strings.LAST_CHUNK_HANDLING, LAST_CHUNK_HANDLING_VALUES, LOOSE, getLastChunkHandlingNode, equalNode);

            var result = fromBase64(string, base64url, Integer.MAX_VALUE, lastChunkHandling, charAtNode);
            if (result.error() != null) {
                throw result.error();
            }

            int resultLength = result.written();
            JSTypedArrayObject uint8Array = constructTypedArrayNode.typedArrayCreate(
                            getRealm().getArrayBufferViewConstructor(TypedArrayFactory.Uint8Array), resultLength);
            setUint8ArrayBytes(uint8Array, result.bytes(), resultLength);
            return uint8Array;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static Object doNotString(Object string, Object options) {
            throw Errors.createTypeErrorNotAString(string);
        }
    }

    public abstract static class SetFromHexNode extends SetFromBaseNode {

        protected SetFromHexNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isUint8Array(into)"})
        protected final JSObject doUint8Array(JSTypedArrayObject into, TruffleString string,
                        @Cached TruffleString.ReadCharUTF16Node charAtNode) {
            int byteLength = getByteLengthOrThrow(into);

            var result = fromHex(string, byteLength, charAtNode);

            assert !into.getArrayBuffer().isDetached();
            assert result.written() <= byteLength;
            setUint8ArrayBytes(into, result.bytes(), result.written());

            if (result.error() != null) {
                throw result.error();
            }
            return createResultObject(result.read(), result.written());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUint8Array(into)", "!isString(string)"})
        protected static Object doNotString(JSTypedArrayObject into, Object string) {
            throw Errors.createTypeErrorNotAString(string);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static Object doNotUint8Array(Object thisObj, Object string) {
            throw Errors.createTypeErrorUint8ArrayExpected();
        }
    }

    public abstract static class Uint8ArrayFromHexNode extends Uint8ArrayBaseNode {

        protected Uint8ArrayFromHexNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSObject doString(TruffleString string,
                        @Cached(parameters = {"getContext()", "true"}) ArraySpeciesConstructorNode constructTypedArrayNode,
                        @Cached TruffleString.ReadCharUTF16Node charAtNode) {
            var result = fromHex(string, Integer.MAX_VALUE, charAtNode);
            if (result.error() != null) {
                throw result.error();
            }

            int resultLength = result.written();
            JSTypedArrayObject uint8Array = constructTypedArrayNode.typedArrayCreate(
                            getRealm().getArrayBufferViewConstructor(TypedArrayFactory.Uint8Array), resultLength);
            setUint8ArrayBytes(uint8Array, result.bytes(), resultLength);
            return uint8Array;
        }

        @Fallback
        protected static Object doNotString(Object string) {
            throw Errors.createTypeErrorNotAString(string);
        }
    }

    @ImportStatic({Boolean.class, Strings.class, JSConfig.class})
    public abstract static class ToBase64Node extends Uint8ArrayBaseNode {

        protected ToBase64Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isUint8Array(thisObj)"})
        protected final TruffleString doUint8Array(JSTypedArrayObject thisObj, Object options,
                        @Cached(parameters = {"getContext()"}) GetOptionsObjectNode getOptionsObjectNode,
                        @Cached(parameters = {"ALPHABET", "getContext()"}) PropertyGetNode getAlphabetNode,
                        @Cached(parameters = {"getContext()", "OMIT_PADDING", "FALSE"}) GetBooleanOptionNode getOmitPaddingOptionNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
            Object opts = getOptionsObjectNode.execute(options);
            TruffleString alphabet = getStringOption(opts, Strings.ALPHABET, ALPHABET_VALUES, BASE64, getAlphabetNode, equalNode);
            boolean base64url = Strings.equals(equalNode, alphabet, BASE64URL);
            boolean omitPadding = getOmitPaddingOptionNode.executeValue(opts);

            byte[] bytes = getUint8ArrayBytes(thisObj, interop);

            String result = base64EncodeToString(bytes, base64url, omitPadding);
            return Strings.fromJavaString(fromJavaStringNode, result);
        }

        @TruffleBoundary
        private static String base64EncodeToString(byte[] bytes, boolean base64url, boolean omitPadding) {
            Base64.Encoder encoder = base64url ? Base64.getUrlEncoder() : Base64.getEncoder();
            if (omitPadding) {
                encoder = encoder.withoutPadding();
            }
            return encoder.encodeToString(bytes);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static Object doNotUint8Array(Object thisObj, Object options) {
            throw Errors.createTypeErrorUint8ArrayExpected();
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ToHexNode extends Uint8ArrayBaseNode {

        protected ToHexNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private static byte hexDigit(int digit) {
            assert (digit & 0xf) == digit;
            return (byte) ((digit >= 10) ? 'a' - 10 + digit : '0' + digit);
        }

        @Specialization(guards = {"isUint8Array(thisObj)"})
        protected final TruffleString doUint8Array(JSTypedArrayObject thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached TruffleString.FromByteArrayNode fromByteArrayNode,
                        @Cached TruffleString.SwitchEncodingNode switchEncodingNode) {
            byte[] buffer = getUint8ArrayBytes(thisObj, interop);
            if (buffer.length > JSConfig.SOFT_MAX_ARRAY_LENGTH / 2) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidArrayLength(this);
            }
            byte[] hex = new byte[buffer.length * 2];

            for (int i = 0; i < buffer.length; i++) {
                byte b = buffer[i];
                hex[i * 2] = hexDigit((b >>> 4) & 0xf);
                hex[i * 2 + 1] = hexDigit(b & 0xf);
            }

            return switchEncodingNode.execute(fromByteArrayNode.execute(hex, TruffleString.Encoding.US_ASCII, false), TruffleString.Encoding.UTF_16);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static Object doNotUint8Array(Object thisObj) {
            throw Errors.createTypeErrorUint8ArrayExpected();
        }
    }
}

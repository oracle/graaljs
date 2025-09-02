/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ReadOnlyBufferException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.UUID;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.CryptoBuiltinsFactory.JSCryptoGetRandomValuesNodeGen;
import com.oracle.truffle.js.builtins.CryptoBuiltinsFactory.JSCryptoRandomUUIDNodeGen;
import com.oracle.truffle.js.nodes.ThrowTypeErrorRootNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedBigIntArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedIntArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class CryptoBuiltins extends JSBuiltinsContainer.SwitchEnum<CryptoBuiltins.CryptoPrototype> {

    public static final TruffleString OBJECT_NAME = Strings.constant("crypto");
    public static final TruffleString FUNCTION_NAME = Strings.constant("Crypto");
    private static final JSBuiltinsContainer BUILTINS = new CryptoBuiltins();

    private CryptoBuiltins() {
        super(OBJECT_NAME, CryptoPrototype.class);
    }

    public enum CryptoPrototype implements BuiltinEnum<CryptoPrototype> {
        getRandomValues(1),
        randomUUID(0);

        private final int length;

        CryptoPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    public static JSObject createCryptoPrototype(JSRealm realm) {
        JSObject cryptoPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putToStringTag(cryptoPrototype, FUNCTION_NAME);
        JSObjectUtil.putFunctionsFromContainer(realm, cryptoPrototype, BUILTINS);
        return cryptoPrototype;
    }

    public static JSFunctionObject createCryptoFunction(JSRealm realm, JSObject cryptoPrototype) {
        JSFunctionData cryptoFunctionData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.Crypto, CryptoBuiltins::createCryptoFunctionData);
        JSFunctionObject function = JSFunction.create(realm, cryptoFunctionData);
        JSObjectUtil.putConstructorPrototypeProperty(function, cryptoPrototype);
        JSObjectUtil.putConstructorProperty(cryptoPrototype, function);
        return function;
    }

    private static JSFunctionData createCryptoFunctionData(JSContext context) {
        CallTarget throwTypeErrorCallTarget = new ThrowTypeErrorRootNode(context.getLanguage(), false).getCallTarget();
        return JSFunctionData.create(context, throwTypeErrorCallTarget, throwTypeErrorCallTarget, 0, FUNCTION_NAME, false, false, false, true);
    }

    public static JSObject createCryptoObject(JSRealm realm, JSObject cryptoPrototype) {
        return JSOrdinary.createWithPrototype(cryptoPrototype, realm.getContext());
    }

    public static SecureRandom getSecureRandomInstance() {
        // prefer SecureRandom using non-blocking source
        if (Security.getAlgorithms("SecureRandom").contains("NATIVEPRNGNONBLOCKING")) {
            try {
                return SecureRandom.getInstance("NATIVEPRNGNONBLOCKING");
            } catch (NoSuchAlgorithmException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        } else {
            return new SecureRandom();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, CryptoPrototype builtinEnum) {
        return switch (builtinEnum) {
            case getRandomValues ->
                JSCryptoGetRandomValuesNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case randomUUID ->
                JSCryptoRandomUUIDNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
        };
    }

    public abstract static class JSCryptoGetRandomValuesNode extends JSBuiltinNode {
        private static final int BYTE_LENGTH_LIMIT = 1 << 16;
        private static final String FAILED_MESSAGE_PREFIX = "Failed to execute 'getRandomValues' on 'Crypto': ";

        public JSCryptoGetRandomValuesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected Object getRandomValues(Object thisObj, JSTypedArrayObject typedArray) {
            if (!getRealm().isCryptoObject(thisObj)) {
                throw Errors.createTypeError("Illegal invocation");
            }
            TypedArray arrayType = typedArray.getArrayType();
            if (!(arrayType instanceof TypedIntArray || arrayType instanceof TypedBigIntArray)) {
                throw Errors.createTypeError(FAILED_MESSAGE_PREFIX + "The provided ArrayBufferView is of type '" + arrayType.getName() + "', which is not an integer array type.");
            }
            JSArrayBufferObject arrayBuffer = typedArray.getArrayBuffer();
            if (arrayBuffer.isDetached()) {
                return typedArray;
            }
            int byteOffset = typedArray.getByteOffset();
            int byteLength = typedArray.getByteLength();
            if (byteLength > BYTE_LENGTH_LIMIT) {
                throw Errors.createRangeError(
                                FAILED_MESSAGE_PREFIX + "The ArrayBufferView's byte length (" + byteLength + ") exceeds the number of bytes of entropy available via this API.", this);
            }
            byte[] randomBytes = new byte[byteLength];
            getRealm().getSecureRandom().nextBytes(randomBytes);
            if (arrayBuffer instanceof JSArrayBufferObject.Heap heapBuffer) {
                System.arraycopy(randomBytes, 0, heapBuffer.getByteArray(), byteOffset, byteLength);
            } else if (arrayBuffer instanceof JSArrayBufferObject.DirectBase directBuffer) {
                ByteBuffer byteBuffer = directBuffer.getByteBuffer();
                try {
                    byteBuffer.put(byteOffset, randomBytes);
                } catch (IndexOutOfBoundsException | ReadOnlyBufferException e) {
                    throw Errors.createError("Could not fill ByteBuffer with random values", e);
                }
            } else if (arrayBuffer instanceof JSArrayBufferObject.Interop interopBuffer) {
                Object foreignBuffer = interopBuffer.getInteropBuffer();
                assert byteLength == interopBuffer.getByteLength();
                try {
                    for (int i = 0; i < byteLength; i++) {
                        InteropLibrary.getUncached().writeBufferByte(foreignBuffer, byteOffset + i, randomBytes[i]);
                    }
                } catch (InteropException e) {
                    throw Errors.createError(FAILED_MESSAGE_PREFIX + "Could not fill interop buffer with random values.", e);
                }
            } else {
                throw Errors.createTypeError(FAILED_MESSAGE_PREFIX + "Unexpected JSTypedArrayObject '" + typedArray.getClass().getName() + "'.");
            }
            return typedArray;
        }

        @Fallback
        @SuppressWarnings("unused")
        protected JSException doFallback(Object thisObj, Object argument) {
            if (!getRealm().isCryptoObject(thisObj)) {
                throw Errors.createTypeError("Illegal invocation");
            }
            throw Errors.createTypeError(FAILED_MESSAGE_PREFIX + "parameter 1 is not of type 'ArrayBufferView'.", this);
        }
    }

    public abstract static class JSCryptoRandomUUIDNode extends JSBuiltinNode {
        public JSCryptoRandomUUIDNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected TruffleString randomUUID(Object thisObj) {
            if (!getRealm().isCryptoObject(thisObj)) {
                throw Errors.createTypeError("Illegal invocation");
            }
            return Strings.fromJavaString(UUID.randomUUID().toString());
        }
    }
}

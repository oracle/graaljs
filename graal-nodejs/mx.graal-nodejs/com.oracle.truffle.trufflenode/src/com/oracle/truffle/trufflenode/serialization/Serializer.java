/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.serialization;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBigIntObject;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSBooleanObject;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDataViewObject;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDateObject;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapObject;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSetObject;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSStringObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSUncheckedProxyHandlerObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemoryObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import com.oracle.truffle.trufflenode.threading.JavaMessagePortData;

/**
 * Implementation of {@code v8::(internal::)ValueSerializer}.
 */
public class Serializer {
    static final byte VERSION = (byte) 0xFF; // SerializationTag::kVersion
    static final byte LATEST_VERSION = (byte) 15; // kLatestVersion
    static final String NATIVE_UTF16_ENCODING = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? "UTF-16BE" : "UTF-16LE";

    public static final TruffleString COULD_NOT_BE_CLONED = Strings.constant(" could not be cloned.");
    public static final TruffleString HASH_BRACKETS_OBJECT = Strings.constant("#<Object>");

    /** Pointer to the corresponding v8::ValueSerializer. */
    private final long delegate;
    /** Buffer used for serialization. */
    private ByteBuffer buffer = allocateBuffer(1024);
    /** ID of the next serialized object. **/
    private int nextId;
    /** Maps a serialized object to its ID. */
    private final Map<Object, Integer> objectMap = new IdentityHashMap<>();
    /** Maps a transferred object to its transfer ID. */
    private final Map<Object, Integer> transferMap = new IdentityHashMap<>();
    /** Determines whether {@code ArrayBuffer}s should be serialized as host objects. */
    private boolean treatArrayBufferViewsAsHostObjects;

    private final Env env;
    private final GraalJSAccess access;
    private final boolean hasCustomHostObject;

    public Serializer(Env env, GraalJSAccess access, long delegate) {
        this.delegate = delegate;
        this.env = env;
        this.access = access;
        this.hasCustomHostObject = NativeAccess.hasCustomHostObject(delegate);
    }

    public void setTreatArrayBufferViewsAsHostObjects(boolean treatArrayBufferViewsAsHostObjects) {
        this.treatArrayBufferViewsAsHostObjects = treatArrayBufferViewsAsHostObjects;
    }

    private static ByteBuffer allocateBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    private void ensureFreeSpace(int spaceNeeded) {
        ByteBuffer oldBuffer = buffer;
        int capacity = oldBuffer.capacity();
        int capacityNeeded = oldBuffer.position() + spaceNeeded;
        if (capacityNeeded > capacity) {
            int newCapacity = Math.max(capacityNeeded, 2 * capacity);
            ByteBuffer newBuffer = allocateBuffer(newCapacity);
            oldBuffer.flip();
            newBuffer.put(oldBuffer);
            buffer = newBuffer;
        }
    }

    public void writeHeader() {
        ensureFreeSpace(2);
        buffer.put(VERSION);
        buffer.put(LATEST_VERSION);
    }

    private void writeTag(SerializationTag tag) {
        writeByte(tag.getTag());
    }

    private void writeTag(ArrayBufferViewTag tag) {
        writeByte(tag.getTag());
    }

    private void writeTag(ErrorTag tag) {
        writeByte(tag.getTag());
    }

    private void writeByte(byte b) {
        ensureFreeSpace(1);
        buffer.put(b);
    }

    public void writeValue(Object value) {
        if (value == Boolean.TRUE) {
            writeTag(SerializationTag.TRUE);
        } else if (value == Boolean.FALSE) {
            writeTag(SerializationTag.FALSE);
        } else if (value == Undefined.instance) {
            writeTag(SerializationTag.UNDEFINED);
        } else if (value == Null.instance) {
            writeTag(SerializationTag.NULL);
        } else if (value instanceof Integer) {
            writeInt((Integer) value);
        } else if (JSRuntime.isNumber(value)) {
            double doubleValue = ((Number) value).doubleValue();
            writeIntOrDouble(doubleValue);
        } else if (value instanceof TruffleString) {
            writeString((TruffleString) value);
        } else if (JSRuntime.isBigInt(value)) {
            writeTag(SerializationTag.BIG_INT);
            writeBigIntContents((BigInt) value);
        } else if (value instanceof Symbol) {
            NativeAccess.throwDataCloneError(delegate, Strings.concat(JSRuntime.safeToString(value), COULD_NOT_BE_CLONED));
        } else if (env.isHostObject(value) && access.getCurrentMessagePortData() != null) {
            writeSharedJavaObject(env.asHostObject(value));
        } else if (value instanceof Long) {
            if (access.getCurrentMessagePortData() != null) {
                writeSharedJavaObject(value);
            } else {
                writeIntOrDouble(((Long) value).doubleValue());
            }
        } else {
            writeObject(value);
        }
    }

    private void writeObject(Object object) {
        Integer id = objectMap.get(object);
        if (id != null) {
            writeTag(SerializationTag.OBJECT_REFERENCE);
            writeVarInt(id);
            return;
        }
        if (!treatArrayBufferViewsAsHostObjects && JSArrayBufferView.isJSArrayBufferView(object)) {
            JSArrayBufferObject arrayBuffer = JSArrayBufferView.getArrayBuffer((JSTypedArrayObject) object);
            assignId(arrayBuffer);
            if (JSSharedArrayBuffer.isJSSharedArrayBuffer(arrayBuffer)) {
                writeJSSharedArrayBuffer(arrayBuffer);
            } else {
                writeJSArrayBuffer(arrayBuffer);
            }
        }
        assignId(object);
        if (JSDate.isJSDate(object)) {
            writeTag(SerializationTag.DATE);
            writeDate((JSDateObject) object);
        } else if (JSBoolean.isJSBoolean(object)) {
            writeJSBoolean((JSBooleanObject) object);
        } else if (JSNumber.isJSNumber(object)) {
            writeJSNumber((JSNumberObject) object);
        } else if (JSBigInt.isJSBigInt(object)) {
            writeTag(SerializationTag.BIG_INT_OBJECT);
            writeBigIntContents(JSBigInt.valueOf((JSBigIntObject) object));
        } else if (JSString.isJSString(object)) {
            writeJSString((JSStringObject) object);
        } else if (JSRegExp.isJSRegExp(object)) {
            writeJSRegExp((JSRegExpObject) object);
        } else if (JSArrayBuffer.isJSDirectArrayBuffer(object)) {
            writeJSArrayBuffer((JSArrayBufferObject) object);
        } else if (JSSharedArrayBuffer.isJSSharedArrayBuffer(object)) {
            writeJSSharedArrayBuffer((JSArrayBufferObject) object);
        } else if (JSMap.isJSMap(object)) {
            writeJSMap((JSMapObject) object);
        } else if (JSSet.isJSSet(object)) {
            writeJSSet((JSSetObject) object);
        } else if (JSArray.isJSArray(object)) {
            writeJSArray((JSArrayObject) object);
        } else if (JSArrayBufferView.isJSArrayBufferView(object)) {
            writeJSArrayBufferView((JSTypedArrayObject) object);
        } else if (JSDataView.isJSDataView(object)) {
            writeJSDataView((JSDataViewObject) object);
        } else if (JSError.isJSError(object)) {
            writeJSError((JSErrorObject) object);
        } else if (JSWebAssemblyModule.isJSWebAssemblyModule(object)) {
            writeJSWebAssemblyModule((JSWebAssemblyModuleObject) object);
        } else if (JSWebAssemblyMemory.isJSWebAssemblyMemory(object)) {
            writeJSWebAssemblyMemory((JSWebAssemblyMemoryObject) object);
        } else if (JSProxy.isJSProxy(object)) {
            JSProxyObject proxy = (JSProxyObject) object;
            if (proxy.getProxyHandler() instanceof JSUncheckedProxyHandlerObject) {
                // instance of an ObjectTemplate with a property handler
                writeHostObject(proxy);
            } else {
                boolean callable = JSRuntime.isCallableProxy(proxy);
                TruffleString objectStr;
                if (callable) {
                    objectStr = JSRuntime.safeToString(JSProxy.getTargetNonProxy(proxy));
                } else {
                    objectStr = HASH_BRACKETS_OBJECT;
                }
                TruffleString message = Strings.concat(objectStr, COULD_NOT_BE_CLONED);
                NativeAccess.throwDataCloneError(delegate, message);
            }
        } else if (JSFunction.isJSFunction(object)) {
            NativeAccess.throwDataCloneError(delegate, Strings.concat(JSRuntime.safeToString(object), COULD_NOT_BE_CLONED));
        } else if (JSDynamicObject.isJSDynamicObject(object)) {
            JSDynamicObject dynamicObject = (JSDynamicObject) object;
            if (hasCustomHostObject ? NativeAccess.isHostObject(delegate, object) : (GraalJSAccess.internalFieldCount(dynamicObject) != 0)) {
                writeHostObject(dynamicObject);
            } else {
                writeJSObject(dynamicObject);
            }
        } else {
            writeHostObject(object);
        }
    }

    private void writeInt(int value) {
        writeTag(SerializationTag.INT32);
        int zigzag = (value << 1) ^ (value >> 31);
        writeVarInt(Integer.toUnsignedLong(zigzag));
    }

    public void writeVarInt(long value) {
        long rest = value;
        byte[] bytes = new byte[10];
        int idx = 0;
        do {
            byte b = (byte) rest;
            b |= (byte) 0x80;
            bytes[idx] = b;
            idx++;
            rest >>>= 7;
        } while (rest != 0);
        bytes[idx - 1] &= 0x7f;
        writeBytes(bytes, idx);
    }

    private void writeBytes(byte[] bytes, int length) {
        ensureFreeSpace(length);
        buffer.put(bytes, 0, length);
    }

    public void writeBytes(ByteBuffer bytes) {
        ensureFreeSpace(bytes.remaining());
        buffer.put(bytes);
    }

    public void writeIntOrDouble(double value) {
        if (JSRuntime.doubleIsRepresentableAsInt(value)) {
            writeInt((int) value);
        } else {
            writeTag(SerializationTag.DOUBLE);
            writeDouble(value);
        }
    }

    public void writeDouble(double value) {
        ensureFreeSpace(8);
        buffer.putDouble(value);
    }

    private void writeString(TruffleString string) {
        byte[] bytes;
        SerializationTag tag;
        int length = Strings.length(string);
        if (string.getCodeRangeUncached(TruffleString.Encoding.UTF_16).isSubsetOf(TruffleString.CodeRange.LATIN_1)) {
            tag = SerializationTag.ONE_BYTE_STRING;
            bytes = new byte[length];
            string.switchEncodingUncached(TruffleString.Encoding.ISO_8859_1).copyToByteArrayUncached(0, bytes, 0, length, TruffleString.Encoding.ISO_8859_1);
        } else {
            tag = SerializationTag.TWO_BYTE_STRING;
            bytes = new byte[length << 1];
            string.copyToByteArrayUncached(0, bytes, 0, length << 1, TruffleString.Encoding.UTF_16);
        }
        writeTag(tag);
        writeVarInt(bytes.length);
        writeBytes(bytes, bytes.length);
    }

    private void writeDate(JSDateObject date) {
        writeDouble(date.getTimeMillis());
    }

    private void writeJSBoolean(JSBooleanObject bool) {
        writeTag(JSBoolean.valueOf(bool) ? SerializationTag.TRUE_OBJECT : SerializationTag.FALSE_OBJECT);
    }

    private void writeJSNumber(JSNumberObject number) {
        double value = JSNumber.valueOf(number).doubleValue();
        writeTag(SerializationTag.NUMBER_OBJECT);
        writeDouble(value);
    }

    private void writeJSString(JSStringObject string) {
        TruffleString value = JSString.getString(string);
        writeTag(SerializationTag.STRING_OBJECT);
        writeString(value);
    }

    private void writeJSRegExp(JSRegExpObject regExp) {
        TruffleString pattern = (TruffleString) GraalJSAccess.regexpPattern(regExp);
        int flags = GraalJSAccess.regexpV8Flags(regExp);
        writeTag(SerializationTag.REGEXP);
        writeString(pattern);
        writeVarInt(flags);
    }

    private void writeJSArrayBuffer(JSArrayBufferObject arrayBuffer) {
        assert JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer);
        Integer id = transferMap.get(arrayBuffer);
        if (id == null) {
            int byteLength = arrayBuffer.getByteLength();
            ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
            writeTag(SerializationTag.ARRAY_BUFFER);
            writeVarInt(byteLength);
            ensureFreeSpace(byteLength);
            for (int i = 0; i < byteLength; i++) {
                buffer.put(byteBuffer.get(i));
            }
        } else {
            writeTag(SerializationTag.ARRAY_BUFFER_TRANSFER);
            writeVarInt(Integer.toUnsignedLong(id));
        }
    }

    private void writeJSSharedArrayBuffer(JSArrayBufferObject sharedArrayBuffer) {
        int id = NativeAccess.getSharedArrayBufferId(delegate, sharedArrayBuffer);
        writeTag(SerializationTag.SHARED_ARRAY_BUFFER);
        writeVarInt(id);
    }

    private void writeJSWebAssemblyModule(JSWebAssemblyModuleObject wasmModule) {
        int id = NativeAccess.getWasmModuleTransferId(delegate, wasmModule);
        writeTag(SerializationTag.WASM_MODULE_TRANSFER);
        writeVarInt(id);
    }

    private void writeJSWebAssemblyMemory(JSWebAssemblyMemoryObject wasmMemory) {
        if (wasmMemory.isShared()) {
            writeTag(SerializationTag.WASM_MEMORY_TRANSFER);

            // Write wasm memory
            writeSharedJavaObject(wasmMemory.getWASMMemory());

            // Write waiter list of the underlying SharedArrayBuffer
            JSRealm realm = JSRealm.get(null);
            JSContext context = realm.getContext();
            JSArrayBufferObject arrayBuffer = wasmMemory.getBufferObject(context, realm);
            JSAgentWaiterList waiterList = JSArrayBufferObject.getWaiterList(arrayBuffer);
            writeSharedJavaObject(waiterList);
        } else {
            // non-shared WebAssembly.Memory cannot be cloned
            NativeAccess.throwDataCloneError(delegate, Strings.concat(JSRuntime.safeToString(wasmMemory), COULD_NOT_BE_CLONED));
        }
    }

    private void writeJSObject(JSDynamicObject object) {
        assert JSDynamicObject.isJSDynamicObject(object);
        writeTag(SerializationTag.BEGIN_JS_OBJECT);
        List<TruffleString> names = JSObject.enumerableOwnNames(object);
        writeJSObjectProperties(object, names);
        writeTag(SerializationTag.END_JS_OBJECT);
        writeVarInt(names.size());
    }

    private void writeJSObjectProperties(JSDynamicObject object, List<TruffleString> keys) {
        assert JSDynamicObject.isJSDynamicObject(object);
        for (TruffleString key : keys) {
            if (JSRuntime.isArrayIndex(key)) {
                try {
                    writeIntOrDouble(Strings.parseDouble(key));
                } catch (TruffleString.NumberFormatException e) {
                    throw CompilerDirectives.shouldNotReachHere(e);
                }
            } else {
                writeString(key);
            }
            Object value = JSObject.get(object, key);
            writeValue(value);
        }
    }

    private void writeJSMap(JSMapObject object) {
        writeTag(SerializationTag.BEGIN_JS_MAP);
        JSHashMap map = JSMap.getInternalMap(object);
        JSHashMap.Cursor cursor = map.getEntries();
        int count = 0;
        while (cursor.advance()) {
            count++;
            writeValue(cursor.getKey());
            writeValue(cursor.getValue());
        }
        writeTag(SerializationTag.END_JS_MAP);
        writeVarInt(2 * count);
    }

    private void writeJSSet(JSSetObject object) {
        writeTag(SerializationTag.BEGIN_JS_SET);
        JSHashMap map = JSSet.getInternalSet(object);
        JSHashMap.Cursor cursor = map.getEntries();
        int count = 0;
        while (cursor.advance()) {
            count++;
            writeValue(cursor.getKey());
        }
        writeTag(SerializationTag.END_JS_SET);
        writeVarInt(count);
    }

    private void writeJSArray(JSArrayObject object) {
        assert JSArray.isJSArray(object);
        long length = JSAbstractArray.arrayGetLength(object);
        List<TruffleString> names = JSObject.enumerableOwnNames(object);
        boolean dense = names.size() >= length;
        if (dense) {
            for (int i = 0; i < length; i++) {
                if (!Strings.fromInt(i).equals(names.get(i))) {
                    dense = false;
                    break;
                }
            }
        }
        if (dense) {
            names = names.subList((int) length, names.size());
            writeTag(SerializationTag.BEGIN_DENSE_JS_ARRAY);
            writeVarInt(length);
            for (int i = 0; i < length; i++) {
                writeValue(JSObject.get(object, i));
            }
            writeJSObjectProperties(object, names);
            writeTag(SerializationTag.END_DENSE_JS_ARRAY);
        } else {
            writeTag(SerializationTag.BEGIN_SPARSE_JS_ARRAY);
            writeVarInt(length);
            writeJSObjectProperties(object, names);
            writeTag(SerializationTag.END_SPARSE_JS_ARRAY);
        }
        writeVarInt(names.size());
        writeVarInt(length);
    }

    private void writeJSArrayBufferView(JSTypedArrayObject view) {
        if (treatArrayBufferViewsAsHostObjects) {
            writeHostObject(view);
        } else {
            int offset = view.getByteOffset();
            TypedArray typedArray = view.getArrayType();
            int length = typedArray.lengthInt(view) * typedArray.bytesPerElement();
            ArrayBufferViewTag tag = ArrayBufferViewTag.fromFactory(typedArray.getFactory());
            writeJSArrayBufferView(tag, offset, length);
        }
    }

    private void writeJSDataView(JSDataViewObject view) {
        if (treatArrayBufferViewsAsHostObjects) {
            writeTag(SerializationTag.HOST_OBJECT);
            NativeAccess.writeHostObject(delegate, view);
        } else {
            int offset = view.getByteOffset();
            int length = view.getByteLength();
            writeJSArrayBufferView(ArrayBufferViewTag.DATA_VIEW, offset, length);
        }
    }

    private void writeJSArrayBufferView(ArrayBufferViewTag tag, int offset, int length) {
        writeTag(SerializationTag.ARRAY_BUFFER_VIEW);
        writeTag(tag);
        writeVarInt(offset);
        writeVarInt(length);
    }

    private void writeBigIntContents(BigInt value) {
        BigInteger bigInteger = value.bigIntegerValue();
        boolean negative = bigInteger.signum() == -1;
        if (negative) {
            bigInteger = bigInteger.negate();
        }
        int bitLength = bigInteger.bitLength();
        int digits = (bitLength + 63) / 64;
        int bytes = digits * 8;
        int bitfield = bytes;
        bitfield <<= 1;
        if (negative) {
            bitfield++;
        }
        writeVarInt(bitfield);
        for (int i = 0; i < bytes; i++) {
            byte b = 0;
            for (int bit = 8 * (i + 1) - 1; bit >= 8 * i; bit--) {
                b <<= 1;
                if (bigInteger.testBit(bit)) {
                    b++;
                }
            }
            writeByte(b);
        }
    }

    private void writeJSError(JSErrorObject error) {
        writeTag(SerializationTag.ERROR);
        writeErrorTypeTag(error);

        PropertyDescriptor desc = JSObject.getOwnProperty(error, JSError.MESSAGE);
        if (desc != null && desc.isDataDescriptor()) {
            writeTag(ErrorTag.MESSAGE);
            TruffleString message = JSRuntime.toString(desc.getValue());
            writeString(message);
        }

        Object stack = JSObject.get(error, JSError.STACK_NAME);
        if (stack instanceof TruffleString stackStr) {
            writeTag(ErrorTag.STACK);
            writeString(stackStr);
        }

        writeTag(ErrorTag.END);
    }

    private void writeErrorTypeTag(JSErrorObject error) {
        Throwable exception = JSError.getException(error);
        JSErrorType errorType = JSErrorType.Error;
        if (exception instanceof JSException) {
            errorType = ((JSException) exception).getErrorType();
        }
        ErrorTag tag;
        switch (errorType) {
            case EvalError:
                tag = ErrorTag.EVAL_ERROR;
                break;
            case RangeError:
                tag = ErrorTag.RANGE_ERROR;
                break;
            case ReferenceError:
                tag = ErrorTag.REFERENCE_ERROR;
                break;
            case SyntaxError:
                tag = ErrorTag.SYNTAX_ERROR;
                break;
            case TypeError:
                tag = ErrorTag.TYPE_ERROR;
                break;
            case URIError:
                tag = ErrorTag.URI_ERROR;
                break;
            default:
                tag = null;
                assert (errorType == JSErrorType.Error) || (errorType == JSErrorType.AggregateError);
                break;
        }
        if (tag != null) {
            writeTag(tag);
        }
    }

    private void writeSharedJavaObject(Object value) {
        JavaMessagePortData messagePort = access.getCurrentMessagePortData();
        writeTag(SerializationTag.SHARED_JAVA_OBJECT);
        writeVarInt(messagePort.getMessagePortDataPointer());
        assignId(value);
        messagePort.enqueueJavaRef(value);
    }

    private void writeHostObject(Object object) {
        writeTag(SerializationTag.HOST_OBJECT);
        NativeAccess.writeHostObject(delegate, object);
    }

    public void transferArrayBuffer(int id, Object arrayBuffer) {
        transferMap.put(arrayBuffer, id);
    }

    public int size() {
        return buffer.position();
    }

    public void release(ByteBuffer targetBuffer) {
        buffer.flip();
        targetBuffer.put(buffer);
        buffer.clear();
    }

    private void assignId(Object object) {
        objectMap.put(object, nextId++);
    }

}

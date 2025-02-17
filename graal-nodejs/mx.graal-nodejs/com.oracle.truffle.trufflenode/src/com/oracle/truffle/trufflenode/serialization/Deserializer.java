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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapObject;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSetObject;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemoryObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import com.oracle.truffle.trufflenode.threading.JavaMessagePortData;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingManager;

/**
 * Implementation of {@code v8::(internal::)ValueDeserializer}.
 */
public class Deserializer {
    /** Pointer to the corresponding v8::ValueDeserializer. */
    private final long delegate;
    /** Buffer used for serialization. */
    private final ByteBuffer buffer;
    /** Version of the data format used during serialization. */
    private int version;
    /** ID of the next deserialized object. */
    private int nextId;
    /** Maps ID of a deserialized object to the object itself. */
    private Map<Integer, Object> objectMap = new HashMap<>();
    /** Maps transfer ID to the transferred object. */
    private Map<Integer, JSDynamicObject> transferMap = new HashMap<>();
    /** Cache for the last VM-level communication channel. */
    private JavaMessagePortData messagePortCache = null;

    public Deserializer(long delegate, ByteBuffer buffer) {
        this.delegate = delegate;
        this.buffer = buffer.order(ByteOrder.nativeOrder());
    }

    public void readHeader() {
        if (buffer.remaining() >= 2 && buffer.get() == Serializer.VERSION) {
            version = buffer.get();
        }
        if (version == 0 || version > Serializer.LATEST_VERSION) {
            throw Errors.createError("Unable to deserialize cloned data due to invalid or unsupported version.");
        }
    }

    public Object readValue(JSRealm realm) {
        try {
            SerializationTag tag = readTag();
            return readValue(realm, tag);
        } catch (GraalJSException jsex) {
            throw jsex;
        } catch (Exception ex) {
            throw Errors.createError("Unable to deserialize cloned data.");
        }
    }

    private Object readValue(JSRealm realm, SerializationTag tag) {
        JSContext context = realm.getContext();
        switch (tag) {
            case TRUE:
                return true;
            case FALSE:
                return false;
            case UNDEFINED:
                return Undefined.instance;
            case NULL:
                return Null.instance;
            case INT32:
                return readInt();
            case UINT32:
                return readVarInt();
            case DOUBLE:
                return readDouble();
            case BIG_INT:
                return readBigInt();
            case ONE_BYTE_STRING:
                return readOneByteString();
            case TWO_BYTE_STRING:
                return readTwoByteString();
            case UTF8_STRING:
                return readUTF8String();
            case DATE:
                return readDate(context, realm);
            case TRUE_OBJECT:
                return assignId(JSBoolean.create(context, realm, true));
            case FALSE_OBJECT:
                return assignId(JSBoolean.create(context, realm, false));
            case NUMBER_OBJECT:
                return readJSNumber(context, realm);
            case BIG_INT_OBJECT:
                return readJSBigInt(context, realm);
            case STRING_OBJECT:
                return readJSString(context, realm);
            case REGEXP:
                return readJSRegExp(context, realm);
            case ARRAY_BUFFER:
                return readJSArrayBuffer(context, realm);
            case ARRAY_BUFFER_TRANSFER:
                return readTransferredJSArrayBuffer(context, realm);
            case SHARED_ARRAY_BUFFER:
                return readSharedArrayBuffer(context, realm);
            case BEGIN_JS_OBJECT:
                return readJSObject(context, realm);
            case BEGIN_JS_MAP:
                return readJSMap(context, realm);
            case BEGIN_JS_SET:
                return readJSSet(context, realm);
            case BEGIN_DENSE_JS_ARRAY:
                return readDenseArray(context, realm);
            case BEGIN_SPARSE_JS_ARRAY:
                return readSparseArray(context, realm);
            case OBJECT_REFERENCE:
                return readObjectReference();
            case HOST_OBJECT:
                return readHostObject();
            case ERROR:
                return readJSError(realm);
            case WASM_MODULE_TRANSFER:
                return readWasmModuleTransfer();
            case WASM_MEMORY_TRANSFER:
                return readWasmMemoryTransfer(realm);
            case SHARED_JAVA_OBJECT:
                Object hostValue = readSharedJavaObject();
                return realm.getEnv().asGuestValue(hostValue);
            default:
                throw Errors.createError("Deserialization of a value tagged " + tag);
        }
    }

    private SerializationTag readTag() {
        SerializationTag tag;
        do {
            tag = SerializationTag.fromTag(buffer.get());
        } while (tag == SerializationTag.PADDING);
        return tag;
    }

    private SerializationTag peekTag() {
        return buffer.hasRemaining() ? SerializationTag.fromTag(buffer.get(buffer.position())) : null;
    }

    private ArrayBufferViewTag readArrayBufferViewTag() {
        return ArrayBufferViewTag.fromTag(buffer.get());
    }

    private ErrorTag readErrorTag() {
        return ErrorTag.fromTag(buffer.get());
    }

    private int readInt() {
        long zigzag = readVarLong();
        long value = (zigzag >> 1) ^ -(zigzag & 1);
        return (int) value;
    }

    public int readVarInt() {
        return (int) readVarLong();
    }

    public long readVarLong() {
        long value = 0;
        int shift = 0;
        byte b;
        do {
            if (buffer.hasRemaining()) {
                b = buffer.get();
            } else {
                throw underflowError();
            }
            value |= (b & 0x7fL) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    public double readDouble() {
        if (buffer.remaining() < 8) {
            throw underflowError();
        } else {
            return buffer.getDouble();
        }
    }

    private TruffleString readString() {
        SerializationTag tag = readTag();
        switch (tag) {
            case ONE_BYTE_STRING:
                return readOneByteString();
            case TWO_BYTE_STRING:
                return readTwoByteString();
            case UTF8_STRING:
                return readUTF8String();
            default:
                throw Errors.shouldNotReachHere();
        }
    }

    private BigInt readBigInt() {
        int bitField = readVarInt();
        boolean negative = (bitField & 1) != 0;
        bitField >>= 1;
        BigInteger bigInteger = BigInteger.ZERO;
        for (int i = 0; i < bitField; i++) {
            byte b = buffer.get();
            for (int bit = 8 * i; bit < 8 * (i + 1); bit++) {
                if ((b & 1) != 0) {
                    bigInteger = bigInteger.setBit(bit);
                }
                b >>>= 1;
            }
        }
        if (negative) {
            bigInteger = bigInteger.negate();
        }
        return new BigInt(bigInteger);
    }

    private TruffleString readOneByteString() {
        return readString(TruffleString.Encoding.ISO_8859_1);
    }

    private TruffleString readTwoByteString() {
        return readString(TruffleString.Encoding.UTF_16);
    }

    private TruffleString readUTF8String() {
        return readString(TruffleString.Encoding.UTF_8);
    }

    private TruffleString readString(TruffleString.Encoding encoding) {
        int byteCount = readVarInt();
        byte[] bytes = new byte[byteCount];
        buffer.get(bytes);
        return TruffleString.fromByteArrayUncached(bytes, encoding, false).switchEncodingUncached(TruffleString.Encoding.UTF_16);
    }

    private JSDynamicObject readDate(JSContext context, JSRealm realm) {
        double millis = readDouble();
        return assignId(JSDate.create(context, realm, millis));
    }

    private JSDynamicObject readJSNumber(JSContext context, JSRealm realm) {
        double value = readDouble();
        return assignId(JSNumber.create(context, realm, value));
    }

    private JSDynamicObject readJSBigInt(JSContext context, JSRealm realm) {
        BigInt value = readBigInt();
        return assignId(JSBigInt.create(context, realm, value));
    }

    private JSDynamicObject readJSString(JSContext context, JSRealm realm) {
        TruffleString value = readString();
        return assignId(JSString.create(context, realm, value));
    }

    private Object readJSRegExp(JSContext context, JSRealm realm) {
        TruffleString pattern = readString();
        int flags = readVarInt();
        return assignId(GraalJSAccess.regexpCreate(context, realm, pattern, flags));
    }

    private JSDynamicObject readJSArrayBuffer(JSContext context, JSRealm realm) {
        int byteLength = readVarInt();
        JSArrayBufferObject arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(context, realm, byteLength);
        ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        for (int i = 0; i < byteLength; i++) {
            byteBuffer.put(i, buffer.get());
        }
        assignId(arrayBuffer);
        return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(context, realm, arrayBuffer) : arrayBuffer;
    }

    private JSDynamicObject readJSObject(JSContext context, JSRealm realm) {
        JSDynamicObject object = JSOrdinary.create(context, realm);
        assignId(object);
        int read = readJSObjectProperties(realm, object, SerializationTag.END_JS_OBJECT);
        int expected = readVarInt();
        if (read != expected) {
            throw Errors.createError("unexpected number of properties");
        }
        return object;
    }

    private int readJSObjectProperties(JSRealm realm, JSDynamicObject object, SerializationTag endTag) {
        SerializationTag tag;
        int count = 0;
        while ((tag = readTag()) != endTag) {
            count++;
            Object key = readValue(realm, tag);
            Object value = readValue(realm);
            JSObject.defineOwnProperty(object, JSRuntime.toPropertyKey(key), PropertyDescriptor.createDataDefault(value));
        }
        return count;
    }

    private JSDynamicObject readJSMap(JSContext context, JSRealm realm) {
        JSMapObject object = JSMap.create(context, realm);
        JSHashMap internalMap = JSMap.getInternalMap(object);
        assignId(object);
        SerializationTag tag;
        int read = 0;
        while ((tag = readTag()) != SerializationTag.END_JS_MAP) {
            read++;
            Object key = readValue(realm, tag);
            Object value = readValue(realm);
            internalMap.put(key, value);
        }
        int expected = readVarInt();
        if (2 * read != expected) {
            throw Errors.createError("unexpected number of entries");
        }
        return object;
    }

    private JSDynamicObject readJSSet(JSContext context, JSRealm realm) {
        JSSetObject object = JSSet.create(context, realm);
        JSHashMap internalMap = JSSet.getInternalSet(object);
        assignId(object);
        SerializationTag tag;
        int read = 0;
        while ((tag = readTag()) != SerializationTag.END_JS_SET) {
            read++;
            Object value = readValue(realm, tag);
            internalMap.put(value, value);
        }
        int expected = readVarInt();
        if (read != expected) {
            throw Errors.createError("unexpected number of values");
        }
        return object;
    }

    private JSDynamicObject readDenseArray(JSContext context, JSRealm realm) {
        int length = readVarInt();
        Object[] elements = new Object[length];
        JSDynamicObject array = JSArray.createConstantObjectArray(context, realm, elements);
        assignId(array);
        List<Integer> holes = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            SerializationTag tag = readTag();
            if (tag == SerializationTag.THE_HOLE) {
                holes.add(i);
            } else {
                elements[i] = readValue(realm, tag);
            }
        }
        for (int hole : holes) {
            JSObject.delete(array, hole);
        }
        int read = readJSObjectProperties(realm, array, SerializationTag.END_DENSE_JS_ARRAY);
        int expected = readVarInt();
        if (read != expected) {
            throw Errors.createError("unexpected number of properties");
        }
        int length2 = readVarInt();
        if (length != length2) {
            throw Errors.createError("length ambiguity");
        }
        return array;
    }

    private JSDynamicObject readSparseArray(JSContext context, JSRealm realm) {
        long length = readVarLong();
        JSDynamicObject array = JSArray.createSparseArray(context, realm, length);
        assignId(array);
        int read = readJSObjectProperties(realm, array, SerializationTag.END_SPARSE_JS_ARRAY);
        int expected = readVarInt();
        if (read != expected) {
            throw Errors.createError("unexpected number of properties");
        }
        long length2 = readVarLong();
        if (length != length2) {
            throw Errors.createError("length ambiguity");
        }
        return array;
    }

    private JSDynamicObject readJSArrayBufferView(JSContext context, JSRealm realm, JSArrayBufferObject arrayBuffer) {
        assert JSArrayBuffer.isJSDirectOrSharedArrayBuffer(arrayBuffer);
        SerializationTag arrayBufferViewTag = readTag();
        assert arrayBufferViewTag == SerializationTag.ARRAY_BUFFER_VIEW;
        ArrayBufferViewTag tag = readArrayBufferViewTag();
        int offset = readVarInt();
        int byteLength = readVarInt();
        JSDynamicObject view;
        if (tag == ArrayBufferViewTag.DATA_VIEW) {
            view = JSDataView.createDataView(context, realm, arrayBuffer, offset, byteLength);
        } else {
            TypedArrayFactory factory = tag.getFactory();
            TypedArray array = factory.createArrayType(TypedArray.BUFFER_TYPE_DIRECT, offset != 0, true);
            int length = byteLength / factory.getBytesPerElement();
            view = JSArrayBufferView.createArrayBufferView(context, realm, arrayBuffer, factory, array, offset, length);
        }
        return assignId(view);
    }

    private Object readObjectReference() {
        int id = readVarInt();
        Object object = objectMap.get(id);
        if (object == null) {
            throw Errors.createError("invalid object reference");
        }
        return object;
    }

    private Object readJSError(JSRealm realm) {
        JSErrorType errorType = JSErrorType.Error;
        Object message = Undefined.instance;
        Object stack = Undefined.instance;
        boolean done = false;
        while (!done) {
            ErrorTag tag = readErrorTag();
            if (tag == null) {
                break;
            }
            switch (tag) {
                case EVAL_ERROR:
                    errorType = JSErrorType.EvalError;
                    break;
                case RANGE_ERROR:
                    errorType = JSErrorType.RangeError;
                    break;
                case REFERENCE_ERROR:
                    errorType = JSErrorType.ReferenceError;
                    break;
                case SYNTAX_ERROR:
                    errorType = JSErrorType.SyntaxError;
                    break;
                case TYPE_ERROR:
                    errorType = JSErrorType.TypeError;
                    break;
                case URI_ERROR:
                    errorType = JSErrorType.URIError;
                    break;
                case MESSAGE:
                    message = readString();
                    break;
                case STACK:
                    stack = readString();
                    break;
                default:
                    assert (tag == ErrorTag.END);
                    done = true;
                    break;
            }
        }
        JSErrorObject error = JSError.create(errorType, realm, message);
        assignId(error);
        JSObject.set(error, JSError.STACK_NAME, stack);
        return error;
    }

    private Object readHostObject() {
        return assignId(NativeAccess.readHostObject(delegate));
    }

    public void transferArrayBuffer(int id, JSDynamicObject arrayBuffer) {
        transferMap.put(id, arrayBuffer);
    }

    public JSDynamicObject readTransferredJSArrayBuffer(JSContext context, JSRealm realm) {
        int id = readVarInt();
        JSDynamicObject arrayBuffer = transferMap.get(id);
        if (arrayBuffer == null) {
            throw Errors.createError("Invalid transfer id " + id);
        }
        assignId(arrayBuffer);
        return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(context, realm, (JSArrayBufferObject) arrayBuffer) : arrayBuffer;
    }

    public Object readSharedArrayBuffer(JSContext context, JSRealm realm) {
        int id = readVarInt();
        Object sharedArrayBuffer = NativeAccess.getSharedArrayBufferFromId(delegate, id);
        assert JSSharedArrayBuffer.isJSSharedArrayBuffer(sharedArrayBuffer);
        assignId(sharedArrayBuffer);
        return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(context, realm, (JSArrayBufferObject) sharedArrayBuffer) : sharedArrayBuffer;
    }

    public Object readWasmModuleTransfer() {
        int id = readVarInt();
        Object wasmModule = NativeAccess.getWasmModuleFromId(delegate, id);
        assert JSWebAssemblyModule.isJSWebAssemblyModule(wasmModule);
        return assignId(wasmModule);
    }

    public Object readWasmMemoryTransfer(JSRealm realm) {
        SerializationTag sharedJavaObjectTag = readTag();
        assert sharedJavaObjectTag == SerializationTag.SHARED_JAVA_OBJECT;
        Object wasmMemory = readSharedJavaObject();

        sharedJavaObjectTag = readTag();
        assert sharedJavaObjectTag == SerializationTag.SHARED_JAVA_OBJECT;
        JSAgentWaiterList waiterList = (JSAgentWaiterList) readSharedJavaObject();

        JSContext context = realm.getContext();
        JSWebAssemblyMemoryObject webAssemblyMemory = JSWebAssemblyMemory.create(context, realm, wasmMemory, true);
        JSArrayBufferObject arrayBuffer = webAssemblyMemory.getBufferObject(context, realm);
        JSSharedArrayBuffer.setWaiterList(arrayBuffer, waiterList);

        return assignId(webAssemblyMemory);
    }

    public Object readSharedJavaObject() {
        long messagePortPointer = readVarLong();
        if (messagePortCache == null || messagePortCache.getMessagePortDataPointer() != messagePortPointer) {
            messagePortCache = SharedMemMessagingManager.getMessagePortDataFor(messagePortPointer);
        }
        Object element = messagePortCache.removeJavaRef();
        assert element != null;
        return element;
    }

    public int readBytes(int length) {
        if (buffer.remaining() < length) {
            throw underflowError();
        }
        int position = buffer.position();
        buffer.position(position + length);
        return position;
    }

    private <T> T assignId(T object) {
        objectMap.put(nextId++, object);
        return object;
    }

    public int getWireFormatVersion() {
        return version;
    }

    private static JSException underflowError() {
        return Errors.createError("underflow");
    }

}

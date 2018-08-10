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
package com.oracle.truffle.trufflenode.serialization;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Integer, DynamicObject> transferMap = new HashMap<>();

    public Deserializer(long delegate, ByteBuffer buffer) {
        this.delegate = delegate;
        this.buffer = buffer.order(ByteOrder.nativeOrder());
    }

    public void readHeader() {
        if (buffer.get() == Serializer.VERSION) {
            version = buffer.get();
            if (version > Serializer.LATEST_VERSION) {
                throw Errors.createError("Unable to deserialize cloned data due to invalid or unsupported version.");
            }
        }
    }

    public Object readValue(JSContext context) {
        SerializationTag tag = readTag();
        return readValue(context, tag);
    }

    private Object readValue(JSContext context, SerializationTag tag) {
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
            case ONE_BYTE_STRING:
                return readOneByteString();
            case TWO_BYTE_STRING:
                return readTwoByteString();
            case UTF8_STRING:
                return readUTF8String();
            case DATE:
                return readDate(context);
            case TRUE_OBJECT:
                return assignId(JSBoolean.create(context, true));
            case FALSE_OBJECT:
                return assignId(JSBoolean.create(context, false));
            case NUMBER_OBJECT:
                return readJSNumber(context);
            case STRING_OBJECT:
                return readJSString(context);
            case REGEXP:
                return readJSRegExp(context);
            case ARRAY_BUFFER:
                return readJSArrayBuffer(context);
            case ARRAY_BUFFER_TRANSFER:
                return readTransferredJSArrayBuffer(context);
            case SHARED_ARRAY_BUFFER:
                return readTransferredJSArrayBuffer(context);
            case BEGIN_JS_OBJECT:
                return readJSObject(context);
            case BEGIN_JS_MAP:
                return readJSMap(context);
            case BEGIN_JS_SET:
                return readJSSet(context);
            case BEGIN_DENSE_JS_ARRAY:
                return readDenseArray(context);
            case BEGIN_SPARSE_JS_ARRAY:
                return readSparseArray(context);
            case OBJECT_REFERENCE:
                return readObjectReference();
            case HOST_OBJECT:
                return readHostObject();
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
            b = buffer.get();
            value |= (b & 0x7fL) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    public double readDouble() {
        return buffer.getDouble();
    }

    private String readString() {
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

    private String readOneByteString() {
        int charCount = readVarInt();
        char[] chars = new char[charCount];
        for (int i = 0; i < charCount; i++) {
            byte b = buffer.get();
            chars[i] = (char) (b & 0xff);
        }
        return new String(chars);
    }

    private String readTwoByteString() {
        return readString(Serializer.NATIVE_UTF16_ENCODING);
    }

    private String readUTF8String() {
        return readString("UTF-8");
    }

    private String readString(String encoding) {
        int byteCount = readVarInt();
        byte[] bytes = new byte[byteCount];
        buffer.get(bytes);
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException ueex) {
            throw Errors.shouldNotReachHere();
        }
    }

    private DynamicObject readDate(JSContext context) {
        double millis = readDouble();
        return assignId(JSDate.create(context, millis));
    }

    private DynamicObject readJSNumber(JSContext context) {
        double value = readDouble();
        return assignId(JSNumber.create(context, value));
    }

    private DynamicObject readJSString(JSContext context) {
        String value = readString();
        return assignId(JSString.create(context, value));
    }

    private Object readJSRegExp(JSContext context) {
        String pattern = readString();
        int flags = readVarInt();
        return assignId(GraalJSAccess.regexpCreate(context, pattern, flags));
    }

    private DynamicObject readJSArrayBuffer(JSContext context) {
        int byteLength = readVarInt();
        DynamicObject arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(context, byteLength);
        ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        for (int i = 0; i < byteLength; i++) {
            byteBuffer.put(i, buffer.get());
        }
        assignId(arrayBuffer);
        return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(context, arrayBuffer) : arrayBuffer;
    }

    private DynamicObject readJSObject(JSContext context) {
        DynamicObject object = JSUserObject.create(context);
        assignId(object);
        int read = readJSObjectProperties(context, object, SerializationTag.END_JS_OBJECT);
        int expected = readVarInt();
        if (read != expected) {
            throw Errors.createError("unexpected number of properties");
        }
        return object;
    }

    private int readJSObjectProperties(JSContext context, DynamicObject object, SerializationTag endTag) {
        SerializationTag tag;
        int count = 0;
        while ((tag = readTag()) != endTag) {
            count++;
            Object key = readValue(context, tag);
            Object value = readValue(context);
            JSObject.defineOwnProperty(object, JSRuntime.toPropertyKey(key), PropertyDescriptor.createDataDefault(value));
        }
        return count;
    }

    private DynamicObject readJSMap(JSContext context) {
        DynamicObject object = JSMap.create(context);
        JSHashMap internalMap = JSMap.getInternalMap(object);
        assignId(object);
        SerializationTag tag;
        int read = 0;
        while ((tag = readTag()) != SerializationTag.END_JS_MAP) {
            read++;
            Object key = readValue(context, tag);
            Object value = readValue(context);
            internalMap.put(key, value);
        }
        int expected = readVarInt();
        if (2 * read != expected) {
            throw Errors.createError("unexpected number of entries");
        }
        return object;
    }

    private DynamicObject readJSSet(JSContext context) {
        DynamicObject object = JSSet.create(context);
        JSHashMap internalMap = JSSet.getInternalSet(object);
        assignId(object);
        SerializationTag tag;
        int read = 0;
        while ((tag = readTag()) != SerializationTag.END_JS_SET) {
            read++;
            Object value = readValue(context, tag);
            internalMap.put(value, value);
        }
        int expected = readVarInt();
        if (read != expected) {
            throw Errors.createError("unexpected number of values");
        }
        return object;
    }

    private DynamicObject readDenseArray(JSContext context) {
        int length = readVarInt();
        Object[] elements = new Object[length];
        DynamicObject array = JSArray.createConstantObjectArray(context, elements);
        assignId(array);
        List<Integer> holes = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            SerializationTag tag = readTag();
            if (tag == SerializationTag.THE_HOLE) {
                holes.add(i);
            } else {
                elements[i] = readValue(context, tag);
            }
        }
        for (int hole : holes) {
            JSObject.delete(array, hole);
        }
        int read = readJSObjectProperties(context, array, SerializationTag.END_DENSE_JS_ARRAY);
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

    private DynamicObject readSparseArray(JSContext context) {
        long length = readVarLong();
        DynamicObject array = JSArray.createSparseArray(context, length);
        assignId(array);
        int read = readJSObjectProperties(context, array, SerializationTag.END_SPARSE_JS_ARRAY);
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

    private DynamicObject readJSArrayBufferView(JSContext context, DynamicObject arrayBuffer) {
        assert JSArrayBuffer.isJSDirectOrSharedArrayBuffer(arrayBuffer);
        SerializationTag arrayBufferViewTag = readTag();
        assert arrayBufferViewTag == SerializationTag.ARRAY_BUFFER_VIEW;
        ArrayBufferViewTag tag = readArrayBufferViewTag();
        int offset = readVarInt();
        int byteLength = readVarInt();
        DynamicObject view;
        if (tag == ArrayBufferViewTag.DATA_VIEW) {
            view = JSDataView.createDataView(context, arrayBuffer, offset, byteLength);
        } else {
            TypedArrayFactory factory = tag.getFactory();
            TypedArray array = factory.createArrayType(true, offset != 0);
            int length = byteLength / factory.bytesPerElement();
            view = JSArrayBufferView.createArrayBufferView(context, arrayBuffer, array, offset, length);
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

    private Object readHostObject() {
        return assignId(NativeAccess.readHostObject(delegate));
    }

    public void transferArrayBuffer(int id, DynamicObject arrayBuffer) {
        transferMap.put(id, arrayBuffer);
    }

    public DynamicObject readTransferredJSArrayBuffer(JSContext context) {
        int id = readVarInt();
        DynamicObject arrayBuffer = transferMap.get(id);
        if (arrayBuffer == null) {
            throw Errors.createError("Invalid transfer id " + id);
        }
        assignId(arrayBuffer);
        return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(context, arrayBuffer) : arrayBuffer;
    }

    public int readBytes(int length) {
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

}

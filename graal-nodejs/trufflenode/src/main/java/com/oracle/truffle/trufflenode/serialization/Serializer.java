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
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@code v8::(internal::)ValueSerializer}.
 */
public class Serializer {
    static final byte VERSION = (byte) 0xFF; // SerializationTag::kVersion
    static final byte LATEST_VERSION = (byte) 13; // kLatestVersion
    static final String NATIVE_UTF16_ENCODING = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? "UTF-16BE" : "UTF-16LE";

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

    public Serializer(long delegate) {
        this.delegate = delegate;
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
        } else if (JSRuntime.isString(value)) {
            writeString(JSRuntime.toString(value));
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
            DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer((DynamicObject) object);
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
            writeDate((DynamicObject) object);
        } else if (JSBoolean.isJSBoolean(object)) {
            writeJSBoolean((DynamicObject) object);
        } else if (JSNumber.isJSNumber(object)) {
            writeJSNumber((DynamicObject) object);
        } else if (JSString.isJSString(object)) {
            writeJSString((DynamicObject) object);
        } else if (JSRegExp.isJSRegExp(object)) {
            writeJSRegExp((DynamicObject) object);
        } else if (JSArrayBuffer.isJSDirectArrayBuffer(object)) {
            writeJSArrayBuffer((DynamicObject) object);
        } else if (JSSharedArrayBuffer.isJSSharedArrayBuffer(object)) {
            writeJSSharedArrayBuffer((DynamicObject) object);
        } else if (JSMap.isJSMap(object)) {
            writeJSMap((DynamicObject) object);
        } else if (JSSet.isJSSet(object)) {
            writeJSSet((DynamicObject) object);
        } else if (JSArray.isJSArray(object)) {
            writeJSArray((DynamicObject) object);
        } else if (JSArrayBufferView.isJSArrayBufferView(object)) {
            writeJSArrayBufferView((DynamicObject) object);
        } else if (JSDataView.isJSDataView(object)) {
            writeJSDataView((DynamicObject) object);
        } else if (JSProxy.isProxy(object)) {
            boolean callable = JSRuntime.isCallableProxy((DynamicObject) object);
            String message = (callable ? "[object Function]" : "[object Object]") + " could not be cloned.";
            NativeAccess.throwDataCloneError(delegate, message);
        } else if (JSFunction.isJSFunction(object)) {
            NativeAccess.throwDataCloneError(delegate, JSRuntime.safeToString(object) + " could not be cloned");
        } else if (JSObject.isJSObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            if (GraalJSAccess.internalFieldCount(dynamicObject) == 0) {
                writeJSObject(dynamicObject);
            } else {
                writeHostObject(dynamicObject);
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
            b |= 0x80;
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

    private void writeString(String string) {
        try {
            byte[] bytes;
            SerializationTag tag;
            String encoding;
            if (isOneByteString(string)) {
                tag = SerializationTag.ONE_BYTE_STRING;
                encoding = "ISO-8859-1";
            } else {
                tag = SerializationTag.TWO_BYTE_STRING;
                encoding = NATIVE_UTF16_ENCODING;
            }
            writeTag(tag);
            bytes = string.getBytes(encoding);
            writeVarInt(bytes.length);
            writeBytes(bytes, bytes.length);
        } catch (UnsupportedEncodingException ueex) {
            throw Errors.shouldNotReachHere();
        }
    }

    private static boolean isOneByteString(String string) {
        for (char c : string.toCharArray()) {
            if (c >= 256) {
                return false;
            }
        }
        return true;
    }

    private void writeDate(DynamicObject date) {
        assert JSDate.isJSDate(date);
        writeDouble(JSDate.getTimeMillisField(date));
    }

    private void writeJSBoolean(DynamicObject bool) {
        assert JSBoolean.isJSBoolean(bool);
        writeTag(JSBoolean.valueOf(bool) ? SerializationTag.TRUE_OBJECT : SerializationTag.FALSE_OBJECT);
    }

    private void writeJSNumber(DynamicObject number) {
        assert JSNumber.isJSNumber(number);
        double value = JSNumber.valueOf(number).doubleValue();
        writeTag(SerializationTag.NUMBER_OBJECT);
        writeDouble(value);
    }

    private void writeJSString(DynamicObject string) {
        assert JSString.isJSString(string);
        String value = JSString.getString(string);
        writeTag(SerializationTag.STRING_OBJECT);
        writeString(value);
    }

    private void writeJSRegExp(DynamicObject regExp) {
        assert JSRegExp.isJSRegExp(regExp);
        String pattern = GraalJSAccess.regexpPattern(regExp);
        int flags = GraalJSAccess.regexpV8Flags(regExp);
        writeTag(SerializationTag.REGEXP);
        writeString(pattern);
        writeVarInt(flags);
    }

    private void writeJSArrayBuffer(DynamicObject arrayBuffer) {
        assert JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer);
        Integer id = transferMap.get(arrayBuffer);
        if (id == null) {
            int byteLength = JSArrayBuffer.getDirectByteLength(arrayBuffer);
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

    private void writeJSSharedArrayBuffer(DynamicObject sharedArrayBuffer) {
        int id = NativeAccess.getSharedArrayBufferId(delegate, sharedArrayBuffer);
        writeTag(SerializationTag.SHARED_ARRAY_BUFFER);
        writeVarInt(id);
    }

    private void writeJSObject(DynamicObject object) {
        assert JSObject.isJSObject(object);
        writeTag(SerializationTag.BEGIN_JS_OBJECT);
        List<String> names = JSObject.enumerableOwnNames(object);
        writeJSObjectProperties(object, names);
        writeTag(SerializationTag.END_JS_OBJECT);
        writeVarInt(names.size());
    }

    private void writeJSObjectProperties(DynamicObject object, List<String> keys) {
        assert JSObject.isJSObject(object);
        for (String key : keys) {
            if (JSRuntime.isArrayIndex(key)) {
                writeIntOrDouble(Double.parseDouble(key));
            } else {
                writeString(key);
            }
            Object value = JSObject.get(object, key);
            writeValue(value);
        }
    }

    private void writeJSMap(DynamicObject object) {
        assert JSMap.isJSMap(object);
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

    private void writeJSSet(DynamicObject object) {
        assert JSSet.isJSSet(object);
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

    private void writeJSArray(DynamicObject object) {
        assert JSArray.isJSArray(object);
        long length = JSAbstractArray.arrayGetLength(object);
        List<String> names = JSObject.enumerableOwnNames(object);
        boolean dense = names.size() >= length;
        if (dense) {
            for (int i = 0; i < length; i++) {
                if (!Integer.toString(i).equals(names.get(i))) {
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

    private void writeJSArrayBufferView(DynamicObject view) {
        if (treatArrayBufferViewsAsHostObjects) {
            writeHostObject(view);
        } else {
            int offset = JSArrayBufferView.typedArrayGetOffset(view);
            TypedArray typedArray = JSArrayBufferView.typedArrayGetArrayType(view);
            int length = typedArray.lengthInt(view) * typedArray.bytesPerElement();
            ArrayBufferViewTag tag = ArrayBufferViewTag.fromFactory(typedArray.getFactory());
            writeJSArrayBufferView(tag, offset, length);
        }
    }

    private void writeJSDataView(DynamicObject view) {
        if (treatArrayBufferViewsAsHostObjects) {
            writeTag(SerializationTag.HOST_OBJECT);
            NativeAccess.writeHostObject(delegate, view);
        } else {
            int offset = JSDataView.typedArrayGetOffset(view);
            int length = JSDataView.typedArrayGetLength(view);
            writeJSArrayBufferView(ArrayBufferViewTag.DATA_VIEW, offset, length);
        }
    }

    private void writeJSArrayBufferView(ArrayBufferViewTag tag, int offset, int length) {
        writeTag(SerializationTag.ARRAY_BUFFER_VIEW);
        writeTag(tag);
        writeVarInt(offset);
        writeVarInt(length);
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
    }

    private void assignId(Object object) {
        objectMap.put(object, nextId++);
    }

}

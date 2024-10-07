/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferViewBase;
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
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapObject;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSetObject;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSStringObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemoryObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.graalvm.collections.EconomicSet;

public class SerializedData {
    private final List<Object> data = new ArrayList<>();
    private int nextId;
    private Map<Object, Integer> memory;

    public SerializedData(Object value) {
        this(value, null);
    }

    public SerializedData(Object value, EconomicSet<JSArrayBufferObject> transferSet) {
        memory = new IdentityHashMap<>();

        int transferableCount = (transferSet == null) ? 0 : transferSet.size();
        data.add(transferableCount);
        if (transferableCount != 0) {
            for (JSArrayBufferObject transferable : transferSet) {
                if (transferable instanceof JSArrayBufferObject.Shared) {
                    throw Errors.createError("Cannot transfer SharedArrayBuffer");
                }
                serializeTransferable(transferable);
            }
        }

        serializeValue(value);

        if (transferableCount != 0) {
            for (JSArrayBufferObject transferable : transferSet) {
                JSArrayBuffer.detachArrayBuffer(transferable);
            }
        }

        memory = null;
    }

    private void serializeValue(Object value) {
        if (value == Undefined.instance || value == Null.instance || value instanceof Boolean || value instanceof Integer || value instanceof Double || value instanceof Long ||
                        value instanceof BigInt || value instanceof TruffleString) {
            data.add(Type.Primitive);
            data.add(value);
            return;
        } else if (value instanceof Symbol) {
            throw couldNotBeClonedError(value);
        }

        Integer id = memory.get(value);
        if (id != null) {
            data.add(Type.Duplicate);
            data.add(id);
            return;
        }

        assignId(value);
        if (value instanceof JSBooleanObject booleanObject) {
            data.add(Type.Boolean);
            data.add(booleanObject.getBooleanValue());
        } else if (value instanceof JSNumberObject numberObject) {
            data.add(Type.Number);
            data.add(numberObject.getNumber());
        } else if (value instanceof JSBigIntObject bigIntObject) {
            data.add(Type.BigInt);
            data.add(bigIntObject.getBigIntValue());
        } else if (value instanceof JSStringObject stringObject) {
            data.add(Type.String);
            data.add(stringObject.getString());
        } else if (value instanceof JSDateObject dateObject) {
            data.add(Type.Date);
            data.add(dateObject.getTimeMillis());
        } else if (value instanceof JSRegExpObject regExpObject) {
            serializeRegExp(regExpObject);
        } else if (value instanceof JSArrayBufferObject.Shared sharedArrayBuffer) {
            serializeSharedArrayBuffer(sharedArrayBuffer);
        } else if (value instanceof JSArrayBufferObject arrayBuffer) {
            serializeArrayBuffer(arrayBuffer);
        } else if (value instanceof JSTypedArrayObject typedArray) {
            serializeArrayBufferView(typedArray);
        } else if (value instanceof JSDataViewObject dataView) {
            serializeDataView(dataView);
        } else if (value instanceof JSMapObject mapObject) {
            serializeMap(mapObject);
        } else if (value instanceof JSSetObject setObject) {
            serializeSet(setObject);
        } else if (value instanceof JSErrorObject errorObject) {
            serializeError(errorObject);
        } else if (value instanceof JSArrayObject arrayObject) {
            data.add(Type.Array);
            data.add(JSAbstractArray.arrayGetLength(arrayObject));
            serializeProperties(arrayObject);
        } else if (value instanceof JSWebAssemblyMemoryObject memoryObject) {
            serializeWebAssemblyMemory(memoryObject);
        } else if (value instanceof JSWebAssemblyModuleObject moduleObject) {
            serializeWebAssemblyModule(moduleObject);
        } else if (value instanceof JSObject object) {
            data.add(Type.Object);
            serializeProperties(object);
        } else {
            throw couldNotBeClonedError(value);
        }
    }

    private static JSException couldNotBeClonedError(Object value) {
        return Errors.createError(JSRuntime.safeToString(value) + " could not be cloned.");
    }

    private void assignId(Object value) {
        memory.put(value, nextId++);
    }

    private void serializeTransferable(JSArrayBufferObject arrayBuffer) {
        assignId(arrayBuffer);
        Object content;
        if (arrayBuffer instanceof JSArrayBufferObject.Heap heapBuffer) {
            content = heapBuffer.getByteArray();
        } else if (arrayBuffer instanceof JSArrayBufferObject.Direct directBuffer) {
            content = directBuffer.getByteBuffer();
        } else {
            content = ((JSArrayBufferObject.Interop) arrayBuffer).getInteropBuffer();
        }
        serializeArrayBufferImpl(content, arrayBuffer.getByteLength(), arrayBuffer.getMaxByteLength());
    }

    private void serializeRegExp(JSRegExpObject regExp) {
        Object compiledRegex = regExp.getCompiledRegex();
        TruffleString pattern = TRegexUtil.InteropReadStringMemberNode.getUncached().execute(null, compiledRegex, TRegexUtil.Props.CompiledRegex.PATTERN);
        Object flagsObject = TRegexUtil.InteropReadMemberNode.getUncached().execute(null, compiledRegex, TRegexUtil.Props.CompiledRegex.FLAGS);
        TruffleString flags = TRegexUtil.InteropReadStringMemberNode.getUncached().execute(null, flagsObject, TRegexUtil.Props.Flags.SOURCE);
        data.add(Type.RegExp);
        data.add(pattern);
        data.add(flags);
    }

    private void serializeSharedArrayBuffer(JSArrayBufferObject.Shared sharedArrayBuffer) {
        data.add(Type.SharedArrayBuffer);
        data.add(sharedArrayBuffer.getByteBuffer());
        data.add(sharedArrayBuffer.getByteLengthObject());
        data.add(sharedArrayBuffer.getMaxByteLength());
        data.add(sharedArrayBuffer.getWaiterList());
    }

    private void serializeArrayBuffer(JSArrayBufferObject arrayBuffer) {
        Object content;
        if (arrayBuffer instanceof JSArrayBufferObject.Heap heapBuffer) {
            byte[] original = heapBuffer.getByteArray();
            content = Arrays.copyOf(original, original.length);
        } else if (arrayBuffer instanceof JSArrayBufferObject.Direct directBuffer) {
            ByteBuffer original = directBuffer.getByteBuffer();
            ByteBuffer copy = ByteBuffer.allocateDirect(original.capacity());
            copy.put(original.duplicate().rewind());
            content = copy;
        } else {
            JSArrayBufferObject.Interop interopBuffer = (JSArrayBufferObject.Interop) arrayBuffer;
            Object foreignBuffer = interopBuffer.getInteropBuffer();
            int length = interopBuffer.getByteLength();
            byte[] array = new byte[length];
            try {
                InteropLibrary.getUncached().readBuffer(foreignBuffer, 0, array, 0, length);
                if (arrayBuffer.getJSContext().isOptionDirectByteBuffer()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(length);
                    byteBuffer.put(array);
                    content = byteBuffer;
                } else {
                    content = array;
                }
            } catch (InteropException iex) {
                throw couldNotBeClonedError(interopBuffer);
            }
        }
        serializeArrayBufferImpl(content, arrayBuffer.getByteLength(), arrayBuffer.getMaxByteLength());
    }

    private void serializeArrayBufferImpl(Object content, int byteLength, int maxByteLength) {
        data.add(Type.ArrayBuffer);
        data.add(content);
        data.add(byteLength);
        data.add(maxByteLength);
    }

    private void serializeArrayBufferView(JSTypedArrayObject arrayBufferView) {
        if (JSArrayBufferView.isOutOfBounds(arrayBufferView, arrayBufferView.getJSContext())) {
            throw couldNotBeClonedError(arrayBufferView);
        }
        data.add(Type.ArrayBufferView);
        data.add(arrayBufferView.getArrayType());
        data.add(arrayBufferView.hasAutoLength() ? JSArrayBufferViewBase.AUTO_LENGTH : arrayBufferView.getLength());
        data.add(arrayBufferView.getByteOffset());
        serializeValue(arrayBufferView.getArrayBuffer());
    }

    private void serializeDataView(JSDataViewObject dataView) {
        data.add(Type.DataView);
        data.add(dataView.hasAutoLength() ? JSArrayBufferViewBase.AUTO_LENGTH : dataView.getByteLength());
        data.add(dataView.getByteOffset());
        serializeValue(dataView.getArrayBuffer());
    }

    private void serializeMap(JSMapObject mapObject) {
        data.add(Type.Map);
        JSHashMap map = mapObject.getMap();
        data.add(map.size());
        JSHashMap.Cursor cursor = map.getEntries();
        while (cursor.advance()) {
            serializeValue(cursor.getKey());
            serializeValue(cursor.getValue());
        }
    }

    private void serializeSet(JSSetObject setObject) {
        data.add(Type.Set);
        JSHashMap map = setObject.getMap();
        data.add(map.size());
        JSHashMap.Cursor cursor = map.getEntries();
        while (cursor.advance()) {
            serializeValue(cursor.getKey());
        }
    }

    private void serializeError(JSErrorObject errorObject) {
        Throwable exception = errorObject.getException();
        JSErrorType type = JSErrorType.Error;
        if (exception instanceof JSException jsException) {
            type = jsException.getErrorType();
        }

        Object message;
        PropertyDescriptor desc = JSObject.getOwnProperty(errorObject, Strings.MESSAGE);
        if (desc != null && desc.isDataDescriptor()) {
            message = JSRuntime.toString(desc.getValue());
        } else {
            message = Undefined.instance;
        }

        Object stack = JSObject.get(errorObject, JSError.STACK_NAME);

        Object cause;
        desc = JSObject.getOwnProperty(errorObject, Strings.CAUSE);
        if (desc != null && desc.isDataDescriptor()) {
            cause = desc.getValue();
        } else {
            cause = Undefined.instance;
        }
        data.add(Type.Error);
        data.add(type);
        data.add(message);
        data.add(stack instanceof TruffleString ? stack : Undefined.instance);
        data.add(cause);
    }

    private void serializeWebAssemblyMemory(JSWebAssemblyMemoryObject memoryObject) {
        if (memoryObject.isShared()) {
            Object wasmMemory = memoryObject.getWASMMemory();
            JSArrayBufferObject arrayBuffer = memoryObject.getBufferObject(memoryObject.getJSContext(), JSRealm.get(null));
            JSAgentWaiterList waiterList = JSSharedArrayBuffer.getWaiterList(arrayBuffer);
            data.add(Type.WebAssemblyMemory);
            data.add(wasmMemory);
            data.add(waiterList);
        } else {
            throw couldNotBeClonedError(memoryObject);
        }
    }

    private void serializeWebAssemblyModule(JSWebAssemblyModuleObject moduleObject) {
        data.add(Type.WebAssemblyModule);
        data.add(moduleObject.getWASMModule());
    }

    private void serializeProperties(JSObject object) {
        List<TruffleString> names = JSObject.enumerableOwnNames(object);
        data.add(names.size());
        for (TruffleString key : names) {
            Object value = JSObject.get(object, key);
            serializeValue(key);
            serializeValue(value);
        }
    }

    public Object deserialize(JSRealm realm) {
        Iterator<Object> iter = data.iterator();
        List<Object> deserialized = new ArrayList<>();
        int transferableCount = (int) iter.next();
        for (int i = 0; i < transferableCount; i++) {
            deserializeValue(realm, iter, deserialized);
        }
        return deserializeValue(realm, iter, deserialized);
    }

    private static Object deserializeValue(JSRealm realm, Iterator<Object> iter, List<Object> deserialized) {
        JSContext context = realm.getContext();
        Type type = (Type) iter.next();
        Object result = switch (type) {
            case Primitive -> iter.next();
            case Duplicate -> deserialized.get((Integer) iter.next());
            case Boolean -> JSBoolean.create(context, realm, (Boolean) iter.next());
            case Number -> JSNumber.create(context, realm, (Number) iter.next());
            case BigInt -> JSBigInt.create(context, realm, (BigInt) iter.next());
            case String -> JSString.create(context, realm, (TruffleString) iter.next());
            case Date -> JSDate.create(context, realm, (Double) iter.next());
            case RegExp -> deserializeRegExp(realm, iter);
            case SharedArrayBuffer -> deserializeSharedArrayBuffer(realm, iter);
            case ArrayBuffer -> deserializeArrayBuffer(realm, iter);
            case ArrayBufferView -> deserializeArrayBufferView(realm, iter, deserialized);
            case DataView -> deserializeDataView(realm, iter, deserialized);
            case Map -> deserializeMap(realm, iter, deserialized);
            case Set -> deserializeSet(realm, iter, deserialized);
            case Error -> deserializeError(realm, iter);
            case Array -> deserializeArray(realm, iter, deserialized);
            case Object -> deserializeObject(realm, iter, deserialized);
            case WebAssemblyMemory -> deserializeWebAssemblyMemory(realm, iter);
            case WebAssemblyModule -> deserializeWebAssemblyModule(realm, iter);
        };
        if (type != Type.Primitive && type != Type.Duplicate && type != Type.Map && type != Type.Set && type != Type.Array && type != Type.Object) {
            deserialized.add(result);
        }
        return result;
    }

    private static Object deserializeRegExp(JSRealm realm, Iterator<Object> iter) {
        TruffleString pattern = (TruffleString) iter.next();
        TruffleString flags = (TruffleString) iter.next();
        JSContext context = realm.getContext();
        Object compiledRegexp = RegexCompilerInterface.compile(pattern, flags, context, realm);
        return JSRegExp.create(context, realm, compiledRegexp);
    }

    private static Object deserializeSharedArrayBuffer(JSRealm realm, Iterator<Object> iter) {
        ByteBuffer byteBuffer = (ByteBuffer) iter.next();
        AtomicInteger byteLength = (AtomicInteger) iter.next();
        int maxByteLength = (int) iter.next();
        JSAgentWaiterList waiterList = (JSAgentWaiterList) iter.next();
        JSArrayBufferObject sharedArrayBuffer = JSSharedArrayBuffer.createSharedArrayBuffer(realm.getContext(), realm, byteBuffer, byteLength, maxByteLength);
        ((JSArrayBufferObject.Shared) sharedArrayBuffer).setWaiterList(waiterList);
        return sharedArrayBuffer;
    }

    private static JSArrayBufferObject deserializeArrayBuffer(JSRealm realm, Iterator<Object> iter) {
        JSContext context = realm.getContext();
        Object content = iter.next();
        int byteLength = (int) iter.next();
        int maxByteLength = (int) iter.next();
        if (content instanceof byte[] || content == null) {
            return JSArrayBuffer.createArrayBuffer(context, realm, (byte[]) content, byteLength, maxByteLength);
        } else if (content instanceof ByteBuffer buffer) {
            return JSArrayBuffer.createDirectArrayBuffer(context, realm, buffer, byteLength, maxByteLength);
        } else {
            return JSArrayBuffer.createInteropArrayBuffer(context, realm, content);
        }
    }

    private static Object deserializeArrayBufferView(JSRealm realm, Iterator<Object> iter, List<Object> deserialized) {
        TypedArray arrayType = (TypedArray) iter.next();
        int length = (int) iter.next();
        int byteOffset = (int) iter.next();
        JSArrayBufferObject arrayBuffer = (JSArrayBufferObject) deserializeValue(realm, iter, deserialized);
        return JSArrayBufferView.createArrayBufferView(realm.getContext(), realm, arrayBuffer, arrayType.getFactory(), arrayType, byteOffset, length);
    }

    private static Object deserializeDataView(JSRealm realm, Iterator<Object> iter, List<Object> deserialized) {
        int byteLength = (int) iter.next();
        int byteOffset = (int) iter.next();
        JSArrayBufferObject arrayBuffer = (JSArrayBufferObject) deserializeValue(realm, iter, deserialized);
        return JSDataView.createDataView(realm.getContext(), realm, arrayBuffer, byteOffset, byteLength);
    }

    private static Object deserializeMap(JSRealm realm, Iterator<Object> iter, List<Object> deserialized) {
        JSMapObject mapObject = JSMap.create(realm.getContext(), realm);
        deserialized.add(mapObject);
        JSHashMap map = mapObject.getMap();
        int size = (int) iter.next();
        for (int i = 0; i < size; i++) {
            Object key = deserializeValue(realm, iter, deserialized);
            Object value = deserializeValue(realm, iter, deserialized);
            map.put(key, value);
        }
        return mapObject;
    }

    private static Object deserializeSet(JSRealm realm, Iterator<Object> iter, List<Object> deserialized) {
        JSSetObject setObject = JSSet.create(realm.getContext(), realm);
        deserialized.add(setObject);
        JSHashMap map = setObject.getMap();
        int size = (int) iter.next();
        for (int i = 0; i < size; i++) {
            Object value = deserializeValue(realm, iter, deserialized);
            map.put(value, value);
        }
        return setObject;
    }

    private static Object deserializeError(JSRealm realm, Iterator<Object> iter) {
        JSErrorType type = (JSErrorType) iter.next();
        Object message = iter.next();
        Object stack = iter.next();
        Object cause = iter.next();
        JSErrorObject errorObject = JSError.create(type, realm, message);
        JSObject.set(errorObject, JSError.STACK_NAME, stack);
        if (cause != Undefined.instance) {
            JSObject.defineOwnProperty(errorObject, Strings.CAUSE, PropertyDescriptor.createData(cause, JSAttributes.getDefaultNotEnumerable()));
        }
        return errorObject;
    }

    private static Object deserializeArray(JSRealm realm, Iterator<Object> iter, List<Object> deserialized) {
        long length = (long) iter.next();
        JSArrayObject array = JSArray.createEmptyChecked(realm.getContext(), realm, length);
        deserialized.add(array);
        deserializeProperties(realm, iter, array, deserialized);
        return array;
    }

    private static Object deserializeObject(JSRealm realm, Iterator<Object> iter, List<Object> deserialized) {
        JSObject object = JSOrdinary.create(realm.getContext(), realm);
        deserialized.add(object);
        deserializeProperties(realm, iter, object, deserialized);
        return object;
    }

    private static void deserializeProperties(JSRealm realm, Iterator<Object> iter, JSObject object, List<Object> deserialized) {
        int count = (int) iter.next();
        for (int i = 0; i < count; i++) {
            Object key = deserializeValue(realm, iter, deserialized);
            Object value = deserializeValue(realm, iter, deserialized);
            JSObject.set(object, key, value);
        }
    }

    private static Object deserializeWebAssemblyMemory(JSRealm realm, Iterator<Object> iter) {
        Object wasmMemory = iter.next();
        JSAgentWaiterList waiterList = (JSAgentWaiterList) iter.next();
        JSContext context = realm.getContext();
        JSWebAssemblyMemoryObject webAssemblyMemory = JSWebAssemblyMemory.create(context, realm, wasmMemory, true);
        JSArrayBufferObject arrayBuffer = webAssemblyMemory.getBufferObject(context, realm);
        JSSharedArrayBuffer.setWaiterList(arrayBuffer, waiterList);
        return webAssemblyMemory;
    }

    private static Object deserializeWebAssemblyModule(JSRealm realm, Iterator<Object> iter) {
        Object wasmModule = iter.next();
        return JSWebAssemblyModule.create(realm.getContext(), realm, wasmModule);
    }

    private enum Type {
        Primitive,
        Duplicate,
        Boolean,
        Number,
        BigInt,
        String,
        Date,
        RegExp,
        SharedArrayBuffer,
        ArrayBuffer,
        ArrayBufferView,
        DataView,
        Map,
        Set,
        Error,
        Array,
        Object,
        WebAssemblyMemory,
        WebAssemblyModule
    }

}

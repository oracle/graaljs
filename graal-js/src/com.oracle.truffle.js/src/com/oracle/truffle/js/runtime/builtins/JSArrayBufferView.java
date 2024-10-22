/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.TypedArrayFunctionBuiltins;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

public final class JSArrayBufferView extends JSNonProxy {
    public static final TruffleString CLASS_NAME = Strings.constant("TypedArray");
    public static final TruffleString PROTOTYPE_NAME = Strings.concat(CLASS_NAME, Strings.DOT_PROTOTYPE);

    private static final TruffleString BYTES_PER_ELEMENT = Strings.constant("BYTES_PER_ELEMENT");

    public static final JSArrayBufferView INSTANCE = new JSArrayBufferView();

    public static TypedArray typedArrayGetArrayType(JSDynamicObject thisObj) {
        return ((JSTypedArrayObject) thisObj).getArrayType();
    }

    public static int typedArrayGetLength(JSDynamicObject thisObj) {
        return ((JSTypedArrayObject) thisObj).getLength();
    }

    public static int typedArrayGetByteOffset(JSDynamicObject thisObj) {
        return ((JSTypedArrayObject) thisObj).getByteOffset();
    }

    public static TruffleString typedArrayGetName(JSDynamicObject thisObj) {
        return typedArrayGetArrayType(thisObj).getName();
    }

    private JSArrayBufferView() {
    }

    public static JSArrayBufferObject getArrayBuffer(JSDynamicObject thisObj) {
        return ((JSTypedArrayObject) thisObj).getArrayBuffer();
    }

    public static int getByteLength(JSDynamicObject store, JSContext ctx) {
        if (JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) store, ctx)) {
            return 0;
        }
        TypedArray typedArray = typedArrayGetArrayType(store);
        return typedArray.lengthInt(store) * typedArray.bytesPerElement();
    }

    public static int getByteOffset(JSDynamicObject store, JSContext ctx) {
        if (JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) store, ctx)) {
            return 0;
        }
        return typedArrayGetByteOffset(store);
    }

    @TruffleBoundary
    @Override
    public Object getHelper(JSDynamicObject store, Object receiver, long index, Node encapsulatingNode) {
        return getOwnHelper(store, receiver, index, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object receiver, long index, Node encapsulatingNode) {
        if (JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) store, store.getJSContext())) {
            return Undefined.instance;
        }
        return typedArrayGetArrayType(store).getElement(store, index);
    }

    /**
     * 9.4.5.4 [[Get]] for Integer Indexed exotic object.
     */
    @TruffleBoundary
    @Override
    public Object getHelper(JSDynamicObject store, Object receiver, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(name);
            if (numericIndex != Undefined.instance) {
                return integerIndexedElementGet(store, numericIndex);
            }
        }
        return super.getHelper(store, receiver, key, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object receiver, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(name);
            if (numericIndex != Undefined.instance) {
                return integerIndexedElementGet(store, numericIndex);
            }
        }
        return super.getOwnHelper(store, receiver, key, encapsulatingNode);
    }

    @TruffleBoundary
    private static Object integerIndexedElementGet(JSDynamicObject thisObj, Object numericIndex) {
        assert JSRuntime.isNumber(numericIndex);
        if (JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) thisObj, thisObj.getJSContext())) {
            return Undefined.instance;
        }
        if (!JSRuntime.isInteger(numericIndex)) {
            return Undefined.instance;
        }
        if (numericIndex instanceof Double && JSRuntime.isNegativeZero(((Double) numericIndex).doubleValue())) {
            return Undefined.instance;
        }
        long index = ((Number) numericIndex).longValue();
        int length = ((JSTypedArrayObject) thisObj).getLength();
        if (index < 0 || index >= length) {
            return Undefined.instance;
        }
        return JSArrayBufferView.typedArrayGetArrayType(thisObj).getElement(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (thisObj == receiver) {
            Object numValue = convertValue(thisObj, value);
            if (!JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) thisObj, thisObj.getJSContext())) {
                typedArrayGetArrayType(thisObj).setElement(thisObj, index, numValue, isStrict);
            }
            return true;
        }
        if (!isValidIntegerIndex(thisObj, index)) {
            return true;
        }
        return super.set(thisObj, index, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(name);
            if (numericIndex != Undefined.instance) {
                if (thisObj == receiver) {
                    // TypedArraySetElement
                    Object numValue = convertValue(thisObj, value);
                    long index = validIntegerIndex(thisObj, (Number) numericIndex);
                    if (index != -1) {
                        typedArrayGetArrayType(thisObj).setElement(thisObj, index, numValue, isStrict);
                    }
                    return true;
                }
                if (!isValidIntegerIndex(thisObj, (Number) numericIndex)) {
                    return true;
                }
            }
        }
        return super.set(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    public static boolean isValidIntegerIndex(JSDynamicObject thisObj, Number numericIndex) {
        return validIntegerIndex(thisObj, numericIndex) != -1;
    }

    // IsValidIntegerIndex() ? theIndex : -1
    @TruffleBoundary
    private static long validIntegerIndex(JSDynamicObject thisObj, Number numericIndex) {
        if (JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) thisObj, thisObj.getJSContext())) {
            return -1;
        }
        if (!JSRuntime.isInteger(numericIndex) && !(numericIndex instanceof Long)) {
            return -1;
        }
        if (numericIndex instanceof Double && JSRuntime.isNegativeZero(((Double) numericIndex).doubleValue())) {
            return -1;
        }
        int length = JSArrayBufferView.typedArrayGetLength(thisObj);
        long index = numericIndex.longValue();
        return (0 <= index && index < length) ? index : -1;
    }

    private static Object convertValue(JSDynamicObject thisObj, Object value) {
        return JSArrayBufferView.isBigIntArrayBufferView(thisObj) ? JSRuntime.toBigInt(value) : JSRuntime.toNumber(value);
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(JSDynamicObject thisObj, long index) {
        return hasOwnProperty(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(name);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasProperty(thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, long index) {
        if (isOutOfBounds((JSTypedArrayObject) thisObj, thisObj.getJSContext())) {
            return false;
        }
        return typedArrayGetArrayType(thisObj).hasElement(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(name);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasOwnProperty(thisObj, key);
    }

    private static boolean hasNumericIndex(JSDynamicObject thisObj, Object numericIndex) {
        if (isOutOfBounds((JSTypedArrayObject) thisObj, thisObj.getJSContext())) {
            return false;
        }
        if (!JSRuntime.isInteger(numericIndex)) {
            return false;
        }
        double d = JSRuntime.doubleValue((Number) numericIndex);
        if (JSRuntime.isNegativeZero(d) || d < 0) {
            return false;
        }
        return d < JSArrayBufferView.typedArrayGetLength(thisObj);
    }

    public static JSTypedArrayObject createArrayBufferView(JSContext context, JSRealm realm, JSArrayBufferObject arrayBuffer,
                    TypedArrayFactory typedArrayFactory, TypedArray arrayType, int offset, int length) {
        CompilerAsserts.partialEvaluationConstant(typedArrayFactory);
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        JSObjectFactory objectFactory = context.getArrayBufferViewFactory(typedArrayFactory);
        return createArrayBufferView(objectFactory, realm, arrayBuffer, arrayType, offset, length);
    }

    public static JSTypedArrayObject createArrayBufferView(JSObjectFactory objectFactory, JSRealm realm, JSArrayBufferObject arrayBuffer,
                    TypedArray arrayType, int offset, int length) {
        return createArrayBufferView(objectFactory, realm, arrayBuffer, arrayType, offset, length, objectFactory.getPrototype(realm));
    }

    public static JSTypedArrayObject createArrayBufferViewWithProto(JSObjectFactory objectFactory, JSRealm realm, JSArrayBufferObject arrayBuffer,
                    TypedArray arrayType, int offset, int length, JSDynamicObject prototype) {
        return createArrayBufferView(objectFactory, realm, arrayBuffer, arrayType, offset, length, prototype);
    }

    private static JSTypedArrayObject createArrayBufferView(JSObjectFactory objectFactory, JSRealm realm, JSArrayBufferObject arrayBuffer,
                    TypedArray arrayType, int offset, int length, JSDynamicObject prototype) {
        assert !JSArrayBuffer.isDetachedBuffer(arrayBuffer);
        assert offset >= 0 && offset + length * arrayType.bytesPerElement() <= arrayBuffer.getByteLength();
        assert offset != 0 == arrayType.hasOffset();
        var shape = objectFactory.getShape(realm, prototype);
        var newObj = objectFactory.initProto(new JSTypedArrayObject(shape, prototype, arrayType, arrayBuffer, length, offset), realm, prototype);
        return objectFactory.trackAllocation(newObj);
    }

    private static JSObject createArrayBufferViewPrototype(JSRealm realm, JSDynamicObject ctor, int bytesPerElement, TypedArrayFactory factory, JSDynamicObject taPrototype) {
        JSContext context = realm.getContext();
        JSObject prototype = context.getEcmaScriptVersion() >= 6
                        ? JSObjectUtil.createOrdinaryPrototypeObject(realm, taPrototype)
                        : createLegacyArrayBufferViewPrototype(realm, factory, taPrototype);
        JSObjectUtil.putDataProperty(prototype, BYTES_PER_ELEMENT, bytesPerElement, JSAttributes.notConfigurableNotEnumerableNotWritable());
        JSObjectUtil.putConstructorProperty(prototype, ctor);
        return prototype;
    }

    private static JSObject createLegacyArrayBufferViewPrototype(JSRealm realm, TypedArrayFactory factory, JSDynamicObject taPrototype) {
        JSContext context = realm.getContext();
        byte[] byteArray = new byte[0];
        JSObjectFactory bufferFactory = context.getArrayBufferFactory();
        JSArrayBufferObject emptyArrayBuffer = bufferFactory.initProto(JSArrayBufferObject.createHeapArrayBuffer(bufferFactory.getShape(realm), bufferFactory.getPrototype(realm), byteArray), realm);
        TypedArray arrayType = factory.createArrayType(TypedArray.BUFFER_TYPE_ARRAY, false, true);
        Shape shape = JSShape.createPrototypeShape(context, INSTANCE, taPrototype);
        JSObject prototype = JSTypedArrayObject.create(shape, taPrototype, arrayType, emptyArrayBuffer, 0, 0);
        JSObjectUtil.setOrVerifyPrototype(context, prototype, taPrototype);
        return prototype;
    }

    public static Shape makeInitialArrayBufferViewShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm, TypedArrayFactory factory, JSConstructor taConstructor) {
        JSFunctionObject arrayBufferViewConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, factory.getName());
        JSObject.setPrototype(arrayBufferViewConstructor, taConstructor.getFunctionObject());

        JSObject arrayBufferViewPrototype = createArrayBufferViewPrototype(realm, arrayBufferViewConstructor, factory.getBytesPerElement(), factory, taConstructor.getPrototype());
        JSObjectUtil.putConstructorPrototypeProperty(arrayBufferViewConstructor, arrayBufferViewPrototype);
        JSObjectUtil.putDataProperty(arrayBufferViewConstructor, BYTES_PER_ELEMENT, factory.getBytesPerElement(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        return new JSConstructor(arrayBufferViewConstructor, arrayBufferViewPrototype);
    }

    private static JSObject createTypedArrayPrototype(final JSRealm realm, JSDynamicObject ctor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TypedArrayPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, TypedArrayPrototypeBuiltins.BUILTINS);

        // The initial value of the @@iterator property is the same function object as the initial
        // value of the %TypedArray%.prototype.values property.
        Object valuesFunction = JSDynamicObject.getOrNull(prototype, Strings.VALUES);
        JSObjectUtil.putDataProperty(prototype, Symbol.SYMBOL_ITERATOR, valuesFunction, JSAttributes.getDefaultNotEnumerable());
        // %TypedArray%.prototype.toString is the same function object as Array.prototype.toString
        Object toStringFunction = JSDynamicObject.getOrNull(realm.getArrayPrototype(), Strings.TO_STRING);
        JSObjectUtil.putDataProperty(prototype, Strings.TO_STRING, toStringFunction, JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    public static JSConstructor createTypedArrayConstructor(JSRealm realm) {
        JSFunctionObject taConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, CLASS_NAME);
        JSObject taPrototype = createTypedArrayPrototype(realm, taConstructor);
        JSObjectUtil.putConstructorPrototypeProperty(taConstructor, taPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, taConstructor, TypedArrayFunctionBuiltins.BUILTINS);
        putConstructorSpeciesGetter(realm, taConstructor);
        return new JSConstructor(taConstructor, taPrototype);
    }

    public static boolean isJSArrayBufferView(Object obj) {
        return obj instanceof JSTypedArrayObject;
    }

    public static boolean isBigIntArrayBufferView(JSDynamicObject obj) {
        return typedArrayGetArrayType(obj) instanceof TypedArray.TypedBigIntArray;
    }

    // IsTypedArrayOutOfBounds()
    public static boolean isOutOfBounds(JSTypedArrayObject typedArray, JSContext ctx) {
        if (!ctx.getTypedArrayNotDetachedAssumption().isValid() && hasDetachedBuffer(typedArray)) {
            return true;
        }
        if (ctx.getArrayBufferNotShrunkAssumption().isValid()) {
            return false;
        } else {
            long bufferByteLength = typedArray.getArrayBuffer().getByteLength();
            int byteOffsetStart = typedArray.getByteOffset();
            long byteOffsetEnd;
            if (typedArray.hasAutoLength()) {
                byteOffsetEnd = bufferByteLength;
            } else {
                byteOffsetEnd = byteOffsetStart + typedArray.getByteLength();
            }
            return (byteOffsetStart > bufferByteLength || byteOffsetEnd > bufferByteLength);
        }
    }

    private static boolean hasDetachedBuffer(JSDynamicObject obj) {
        assert isJSArrayBufferView(obj);
        return JSArrayBuffer.isDetachedBuffer(getArrayBuffer(obj));
    }

    @Override
    @TruffleBoundary
    public List<Object> getOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        if (!strings) {
            return super.getOwnPropertyKeys(thisObj, strings, symbols);
        }
        List<Object> keys = ordinaryOwnPropertyKeys(thisObj, strings, symbols);
        if (isOutOfBounds((JSTypedArrayObject) thisObj, thisObj.getJSContext())) {
            return keys;
        }
        List<Object> indices = typedArrayGetArrayType(thisObj).ownPropertyKeys(thisObj);
        return IteratorUtil.concatLists(indices, keys);
    }

    @Override
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(name);
            if (numericIndex != Undefined.instance) {
                boolean success = defineOwnPropertyIndex(thisObj, (Number) numericIndex, descriptor);
                if (doThrow && !success) {
                    throw Errors.createTypeErrorCannotRedefineProperty(numericIndex);
                }
                return success;
            }
        }
        return super.defineOwnProperty(thisObj, key, descriptor, doThrow);
    }

    @TruffleBoundary
    private static boolean defineOwnPropertyIndex(JSDynamicObject thisObj, Number numericIndex, PropertyDescriptor desc) {
        long index = validIntegerIndex(thisObj, numericIndex);
        if (index == -1) {
            return false;
        }
        if (desc.isAccessorDescriptor()) {
            return false;
        }
        if (desc.hasConfigurable() && !desc.getConfigurable()) {
            return false;
        }
        if (desc.hasEnumerable() && !desc.getEnumerable()) {
            return false;
        }
        if (desc.hasWritable() && !desc.getWritable()) {
            return false;
        }
        if (desc.hasValue()) {
            // TypedArraySetElement
            Object value = desc.getValue();
            Object numValue = convertValue(thisObj, value);
            if (!JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) thisObj, thisObj.getJSContext())) {
                assert index >= 0 && index < JSArrayBufferView.typedArrayGetLength(thisObj);
                JSArrayBufferView.typedArrayGetArrayType(thisObj).setElement(thisObj, index, numValue, true);
            }
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            long numericIndex = JSRuntime.propertyKeyToIntegerIndex(name);
            if (numericIndex >= 0) {
                Object value = getOwnHelper(thisObj, thisObj, numericIndex, null);
                if (value == Undefined.instance) {
                    return null;
                }
                return PropertyDescriptor.createData(value, true, true, true);
            }
        }
        return ordinaryGetOwnProperty(thisObj, key);
    }

    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof TruffleString name) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(name);
            if (numericIndex != Undefined.instance) {
                if (hasNumericIndex(thisObj, numericIndex)) {
                    if (isStrict) {
                        throw Errors.createTypeErrorNotConfigurableProperty(key);
                    }
                    return false;
                } else {
                    return true;
                }
            }
        }
        return super.delete(thisObj, key, isStrict);
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}

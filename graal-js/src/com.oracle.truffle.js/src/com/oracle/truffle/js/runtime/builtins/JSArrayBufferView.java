/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.TypedArrayFunctionBuiltins;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

public final class JSArrayBufferView extends JSNonProxy {
    public static final TruffleString CLASS_NAME = Strings.constant("TypedArray");
    public static final TruffleString PROTOTYPE_NAME = Strings.concat(CLASS_NAME, Strings.DOT_PROTOTYPE);

    private static final TruffleString BYTES_PER_ELEMENT = Strings.constant("BYTES_PER_ELEMENT");
    private static final TruffleString BYTE_LENGTH = Strings.constant("byteLength");
    private static final TruffleString LENGTH = JSAbstractArray.LENGTH;
    private static final TruffleString BUFFER = Strings.constant("buffer");
    private static final TruffleString BYTE_OFFSET = Strings.constant("byteOffset");
    private static final TruffleString GET_SYMBOL_TO_STRING_TAG_NAME = Strings.constant("get [Symbol.toStringTag]");

    public static final JSArrayBufferView INSTANCE = new JSArrayBufferView();

    private static TypedArrayAccess typedArray() {
        return TypedArrayAccess.SINGLETON;
    }

    public static TypedArray typedArrayGetArrayType(JSDynamicObject thisObj) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        return typedArray().getArrayType(thisObj);
    }

    public static int typedArrayGetLength(JSDynamicObject thisObj) {
        return typedArray().getLength(thisObj);
    }

    public static int typedArrayGetOffset(JSDynamicObject thisObj) {
        return typedArray().getOffset(thisObj);
    }

    public static byte[] typedArrayGetByteArray(JSDynamicObject thisObj) {
        return typedArray().getByteArray(thisObj);
    }

    public static ByteBuffer typedArrayGetByteBuffer(JSDynamicObject thisObj) {
        return DirectByteBufferHelper.cast(typedArray().getByteBuffer(thisObj));
    }

    private static TruffleString typedArrayGetName(JSDynamicObject thisObj) {
        return typedArrayGetArrayType(thisObj).getName();
    }

    private JSArrayBufferView() {
    }

    public static JSArrayBufferObject getArrayBuffer(JSDynamicObject thisObj) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        return typedArray().getArrayBuffer(thisObj);
    }

    public static int getByteLength(JSDynamicObject store, JSContext ctx) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        TypedArray typedArray = typedArrayGetArrayType(store);
        return typedArray.lengthInt(store) * typedArray.bytesPerElement();
    }

    public static int getByteLength(JSDynamicObject store, JSContext ctx, ValueProfile profile) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        TypedArray typedArray = profile.profile(typedArrayGetArrayType(store));
        return typedArray.lengthInt(store) * typedArray.bytesPerElement();
    }

    public static int getByteOffset(JSDynamicObject store, JSContext ctx) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        return typedArrayGetOffset(store);
    }

    @TruffleBoundary
    @Override
    public Object getHelper(JSDynamicObject store, Object receiver, long index, Node encapsulatingNode) {
        return getOwnHelper(store, receiver, index, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object receiver, long index, Node encapsulatingNode) {
        if (JSArrayBufferView.hasDetachedBuffer(store)) {
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
        if (Strings.isTString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((TruffleString) key);
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
        if (Strings.isTString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((TruffleString) key);
            if (numericIndex != Undefined.instance) {
                return integerIndexedElementGet(store, numericIndex);
            }
        }
        return super.getOwnHelper(store, receiver, key, encapsulatingNode);
    }

    @TruffleBoundary
    private static Object integerIndexedElementGet(JSDynamicObject thisObj, Object numericIndex) {
        assert JSRuntime.isNumber(numericIndex);
        if (JSArrayBufferView.hasDetachedBuffer(thisObj)) {
            return Undefined.instance;
        }
        if (!JSRuntime.isInteger(numericIndex)) {
            return Undefined.instance;
        }
        if (numericIndex instanceof Double && JSRuntime.isNegativeZero(((Double) numericIndex).doubleValue())) {
            return Undefined.instance;
        }
        long index = ((Number) numericIndex).longValue();
        int length = JSArrayBufferView.typedArrayGetLength(thisObj);
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
            if (!JSArrayBufferView.hasDetachedBuffer(thisObj)) {
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
        if (Strings.isTString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((TruffleString) key);
            if (numericIndex != Undefined.instance) {
                if (thisObj == receiver) {
                    // IntegerIndexedElementSet
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
        if (JSArrayBufferView.hasDetachedBuffer(thisObj)) {
            return -1;
        }
        if (!JSRuntime.isInteger(numericIndex)) {
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
        if (Strings.isTString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((TruffleString) key);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasProperty(thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, long index) {
        if (JSArrayBufferView.hasDetachedBuffer(thisObj)) {
            return false;
        }
        return typedArrayGetArrayType(thisObj).hasElement(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (Strings.isTString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((TruffleString) key);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasOwnProperty(thisObj, key);
    }

    private static boolean hasNumericIndex(JSDynamicObject thisObj, Object numericIndex) {
        if (JSArrayBufferView.hasDetachedBuffer(thisObj)) {
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

    public static JSTypedArrayObject createArrayBufferView(JSContext context, JSRealm realm, JSDynamicObject arrayBuffer, TypedArray arrayType, int offset, int length) {
        CompilerAsserts.partialEvaluationConstant(arrayType);
        assert JSArrayBuffer.isJSAbstractBuffer(arrayBuffer);
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        JSObjectFactory objectFactory = context.getArrayBufferViewFactory(arrayType.getFactory());
        return createArrayBufferView(context, realm, objectFactory, arrayBuffer, arrayType, offset, length);
    }

    public static JSTypedArrayObject createArrayBufferView(JSContext context, JSRealm realm, JSObjectFactory objectFactory,
                    JSDynamicObject arrayBuffer, TypedArray arrayType, int offset, int length) {
        return createArrayBufferView(context, realm, objectFactory, arrayBuffer, arrayType, offset, length, objectFactory.getPrototype(realm));
    }

    public static JSTypedArrayObject createArrayBufferViewWithProto(JSContext context, JSRealm realm, JSObjectFactory objectFactory,
                    JSDynamicObject arrayBuffer, TypedArray arrayType, int offset, int length, JSDynamicObject prototype) {
        return createArrayBufferView(context, realm, objectFactory, arrayBuffer, arrayType, offset, length, prototype);
    }

    private static JSTypedArrayObject createArrayBufferView(JSContext context, JSRealm realm, JSObjectFactory objectFactory,
                    JSDynamicObject arrayBuffer, TypedArray arrayType, int offset, int length, JSDynamicObject prototype) {
        assert !JSArrayBuffer.isDetachedBuffer(arrayBuffer);
        assert offset >= 0 && offset + length * arrayType.bytesPerElement() <= ((JSArrayBufferObject) arrayBuffer).getByteLength();
        assert offset != 0 == arrayType.hasOffset();

        JSTypedArrayObject obj = JSTypedArrayObject.create(objectFactory.getShape(realm), arrayType, (JSArrayBufferObject) arrayBuffer, length, offset);
        objectFactory.initProto(obj, prototype);
        return context.trackAllocation(obj);
    }

    private static JSObject createArrayBufferViewPrototype(JSRealm realm, JSDynamicObject ctor, int bytesPerElement, TypedArrayFactory factory, JSDynamicObject taPrototype) {
        JSContext context = realm.getContext();
        JSObject prototype = context.getEcmaScriptVersion() >= 6
                        ? JSObjectUtil.createOrdinaryPrototypeObject(realm, taPrototype)
                        : createLegacyArrayBufferViewPrototype(realm, factory, taPrototype);
        JSObjectUtil.putDataProperty(context, prototype, BYTES_PER_ELEMENT, bytesPerElement, JSAttributes.notConfigurableNotEnumerableNotWritable());
        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        return prototype;
    }

    private static JSObject createLegacyArrayBufferViewPrototype(JSRealm realm, TypedArrayFactory factory, JSDynamicObject taPrototype) {
        JSContext context = realm.getContext();
        byte[] byteArray = new byte[0];
        JSObjectFactory bufferFactory = context.getArrayBufferFactory();
        JSArrayBufferObject emptyArrayBuffer = bufferFactory.initProto(JSArrayBufferObject.createHeapArrayBuffer(bufferFactory.getShape(realm), byteArray), realm);
        TypedArray arrayType = factory.createArrayType(context.isOptionDirectByteBuffer(), false);
        Shape shape = JSShape.createPrototypeShape(context, INSTANCE, taPrototype);
        JSObject prototype = JSTypedArrayObject.create(shape, arrayType, emptyArrayBuffer, 0, 0);
        JSObjectUtil.setOrVerifyPrototype(context, prototype, taPrototype);
        return prototype;
    }

    protected static void putArrayBufferViewPrototypeGetter(JSRealm realm, JSDynamicObject prototype, TruffleString key, BuiltinFunctionKey functionKey, ArrayBufferViewGetter getter) {
        assert JSRuntime.isPropertyKey(key);
        JSFunctionData lengthGetterData = realm.getContext().getOrCreateBuiltinFunctionData(functionKey, (c) -> {
            return JSFunctionData.createCallOnly(c, new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = JSArguments.getThisObject(frame.getArguments());
                    if (isJSArrayBufferView(obj)) {
                        return getter.apply((JSTypedArrayObject) obj);
                    }
                    errorBranch.enter();
                    throw Errors.createTypeError("method called on incompatible receiver");
                }
            }.getCallTarget(), 0, Strings.concat(Strings.GET_SPC, key));
        });
        JSDynamicObject lengthGetter = JSFunction.create(realm, lengthGetterData);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, key, lengthGetter, Undefined.instance);
    }

    private abstract static class ArrayBufferViewGetter extends Node {
        public abstract Object apply(JSDynamicObject view);
    }

    public static Shape makeInitialArrayBufferViewShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm, TypedArrayFactory factory, JSConstructor taConstructor) {
        JSContext ctx = realm.getContext();
        JSFunctionObject arrayBufferViewConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, factory.getName());
        JSObject.setPrototype(arrayBufferViewConstructor, taConstructor.getFunctionObject());

        JSObject arrayBufferViewPrototype = createArrayBufferViewPrototype(realm, arrayBufferViewConstructor, factory.getBytesPerElement(), factory, taConstructor.getPrototype());
        JSObjectUtil.putConstructorPrototypeProperty(ctx, arrayBufferViewConstructor, arrayBufferViewPrototype);
        JSObjectUtil.putDataProperty(ctx, arrayBufferViewConstructor, BYTES_PER_ELEMENT, factory.getBytesPerElement(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        putConstructorSpeciesGetter(realm, arrayBufferViewConstructor);
        return new JSConstructor(arrayBufferViewConstructor, arrayBufferViewPrototype);
    }

    private static JSObject createTypedArrayPrototype(final JSRealm realm, JSDynamicObject ctor) {
        JSContext ctx = realm.getContext();
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TypedArrayPrototypeBuiltins.BUILTINS);
        putArrayBufferViewPrototypeGetter(realm, prototype, LENGTH, BuiltinFunctionKey.ArrayBufferViewLength, new ArrayBufferViewGetter() {
            private final ConditionProfile detachedBufferProfile = ConditionProfile.create();

            @Override
            public Object apply(JSDynamicObject view) {
                if (detachedBufferProfile.profile(JSArrayBufferView.hasDetachedBuffer(view, ctx))) {
                    return 0;
                }
                return typedArrayGetLength(view);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BUFFER, BuiltinFunctionKey.ArrayBufferViewBuffer, new ArrayBufferViewGetter() {
            @Override
            public Object apply(JSDynamicObject view) {
                return getArrayBuffer(view);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BYTE_LENGTH, BuiltinFunctionKey.ArrayBufferViewByteLength, new ArrayBufferViewGetter() {
            @Override
            public Object apply(JSDynamicObject view) {
                return getByteLength(view, ctx);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BYTE_OFFSET, BuiltinFunctionKey.ArrayBufferViewByteByteOffset, new ArrayBufferViewGetter() {
            @Override
            public Object apply(JSDynamicObject view) {
                return getByteOffset(view, ctx);
            }
        });
        JSFunctionData toStringData = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ArrayBufferViewToString, (c) -> {
            return JSFunctionData.createCallOnly(ctx, new JavaScriptRootNode(ctx.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = JSArguments.getThisObject(frame.getArguments());
                    if (isJSArrayBufferView(obj)) {
                        return typedArrayGetName((JSTypedArrayObject) obj);
                    }
                    return Undefined.instance;
                }
            }.getCallTarget(), 0, GET_SYMBOL_TO_STRING_TAG_NAME);
        });
        JSDynamicObject toStringTagGetter = JSFunction.create(realm, toStringData);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, Symbol.SYMBOL_TO_STRING_TAG, toStringTagGetter, Undefined.instance);
        // The initial value of the @@iterator property is the same function object as the initial
        // value of the %TypedArray%.prototype.values property.
        Object valuesFunction = JSDynamicObject.getOrNull(prototype, Strings.VALUES);
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_ITERATOR, valuesFunction, JSAttributes.getDefaultNotEnumerable());
        // %TypedArray%.prototype.toString is the same function object as Array.prototype.toString
        Object toStringFunction = JSDynamicObject.getOrNull(realm.getArrayPrototype(), Strings.TO_STRING);
        JSObjectUtil.putDataProperty(ctx, prototype, Strings.TO_STRING, toStringFunction, JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    public static JSConstructor createTypedArrayConstructor(JSRealm realm) {
        JSContext ctx = realm.getContext();
        JSFunctionObject taConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, CLASS_NAME);
        JSObject taPrototype = createTypedArrayPrototype(realm, taConstructor);
        JSObjectUtil.putConstructorPrototypeProperty(ctx, taConstructor, taPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, taConstructor, TypedArrayFunctionBuiltins.BUILTINS);
        putConstructorSpeciesGetter(realm, taConstructor);
        return new JSConstructor(taConstructor, taPrototype);
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return typedArrayGetName(object);
    }

    public static boolean isJSArrayBufferView(Object obj) {
        return obj instanceof JSTypedArrayObject;
    }

    public static boolean isBigIntArrayBufferView(JSDynamicObject obj) {
        return typedArrayGetArrayType(obj) instanceof TypedArray.TypedBigIntArray;
    }

    public static boolean hasDetachedBuffer(JSDynamicObject obj, JSContext ctx) {
        assert isJSArrayBufferView(obj);
        if (ctx.getTypedArrayNotDetachedAssumption().isValid()) {
            return false;
        } else {
            return hasDetachedBuffer(obj);
        }
    }

    public static boolean hasDetachedBuffer(JSDynamicObject obj) {
        assert isJSArrayBufferView(obj);
        return JSArrayBuffer.isDetachedBuffer(getArrayBuffer(obj));
    }

    @Override
    @TruffleBoundary
    public List<Object> getOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        if (!strings) {
            return super.getOwnPropertyKeys(thisObj, strings, symbols);
        }
        List<Object> indices = typedArrayGetArrayType(thisObj).ownPropertyKeys(thisObj);
        List<Object> keys = ordinaryOwnPropertyKeys(thisObj, strings, symbols);
        return IteratorUtil.concatLists(indices, keys);
    }

    @Override
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        if (Strings.isTString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((TruffleString) key);
            if (numericIndex != Undefined.instance) {
                boolean success = defineOwnPropertyIndex(thisObj, (Number) numericIndex, descriptor);
                if (doThrow && !success) {
                    // path only hit in V8CompatibilityMode; see JSRuntime.definePropertyOrThrow
                    throw Errors.createTypeError("Cannot defineOwnProperty on TypedArray");
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
            // IntegerIndexedElementSet
            Object value = desc.getValue();
            Object numValue = convertValue(thisObj, value);
            if (!JSArrayBufferView.hasDetachedBuffer(thisObj)) {
                assert index >= 0 && index < JSArrayBufferView.typedArrayGetLength(thisObj);
                JSArrayBufferView.typedArrayGetArrayType(thisObj).setElement(thisObj, index, numValue, true);
            }
        }
        return true;
    }

    @Override
    public boolean setIntegrityLevel(JSDynamicObject thisObj, boolean freeze, boolean doThrow) {
        preventExtensions(thisObj, doThrow);
        if (freeze && typedArrayGetLength(thisObj) > 0) {
            throwCannotRedefine();
        }
        return true;
    }

    @Override
    public boolean testIntegrityLevel(JSDynamicObject thisObj, boolean frozen) {
        if (frozen && typedArrayGetLength(thisObj) > 0) {
            return false;
        }
        return super.testIntegrityLevelFast(thisObj, frozen);
    }

    private static void throwCannotRedefine() {
        throw Errors.createTypeError("Cannot redefine a property of an object with external array elements");
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (Strings.isTString(key)) {
            long numericIndex = JSRuntime.propertyKeyToIntegerIndex(key);
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
    public TruffleString toDisplayStringImpl(JSDynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToDisplayString(obj, allowSideEffects, format, depth, typedArrayGetName(obj));
        }
    }

    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        if (Strings.isTString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((TruffleString) key);
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

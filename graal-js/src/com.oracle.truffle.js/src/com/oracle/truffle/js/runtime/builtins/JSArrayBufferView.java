/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.TypedArrayFunctionBuiltins;
import com.oracle.truffle.js.builtins.TypedArrayPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
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
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

public final class JSArrayBufferView extends JSNonProxy {
    public static final String CLASS_NAME = "TypedArray";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    public static final JSArrayBufferView INSTANCE = new JSArrayBufferView();

    private static final String BYTES_PER_ELEMENT = "BYTES_PER_ELEMENT";
    private static final String BYTE_LENGTH = "byteLength";
    private static final String LENGTH = JSAbstractArray.LENGTH;
    private static final String BUFFER = "buffer";
    private static final String BYTE_OFFSET = "byteOffset";

    private static TypedArrayAccess typedArray() {
        return TypedArrayAccess.SINGLETON;
    }

    public static TypedArray typedArrayGetArrayType(DynamicObject thisObj) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        return typedArray().getArrayType(thisObj);
    }

    public static int typedArrayGetLength(DynamicObject thisObj) {
        return typedArray().getLength(thisObj);
    }

    public static int typedArrayGetOffset(DynamicObject thisObj) {
        return typedArray().getOffset(thisObj);
    }

    public static byte[] typedArrayGetByteArray(DynamicObject thisObj) {
        return typedArray().getByteArray(thisObj);
    }

    public static ByteBuffer typedArrayGetByteBuffer(DynamicObject thisObj) {
        return DirectByteBufferHelper.cast(typedArray().getByteBuffer(thisObj));
    }

    private static String typedArrayGetName(DynamicObject thisObj) {
        return typedArrayGetArrayType(thisObj).getName();
    }

    private JSArrayBufferView() {
    }

    public static DynamicObject getArrayBuffer(DynamicObject thisObj) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        return typedArray().getArrayBuffer(thisObj);
    }

    public static int getByteLength(DynamicObject store, JSContext ctx) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        TypedArray typedArray = typedArrayGetArrayType(store);
        return typedArray.lengthInt(store) * typedArray.bytesPerElement();
    }

    public static int getByteLength(DynamicObject store, JSContext ctx, ValueProfile profile) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        TypedArray typedArray = profile.profile(typedArrayGetArrayType(store));
        return typedArray.lengthInt(store) * typedArray.bytesPerElement();
    }

    public static int getByteOffset(DynamicObject store, JSContext ctx) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        return typedArrayGetOffset(store);
    }

    @TruffleBoundary
    @Override
    public Object getHelper(DynamicObject store, Object receiver, long index, Node encapsulatingNode) {
        return getOwnHelper(store, receiver, index, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object receiver, long index, Node encapsulatingNode) {
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
    public Object getHelper(DynamicObject store, Object receiver, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof String) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
            if (numericIndex != Undefined.instance) {
                return integerIndexedElementGet(store, numericIndex);
            }
        }
        return super.getHelper(store, receiver, key, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object receiver, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof String) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
            if (numericIndex != Undefined.instance) {
                return integerIndexedElementGet(store, numericIndex);
            }
        }
        return super.getOwnHelper(store, receiver, key, encapsulatingNode);
    }

    @TruffleBoundary
    private static Object integerIndexedElementGet(DynamicObject thisObj, Object numericIndex) {
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
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (thisObj != receiver) { // off-spec
            return ordinarySetIndex(thisObj, index, value, receiver, isStrict, encapsulatingNode);
        }
        Object numValue = convertValue(thisObj, value);
        if (!JSArrayBufferView.hasDetachedBuffer(thisObj)) {
            typedArrayGetArrayType(thisObj).setElement(thisObj, index, numValue, isStrict);
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (thisObj != receiver) { // off-spec
            return ordinarySet(thisObj, key, value, receiver, isStrict, encapsulatingNode);
        }
        if (key instanceof String) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
            if (numericIndex != Undefined.instance) {
                // IntegerIndexedElementSet
                Object numValue = convertValue(thisObj, value);
                if (JSArrayBufferView.hasDetachedBuffer(thisObj)) {
                    return true;
                }
                if (!JSRuntime.isInteger(numericIndex)) {
                    return true;
                }
                if (numericIndex instanceof Double && JSRuntime.isNegativeZero(((Double) numericIndex).doubleValue())) {
                    return true;
                }
                int length = JSArrayBufferView.typedArrayGetLength(thisObj);
                long index = ((Number) numericIndex).longValue();
                if (0 <= index && index < length) {
                    typedArrayGetArrayType(thisObj).setElement(thisObj, index, numValue, isStrict);
                }
                return true;
            }
        }
        return super.set(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    private static Object convertValue(DynamicObject thisObj, Object value) {
        return JSArrayBufferView.isBigIntArrayBufferView(thisObj) ? JSRuntime.toBigInt(value) : JSRuntime.toNumber(value);
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, long index) {
        return hasOwnProperty(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof String) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasProperty(thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long index) {
        if (JSArrayBufferView.hasDetachedBuffer(thisObj)) {
            return false;
        }
        return typedArrayGetArrayType(thisObj).hasElement(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof String) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasOwnProperty(thisObj, key);
    }

    private static boolean hasNumericIndex(DynamicObject thisObj, Object numericIndex) {
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

    public static DynamicObject createArrayBufferView(JSContext context, DynamicObject arrayBuffer, TypedArray arrayType, int offset, int length) {
        CompilerAsserts.partialEvaluationConstant(arrayType);
        assert JSArrayBuffer.isJSAbstractBuffer(arrayBuffer);
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        JSObjectFactory objectFactory = context.getArrayBufferViewFactory(arrayType.getFactory());
        return createArrayBufferView(context, objectFactory, arrayBuffer, arrayType, offset, length);
    }

    public static DynamicObject createArrayBufferView(JSContext context, JSObjectFactory objectFactory, DynamicObject arrayBuffer, TypedArray arrayType, int offset, int length) {
        JSRealm realm = context.getRealm();
        return createArrayBufferView(context, objectFactory, arrayBuffer, arrayType, offset, length, realm, objectFactory.getPrototype(realm));
    }

    public static DynamicObject createArrayBufferViewWithProto(JSContext context, JSObjectFactory objectFactory, DynamicObject arrayBuffer, TypedArray arrayType, int offset, int length,
                    DynamicObject prototype) {
        JSRealm realm = context.getRealm();
        return createArrayBufferView(context, objectFactory, arrayBuffer, arrayType, offset, length, realm, prototype);
    }

    private static DynamicObject createArrayBufferView(JSContext context, JSObjectFactory objectFactory, DynamicObject arrayBuffer, TypedArray arrayType, int offset, int length,
                    JSRealm realm, DynamicObject prototype) {
        assert !JSArrayBuffer.isDetachedBuffer(arrayBuffer);
        assert offset >= 0 && offset + length * arrayType.bytesPerElement() <= ((JSArrayBufferObject) arrayBuffer).getByteLength();
        assert offset != 0 == arrayType.hasOffset();

        DynamicObject obj = JSTypedArrayObject.create(objectFactory.getShape(realm), arrayType, (JSArrayBufferObject) arrayBuffer, length, offset);
        objectFactory.initProto(obj, prototype);
        assert JSArrayBuffer.isJSAbstractBuffer(arrayBuffer);
        assert isJSArrayBufferView(obj);
        return context.trackAllocation(obj);
    }

    private static DynamicObject createArrayBufferViewPrototype(JSRealm realm, DynamicObject ctor, int bytesPerElement, TypedArrayFactory factory, DynamicObject taPrototype) {
        JSContext context = realm.getContext();
        DynamicObject prototype = context.getEcmaScriptVersion() >= 6
                        ? JSObjectUtil.createOrdinaryPrototypeObject(realm, taPrototype)
                        : createLegacyArrayBufferViewPrototype(realm, factory, taPrototype);
        JSObjectUtil.putDataProperty(context, prototype, BYTES_PER_ELEMENT, bytesPerElement, JSAttributes.notConfigurableNotEnumerableNotWritable());
        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        return prototype;
    }

    private static DynamicObject createLegacyArrayBufferViewPrototype(JSRealm realm, TypedArrayFactory factory, DynamicObject taPrototype) {
        JSContext context = realm.getContext();
        byte[] byteArray = new byte[0];
        JSObjectFactory bufferFactory = context.getArrayBufferFactory();
        DynamicObject emptyArrayBuffer = bufferFactory.initProto(JSArrayBufferObject.createHeapArrayBuffer(bufferFactory.getShape(realm), byteArray), realm);
        TypedArray arrayType = factory.createArrayType(context.isOptionDirectByteBuffer(), false);
        Shape shape = JSShape.createPrototypeShape(context, INSTANCE, taPrototype);
        DynamicObject prototype = JSTypedArrayObject.create(shape, arrayType, (JSArrayBufferObject) emptyArrayBuffer, 0, 0);
        JSObjectUtil.setOrVerifyPrototype(context, prototype, taPrototype);
        return prototype;
    }

    protected static void putArrayBufferViewPrototypeGetter(JSRealm realm, DynamicObject prototype, String key, BuiltinFunctionKey functionKey, ArrayBufferViewGetter getter) {
        JSFunctionData lengthGetterData = realm.getContext().getOrCreateBuiltinFunctionData(functionKey, (c) -> {
            return JSFunctionData.createCallOnly(c, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                @Child private ArrayBufferViewGetter getterNode = getter;
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
            }), 0, "get " + key);
        });
        DynamicObject lengthGetter = JSFunction.create(realm, lengthGetterData);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, key, lengthGetter, Undefined.instance);
    }

    private abstract static class ArrayBufferViewGetter extends Node {
        public abstract Object apply(DynamicObject view);
    }

    public static Shape makeInitialArrayBufferViewShape(JSContext ctx, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm, TypedArrayFactory factory, JSConstructor taConstructor) {
        JSContext ctx = realm.getContext();
        DynamicObject arrayBufferViewConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, factory.getName());
        JSObject.setPrototype(arrayBufferViewConstructor, taConstructor.getFunctionObject());

        DynamicObject arrayBufferViewPrototype = createArrayBufferViewPrototype(realm, arrayBufferViewConstructor, factory.getBytesPerElement(), factory, taConstructor.getPrototype());
        JSObjectUtil.putConstructorPrototypeProperty(ctx, arrayBufferViewConstructor, arrayBufferViewPrototype);
        JSObjectUtil.putDataProperty(ctx, arrayBufferViewConstructor, BYTES_PER_ELEMENT, factory.getBytesPerElement(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        putConstructorSpeciesGetter(realm, arrayBufferViewConstructor);
        return new JSConstructor(arrayBufferViewConstructor, arrayBufferViewPrototype);
    }

    private static DynamicObject createTypedArrayPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TypedArrayPrototypeBuiltins.BUILTINS);
        putArrayBufferViewPrototypeGetter(realm, prototype, LENGTH, BuiltinFunctionKey.ArrayBufferViewLength, new ArrayBufferViewGetter() {
            private final ConditionProfile detachedBufferProfile = ConditionProfile.create();

            @Override
            public Object apply(DynamicObject view) {
                if (detachedBufferProfile.profile(JSArrayBufferView.hasDetachedBuffer(view, ctx))) {
                    return 0;
                }
                return typedArrayGetLength(view);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BUFFER, BuiltinFunctionKey.ArrayBufferViewBuffer, new ArrayBufferViewGetter() {
            @Override
            public Object apply(DynamicObject view) {
                return getArrayBuffer(view);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BYTE_LENGTH, BuiltinFunctionKey.ArrayBufferViewByteLength, new ArrayBufferViewGetter() {
            @Override
            public Object apply(DynamicObject view) {
                return getByteLength(view, ctx);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BYTE_OFFSET, BuiltinFunctionKey.ArrayBufferViewByteByteOffset, new ArrayBufferViewGetter() {
            @Override
            public Object apply(DynamicObject view) {
                return getByteOffset(view, ctx);
            }
        });
        JSFunctionData toStringData = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ArrayBufferViewToString, (c) -> {
            return JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(ctx.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = JSArguments.getThisObject(frame.getArguments());
                    if (isJSArrayBufferView(obj)) {
                        return typedArrayGetName((JSTypedArrayObject) obj);
                    }
                    return Undefined.instance;
                }
            }), 0, "get [Symbol.toStringTag]");
        });
        DynamicObject toStringTagGetter = JSFunction.create(realm, toStringData);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, Symbol.SYMBOL_TO_STRING_TAG, toStringTagGetter, Undefined.instance);
        // The initial value of the @@iterator property is the same function object as the initial
        // value of the %TypedArray%.prototype.values property.
        Object valuesFunction = JSDynamicObject.getOrNull(prototype, "values");
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_ITERATOR, valuesFunction, JSAttributes.getDefaultNotEnumerable());
        // %TypedArray%.prototype.toString is the same function object as Array.prototype.toString
        Object toStringFunction = JSDynamicObject.getOrNull(realm.getArrayPrototype(), "toString");
        JSObjectUtil.putDataProperty(ctx, prototype, "toString", toStringFunction, JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    public static JSConstructor createTypedArrayConstructor(JSRealm realm) {
        JSContext ctx = realm.getContext();
        DynamicObject taConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, CLASS_NAME);
        DynamicObject taPrototype = createTypedArrayPrototype(realm, taConstructor);
        JSObjectUtil.putConstructorPrototypeProperty(ctx, taConstructor, taPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, taConstructor, TypedArrayFunctionBuiltins.BUILTINS);
        putConstructorSpeciesGetter(realm, taConstructor);
        return new JSConstructor(taConstructor, taPrototype);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return typedArrayGetName(object);
    }

    public static boolean isJSArrayBufferView(Object obj) {
        return obj instanceof JSTypedArrayObject;
    }

    public static boolean isBigIntArrayBufferView(DynamicObject obj) {
        return typedArrayGetArrayType(obj) instanceof TypedArray.TypedBigIntArray;
    }

    public static boolean hasDetachedBuffer(DynamicObject obj, JSContext ctx) {
        assert isJSArrayBufferView(obj);
        if (ctx.getTypedArrayNotDetachedAssumption().isValid()) {
            return false;
        } else {
            return hasDetachedBuffer(obj);
        }
    }

    public static boolean hasDetachedBuffer(DynamicObject obj) {
        assert isJSArrayBufferView(obj);
        return JSArrayBuffer.isDetachedBuffer(getArrayBuffer(obj));
    }

    @Override
    @TruffleBoundary
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        if (!strings) {
            return super.getOwnPropertyKeys(thisObj, strings, symbols);
        }
        List<Object> indices = typedArrayGetArrayType(thisObj).ownPropertyKeys(thisObj);
        List<Object> keys = ordinaryOwnPropertyKeys(thisObj, strings, symbols);
        return IteratorUtil.concatLists(indices, keys);
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof String) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
            if (numericIndex != Undefined.instance) {
                boolean success = defineOwnPropertyIndex(thisObj, numericIndex, descriptor);
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
    private static boolean defineOwnPropertyIndex(DynamicObject thisObj, Object numericIndex, PropertyDescriptor desc) {
        // IsValidIntegerIndex
        if (JSArrayBufferView.hasDetachedBuffer(thisObj) || !JSRuntime.isInteger(numericIndex)) {
            return false;
        }
        double dIndex = ((Number) numericIndex).doubleValue();
        if (JSRuntime.isNegativeZero(dIndex)) {
            return false;
        }
        int length = JSArrayBufferView.typedArrayGetLength(thisObj);
        long index = (long) dIndex;
        if (index < 0 || index >= length) {
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
                assert index >= 0 && index < length;
                JSArrayBufferView.typedArrayGetArrayType(thisObj).setElement(thisObj, index, numValue, true);
            }
        }
        return true;
    }

    @Override
    public boolean setIntegrityLevel(DynamicObject thisObj, boolean freeze, boolean doThrow) {
        preventExtensions(thisObj, doThrow);
        if (freeze && typedArrayGetLength(thisObj) > 0) {
            throwCannotRedefine();
        }
        return true;
    }

    @Override
    public boolean testIntegrityLevel(DynamicObject thisObj, boolean frozen) {
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
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof String) {
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
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects, JSContext context) {
        if (context.isOptionNashornCompatibilityMode()) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToConsoleString(obj, typedArrayGetName(obj), depth, allowSideEffects);
        }
    }

    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof String) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
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

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
package com.oracle.truffle.js.runtime.builtins;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public final class JSArrayBufferView extends JSBuiltinObject {
    public static final String CLASS_NAME = "ArrayBufferView";
    public static final String PROTOTYPE_NAME = "ArrayBufferView.prototype";
    public static final String TYPED_ARRAY_CLASS_NAME = "TypedArray";

    public static final JSArrayBufferView INSTANCE = new JSArrayBufferView();

    private static final String BYTES_PER_ELEMENT = "BYTES_PER_ELEMENT";
    private static final String BYTE_LENGTH = "byteLength";
    private static final String LENGTH = JSAbstractArray.LENGTH;
    private static final String BUFFER = "buffer";
    private static final String BYTE_OFFSET = "byteOffset";
    private static final HiddenKey ARRAY_BUFFER_ID = new HiddenKey("arrayBuffer");
    private static final Property ARRAY_BUFFER_PROPERTY;

    private static final HiddenKey ARRAY_ID = new HiddenKey("array");
    private static final HiddenKey ARRAY_TYPE_ID = new HiddenKey("arraytype");
    private static final HiddenKey OFFSET_ID = new HiddenKey("offset");
    private static final HiddenKey LENGTH_ID = new HiddenKey(LENGTH);
    private static final HiddenKey SHAREABLE_ID = new HiddenKey("shareable");

    private static final Property BYTE_ARRAY_PROPERTY;
    private static final Property BYTE_BUFFER_PROPERTY;
    private static final Property ARRAY_TYPE_PROPERTY;
    private static final Property ARRAY_LENGTH_PROPERTY;
    private static final Property ARRAY_OFFSET_PROPERTY;
    private static final Property ARRAY_SHAREABLE;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        BYTE_ARRAY_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_ID, allocator.copy().locationForType(byte[].class, EnumSet.of(LocationModifier.NonNull)));
        BYTE_BUFFER_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_ID, allocator.locationForType(ByteBuffer.class, EnumSet.of(LocationModifier.NonNull)));
        ARRAY_TYPE_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_TYPE_ID, allocator.locationForType(TypedArray.class, EnumSet.of(LocationModifier.NonNull)));
        ARRAY_BUFFER_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_BUFFER_ID, allocator.locationForType(JSObject.CLASS, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
        ARRAY_LENGTH_PROPERTY = JSObjectUtil.makeHiddenProperty(LENGTH_ID, allocator.locationForType(int.class));
        ARRAY_OFFSET_PROPERTY = JSObjectUtil.makeHiddenProperty(OFFSET_ID, allocator.locationForType(int.class));
        ARRAY_SHAREABLE = JSObjectUtil.makeHiddenProperty(SHAREABLE_ID, allocator.locationForType(boolean.class));
    }

    public static TypedArray typedArrayGetArrayType(DynamicObject thisObj) {
        return typedArrayGetArrayType(thisObj, JSArrayBufferView.isJSArrayBufferView(thisObj));
    }

    public static TypedArray typedArrayGetArrayType(DynamicObject thisObj, boolean condition) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        return (TypedArray) ARRAY_TYPE_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetArrayType(DynamicObject thisObj, TypedArray arrayType) {
        ARRAY_TYPE_PROPERTY.setSafe(thisObj, arrayType, null);
    }

    public static int typedArrayGetLength(DynamicObject thisObj) {
        return typedArrayGetLength(thisObj, JSArrayBufferView.isJSArrayBufferView(thisObj));
    }

    public static int typedArrayGetLength(DynamicObject thisObj, boolean condition) {
        return (int) ARRAY_LENGTH_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetLength(DynamicObject thisObj, int length) {
        ARRAY_LENGTH_PROPERTY.setSafe(thisObj, length, null);
    }

    public static int typedArrayGetOffset(DynamicObject thisObj) {
        return typedArrayGetOffset(thisObj, JSArrayBufferView.isJSArrayBufferView(thisObj));
    }

    public static int typedArrayGetOffset(DynamicObject thisObj, boolean condition) {
        return (int) ARRAY_OFFSET_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetOffset(DynamicObject thisObj, int arrayOffset) {
        ARRAY_OFFSET_PROPERTY.setSafe(thisObj, arrayOffset, null);
    }

    public static byte[] typedArrayGetByteArray(DynamicObject thisObj) {
        return typedArrayGetByteArray(thisObj, JSArrayBufferView.isJSArrayBufferView(thisObj));
    }

    public static byte[] typedArrayGetByteArray(DynamicObject thisObj, boolean condition) {
        return (byte[]) BYTE_ARRAY_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetArray(DynamicObject thisObj, byte[] arrayOffset) {
        BYTE_ARRAY_PROPERTY.setSafe(thisObj, arrayOffset, null);
    }

    public static ByteBuffer typedArrayGetByteBuffer(DynamicObject thisObj, boolean condition) {
        return DirectByteBufferHelper.cast((ByteBuffer) BYTE_BUFFER_PROPERTY.get(thisObj, condition));
    }

    private static String typedArrayGetName(DynamicObject thisObj) {
        return typedArrayGetArrayType(thisObj).getName();
    }

    private JSArrayBufferView() {
    }

    public static DynamicObject getArrayBuffer(DynamicObject thisObj, boolean condition) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        return (DynamicObject) ARRAY_BUFFER_PROPERTY.get(thisObj, condition);
    }

    public static DynamicObject getArrayBuffer(DynamicObject thisObj) {
        return getArrayBuffer(thisObj, JSArrayBufferView.isJSArrayBufferView(thisObj));
    }

    public static int getByteLength(DynamicObject store, boolean condition, JSContext ctx) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        TypedArray typedArray = typedArrayGetArrayType(store, condition);
        return typedArray.lengthInt(store, condition) * typedArray.bytesPerElement();
    }

    public static int getByteLength(DynamicObject store, boolean condition, JSContext ctx, ValueProfile profile) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        TypedArray typedArray = profile.profile(typedArrayGetArrayType(store, condition));
        return typedArray.lengthInt(store, condition) * typedArray.bytesPerElement();
    }

    public static int getByteOffset(DynamicObject store, boolean condition, JSContext ctx) {
        assert JSArrayBufferView.isJSArrayBufferView(store);
        if (JSArrayBufferView.hasDetachedBuffer(store, ctx)) {
            return 0;
        }
        return typedArrayGetOffset(store, condition);
    }

    @TruffleBoundary
    @Override
    public Object getHelper(DynamicObject store, Object thisObj, long index) {
        return getOwnHelper(store, thisObj, index);
    }

    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index) {
        checkDetachedView(store);
        return typedArrayGetArrayType(store).getElement(store, index);
    }

    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        checkDetachedView(thisObj);
        typedArrayGetArrayType(thisObj).setElement(thisObj, index, value, isStrict);
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict) {
        if (JSRuntime.isString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(key);
            if (numericIndex != Undefined.instance) {
                return JSRuntime.integerIndexedElementSet(thisObj, numericIndex, value);
            }
        }
        return super.set(thisObj, key, value, receiver, isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, long index) {
        return hasOwnProperty(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, Object key) {
        if (JSRuntime.isString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(key);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasProperty(thisObj, key);
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long index) {
        checkDetachedView(thisObj);
        return typedArrayGetArrayType(thisObj).hasElement(thisObj, index);
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        if (JSRuntime.isString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(key);
            if (numericIndex != Undefined.instance) {
                return hasNumericIndex(thisObj, numericIndex);
            }
        }
        return super.hasOwnProperty(thisObj, key);
    }

    private static boolean hasNumericIndex(DynamicObject thisObj, Object numericIndex) {
        DynamicObject buffer = JSArrayBufferView.getArrayBuffer(thisObj);
        checkDetachedBuffer(buffer);
        if (!JSRuntime.isInteger(numericIndex)) {
            return false;
        }
        double d = JSRuntime.doubleValue((Number) numericIndex);
        if (JSRuntime.isNegativeZero(d) || d < 0) {
            return false;
        }
        if (d >= JSArrayBufferView.typedArrayGetLength(thisObj, JSArrayBufferView.isJSArrayBufferView(thisObj))) {
            return false;
        }
        return true;
    }

    /**
     * 9.4.5.4 [[Get]] for Integer Indexed exotic object.
     */
    @TruffleBoundary
    @Override
    public Object getHelper(DynamicObject store, Object thisObj, Object key) {
        if (JSRuntime.isString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(key);
            if (numericIndex != Undefined.instance) {
                return JSRuntime.integerIndexedElementGet(store, numericIndex);
            }
        }
        return super.getHelper(store, thisObj, key);
    }

    public static DynamicObject createArrayBufferView(JSContext context, DynamicObject arrayBuffer, TypedArray arrayType, int offset, int length) {
        CompilerAsserts.partialEvaluationConstant(arrayType);
        assert JSArrayBuffer.isJSAbstractBuffer(arrayBuffer);
        if (!context.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        DynamicObjectFactory objectFactory = arrayType.isDirect() ? context.getDirectArrayBufferViewFactory(arrayType.getFactory()) : context.getArrayBufferViewFactory(arrayType.getFactory());
        return createArrayBufferView(context, objectFactory, arrayBuffer, arrayType, offset, length);
    }

    public static DynamicObject createArrayBufferView(JSContext context, DynamicObjectFactory objectFactory, DynamicObject arrayBuffer, TypedArray arrayType, int offset, int length) {
        Object backingStorage = arrayType.isDirect() ? JSArrayBuffer.getDirectByteBuffer(arrayBuffer) : JSArrayBuffer.getByteArray(arrayBuffer);
        return createArrayBufferView(context, objectFactory, arrayBuffer, arrayType, offset, length, backingStorage, false);
    }

    private static DynamicObject createArrayBufferView(JSContext context, DynamicObjectFactory objectFactory, DynamicObject arrayBuffer, TypedArray arrayType, int offset, int length,
                    Object backingStorage, boolean shareable) {
        assert offset >= 0 && offset + length * arrayType.bytesPerElement() <= (arrayType.isDirect() ? ((ByteBuffer) backingStorage).limit() : ((byte[]) backingStorage).length);
        assert offset != 0 == arrayType.hasOffset();

        // (backingArray, typedArrayType, arrayBuffer, length, offset)
        DynamicObject arrayBufferView = JSObject.create(context, objectFactory, backingStorage, arrayType, arrayBuffer, length, offset, shareable);
        assert JSArrayBuffer.isJSAbstractBuffer(arrayBuffer);
        assert isJSArrayBufferView(arrayBufferView);
        return arrayBufferView;
    }

    private static DynamicObject createArrayBufferViewPrototype(JSRealm realm, DynamicObject ctor, int bytesPerElement, TypedArrayFactory factory, DynamicObject taPrototype) {
        JSContext context = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, taPrototype, context.getEcmaScriptVersion() < 6 ? INSTANCE : JSUserObject.INSTANCE);
        if (context.getEcmaScriptVersion() < 6) {
            JSObjectUtil.putHiddenProperty(prototype, BYTE_ARRAY_PROPERTY, new byte[0]);
            JSObjectUtil.putHiddenProperty(prototype, ARRAY_TYPE_PROPERTY, factory.createArrayType(context.isOptionDirectByteBuffer(), false));
            JSObjectUtil.putHiddenProperty(prototype, ARRAY_BUFFER_PROPERTY, JSArrayBuffer.createArrayBuffer(context, 0));
            JSObjectUtil.putHiddenProperty(prototype, ARRAY_LENGTH_PROPERTY, 0);
            JSObjectUtil.putHiddenProperty(prototype, ARRAY_OFFSET_PROPERTY, 0);
            JSObjectUtil.putHiddenProperty(prototype, ARRAY_SHAREABLE, false);
        }
        JSObjectUtil.putDataProperty(context, prototype, BYTES_PER_ELEMENT, bytesPerElement, JSAttributes.notConfigurableNotEnumerableNotWritable());
        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        return prototype;
    }

    protected static void putArrayBufferViewPrototypeGetter(JSRealm realm, DynamicObject prototype, String key, ArrayBufferViewGetter getter) {
        JSContext context = realm.getContext();
        DynamicObject lengthGetter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private ArrayBufferViewGetter getterNode = getter;
            private final BranchProfile errorBranch = BranchProfile.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (JSObject.isDynamicObject(obj)) {
                    DynamicObject view = JSObject.castJSObject(obj);
                    boolean condition = isJSArrayBufferView(view);
                    if (condition) {
                        return getter.apply(view, condition);
                    }
                }
                errorBranch.enter();
                throw Errors.createTypeError("method called on incompatible receiver").setRealm(realm);
            }
        }), 0, "get " + key));
        JSObjectUtil.putConstantAccessorProperty(context, prototype, key, lengthGetter, Undefined.instance, JSAttributes.configurableNotEnumerable());
    }

    private abstract static class ArrayBufferViewGetter extends Node {
        public abstract Object apply(DynamicObject view, boolean condition);
    }

    public static Shape makeInitialArrayBufferViewShape(JSContext ctx, DynamicObject prototype, boolean direct) {
        // assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape childTree = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        // hidden properties
        if (direct) {
            childTree = childTree.addProperty(BYTE_BUFFER_PROPERTY);
        } else {
            childTree = childTree.addProperty(BYTE_ARRAY_PROPERTY);
        }
        childTree = childTree.addProperty(ARRAY_TYPE_PROPERTY);
        childTree = childTree.addProperty(ARRAY_BUFFER_PROPERTY);
        childTree = childTree.addProperty(ARRAY_LENGTH_PROPERTY);
        childTree = childTree.addProperty(ARRAY_OFFSET_PROPERTY);
        childTree = childTree.addProperty(ARRAY_SHAREABLE);
        return childTree;
    }

    public static JSConstructor createConstructor(JSRealm realm, TypedArrayFactory factory, JSConstructor taConstructor) {
        JSContext ctx = realm.getContext();
        DynamicObject arrayBufferViewConstructor = realm.lookupFunction(JSConstructor.BUILTINS, factory.getName());
        JSObject.setPrototype(arrayBufferViewConstructor, taConstructor.getFunctionObject());

        DynamicObject arrayBufferViewPrototype = createArrayBufferViewPrototype(realm, arrayBufferViewConstructor, factory.bytesPerElement(), factory, taConstructor.getPrototype());
        JSObjectUtil.putConstructorPrototypeProperty(ctx, arrayBufferViewConstructor, arrayBufferViewPrototype);
        JSObjectUtil.putDataProperty(ctx, arrayBufferViewConstructor, BYTES_PER_ELEMENT, factory.bytesPerElement(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        putConstructorSpeciesGetter(realm, arrayBufferViewConstructor);
        return new JSConstructor(arrayBufferViewConstructor, arrayBufferViewPrototype);
    }

    private static DynamicObject createTypedArrayPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);
        putArrayBufferViewPrototypeGetter(realm, prototype, LENGTH, new ArrayBufferViewGetter() {
            @Override
            public Object apply(DynamicObject view, boolean condition) {
                if (JSArrayBufferView.hasDetachedBuffer(view, ctx)) {
                    return 0;
                }
                return typedArrayGetLength(view, condition);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BUFFER, new ArrayBufferViewGetter() {
            @Override
            public Object apply(DynamicObject view, boolean condition) {
                return getArrayBuffer(view, condition);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BYTE_LENGTH, new ArrayBufferViewGetter() {
            @Override
            public Object apply(DynamicObject view, boolean condition) {
                return getByteLength(view, condition, ctx);
            }
        });
        putArrayBufferViewPrototypeGetter(realm, prototype, BYTE_OFFSET, new ArrayBufferViewGetter() {
            @Override
            public Object apply(DynamicObject view, boolean condition) {
                return getByteOffset(view, condition, ctx);
            }
        });
        DynamicObject toStringTagGetter = JSFunction.create(realm, JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(ctx.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (JSObject.isDynamicObject(obj)) {
                    DynamicObject view = JSObject.castJSObject(obj);
                    if (isJSArrayBufferView(view)) {
                        return typedArrayGetName(view);
                    }
                }
                return Undefined.instance;
            }
        }), 0, "get [Symbol.toStringTag]"));
        JSObjectUtil.putConstantAccessorProperty(ctx, prototype, Symbol.SYMBOL_TO_STRING_TAG, toStringTagGetter, Undefined.instance, JSAttributes.configurableNotEnumerable());
        // The initial value of the @@iterator property is the same function object as the initial
        // value of the %TypedArray%.prototype.values property.
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_ITERATOR, prototype.get("values"), JSAttributes.getDefaultNotEnumerable());
        // %TypedArray%.prototype.toString is the same function object as Array.prototype.toString
        JSObjectUtil.putDataProperty(ctx, prototype, "toString", realm.getArrayConstructor().getPrototype().get("toString"), JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    public static JSConstructor createTypedArrayConstructor(JSRealm realm) {
        JSContext ctx = realm.getContext();
        DynamicObject taConstructor = realm.lookupFunction(JSConstructor.BUILTINS, TYPED_ARRAY_CLASS_NAME);
        DynamicObject taPrototype = createTypedArrayPrototype(realm, taConstructor);
        JSObjectUtil.putConstructorPrototypeProperty(ctx, taConstructor, taPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, taConstructor, CLASS_NAME);
        putConstructorSpeciesGetter(realm, taConstructor);
        return new JSConstructor(taConstructor, taPrototype);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return typedArrayGetName(object);
    }

    public static boolean isJSArrayBufferView(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSArrayBufferView((DynamicObject) obj);
    }

    public static boolean isJSArrayBufferView(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
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
    public List<Object> ownPropertyKeys(DynamicObject thisObj) {
        final List<Object> keys = super.ownPropertyKeysList(thisObj);
        List<Object> list = new ArrayList<>(Math.min(1000, keys.size() + typedArrayGetLength(thisObj)));
        for (int i = 0; i < typedArrayGetLength(thisObj); i++) {
            list.add(Boundaries.stringValueOf(i));
        }
        list.addAll(keys);
        return list;
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object propertyKey, PropertyDescriptor descriptor, boolean doThrow) {
        if (JSRuntime.isString(propertyKey)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(propertyKey);
            if (numericIndex != Undefined.instance) {
                boolean success = defineOwnPropertyIndex(thisObj, numericIndex, descriptor);
                if (doThrow && !success) {
                    // path only hit in V8CompatibilityMode; see JSRuntime.definePropertyOrThrow
                    throw Errors.createTypeError("Cannot defineOwnProperty on TypedArray");
                }
                return success;
            }
        }
        return super.defineOwnProperty(thisObj, propertyKey, descriptor, doThrow);
    }

    @TruffleBoundary
    private static boolean defineOwnPropertyIndex(DynamicObject thisObj, Object numericIndex, PropertyDescriptor desc) {
        if (!JSRuntime.isInteger(numericIndex)) {
            return false;
        }
        double dIndex = ((Number) numericIndex).doubleValue();
        if (JSRuntime.isNegativeZero(dIndex) || dIndex < 0) {
            return false;
        }
        if ((long) dIndex >= JSArrayBufferView.typedArrayGetLength(thisObj)) {
            return false;
        }
        if (desc.isAccessorDescriptor()) {
            return false;
        }
        if (desc.hasConfigurable() && desc.getConfigurable()) {
            return false;
        }
        if (desc.hasEnumerable() && !desc.getEnumerable()) {
            return false;
        }
        if (desc.hasWritable() && !desc.getWritable()) {
            return false;
        }
        if (desc.hasValue()) {
            JSRuntime.integerIndexedElementSet(thisObj, (int) dIndex, desc.getValue());
        }
        return true;
    }

    @Override
    public boolean setIntegrityLevel(DynamicObject thisObj, boolean freeze) {
        preventExtensions(thisObj);
        if (freeze && typedArrayGetLength(thisObj) > 0) {
            throwCannotRedefine();
        }
        return true;
    }

    private static void throwCannotRedefine() {
        throw Errors.createTypeError("Cannot redefine a property of an object with external array elements");
    }

    public static class DefaultJSArrayBufferViewComparator implements Comparator<Object> {
        @SuppressWarnings("unchecked")
        @Override
        public int compare(Object first, Object second) {
            return ((Comparable<Object>) first).compareTo(second);
        }
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object property) {
        assert JSRuntime.isPropertyKey(property);
        if (JSRuntime.isString(property)) {
            long numericIndex = JSRuntime.propertyKeyToIntegerIndex(property);
            if (numericIndex >= 0) {
                Object value = getOwnHelper(thisObj, thisObj, numericIndex);
                if (value == Undefined.instance) {
                    return null;
                }
                return PropertyDescriptor.createData(value, true, true, false);
            }
        }
        return ordinaryGetOwnProperty(thisObj, property);
    }

    private static void checkDetachedBuffer(DynamicObject thisObj) {
        if (JSArrayBuffer.isDetachedBuffer(thisObj)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
    }

    private static void checkDetachedView(DynamicObject thisObj) {
        if (JSArrayBufferView.hasDetachedBuffer(thisObj)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
    }

    @Override
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToConsoleString(obj, typedArrayGetName(obj));
        }
    }

    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        if (JSRuntime.isString(key)) {
            Object numericIndex = JSRuntime.canonicalNumericIndexString(key);
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

}

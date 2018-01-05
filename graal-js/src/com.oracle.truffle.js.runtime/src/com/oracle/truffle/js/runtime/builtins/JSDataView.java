/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.EnumSet;
import java.util.function.Function;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSDataView extends JSBuiltinObject implements JSConstructorFactory.Default {

    public static final String CLASS_NAME = "DataView";

    private static final JSDataView INSTANCE = new JSDataView();

    private static final String BYTE_LENGTH = "byteLength";
    private static final String BUFFER = "buffer";
    private static final String BYTE_OFFSET = "byteOffset";
    private static final HiddenKey ARRAY_BUFFER_ID = new HiddenKey("arrayBuffer");
    private static final HiddenKey OFFSET_ID = new HiddenKey("offset");
    private static final HiddenKey LENGTH_ID = new HiddenKey(JSAbstractArray.LENGTH);
    private static final Property ARRAY_BUFFER_PROPERTY;
    private static final Property ARRAY_LENGTH_PROPERTY;
    private static final Property ARRAY_OFFSET_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        ARRAY_BUFFER_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_BUFFER_ID, allocator.locationForType(JSObject.CLASS, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
        ARRAY_LENGTH_PROPERTY = JSObjectUtil.makeHiddenProperty(LENGTH_ID, allocator.locationForType(int.class));
        ARRAY_OFFSET_PROPERTY = JSObjectUtil.makeHiddenProperty(OFFSET_ID, allocator.locationForType(int.class));
    }

    public static int typedArrayGetLength(DynamicObject thisObj) {
        return (int) ARRAY_LENGTH_PROPERTY.get(thisObj, JSDataView.isJSDataView(thisObj));
    }

    public static int typedArrayGetLength(DynamicObject thisObj, boolean condition) {
        return (int) ARRAY_LENGTH_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetLength(DynamicObject thisObj, int length) {
        ARRAY_LENGTH_PROPERTY.setSafe(thisObj, length, null);
    }

    public static int typedArrayGetOffset(DynamicObject thisObj) {
        return (int) ARRAY_OFFSET_PROPERTY.get(thisObj, JSDataView.isJSDataView(thisObj));
    }

    public static int typedArrayGetOffset(DynamicObject thisObj, boolean condition) {
        return (int) ARRAY_OFFSET_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetOffset(DynamicObject thisObj, int arrayOffset) {
        ARRAY_OFFSET_PROPERTY.setSafe(thisObj, arrayOffset, null);
    }

    private JSDataView() {
    }

    public static DynamicObject getArrayBuffer(DynamicObject thisObj) {
        assert JSDataView.isJSDataView(thisObj);
        return (DynamicObject) ARRAY_BUFFER_PROPERTY.get(thisObj, JSDataView.isJSDataView(thisObj));
    }

    public static DynamicObject createDataView(JSContext context, DynamicObject arrayBuffer, int offset, int length) {
        assert offset >= 0 && offset + length <= (JSArrayBuffer.isJSDirectOrSharedArrayBuffer(arrayBuffer) ? JSArrayBuffer.getDirectByteLength(arrayBuffer) : JSArrayBuffer.getByteLength(arrayBuffer));

        // (arrayBuffer, length, offset)
        DynamicObject dataView = JSObject.create(context, context.getDataViewFactory(), arrayBuffer, length, offset);
        assert JSArrayBuffer.isJSHeapArrayBuffer(arrayBuffer) || JSArrayBuffer.isJSDirectOrSharedArrayBuffer(arrayBuffer);
        assert isJSDataView(dataView);
        return dataView;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        putGetters(realm, prototype);
        JSObjectUtil.putDataProperty(context, prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        return prototype;
    }

    private static void putGetters(JSRealm realm, DynamicObject prototype) {
        putGetter(realm, prototype, BUFFER, view -> getArrayBuffer(view));
        putGetter(realm, prototype, BYTE_LENGTH, view -> typedArrayGetLengthChecked(view));
        putGetter(realm, prototype, BYTE_OFFSET, view -> typedArrayGetOffsetChecked(view));
    }

    public static int typedArrayGetLengthChecked(DynamicObject thisObj) {
        if (JSArrayBuffer.isDetachedBuffer(JSDataView.getArrayBuffer(thisObj))) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        return typedArrayGetLength(thisObj);
    }

    public static int typedArrayGetOffsetChecked(DynamicObject thisObj) {
        if (JSArrayBuffer.isDetachedBuffer(JSDataView.getArrayBuffer(thisObj))) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        return typedArrayGetOffset(thisObj);
    }

    private static void putGetter(JSRealm realm, DynamicObject prototype, String name, Function<DynamicObject, Object> function) {
        JSContext context = realm.getContext();
        DynamicObject getter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            private final BranchProfile notDataViewBranch = BranchProfile.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (JSObject.isDynamicObject(obj)) {
                    DynamicObject dataView = JSObject.castJSObject(obj);
                    if (isJSDataView(dataView)) {
                        return function.apply(dataView);
                    }
                }
                notDataViewBranch.enter();
                throw Errors.createTypeErrorNotADataView().setRealm(realm);
            }
        }), 0, "get " + name));
        JSObjectUtil.putConstantAccessorProperty(context, prototype, name, getter, Undefined.instance, JSAttributes.configurableNotEnumerable());
    }

    public static Shape makeInitialArrayBufferViewShape(JSContext ctx, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape childTree = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        // hidden properties
        childTree = childTree.addProperty(ARRAY_BUFFER_PROPERTY);
        childTree = childTree.addProperty(ARRAY_LENGTH_PROPERTY);
        childTree = childTree.addProperty(ARRAY_OFFSET_PROPERTY);
        return childTree;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    public static boolean isJSDataView(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSDataView((DynamicObject) obj);
    }

    public static boolean isJSDataView(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }
}

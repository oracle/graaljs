/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.EnumSet;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

public final class JSMap extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctionsAndSpecies {

    public static final JSMap INSTANCE = new JSMap();

    public static final String CLASS_NAME = "Map";
    public static final String PROTOTYPE_NAME = "Map.prototype";

    private static final String SIZE = "size";

    private static final HiddenKey MAP_ID = new HiddenKey("map");
    private static final Property MAP_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        MAP_PROPERTY = JSObjectUtil.makeHiddenProperty(MAP_ID, allocator.locationForType(JSHashMap.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSMap() {
    }

    public static JSHashMap getInternalMap(DynamicObject obj) {
        assert isJSMap(obj);
        return (JSHashMap) MAP_PROPERTY.get(obj, isJSMap(obj));
    }

    public static int getMapSize(DynamicObject obj) {
        assert isJSMap(obj);
        return getInternalMap(obj).size();
    }

    private static DynamicObject createSizeGetterFunction(JSRealm realm) {
        JSContext context = realm.getContext();
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = frame.getArguments()[0];
                if (JSMap.isJSMap(obj)) {
                    return JSMap.getMapSize((DynamicObject) obj);
                } else {
                    throw Errors.createTypeError("Map expected");
                }
            }
        });
        DynamicObject sizeGetter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, callTarget, 0, "get " + SIZE));
        JSObject.preventExtensions(sizeGetter);
        return sizeGetter;
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        // sets the size just for the prototype
        JSObjectUtil.putConstantAccessorProperty(ctx, prototype, SIZE, createSizeGetterFunction(realm), Undefined.instance, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        return prototype;
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSMap.INSTANCE, context);
        initialShape = initialShape.addProperty(MAP_PROPERTY);
        return initialShape;
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

    @Override
    @TruffleBoundary
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return "[" + getClassName() + "]";
        } else {
            JSHashMap map = JSMap.getInternalMap(obj);
            return JSRuntime.collectionToConsoleString(obj, getClassName(obj), map);
        }
    }

    public static boolean isJSMap(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSMap((DynamicObject) obj);
    }

    public static boolean isJSMap(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }
}

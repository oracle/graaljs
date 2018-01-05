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

public final class JSSet extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctionsAndSpecies {

    public static final JSSet INSTANCE = new JSSet();

    public static final String CLASS_NAME = "Set";
    public static final String PROTOTYPE_NAME = "Set.prototype";

    private static final String SIZE = "size";

    private static final HiddenKey SET_ID = new HiddenKey("set");
    private static final Property SET_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        SET_PROPERTY = JSObjectUtil.makeHiddenProperty(SET_ID, allocator.locationForType(JSHashMap.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSSet() {
    }

    public static Object normalize(Object value) {
        if (value instanceof Double) {
            return normalizeDouble((Double) value);
        }
        return value;
    }

    public static Object normalizeDouble(double value) {
        if (JSRuntime.isNegativeZero(value)) {
            return 0;
        } else if (JSRuntime.doubleIsRepresentableAsInt(value)) {
            return (int) value;
        }
        return value;
    }

    public static JSHashMap getInternalSet(DynamicObject obj) {
        assert isJSSet(obj);
        return (JSHashMap) SET_PROPERTY.get(obj, isJSSet(obj));
    }

    public static int getSetSize(DynamicObject obj) {
        assert isJSSet(obj);
        return getInternalSet(obj).size();
    }

    private static DynamicObject createSizeGetterFunction(JSRealm realm) {
        JSContext context = realm.getContext();
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = frame.getArguments()[0];
                if (JSSet.isJSSet(obj)) {
                    return JSSet.getSetSize((DynamicObject) obj);
                } else {
                    throw Errors.createTypeError("Set expected");
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
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSSet.INSTANCE, context);
        initialShape = initialShape.addProperty(SET_PROPERTY);
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
            JSHashMap set = JSSet.getInternalSet(obj);
            return JSRuntime.collectionToConsoleString(obj, getClassName(obj), set);
        }
    }

    public static boolean isJSSet(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSSet((DynamicObject) obj);
    }

    public static boolean isJSSet(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }
}

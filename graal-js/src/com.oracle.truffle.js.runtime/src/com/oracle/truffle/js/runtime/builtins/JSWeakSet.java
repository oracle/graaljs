/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.EnumSet;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSWeakSet extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions {

    public static final JSWeakSet INSTANCE = new JSWeakSet();

    public static final String CLASS_NAME = "WeakSet";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    private static final HiddenKey WEAKSET_ID = new HiddenKey("weakset");
    private static final Property WEAKSET_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        WEAKSET_PROPERTY = JSObjectUtil.makeHiddenProperty(WEAKSET_ID, allocator.locationForType(Map.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSWeakSet() {
    }

    @SuppressWarnings("unchecked")
    public static Map<DynamicObject, Object> getInternalWeakMap(DynamicObject obj) {
        assert isJSWeakSet(obj);
        return (Map<DynamicObject, Object>) WEAKSET_PROPERTY.get(obj, isJSWeakSet(obj));
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        return prototype;
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSWeakSet.INSTANCE, context);
        initialShape = initialShape.addProperty(WEAKSET_PROPERTY);
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
            return getClassName();
        }
    }

    public static boolean isJSWeakSet(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSWeakSet((DynamicObject) obj);
    }

    public static boolean isJSWeakSet(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }
}

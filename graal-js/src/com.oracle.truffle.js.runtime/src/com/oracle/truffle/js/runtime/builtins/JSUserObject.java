/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSUserObject extends JSBuiltinObject {

    public static final String TYPE_NAME = "object";
    public static final String CLASS_NAME = "Object";
    public static final String PROTOTYPE_NAME = "Object.prototype";

    public static final JSUserObject INSTANCE = new JSUserObject();

    private JSUserObject() {
    }

    public static DynamicObject create(JSContext context) {
        return JSObject.create(context, context.getInitialUserObjectShape());
    }

    public static DynamicObject create(JSRealm realm) {
        return JSObject.create(realm.getContext(), realm.getInitialUserObjectShape());
    }

    public static DynamicObject createWithPrototype(DynamicObject prototype, JSRealm realm) {
        assert prototype == null || JSRuntime.isObject(prototype);
        return JSObject.create(realm, prototype, INSTANCE);
    }

    public static DynamicObject createWithPrototype(DynamicObject prototype, JSContext context) {
        assert prototype == null || JSRuntime.isObject(prototype);
        return JSObject.create(context, prototype, INSTANCE);
    }

    public static DynamicObject createWithPrototypeInObject(DynamicObject prototype, JSContext context) {
        assert prototype == null || JSRuntime.isObject(prototype);
        Shape shape = context.getEmptyShapePrototypeInObject();
        DynamicObject obj = JSObject.create(context, shape);
        JSShape.getPrototypeProperty(shape).setSafe(obj, prototype, null);
        return obj;
    }

    public static boolean isJSUserObject(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSUserObject((DynamicObject) obj);
    }

    public static boolean isJSUserObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    @TruffleBoundary
    public String getClassName(DynamicObject object) {
        JSContext context = JSObject.getJSContext(object);
        if (context.getEcmaScriptVersion() <= 5) {
            Object toStringTag = get(object, Symbol.SYMBOL_TO_STRING_TAG);
            if (JSRuntime.isString(toStringTag)) {
                return Boundaries.javaToString(toStringTag);
            }
        }
        return CLASS_NAME;
    }

    @TruffleBoundary
    @Override
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToConsoleString(obj, null);
        }
    }

    @TruffleBoundary
    @Override
    public Object get(DynamicObject thisObj, long index) {
        // convert index only once
        return get(thisObj, String.valueOf(index));
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject obj) {
        return true;
    }
}

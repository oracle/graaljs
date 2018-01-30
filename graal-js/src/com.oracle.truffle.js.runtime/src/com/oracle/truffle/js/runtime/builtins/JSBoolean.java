/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSBoolean extends JSPrimitiveObject implements JSConstructorFactory.Default {

    public static final String TYPE_NAME = "boolean";
    public static final String CLASS_NAME = "Boolean";
    public static final String PROTOTYPE_NAME = "Boolean.prototype";
    public static final String TRUE_NAME = "true";
    public static final String FALSE_NAME = "false";

    public static final JSBoolean INSTANCE = new JSBoolean();

    private static final HiddenKey VALUE_ID = new HiddenKey("value");
    private static final Property VALUE_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        VALUE_PROPERTY = JSObjectUtil.makeHiddenProperty(VALUE_ID, allocator.locationForType(boolean.class));
    }

    private JSBoolean() {
    }

    public static DynamicObject create(JSContext context, boolean value) {
        DynamicObject obj = JSObject.create(context, context.getBooleanFactory(), value);
        assert isJSBoolean(obj);
        return obj;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject booleanPrototype = JSObject.create(realm, realm.getObjectPrototype(), INSTANCE);
        JSObjectUtil.putHiddenProperty(booleanPrototype, VALUE_PROPERTY, Boolean.FALSE);
        JSObjectUtil.putConstructorProperty(ctx, booleanPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, booleanPrototype, PROTOTYPE_NAME);
        return booleanPrototype;
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        initialShape = initialShape.addProperty(VALUE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static boolean valueOf(DynamicObject obj) {
        assert isJSBoolean(obj);
        return (boolean) VALUE_PROPERTY.get(obj, isJSBoolean(obj));
    }

    public static boolean isJSBoolean(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSBoolean((DynamicObject) obj);
    }

    public static boolean isJSBoolean(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
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
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }

    @TruffleBoundary
    public static JSException noBooleanError() {
        throw Errors.createTypeError("not a Boolean object");
    }

    @TruffleBoundary
    @Override
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return "[Boolean " + valueOf(obj) + "]";
        } else {
            boolean primitiveValue = JSBoolean.valueOf(obj);
            return JSRuntime.objectToConsoleString(obj, getBuiltinToStringTag(obj),
                            new String[]{JSRuntime.PRIMITIVE_VALUE}, new Object[]{primitiveValue});
        }
    }
}

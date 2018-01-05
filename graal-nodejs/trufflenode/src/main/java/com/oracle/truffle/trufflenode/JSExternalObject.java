/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

/**
 *
 * @author Jan Stola
 */
public final class JSExternalObject extends JSBuiltinObject {

    public static final String CLASS_NAME = "external";
    public static final JSExternalObject INSTANCE = new JSExternalObject();

    private static final HiddenKey POINTER_KEY = new HiddenKey("pointer");
    private static final Property POINTER_PROPERTY;

    private JSExternalObject() {
    }

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        POINTER_PROPERTY = JSObjectUtil.makeHiddenProperty(POINTER_KEY, allocator.locationForType(Long.class));
    }

    public static DynamicObject create(JSContext context, long pointer) {
        ContextData contextData = GraalJSAccess.getContextData(context);
        DynamicObject obj = contextData.getExternalObjectShape().newInstance();
        setPointer(obj, pointer);
        return obj;
    }

    public static Shape makeInitialShape(JSContext ctx) {
        Shape initialShape = ctx.getEmptyShape().changeType(INSTANCE);
        initialShape = initialShape.addProperty(POINTER_PROPERTY);
        return initialShape;
    }

    public static boolean isJSExternalObject(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSExternalObject((DynamicObject) obj);
    }

    public static boolean isJSExternalObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    public static void setPointer(DynamicObject obj, long pointer) {
        assert isJSExternalObject(obj);
        POINTER_PROPERTY.setSafe(obj, pointer, null);
    }

    public static long getPointer(DynamicObject obj) {
        assert isJSExternalObject(obj);
        return (long) POINTER_PROPERTY.get(obj, isJSExternalObject(obj));
    }

}

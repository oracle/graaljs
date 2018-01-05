/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSGlobalObject extends JSBuiltinObject {

    public static final String CLASS_NAME = "global";
    public static final String EVAL_NAME = "eval";

    public static final JSGlobalObject INSTANCE = new JSGlobalObject();

    private JSGlobalObject() {
    }

    public static DynamicObject create(JSRealm realm, DynamicObject objectPrototype) {
        CompilerAsserts.neverPartOfCompilation();
        JSContext context = realm.getContext();
        // keep a separate shape tree for the global object in order not to pollute user objects
        Shape shape = JSShape.makeUniqueRootWithPrototype(JSObject.LAYOUT, INSTANCE, context, objectPrototype);
        return JSObject.create(context, shape);
    }

    public static boolean isJSGlobalObject(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSGlobalObject((DynamicObject) obj);
    }

    public static boolean isJSGlobalObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }
}

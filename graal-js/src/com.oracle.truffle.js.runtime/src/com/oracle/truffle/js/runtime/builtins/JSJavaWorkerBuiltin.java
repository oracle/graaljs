/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.EcmaAgent;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSJavaWorkerBuiltin extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctionsAndSpecies {

    public static final String CLASS_NAME = "JavaInteropWorker";
    public static final String PROTOTYPE_NAME = "JavaInteropWorker.prototype";

    public static final JSJavaWorkerBuiltin INSTANCE = new JSJavaWorkerBuiltin();

    private static final Property AGENT_PROPERTY;
    private static final HiddenKey AGENT = new HiddenKey("Agent");

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        AGENT_PROPERTY = JSObjectUtil.makeHiddenProperty(AGENT, allocator.locationForType(EcmaAgent.class));
    }

    private JSJavaWorkerBuiltin() {
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSJavaWorkerBuiltin.INSTANCE, context);
        initialShape = initialShape.addProperty(AGENT_PROPERTY);
        return initialShape;
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

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    public static boolean isJSInteropWorker(Object obj) {
        return isInstance(obj, INSTANCE);
    }

    public static EcmaAgent getAgent(DynamicObject worker) {
        assert isJSInteropWorker(worker);
        return (EcmaAgent) AGENT_PROPERTY.get(worker, false);
    }

    public static JSConstructor createWorkerConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

}

/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode.UninitializedDefinePropertyNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode.UninitializedPropertySetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.JSAttributes;

@ImportStatic(JSTruffleOptions.class)
public abstract class CreateDataPropertyNode extends JavaScriptBaseNode {
    private final JSContext context;
    protected final Object key;

    protected CreateDataPropertyNode(JSContext context, Object key) {
        this.context = context;
        this.key = key;
    }

    public static CreateDataPropertyNode create(JSContext context, Object key) {
        return CreateDataPropertyNodeGen.create(context, key);
    }

    public abstract void executeVoid(Object object, Object value);

    @Specialization(guards = {"oldShape.check(object)", "oldShape.getProperty(key) == null"}, assumptions = {"oldShape.getValidAssumption()"}, limit = "PropertyCacheLimit")
    protected static void doCached(DynamicObject object, Object value, //
                    @SuppressWarnings("unused") @Cached("object.getShape()") Shape oldShape, //
                    @Cached("makeDefinePropertyCache(object)") PropertySetNode propertyCache) {
        propertyCache.setValue(object, value);
    }

    @Specialization(guards = "isJSObject(object)", replaces = {"doCached"})
    protected final void doUncached(DynamicObject object, Object value) {
        JSRuntime.createDataProperty(object, key, value);
    }

    @Specialization(guards = "!isJSObject(object)")
    protected final void doNonObject(Object object, @SuppressWarnings("unused") Object value) {
        throw Errors.createTypeErrorNotAnObject(object, this);
    }

    protected final PropertySetNode makeDefinePropertyCache(DynamicObject object) {
        Shape shape = object.getShape();
        if (shape.getProperty(key) == null) {
            return new UninitializedDefinePropertyNode(key, shape, context, JSAttributes.getDefault());
        } else {
            return new UninitializedPropertySetNode(key, false, context, true, JSAttributes.getDefault());
        }
    }
}

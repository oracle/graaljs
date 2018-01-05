/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode.UninitializedDefinePropertyNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode.UninitializedPropertySetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ImportStatic(JSTruffleOptions.class)
@NodeChildren({@NodeChild("object"), @NodeChild("value")})
public abstract class CreateMethodPropertyNode extends JavaScriptNode {
    private final JSContext context;
    protected final Object key;

    protected CreateMethodPropertyNode(JSContext context, Object key) {
        this.context = context;
        this.key = key;
    }

    public static CreateMethodPropertyNode create(JSContext context, Object key) {
        return create(context, key, null, null);
    }

    public static CreateMethodPropertyNode create(JSContext context, Object key, JavaScriptNode objectNode, JavaScriptNode valueNode) {
        return CreateMethodPropertyNodeGen.create(context, key, objectNode, valueNode);
    }

    public abstract void executeVoid(Object object, Object value);

    @Specialization(guards = {"oldShape.check(object)", "oldShape.getProperty(key) == null"}, assumptions = {"oldShape.getValidAssumption()"}, limit = "PropertyCacheLimit")
    protected static Object doCached(DynamicObject object, Object value,
                    @SuppressWarnings("unused") @Cached("object.getShape()") Shape oldShape,
                    @Cached("makeDefinePropertyCache(object)") PropertySetNode propertyCache) {
        propertyCache.setValue(object, value);
        return Undefined.instance;
    }

    @Specialization(guards = "isJSObject(object)", replaces = {"doCached"})
    protected final Object doUncached(DynamicObject object, Object value) {
        JSObject.defineOwnProperty(object, key, PropertyDescriptor.createData(value, false, true, true));
        return Undefined.instance;
    }

    @Specialization(guards = "!isJSObject(object)")
    protected static Object doNonObject(@SuppressWarnings("unused") Object object, @SuppressWarnings("unused") Object value) {
        throw Errors.createTypeErrorObjectExpected();
    }

    protected final PropertySetNode makeDefinePropertyCache(DynamicObject object) {
        Shape shape = object.getShape();
        if (shape.getProperty(key) == null) {
            return new UninitializedDefinePropertyNode(key, shape, context, JSAttributes.getDefaultNotEnumerable());
        } else {
            return new UninitializedPropertySetNode(key, false, context, true, JSAttributes.getDefaultNotEnumerable());
        }
    }

    abstract JavaScriptNode getObject();

    abstract JavaScriptNode getValue();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, key, cloneUninitialized(getObject()), cloneUninitialized(getValue()));
    }
}

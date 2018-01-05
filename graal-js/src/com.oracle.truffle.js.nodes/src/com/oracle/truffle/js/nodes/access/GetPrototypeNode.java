/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public abstract class GetPrototypeNode extends JavaScriptBaseNode {
    static final int MAX_SHAPE_COUNT = 1;

    GetPrototypeNode() {
    }

    protected abstract DynamicObject executeDynamicObject(DynamicObject obj);

    public abstract DynamicObject executeJSObject(Object obj);

    public static GetPrototypeNode create() {
        return GetPrototypeNodeGen.create();
    }

    public static JavaScriptNode create(JavaScriptNode object) {
        assert object instanceof RepeatableNode;
        class GetPrototypeOfNode extends JavaScriptNode implements RepeatableNode {
            @Child private JavaScriptNode objectNode = object;
            @Child private GetPrototypeNode getPrototypeNode = GetPrototypeNode.create();

            @Override
            public DynamicObject execute(VirtualFrame frame) {
                return getPrototypeNode.executeJSObject(objectNode.execute(frame));
            }

            @Override
            protected JavaScriptNode copyUninitialized() {
                return new GetPrototypeOfNode();
            }
        }
        return new GetPrototypeOfNode();
    }

    static Property getPrototypeProperty(Shape shape) {
        if (JSShape.getJSClass(shape) == JSProxy.INSTANCE) {
            return null;
        }
        return JSShape.getPrototypeProperty(shape);
    }

    @Specialization(guards = {"obj.getShape() == shape", "prototypeProperty != null"}, limit = "MAX_SHAPE_COUNT")
    public DynamicObject doCachedShape(DynamicObject obj,
                    @Cached("obj.getShape()") Shape shape,
                    @Cached("getPrototypeProperty(shape)") Property prototypeProperty) {
        assert !JSGuards.isJSProxy(obj);
        return (DynamicObject) prototypeProperty.get(obj, shape);
    }

    @Specialization(guards = "!isJSProxy(obj)", replaces = "doCachedShape")
    public DynamicObject doGeneric(DynamicObject obj,
                    @Cached("createClassProfile()") ValueProfile locationClass) {
        Location location = locationClass.profile(JSShape.getPrototypeProperty(obj.getShape()).getLocation());
        return (DynamicObject) location.get(obj, false);
    }

    @Specialization(guards = "isJSProxy(obj)")
    public DynamicObject doProxy(DynamicObject obj,
                    @Cached("create()") JSClassProfile jsclassProfile) {
        return JSObject.getPrototype(obj, jsclassProfile);
    }

    @Specialization(guards = "!isDynamicObject(obj)")
    public DynamicObject doNotObject(@SuppressWarnings("unused") Object obj) {
        return Null.instance;
    }
}

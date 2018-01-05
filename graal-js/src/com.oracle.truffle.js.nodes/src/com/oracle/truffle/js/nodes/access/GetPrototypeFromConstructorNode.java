/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.CompilableFunction;

public class GetPrototypeFromConstructorNode extends JavaScriptNode {
    private final CompilableFunction<JSRealm, DynamicObject> intrinsicDefaultProto;
    @Child private JavaScriptNode constructorNode;
    @Child private PropertyGetNode getPrototypeNode;
    @Child private IsObjectNode isObjectNode;

    protected GetPrototypeFromConstructorNode(JSContext context, JavaScriptNode constructorNode, CompilableFunction<JSRealm, DynamicObject> intrinsicDefaultProto) {
        this.constructorNode = constructorNode;
        this.intrinsicDefaultProto = intrinsicDefaultProto;
        this.getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);
        this.isObjectNode = IsObjectNode.create();
    }

    public static GetPrototypeFromConstructorNode create(JSContext context, JavaScriptNode constructorNode, CompilableFunction<JSRealm, DynamicObject> intrinsicDefaultProto) {
        return new GetPrototypeFromConstructorNode(context, constructorNode, intrinsicDefaultProto);
    }

    @Override
    public final DynamicObject execute(VirtualFrame frame) {
        return executeDynamicObject(frame);
    }

    @Override
    public DynamicObject executeDynamicObject(VirtualFrame frame) {
        Object constructor = constructorNode.execute(frame);
        return executeWithConstructor((DynamicObject) constructor);
    }

    public DynamicObject executeWithConstructor(DynamicObject constructor) {
        assert JSRuntime.isCallable(constructor);
        Object proto = getPrototypeNode.getValue(constructor);
        if (isObjectNode.executeBoolean(proto)) {
            assert JSRuntime.isObject(proto);
            return (DynamicObject) proto;
        } else {
            JSRealm realm = JSFunction.getRealm(constructor);
            return intrinsicDefaultProto.apply(realm);
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new GetPrototypeFromConstructorNode(getPrototypeNode.getContext(), cloneUninitialized(constructorNode), intrinsicDefaultProto);
    }
}

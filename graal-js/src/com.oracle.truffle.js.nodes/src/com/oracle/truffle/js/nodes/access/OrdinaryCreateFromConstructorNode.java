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
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.util.CompilableFunction;

public class OrdinaryCreateFromConstructorNode extends JavaScriptNode {
    @Child private GetPrototypeFromConstructorNode getPrototypeFromConstructorNode;
    @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;

    protected OrdinaryCreateFromConstructorNode(JSContext context, JavaScriptNode constructorNode, CompilableFunction<JSRealm, DynamicObject> intrinsicDefaultProto, JSClass jsclass) {
        this.getPrototypeFromConstructorNode = GetPrototypeFromConstructorNode.create(context, constructorNode, intrinsicDefaultProto);
        this.createObjectNode = CreateObjectNode.createWithCachedPrototype(context, null, jsclass);
    }

    private OrdinaryCreateFromConstructorNode(GetPrototypeFromConstructorNode getPrototypeFromConstructorNode, CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode) {
        this.getPrototypeFromConstructorNode = getPrototypeFromConstructorNode;
        this.createObjectNode = createObjectNode;
    }

    public static OrdinaryCreateFromConstructorNode create(JSContext context, JavaScriptNode constructorNode, CompilableFunction<JSRealm, DynamicObject> intrinsicDefaultProto, JSClass jsclass) {
        return new OrdinaryCreateFromConstructorNode(context, constructorNode, intrinsicDefaultProto, jsclass);
    }

    @Override
    public final DynamicObject execute(VirtualFrame frame) {
        return executeDynamicObject(frame);
    }

    @Override
    public DynamicObject executeDynamicObject(VirtualFrame frame) {
        DynamicObject proto = getPrototypeFromConstructorNode.executeDynamicObject(frame);
        return executeWithPrototype(frame, proto);
    }

    public DynamicObject executeWithConstructor(VirtualFrame frame, DynamicObject constructor) {
        DynamicObject proto = getPrototypeFromConstructorNode.executeWithConstructor(constructor);
        return executeWithPrototype(frame, proto);
    }

    private DynamicObject executeWithPrototype(VirtualFrame frame, DynamicObject proto) {
        return createObjectNode.executeDynamicObject(frame, proto);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new OrdinaryCreateFromConstructorNode(cloneUninitialized(getPrototypeFromConstructorNode), createObjectNode.copyUninitialized());
    }
}

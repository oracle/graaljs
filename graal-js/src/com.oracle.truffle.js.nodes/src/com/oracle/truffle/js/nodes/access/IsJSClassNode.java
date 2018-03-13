/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.builtins.JSClass;

public abstract class IsJSClassNode extends JSUnaryNode {

    protected static final int MAX_SHAPE_COUNT = 1;

    private final JSClass jsclass;

    protected IsJSClassNode(JSClass jsclass, JavaScriptNode operand) {
        super(operand);
        this.jsclass = jsclass;
    }

    public abstract boolean executeBoolean(Object obj);

    @Specialization(guards = "cachedShape.check(object)", limit = "MAX_SHAPE_COUNT")
    @SuppressWarnings("unused")
    protected static boolean doIsInstanceShape(DynamicObject object, //
                    @Cached("object.getShape()") Shape cachedShape, //
                    @Cached("doIsInstance(object)") boolean cachedResult) {
        return cachedResult;
    }

    @Specialization(replaces = "doIsInstanceShape")
    protected boolean doIsInstanceObject(DynamicObject object) {
        return jsclass.isInstance(object);
    }

    @Specialization(replaces = "doIsInstanceObject")
    protected boolean doIsInstance(Object object) {
        return jsclass.isInstance(object);
    }

    public static IsJSClassNode create(JSClass clazz) {
        return create(clazz, null);
    }

    public static IsJSClassNode create(JSClass clazz, JavaScriptNode operand) {
        return IsJSClassNodeGen.create(clazz, operand);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(jsclass, cloneUninitialized(getOperand()));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }
}

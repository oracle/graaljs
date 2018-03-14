/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;

public abstract class RequireObjectNode extends JavaScriptNode {
    protected static final int MAX_SHAPE_COUNT = 1;

    @Child @Executed protected JavaScriptNode operandNode;

    protected RequireObjectNode(JavaScriptNode operand) {
        this.operandNode = operand;
    }

    public abstract Object execute(Object obj);

    @Specialization(guards = "cachedShape.check(object)", limit = "MAX_SHAPE_COUNT")
    protected static Object doObjectShape(DynamicObject object,
                    @SuppressWarnings("unused") @Cached("object.getShape()") Shape cachedShape,
                    @Cached("isJSObject(object)") boolean cachedResult) {
        return requireObject(object, cachedResult);
    }

    @Specialization(replaces = "doObjectShape")
    protected static Object doObject(Object object) {
        return requireObject(object, JSGuards.isJSObject(object));
    }

    private static Object requireObject(Object object, boolean isObject) {
        if (isObject) {
            return object;
        } else {
            throw Errors.createTypeErrorIncompatibleReceiver(object);
        }
    }

    public static RequireObjectNode create() {
        return RequireObjectNodeGen.create(null);
    }

    public static JavaScriptNode create(JavaScriptNode operand) {
        return RequireObjectNodeGen.create(operand);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return RequireObjectNodeGen.create(cloneUninitialized(operandNode));
    }
}

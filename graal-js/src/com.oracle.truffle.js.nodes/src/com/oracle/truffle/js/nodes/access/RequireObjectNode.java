/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;

@NodeChild("operand")
public abstract class RequireObjectNode extends JavaScriptNode {
    protected static final int MAX_SHAPE_COUNT = 1;

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
            throw throwTypeError(object);
        }
    }

    @TruffleBoundary
    private static JSException throwTypeError(Object object) {
        return Errors.createTypeError("method called on incompatible receiver " + object.toString());
    }

    public static RequireObjectNode create() {
        return RequireObjectNodeGen.create(null);
    }

    public static JavaScriptNode create(JavaScriptNode operand) {
        return RequireObjectNodeGen.create(operand);
    }

    abstract JavaScriptNode getOperand();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return RequireObjectNodeGen.create(cloneUninitialized(getOperand()));
    }
}

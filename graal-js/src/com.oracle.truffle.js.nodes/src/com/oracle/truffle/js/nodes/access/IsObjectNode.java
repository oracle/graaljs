/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsObjectNodeGen.IsObjectWrappedNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Checks whether the argument is an object.
 */
@ImportStatic(JSObject.class)
public abstract class IsObjectNode extends JavaScriptBaseNode {

    protected static final int MAX_SHAPE_COUNT = 1;
    protected static final int MAX_JSCLASS_COUNT = 1;

    public abstract boolean executeBoolean(Object obj);

    @SuppressWarnings("unused")
    @Specialization(guards = "cachedShape.check(object)", limit = "MAX_SHAPE_COUNT")
    protected static boolean isObjectShape(DynamicObject object,
                    @Cached("object.getShape()") Shape cachedShape,
                    @Cached("guardIsJSObject(object)") boolean cachedResult) {
        return cachedResult;
    }

    @SuppressWarnings("unused")
    @Specialization(replaces = "isObjectShape", guards = "cachedClass.isInstance(object)", limit = "MAX_JSCLASS_COUNT")
    protected static boolean isObjectJSClass(DynamicObject object,
                    @Cached("getJSClass(object)") JSClass cachedClass,
                    @Cached("guardIsJSObject(object)") boolean cachedResult) {
        return cachedResult;
    }

    @Specialization(replaces = {"isObjectShape", "isObjectJSClass"})
    protected static boolean isObject(Object object,
                    @Cached("createBinaryProfile()") ConditionProfile resultProfile) {
        return resultProfile.profile(JSRuntime.isObject(object));
    }

    public static IsObjectNode create() {
        return IsObjectNodeGen.create();
    }

    // name-clash with JSObject.isJSObject. Different behavior around null/undefined.
    protected boolean guardIsJSObject(DynamicObject obj) {
        return JSGuards.isJSObject(obj);
    }

    /**
     * Wrapper of @link{IsObjectNode} when you really need a JavaScriptNode. IsObjectNode is a
     * JavaScriptBaseNode for footprint reasons.
     */
    public abstract static class IsObjectWrappedNode extends JSUnaryNode {

        @Child private IsObjectNode isObjectNode;

        protected IsObjectWrappedNode() {
            this.isObjectNode = IsObjectNode.create();
        }

        @Specialization
        protected boolean doObject(Object operand) {
            return isObjectNode.executeBoolean(operand);
        }

        public static JavaScriptNode create(JavaScriptNode operand) {
            return IsObjectWrappedNodeGen.create(operand);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return IsObjectWrappedNodeGen.create(cloneUninitialized(getOperand()));
        }
    }
}

/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsArrayNodeGen.IsArrayWrappedNodeGen;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Non-standard IsArray. Checks for array exotic objects.
 *
 * @see JSIsArrayNode
 */
public abstract class IsArrayNode extends JavaScriptBaseNode {

    protected static final int MAX_SHAPE_COUNT = 1;
    protected static final int MAX_JSCLASS_COUNT = 1;

    private final boolean onlyArray;
    private final boolean fastArray;
    private final boolean fastAndTypedArray;

    protected IsArrayNode(boolean onlyArray, boolean fastArray, boolean fastAndTypedArray) {
        this.onlyArray = onlyArray;
        this.fastArray = fastArray;
        this.fastAndTypedArray = fastAndTypedArray;
    }

    public abstract boolean execute(TruffleObject operand);

    @Specialization(guards = "cachedShape.check(object)", limit = "MAX_SHAPE_COUNT")
    protected static boolean doIsArrayShape(DynamicObject object, //
                    @Cached("object.getShape()") Shape cachedShape, //
                    @Cached("isArray(object)") boolean cachedResult) {
        // (aw) must do the shape check again to preserve the unsafe condition,
        // otherwise we could just do: return cachedResult;
        return cachedResult && cachedShape.check(object);
    }

    protected static JSClass getJSClass(DynamicObject object) {
        return JSObject.getJSClass(object);
    }

    @SuppressWarnings("unused")
    @Specialization(replaces = "doIsArrayShape", guards = "cachedClass.isInstance(object)", limit = "MAX_JSCLASS_COUNT")
    protected static boolean doIsArrayJSClass(DynamicObject object, //
                    @Cached("getJSClass(object)") JSClass cachedClass, //
                    @Cached("isArray(object)") boolean cachedResult) {
        return cachedResult;
    }

    @Specialization(replaces = "doIsArrayJSClass")
    protected final boolean isArray(DynamicObject object) {
        if (fastAndTypedArray) {
            return JSArray.isJSFastArray(object) || JSArgumentsObject.isJSFastArgumentsObject(object) || JSArrayBufferView.isJSArrayBufferView(object);
        } else if (fastArray) {
            return JSArray.isJSFastArray(object);
        } else if (onlyArray) {
            return JSArray.isJSArray(object);
        } else {
            return JSObject.hasArray(object);
        }
    }

    @Specialization(guards = "!isDynamicObject(object)")
    protected static boolean isNotDynamicObject(@SuppressWarnings("unused") TruffleObject object) {
        return false;
    }

    public static IsArrayNode createIsAnyArray() {
        return IsArrayNodeGen.create(false, false, false);
    }

    public static IsArrayNode createIsArray() {
        return IsArrayNodeGen.create(true, false, false);
    }

    public static IsArrayNode createIsFastArray() {
        return IsArrayNodeGen.create(true, true, false);
    }

    public static IsArrayNode createIsFastOrTypedArray() {
        return IsArrayNodeGen.create(true, true, true);
    }

    /**
     * Wrapper of @link{IsArrayNode} when you really need a JavaScriptNode. IsArrayNode is a
     * JavaScriptBaseNode for footprint reasons.
     */
    public abstract static class IsArrayWrappedNode extends JSUnaryNode {

        @Child private IsArrayNode isArrayNode;

        protected IsArrayWrappedNode(JavaScriptNode operandNode, IsArrayNode isArrayNode) {
            super(operandNode);
            this.isArrayNode = isArrayNode;
        }

        @Specialization
        protected boolean doObject(Object operand) {
            if (JSObject.isDynamicObject(operand)) {
                return isArrayNode.execute((DynamicObject) operand);
            } else {
                return false;
            }
        }

        public static JavaScriptNode createIsArray(JavaScriptNode operand) {
            return IsArrayWrappedNodeGen.create(operand, IsArrayNode.createIsArray());
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return createIsArray(cloneUninitialized(getOperand()));
        }
    }
}

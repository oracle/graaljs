/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.binary.JSEqualNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * This node optimizes the check whether the argument is null or undefined. Used from the
 * {@link JSEqualNode} for optimizing {@code a == undefined;} and {@code a == null;}
 *
 */
@ImportStatic(JSInteropUtil.class)
public abstract class JSIsNullOrUndefinedNode extends JSUnaryNode {
    protected static final int MAX_SHAPE_COUNT = 1;
    protected static final int MAX_TYPE_COUNT = 1;
    protected static final int MAX_CLASSES = 3;

    @Specialization(guards = "isJSNull(operand)")
    protected static boolean doNull(@SuppressWarnings("unused") Object operand) {
        return true;
    }

    @Specialization(guards = "isUndefined(operand)")
    protected static boolean doUndefined(@SuppressWarnings("unused") Object operand) {
        return true;
    }

    @Specialization
    protected static boolean doSymbol(@SuppressWarnings("unused") Symbol operand) {
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSObject", "object.getShape() == cachedShape"}, limit = "MAX_SHAPE_COUNT")
    protected static boolean doJSObjectCachedShape(DynamicObject object,
                    @Cached("isJSType(object)") boolean isJSObject,
                    @Cached("object.getShape()") Shape cachedShape) {
        assert !JSGuards.isNullOrUndefined(object);
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSObject", "object.getShape().getObjectType() == cachedType"}, replaces = {"doJSObjectCachedShape"}, limit = "MAX_TYPE_COUNT")
    protected static boolean doJSObjectCachedType(DynamicObject object,
                    @Cached("isJSType(object)") boolean isJSObject,
                    @Cached("object.getShape().getObjectType()") ObjectType cachedType) {
        assert !JSGuards.isNullOrUndefined(object);
        return false;
    }

    @Specialization(guards = {"isJSType(object)"}, replaces = {"doJSObjectCachedType"})
    protected static boolean doJSObject(DynamicObject object,
                    @Cached("createBinaryProfile()") ConditionProfile resultProfile) {
        return resultProfile.profile(!JSRuntime.isObject(object));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"operand != null", "cachedClass != null", "cachedClass == operand.getClass()"}, limit = "MAX_CLASSES")
    protected static boolean doJSValueCached(Object operand,
                    @Cached("getNonTruffleObjectClass(operand)") Class<?> cachedClass) {
        return false;
    }

    @Specialization(guards = {"isJSType(operand)"}, replaces = {"doJSValueCached"})
    protected static boolean doJSValueJSObject(DynamicObject operand) {
        return JSGuards.isNullOrUndefined(operand);
    }

    @Specialization(guards = {"!isTruffleObject(operand)"}, replaces = {"doJSValueCached"})
    protected static boolean doJSValue(@SuppressWarnings("unused") Object operand) {
        return false;
    }

    @Specialization(guards = "isForeignObject(operand)")
    protected boolean doForeign(TruffleObject operand,
                    @Cached("createIsNull()") Node isNullNode) {
        return ForeignAccess.sendIsNull(isNullNode, operand);
    }

    public static JSIsNullOrUndefinedNode create(JavaScriptNode operand) {
        return JSIsNullOrUndefinedNodeGen.create(operand);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSIsNullOrUndefinedNodeGen.create(cloneUninitialized(getOperand()));
    }
}

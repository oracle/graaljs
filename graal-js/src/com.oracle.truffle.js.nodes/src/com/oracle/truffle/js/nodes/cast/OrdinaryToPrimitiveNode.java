/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.Hint;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Implements OrdinaryToPrimitive (O, hint).
 */
public class OrdinaryToPrimitiveNode extends JavaScriptBaseNode {
    private final Hint hint;
    private final JSContext context;
    private final ConditionProfile toStringFunctionProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile valueOfFunctionProfile = ConditionProfile.createBinaryProfile();
    @Child private PropertyNode getToString;
    @Child private PropertyNode getValueOf;
    @Child private IsCallableNode isCallable;
    @Child private JSFunctionCallNode callToString;
    @Child private JSFunctionCallNode callValueOf;
    @Child private IsPrimitiveNode isPrimitive;

    protected OrdinaryToPrimitiveNode(JSContext context, Hint hint) {
        assert hint == Hint.String || hint == Hint.Number;
        this.hint = hint;
        this.context = context;
        this.isCallable = IsCallableNode.create();
        this.isPrimitive = IsPrimitiveNode.create();
    }

    public Object execute(DynamicObject object) {
        assert JSGuards.isJSObject(object);
        if (hint == Hint.String) {
            return doHintString(object);
        } else {
            assert hint == Hint.Number;
            return doHintNumber(object);
        }
    }

    public static OrdinaryToPrimitiveNode createHintString(JSContext context) {
        return create(context, Hint.String);
    }

    public static OrdinaryToPrimitiveNode createHintNumber(JSContext context) {
        return create(context, Hint.Number);
    }

    public static OrdinaryToPrimitiveNode create(JSContext context, Hint hint) {
        return new OrdinaryToPrimitiveNode(context, hint);
    }

    protected Object doHintString(DynamicObject object) {
        Object toString = getToString().executeWithTarget(object);
        if (toStringFunctionProfile.profile(isCallable.executeBoolean(toString))) {
            Object result = callToString.executeCall(JSArguments.createZeroArg(object, toString));
            if (isPrimitive.executeBoolean(result)) {
                return result;
            }
        }

        Object valueOf = getValueOf().executeWithTarget(object);
        if (valueOfFunctionProfile.profile(isCallable.executeBoolean(valueOf))) {
            Object result = callValueOf.executeCall(JSArguments.createZeroArg(object, valueOf));
            if (isPrimitive.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue();
    }

    protected Object doHintNumber(DynamicObject object) {
        assert JSGuards.isJSObject(object);
        Object valueOf = getValueOf().executeWithTarget(object);
        if (valueOfFunctionProfile.profile(isCallable.executeBoolean(valueOf))) {
            Object result = callValueOf.executeCall(JSArguments.createZeroArg(object, valueOf));
            if (isPrimitive.executeBoolean(result)) {
                return result;
            }
        }

        Object toString = getToString().executeWithTarget(object);
        if (toStringFunctionProfile.profile(isCallable.executeBoolean(toString))) {
            Object result = callToString.executeCall(JSArguments.createZeroArg(object, toString));
            if (isPrimitive.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue();
    }

    private PropertyNode getToString() {
        if (getToString == null || callToString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getToString = insert(PropertyNode.createMethod(context, null, JSRuntime.TO_STRING));
            callToString = insert(JSFunctionCallNode.createCall());
        }
        return getToString;
    }

    private PropertyNode getValueOf() {
        if (getValueOf == null || callValueOf == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getValueOf = insert(PropertyNode.createMethod(context, null, JSRuntime.VALUE_OF));
            callValueOf = insert(JSFunctionCallNode.createCall());
        }
        return getValueOf;
    }
}

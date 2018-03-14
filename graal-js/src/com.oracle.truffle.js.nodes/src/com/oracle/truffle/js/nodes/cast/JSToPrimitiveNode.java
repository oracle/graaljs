/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This implements ECMA 7.1.1 ToPrimitive.
 *
 */
public abstract class JSToPrimitiveNode extends JavaScriptBaseNode {

    public enum Hint {
        None,
        String,
        Number
    }

    protected final Hint hint;

    protected JSToPrimitiveNode(Hint hint) {
        this.hint = hint;
    }

    public abstract Object execute(Object value);

    public static JSToPrimitiveNode createHintNone() {
        return create(Hint.None);
    }

    public static JSToPrimitiveNode createHintString() {
        return create(Hint.String);
    }

    public static JSToPrimitiveNode createHintNumber() {
        return create(Hint.Number);
    }

    public static JSToPrimitiveNode create(Hint hint) {
        return JSToPrimitiveNodeGen.create(hint);
    }

    @Specialization
    protected int doInt(int value) {
        return value;
    }

    @Specialization
    protected LargeInteger doLargeInteger(LargeInteger value) {
        return value;
    }

    @Specialization
    protected double doDouble(double value) {
        return value;
    }

    @Specialization
    protected boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization
    protected CharSequence doString(CharSequence value) {
        return value;
    }

    @Specialization
    protected Symbol doSymbol(Symbol value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected DynamicObject doNull(@SuppressWarnings("unused") Object value) {
        return Null.instance;
    }

    @Specialization(guards = "isUndefined(value)")
    protected DynamicObject doUndefined(@SuppressWarnings("unused") Object value) {
        return Undefined.instance;
    }

    @Specialization(guards = "isJSSIMD(value)")
    protected DynamicObject doSIMD(DynamicObject value) {
        return value;
    }

    @Specialization(guards = "isJSObject(object)")
    protected Object doJSObject(DynamicObject object,
                    @Cached("createGetToPrimitive(object)") PropertyNode getToPrimitive,
                    @Cached("create()") IsPrimitiveNode isPrimitive,
                    @Cached("createOrdinaryToPrimitive(object)") OrdinaryToPrimitiveNode ordinaryToPrimitive,
                    @Cached("createBinaryProfile()") ConditionProfile exoticToPrimProfile,
                    @Cached("createCall()") JSFunctionCallNode callExoticToPrim) {
        Object exoticToPrim = getToPrimitive.executeWithTarget(object);
        if (exoticToPrimProfile.profile(!JSRuntime.isNullOrUndefined(exoticToPrim))) {
            Object result = callExoticToPrim.executeCall(JSArguments.createOneArg(object, exoticToPrim, getHintName()));
            if (isPrimitive.executeBoolean(result)) {
                return result;
            }
            throw Errors.createTypeError("[Symbol.toPrimitive] method returned a non-primitive object", this);
        }

        return ordinaryToPrimitive.execute(object);
    }

    private String getHintName() {
        switch (hint) {
            case Number:
                return JSRuntime.HINT_NUMBER;
            case String:
                return JSRuntime.HINT_STRING;
            case None:
            default:
                return JSRuntime.HINT_DEFAULT;
        }
    }

    protected final boolean isHintString() {
        return hint == Hint.String;
    }

    protected final boolean isHintNumber() {
        return hint == Hint.Number || hint == Hint.None;
    }

    @Specialization(guards = "isTruffleJavaObject(object)")
    protected Object doTruffleJavaObject(TruffleObject object) {
        Object javaObject = JavaInterop.asJavaObject(object);
        return (javaObject == null) ? Null.instance : doGeneric(javaObject);
    }

    @Specialization(guards = {"isForeignObject(object)", "!isTruffleJavaObject(object)"})
    protected Object doCrossLanguage(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode unboxOrGet) {
        return unboxOrGet.executeWithTarget(object);
    }

    @Fallback
    protected static Object doGeneric(Object value) {
        assert value != null;
        if (value instanceof Number) {
            return value;
        }
        return JSRuntime.toJSNull(Boundaries.javaToString(value));
    }

    protected static PropertyNode createGetToPrimitive(DynamicObject object) {
        JSContext context = JSObject.getJSContext(object);
        return PropertyNode.createMethod(context, null, Symbol.SYMBOL_TO_PRIMITIVE);
    }

    protected OrdinaryToPrimitiveNode createOrdinaryToPrimitive(DynamicObject object) {
        JSContext context = JSObject.getJSContext(object);
        return OrdinaryToPrimitiveNode.create(context, isHintString() ? Hint.String : Hint.Number);
    }
}

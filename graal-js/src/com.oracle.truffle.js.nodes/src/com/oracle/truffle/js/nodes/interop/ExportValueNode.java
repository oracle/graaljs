/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.PropertyReference;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;

/**
 * This node prepares the export of a value via Interop. It transforms values not allowed in Truffle
 * (e.g. {@link JSLazyString}) and binds Functions. See also {@link JSRuntime#exportValue(Object)}.
 *
 * @see JSRuntime#exportValue(Object)
 */
@ImportStatic(JSTruffleOptions.class)
public abstract class ExportValueNode extends JavaScriptBaseNode {
    ExportValueNode() {
    }

    public abstract Object executeWithTarget(Object property, Object thiz);

    @Specialization(guards = {"isJSFunction(function)", "isUndefined(thiz)"})
    protected static TruffleObject doFunctionUndefinedThis(DynamicObject function, @SuppressWarnings("unused") Object thiz) {
        return function;
    }

    @Specialization(guards = {"isJSFunction(function)", "!isUndefined(thiz)", "!isBoundJSFunction(function)", "!BindProgramResult"})
    protected static TruffleObject doNotBindUnboundFunction(DynamicObject function, @SuppressWarnings("unused") Object thiz) {
        return function;
    }

    @Specialization(guards = {"isJSFunction(function)", "!isUndefined(thiz)", "!isBoundJSFunction(function)", "BindProgramResult"})
    protected static TruffleObject doBindUnboundFunction(DynamicObject function, Object thiz) {
        return new InteropBoundFunction(function, thiz);
    }

    @Specialization(guards = {"isJSFunction(function)", "isBoundJSFunction(function)"})
    protected static TruffleObject doBoundFunction(DynamicObject function, @SuppressWarnings("unused") Object thiz) {
        return function;
    }

    @Specialization
    protected static String doLazyString(PropertyReference value, @SuppressWarnings("unused") Object thiz) {
        return value.toString();
    }

    @Specialization
    protected static double doLargeInteger(LargeInteger value, @SuppressWarnings("unused") Object thiz) {
        return value.doubleValue();
    }

    @Specialization(guards = {"!isJSFunction(value)"})
    protected static DynamicObject doObject(DynamicObject value, @SuppressWarnings("unused") Object thiz) {
        return value;
    }

    @Specialization
    protected static int doInt(int value, @SuppressWarnings("unused") Object thiz) {
        return value;
    }

    @Specialization
    protected static double doDouble(double value, @SuppressWarnings("unused") Object thiz) {
        return value;
    }

    @Specialization
    protected static boolean doBoolean(boolean value, @SuppressWarnings("unused") Object thiz) {
        return value;
    }

    @Specialization
    protected static String doString(String value, @SuppressWarnings("unused") Object thiz) {
        return value;
    }

    @Specialization(guards = {"!isJSFunction(value)"}, replaces = "doObject")
    protected static TruffleObject doTruffleObject(TruffleObject value, @SuppressWarnings("unused") Object thiz) {
        return value;
    }

    @Specialization
    protected static Object doJavaClass(JavaClass clazz, @SuppressWarnings("unused") Object thiz) {
        return JavaInterop.asTruffleObject(clazz.getType());
    }

    @TruffleBoundary
    @Fallback
    protected static Object doOther(Object value, @SuppressWarnings("unused") Object thiz) {
        assert !(value instanceof TruffleObject);
        return JavaInterop.asTruffleValue(value);
    }

    public static ExportValueNode create() {
        return ExportValueNodeGen.create();
    }
}

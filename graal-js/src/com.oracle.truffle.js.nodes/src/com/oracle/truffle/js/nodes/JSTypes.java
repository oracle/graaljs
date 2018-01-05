/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyReference;

/**
 * @see JavaScriptNode
 */
@TypeSystem({boolean.class, int.class, double.class, long.class, LargeInteger.class, String.class, DynamicObject.class, TruffleObject.class, char.class, byte.class, float.class, Object[].class})
public class JSTypes {

    @ImplicitCast
    public static double intToDouble(int value) {
        return value;
    }

    @ImplicitCast
    public static double largeIntegerToDouble(LargeInteger value) {
        return value.doubleValue();
    }

    @TypeCast(double.class)
    public static double asDouble(Object value) {
        return ((Double) value).doubleValue();
    }

    @TypeCheck(CharSequence.class)
    public static boolean isCharSequence(Object value) {
        return JSRuntime.isString(value);
    }

    @TypeCheck(DynamicObject.class)
    public static boolean isDynamicObject(Object value) {
        return JSObject.isDynamicObject(value);
    }

    @ImplicitCast
    public static String castString(JSLazyString value) {
        return value.toString();
    }

    @ImplicitCast
    public static String castString(PropertyReference value) {
        return value.toString();
    }

    @ImplicitCast
    public static CharSequence castCharSequence(String value) {
        return value;
    }

    @ImplicitCast
    public static CharSequence castCharSequence(JSLazyString value) {
        return value;
    }

    @ImplicitCast
    public static CharSequence castCharSequence(PropertyReference value) {
        return value;
    }

    @TypeCheck(JavaClass.class)
    public static boolean isJavaClass(Object value) {
        return JSTruffleOptions.NashornJavaInterop ? value instanceof JavaClass : false;
    }

    @TypeCheck(JavaMethod.class)
    public static boolean isJavaMethod(Object value) {
        return JSTruffleOptions.NashornJavaInterop ? value instanceof JavaMethod : false;
    }
}

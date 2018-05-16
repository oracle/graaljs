/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.BigInt;
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
@TypeSystem({boolean.class, int.class, double.class, long.class, LargeInteger.class, BigInt.class, String.class, DynamicObject.class, TruffleObject.class, char.class, byte.class, float.class,
                Object[].class})
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

/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;

/**
 * This node prepares the import of a value from Interop. It transforms values allowed in Truffle,
 * but not supported in Graal.js (e.g. {@link Long}).
 *
 * @see JSRuntime#importValue(Object)
 */
public abstract class JSForeignToJSTypeNode extends JavaScriptBaseNode {
    public abstract Object executeWithTarget(Object target);

    public static JSForeignToJSTypeNode create() {
        return JSForeignToJSTypeNodeGen.create();
    }

    @Specialization
    public int fromInt(int value) {
        return value;
    }

    @Specialization
    public double fromDouble(double value) {
        return value;
    }

    @Specialization
    public String fromString(String value) {
        return value;
    }

    @Specialization
    public Object fromNumber(Number value,
                    @Cached("createBinaryProfile()") ConditionProfile longCase,
                    @Cached("createBinaryProfile()") ConditionProfile byteCase,
                    @Cached("createBinaryProfile()") ConditionProfile shortCase) {
        if (value instanceof Double || value instanceof Integer) {
            return value;
        } else if (byteCase.profile(value instanceof Byte)) {
            return ((Byte) value).intValue();
        } else if (shortCase.profile(value instanceof Short)) {
            return ((Short) value).intValue();
        } else if (longCase.profile(value instanceof Long)) {
            long lValue = value.longValue();
            if (JSRuntime.longIsRepresentableAsInt(lValue)) {
                return (int) lValue;
            } else if (JSRuntime.MIN_SAFE_INTEGER_LONG <= lValue && lValue <= JSRuntime.MAX_SAFE_INTEGER_LONG) {
                return LargeInteger.valueOf(lValue);
            }
            return (double) lValue;
        }
        return JSRuntime.doubleValueVirtual(value);
    }

    @Specialization
    public Object fromBoolean(boolean value) {
        return value;
    }

    @Specialization
    public Object fromChar(char value) {
        return String.valueOf(value);
    }

    @Specialization(guards = "isJavaNull(value)")
    public Object isNull(@SuppressWarnings("unused") Object value) {
        return Null.instance;
    }

    @Specialization
    public Object fromTruffleJavaObject(TruffleObject value) {
        if (JavaInterop.isJavaObject(value)) {
            Object object = JavaInterop.asJavaObject(value);
            if (object == null) {
                return Null.instance;
            }
            if (JSTruffleOptions.NashornJavaInterop) {
                if (object instanceof Class) {
                    return JavaClass.forClass((Class<?>) object);
                } else {
                    return object;
                }
            }
        } else if (value instanceof InteropBoundFunction) {
            return ((InteropBoundFunction) value).getFunction();
        }
        return value;
    }

    @TruffleBoundary
    @Fallback
    public Object fallbackCase(Object value) {
        throw Errors.createTypeError("type " + value.getClass().getSimpleName() + " not supported in JavaScript");
    }
}

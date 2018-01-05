/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.interop.*;
import com.oracle.truffle.js.runtime.objects.*;

public abstract class JSPrimitiveObject extends JSBuiltinObject {
    protected JSPrimitiveObject() {
    }

    @TruffleBoundary
    @Override
    public final Object getHelper(DynamicObject store, Object thisObj, Object key) {
        assert this == JSNumber.INSTANCE || this == JSString.INSTANCE || this == JSBoolean.INSTANCE;

        Object propertyValue = super.getHelper(store, thisObj, key);

        if (JSTruffleOptions.NashornJavaInterop && !(JSObject.isDynamicObject(thisObj))) {
            if (propertyValue == null && key instanceof String) {
                return getJavaProperty(thisObj, (String) key);
            }
        }

        return propertyValue;
    }

    private static Object getJavaProperty(Object thisObj, String name) {
        JavaClass type = JavaClass.forClass(thisObj.getClass());
        JavaMember member = type.getMember(name, JavaClass.INSTANCE, JavaClass.GETTER_METHOD, false);
        if (member instanceof JavaGetter) {
            return ((JavaGetter) member).getValue(thisObj);
        }
        return member;
    }

    @TruffleBoundary
    @Override
    public Object getMethodHelper(DynamicObject store, Object thisObj, Object name) {
        if (JSTruffleOptions.NashornJavaInterop && !(JSObject.isDynamicObject(thisObj)) && name instanceof String) {
            if (hasOwnProperty(store, name)) {
                Object method = getJavaMethod(thisObj, (String) name);
                if (method != null) {
                    return method;
                }
            }
        }

        return super.getMethodHelper(store, thisObj, name);
    }

    private static Object getJavaMethod(Object thisObj, String name) {
        JavaClass type = JavaClass.forClass(thisObj.getClass());
        JavaMember member = type.getMember(name, JavaClass.INSTANCE, JavaClass.METHOD, false);
        return member;
    }
}

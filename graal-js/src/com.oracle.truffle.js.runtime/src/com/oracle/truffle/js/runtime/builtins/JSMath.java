/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

/**
 * see MathBuiltins.
 */
public final class JSMath {

    public static final String CLASS_NAME = "Math";

    private JSMath() {
    }

    public static DynamicObject create(JSRealm realm) {
        JSContext ctx = realm.getContext();
        DynamicObject obj = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putDataProperty(ctx, obj, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());

        JSObjectUtil.putDataProperty(ctx, obj, "E", Math.E);
        JSObjectUtil.putDataProperty(ctx, obj, "PI", Math.PI);
        JSObjectUtil.putDataProperty(ctx, obj, "LN10", 2.302585092994046);
        JSObjectUtil.putDataProperty(ctx, obj, "LN2", 0.6931471805599453);
        JSObjectUtil.putDataProperty(ctx, obj, "LOG2E", 1.4426950408889634);
        JSObjectUtil.putDataProperty(ctx, obj, "LOG10E", 0.4342944819032518);
        JSObjectUtil.putDataProperty(ctx, obj, "SQRT1_2", 0.7071067811865476);
        JSObjectUtil.putDataProperty(ctx, obj, "SQRT2", 1.4142135623730951);

        JSObjectUtil.putFunctionsFromContainer(realm, obj, CLASS_NAME);
        return obj;
    }
}

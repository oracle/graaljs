/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSTestV8 {

    public static final String CLASS_NAME = "TestV8";

    private JSTestV8() {
    }

    public static DynamicObject create(JSRealm realm) {
        JSContext ctx = realm.getContext();
        DynamicObject obj = JSUserObject.create(realm);
        JSObjectUtil.putDataProperty(ctx, obj, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putDataProperty(ctx, obj, "stringMaxLength", JSTruffleOptions.StringLengthLimit);
        JSObjectUtil.putFunctionsFromContainer(realm, obj, CLASS_NAME);
        return obj;
    }
}

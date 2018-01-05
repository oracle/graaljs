/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSJava {

    public static final String CLASS_NAME = "Java";

    private static final String JAVA_WORKER_PROPERTY_NAME = "Worker";

    private JSJava() {
    }

    public static DynamicObject create(JSRealm realm) {
        DynamicObject obj = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putDataProperty(realm.getContext(), obj, JAVA_WORKER_PROPERTY_NAME, realm.getJavaInteropWorkerConstructor().getFunctionObject(),
                        JSAttributes.configurableNotEnumerableNotWritable());
        return obj;
    }
}

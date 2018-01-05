/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class SIMD {
    public static final String CLASS_NAME = "SIMD";

    public static Object create(JSRealm realm) {
        DynamicObject obj = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        return obj;
    }
}

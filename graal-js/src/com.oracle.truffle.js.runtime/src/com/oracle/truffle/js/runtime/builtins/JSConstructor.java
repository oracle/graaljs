/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;

public final class JSConstructor {
    public static final String BUILTINS = "%Constructors%";

    private final DynamicObject constructor;
    private final DynamicObject prototype;

    public JSConstructor(DynamicObject constructor, DynamicObject prototype) {
        this.constructor = constructor;
        this.prototype = prototype;
    }

    public DynamicObject getFunctionObject() {
        return constructor;
    }

    public DynamicObject getPrototype() {
        return prototype;
    }
}

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.object.*;

public final class Accessor {
    private final DynamicObject getter;
    private final DynamicObject setter;

    public Accessor(DynamicObject getter, DynamicObject setter) {
        this.getter = getter == null ? Undefined.instance : getter;
        this.setter = setter == null ? Undefined.instance : setter;
    }

    public DynamicObject getGetter() {
        return getter;
    }

    public DynamicObject getSetter() {
        return setter;
    }
}

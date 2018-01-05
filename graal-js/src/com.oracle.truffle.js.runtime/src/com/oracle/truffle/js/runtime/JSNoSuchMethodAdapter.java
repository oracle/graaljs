/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

public final class JSNoSuchMethodAdapter {
    private final Object function;
    private final Object key;
    private final Object thisObject;

    public JSNoSuchMethodAdapter(Object function, Object key, Object thisObject) {
        this.function = function;
        this.key = key;
        this.thisObject = thisObject;
    }

    public Object getFunction() {
        return function;
    }

    public Object getKey() {
        return key;
    }

    public Object getThisObject() {
        return thisObject;
    }
}

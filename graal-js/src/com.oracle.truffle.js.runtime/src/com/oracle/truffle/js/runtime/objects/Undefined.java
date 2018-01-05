/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.runtime.builtins.*;

/**
 * A singleton instance of this class represents an undefined JavaScript value.
 */
public final class Undefined {

    public static final String NAME = "undefined";
    public static final String TYPE_NAME = NAME;
    private static final JSClass UNDEFINED_CLASS = Null.NULL_CLASS;
    private static final Shape SHAPE = JSShape.makeStaticRoot(JSObject.LAYOUT, UNDEFINED_CLASS, 0);
    public static final DynamicObject instance = JSObject.create(SHAPE);

    private Undefined() {
    }
}

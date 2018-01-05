/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;

/**
 * Expect a {@code double} or an {@code int} value and coerce it to {@code double}.
 */
public abstract class AsDoubleNode extends JavaScriptBaseNode {
    public abstract double executeDouble(Object value) throws UnexpectedResultException;

    public abstract Object execute(Object value);

    public static AsDoubleNode create() {
        return AsDoubleNodeGen.create();
    }

    @Specialization
    protected static double doInteger(int value) {
        return value;
    }

    @Specialization
    protected static double doDouble(double value) {
        return value;
    }

    @Fallback
    protected static Object doNotNumber(Object value) {
        return value;
    }
}

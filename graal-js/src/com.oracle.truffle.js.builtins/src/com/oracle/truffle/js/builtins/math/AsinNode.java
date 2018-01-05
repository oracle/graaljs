/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class AsinNode extends MathOperation {
    public AsinNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @TruffleBoundary
    @Specialization
    protected static double asin(double a) {
        return Math.asin(a);
    }

    @Specialization
    protected double asin(Object a) {
        return asin(toDouble(a));
    }
}

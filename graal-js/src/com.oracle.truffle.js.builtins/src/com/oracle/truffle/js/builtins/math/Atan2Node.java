/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class Atan2Node extends MathOperation {
    public Atan2Node(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @TruffleBoundary
    @Specialization
    protected static double atan2(double a, double b) {
        return Math.atan2(a, b);
    }

    @Specialization
    protected double atan2(Object a, Object b) {
        return atan2(toDouble(a), toDouble(b));
    }
}

/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class AtanhNode extends MathOperation {

    public AtanhNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static double atanh(double x) {
        if (JSRuntime.isNegativeZero(x)) {
            return -0.0;
        }
        return Math.log((1 + x) / (1 - x)) / 2;
    }

    @Specialization
    protected double atanh(Object a) {
        return atanh(toDouble(a));
    }
}

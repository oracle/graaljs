/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class TanhNode extends MathOperation {

    public TanhNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected double tanh(double x) {
        if (JSRuntime.isNegativeZero(x)) {
            return -0.0;
        }
        if (Double.isInfinite(x)) {
            return x > 0 ? 1 : -1;
        } else {
            double y = Math.exp(2 * x);
            return (y - 1) / (y + 1);
        }
    }

    @Specialization
    protected double tanh(Object a) {
        return tanh(toDouble(a));
    }
}

/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class FroundNode extends MathOperation {

    public FroundNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static int fround(int x) {
        return x;
    }

    @Specialization
    protected static double fround(double x) {
        return (float) x;
    }

    @Specialization
    protected double fround(Object a) {
        return fround(toDouble(a));
    }
}

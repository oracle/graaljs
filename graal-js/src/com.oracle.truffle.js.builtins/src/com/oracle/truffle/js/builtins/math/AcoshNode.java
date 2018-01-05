/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class AcoshNode extends MathOperation {

    public AcoshNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static double acosh(double x) {
        return Math.log(x + Math.sqrt(x * x - 1));
    }

    @Specialization
    protected double acosh(Object a) {
        return acosh(toDouble(a));
    }
}

/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class AbsNode extends MathOperation {

    public AbsNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected static int abs(int a, @Cached("createBinaryProfile()") ConditionProfile negative) throws ArithmeticException {
        return negative.profile(a < 0) ? Math.subtractExact(0, a) : a;
    }

    @Specialization
    protected static double absIntSpecial(int a) {
        return Math.abs((double) a);
    }

    @Specialization
    protected static double abs(double a) {
        return Math.abs(a);
    }

    @Specialization
    protected double abs(Object a) {
        return abs(toDouble(a));
    }
}

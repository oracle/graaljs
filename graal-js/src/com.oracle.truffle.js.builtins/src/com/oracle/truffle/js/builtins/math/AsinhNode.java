/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class AsinhNode extends MathOperation {

    public AsinhNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private final ConditionProfile isNegative = ConditionProfile.createBinaryProfile();

    @Specialization
    protected double asinh(double x) {
        if (JSRuntime.isNegativeZero(x)) {
            return -0.0;
        }
        if (x < 0 && Double.isInfinite(x)) {
            return x;
        }
        if (isNegative.profile(x < 0)) {
            return -asinhImpl(-x);
        } else {
            return asinhImpl(x);
        }
    }

    private static double asinhImpl(double x) {
        return Math.log(x + Math.sqrt(x * x + 1));
    }

    @Specialization
    protected double asinh(Object a) {
        return asinh(toDouble(a));
    }
}

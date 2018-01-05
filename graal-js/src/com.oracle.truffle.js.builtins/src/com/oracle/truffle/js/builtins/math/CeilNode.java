/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class CeilNode extends MathOperation {

    public CeilNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static int ceil(int a) {
        return a;
    }

    @Specialization(rewriteOn = SlowPathException.class)
    protected int ceilMightReturnInt(Object a) throws SlowPathException {
        double d = toDouble(a);
        if (Double.isNaN(d) || JSRuntime.isNegativeZero(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            throw new SlowPathException();
        }
        int i = (int) d;
        int result = d > i ? i + 1 : i;
        if (result == 0 && d < 0) {
            throw new SlowPathException(); // special-case: return -0.0
        }
        return result;
    }

    @Specialization
    protected double ceilReturnsDouble(Object a,
                    @Cached("createBinaryProfile()") ConditionProfile isNaN,
                    @Cached("createBinaryProfile()") ConditionProfile isNegativeZero,
                    @Cached("createBinaryProfile()") ConditionProfile requiresNegativeZero,
                    @Cached("createBinaryProfile()") ConditionProfile fitsSafeLong) {
        double d = toDouble(a);
        if (isNaN.profile(Double.isNaN(d))) {
            return Double.NaN;
        }
        if (isNegativeZero.profile(JSRuntime.isNegativeZero(d))) {
            return -0.0;
        }
        if (fitsSafeLong.profile(JSRuntime.isSafeInteger(d))) {
            long i = (long) d;
            long result = d > i ? i + 1 : i;
            if (requiresNegativeZero.profile(result == 0 && d < 0)) {
                return -0.0;
            }
            return result;
        } else {
            return mathCeil(d);
        }
    }

    @TruffleBoundary
    private static double mathCeil(double d) {
        return Math.ceil(d);
    }
}

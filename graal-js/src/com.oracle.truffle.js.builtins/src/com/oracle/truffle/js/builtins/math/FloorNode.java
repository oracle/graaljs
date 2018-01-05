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

public abstract class FloorNode extends MathOperation {

    public FloorNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static int floor(int a) {
        return a;
    }

    @Specialization(rewriteOn = SlowPathException.class)
    protected int floorMightReturnInt(Object a,
                    @Cached("createBinaryProfile()") ConditionProfile smaller) throws SlowPathException {
        double d = toDouble(a);
        if (Double.isNaN(d) || JSRuntime.isNegativeZero(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            throw new SlowPathException();
        }
        int i = (int) d;
        return smaller.profile(d < i) ? i - 1 : i;
    }

    @Specialization
    protected double floorReturnsDouble(Object a,
                    @Cached("createBinaryProfile()") ConditionProfile isNaN,
                    @Cached("createBinaryProfile()") ConditionProfile isNegativeZero,
                    @Cached("createBinaryProfile()") ConditionProfile fitsSafeLong,
                    @Cached("createBinaryProfile()") ConditionProfile smaller) {
        double d = toDouble(a);
        if (isNaN.profile(Double.isNaN(d))) {
            return Double.NaN;
        }
        if (isNegativeZero.profile(JSRuntime.isNegativeZero(d))) {
            return -0.0;
        }
        if (fitsSafeLong.profile(JSRuntime.isSafeInteger(d))) {
            long i = (long) d;
            return smaller.profile(d < i) ? i - 1 : i;
        } else {
            return mathFloor(d);
        }
    }

    @TruffleBoundary
    private static double mathFloor(double d) {
        return Math.floor(d);
    }
}

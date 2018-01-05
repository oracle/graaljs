/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class MinNode extends MathOperation {

    public MinNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private final ConditionProfile leftSmaller = ConditionProfile.createBinaryProfile();
    private final ConditionProfile rightSmaller = ConditionProfile.createBinaryProfile();
    private final ConditionProfile bothEqual = ConditionProfile.createBinaryProfile();
    private final ConditionProfile negativeZero = ConditionProfile.createBinaryProfile();

    private double minDoubleDouble(double a, double b) {
        if (leftSmaller.profile(a < b)) {
            return a;
        } else if (rightSmaller.profile(b < a)) {
            return b;
        } else {
            if (bothEqual.profile(a == b)) {
                if (negativeZero.profile(JSRuntime.isNegativeZero(b))) {
                    return b;
                } else {
                    return a;
                }
            } else {
                return Double.NaN;
            }
        }
    }

    protected static boolean caseIntInt(Object[] args) {
        assert args.length == 2;
        return args[0] instanceof Integer && args[1] instanceof Integer;
    }

    @Specialization(guards = "args.length == 0")
    protected static double min0Param(@SuppressWarnings("unused") Object[] args) {
        return Double.POSITIVE_INFINITY;
    }

    @Specialization(guards = "args.length == 1")
    protected double min1Param(Object[] args) {
        return toDouble(args[0]);
    }

    @Specialization(guards = {"args.length == 2", "caseIntInt(args)"})
    protected static int min2ParamInt(Object[] args,
                    @Cached("createBinaryProfile()") ConditionProfile minProfile) {
        int i1 = (int) args[0];
        int i2 = (int) args[1];
        return min(i1, i2, minProfile);
    }

    @Specialization(guards = {"args.length == 2", "!caseIntInt(args)"})
    protected Object min2Param(Object[] args,
                    @Cached("createBinaryProfile()") ConditionProfile isIntBranch,
                    @Cached("createBinaryProfile()") ConditionProfile minProfile,
                    @Cached("create()") JSToNumberNode toNumber1Node,
                    @Cached("create()") JSToNumberNode toNumber2Node) {
        Number n1 = toNumber1Node.executeNumber(args[0]);
        Number n2 = toNumber2Node.executeNumber(args[1]);
        if (isIntBranch.profile(n1 instanceof Integer && n2 instanceof Integer)) {
            return min(((Integer) n1).intValue(), ((Integer) n2).intValue(), minProfile);
        } else {
            double d1 = JSRuntime.doubleValue(n1);
            double d2 = JSRuntime.doubleValue(n2);
            return minDoubleDouble(d1, d2);
        }
    }

    @Specialization(guards = "args.length >= 3")
    protected double min(Object[] args) {
        double smallest = minDoubleDouble(toDouble(args[0]), toDouble(args[1]));
        for (int i = 2; i < args.length; i++) {
            smallest = minDoubleDouble(smallest, toDouble(args[i]));
        }
        return smallest;
    }

    private static int min(int a, int b, ConditionProfile minProfile) {
        return minProfile.profile(a <= b) ? a : b;
    }
}

/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class MaxNode extends MathOperation {

    public MaxNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private final ConditionProfile leftSmaller = ConditionProfile.createBinaryProfile();
    private final ConditionProfile rightSmaller = ConditionProfile.createBinaryProfile();
    private final ConditionProfile bothEqual = ConditionProfile.createBinaryProfile();
    private final ConditionProfile negativeZero = ConditionProfile.createBinaryProfile();

    private double maxDoubleDouble(double a, double b) {
        if (leftSmaller.profile(a > b)) {
            return a;
        } else if (rightSmaller.profile(b > a)) {
            return b;
        } else {
            if (bothEqual.profile(a == b)) {
                if (negativeZero.profile(JSRuntime.isNegativeZero(a))) {
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
    protected static double max0Param(@SuppressWarnings("unused") Object[] args) {
        return Double.NEGATIVE_INFINITY;
    }

    @Specialization(guards = "args.length == 1")
    protected double max1Param(Object[] args) {
        return toDouble(args[0]);
    }

    @Specialization(guards = {"args.length == 2", "caseIntInt(args)"})
    protected static int max2ParamInt(Object[] args,
                    @Cached("createBinaryProfile()") ConditionProfile maxProfile) {
        int i1 = (int) args[0];
        int i2 = (int) args[1];
        return max(i1, i2, maxProfile);
    }

    @Specialization(guards = {"args.length == 2", "!caseIntInt(args)"})
    protected Object max2Param(Object[] args,
                    @Cached("createBinaryProfile()") ConditionProfile isIntBranch,
                    @Cached("createBinaryProfile()") ConditionProfile maxProfile,
                    @Cached("create()") JSToNumberNode toNumber1Node,
                    @Cached("create()") JSToNumberNode toNumber2Node) {
        Number n1 = toNumber1Node.executeNumber(args[0]);
        Number n2 = toNumber2Node.executeNumber(args[1]);
        if (isIntBranch.profile(n1 instanceof Integer && n2 instanceof Integer)) {
            return max(((Integer) n1).intValue(), ((Integer) n2).intValue(), maxProfile);
        } else {
            double d1 = JSRuntime.doubleValue(n1);
            double d2 = JSRuntime.doubleValue(n2);
            return maxDoubleDouble(d1, d2);
        }
    }

    @Specialization(guards = "args.length >= 3")
    protected double max(Object[] args) {
        double largest = maxDoubleDouble(toDouble(args[0]), toDouble(args[1]));
        for (int i = 2; i < args.length; i++) {
            largest = maxDoubleDouble(largest, toDouble(args[i]));
        }
        return largest;
    }

    private static int max(int a, int b, ConditionProfile maxProfile) {
        return maxProfile.profile(a >= b) ? a : b;
    }
}

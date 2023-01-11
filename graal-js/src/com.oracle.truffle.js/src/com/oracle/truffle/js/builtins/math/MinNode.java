/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class MinNode extends MathOperation {

    public MinNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private static double minDoubleDouble(double a, double b,
                    Node node,
                    InlinedConditionProfile leftSmaller,
                    InlinedConditionProfile rightSmaller,
                    InlinedConditionProfile bothEqual,
                    InlinedConditionProfile negativeZero) {
        if (leftSmaller.profile(node, a < b)) {
            return a;
        } else if (rightSmaller.profile(node, b < a)) {
            return b;
        } else {
            if (bothEqual.profile(node, a == b)) {
                if (negativeZero.profile(node, JSRuntime.isNegativeZero(b))) {
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
    protected int min2ParamInt(Object[] args,
                    @Cached @Shared("minProfile") InlinedConditionProfile minProfile) {
        int i1 = (int) args[0];
        int i2 = (int) args[1];
        return min(i1, i2, this, minProfile);
    }

    @Specialization(guards = {"args.length == 2", "!caseIntInt(args)"})
    protected static Object min2Param(Object[] args,
                    @Bind("this") Node node,
                    @Cached @Exclusive InlinedConditionProfile isIntBranch,
                    @Cached @Shared("minProfile") InlinedConditionProfile minProfile,
                    @Cached JSToNumberNode toNumber1Node,
                    @Cached JSToNumberNode toNumber2Node,
                    @Cached @Shared("leftSmaller") InlinedConditionProfile leftSmaller,
                    @Cached @Shared("rightSmaller") InlinedConditionProfile rightSmaller,
                    @Cached @Shared("bothEqual") InlinedConditionProfile bothEqual,
                    @Cached @Shared("negativeZero") InlinedConditionProfile negativeZero) {
        Number n1 = toNumber1Node.executeNumber(args[0]);
        Number n2 = toNumber2Node.executeNumber(args[1]);
        if (isIntBranch.profile(node, n1 instanceof Integer && n2 instanceof Integer)) {
            return min(((Integer) n1).intValue(), ((Integer) n2).intValue(), node, minProfile);
        } else {
            double d1 = JSRuntime.doubleValue(n1);
            double d2 = JSRuntime.doubleValue(n2);
            return minDoubleDouble(d1, d2,
                            node, leftSmaller, rightSmaller, bothEqual, negativeZero);
        }
    }

    protected static boolean caseIntIntInt(Object[] args) {
        assert args.length == 3;
        return args[0] instanceof Integer && args[1] instanceof Integer && args[2] instanceof Integer;
    }

    @Specialization(guards = {"args.length == 3", "caseIntIntInt(args)"})
    protected int min3ParamInt(Object[] args) {
        return Math.min(Math.min((int) args[0], (int) args[1]), (int) args[2]);
    }

    @Specialization(guards = {"args.length == 3", "!caseIntIntInt(args)"})
    protected double min3ParamOther(Object[] args,
                    @Cached @Shared("leftSmaller") InlinedConditionProfile leftSmaller,
                    @Cached @Shared("rightSmaller") InlinedConditionProfile rightSmaller,
                    @Cached @Shared("bothEqual") InlinedConditionProfile bothEqual,
                    @Cached @Shared("negativeZero") InlinedConditionProfile negativeZero) {
        double smallest = minDoubleDouble(toDouble(args[0]), toDouble(args[1]),
                        this, leftSmaller, rightSmaller, bothEqual, negativeZero);
        return minDoubleDouble(smallest, toDouble(args[2]),
                        this, leftSmaller, rightSmaller, bothEqual, negativeZero);
    }

    @Specialization(guards = "args.length >= 4")
    protected double minGeneric(Object[] args,
                    @Cached @Shared("leftSmaller") InlinedConditionProfile leftSmaller,
                    @Cached @Shared("rightSmaller") InlinedConditionProfile rightSmaller,
                    @Cached @Shared("bothEqual") InlinedConditionProfile bothEqual,
                    @Cached @Shared("negativeZero") InlinedConditionProfile negativeZero) {
        double smallest = minDoubleDouble(toDouble(args[0]), toDouble(args[1]),
                        this, leftSmaller, rightSmaller, bothEqual, negativeZero);
        for (int i = 2; i < args.length; i++) {
            smallest = minDoubleDouble(smallest, toDouble(args[i]),
                            this, leftSmaller, rightSmaller, bothEqual, negativeZero);
        }
        return smallest;
    }

    private static int min(int a, int b, Node node, InlinedConditionProfile minProfile) {
        return minProfile.profile(node, a <= b) ? a : b;
    }
}

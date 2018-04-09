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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode.JSToNumberWrapperNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class RoundNode extends MathOperation {

    RoundNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    public static RoundNode create(JSContext context, JSBuiltin builtin, JavaScriptNode[] arguments) {
        return RoundNodeGen.create(context, builtin, createCast(arguments));
    }

    protected static JavaScriptNode[] createCast(JavaScriptNode[] argumentNodes) {
        argumentNodes[0] = JSToNumberWrapperNode.create(argumentNodes[0]);
        return argumentNodes;
    }

    protected static boolean isCornercase(double d) {
        return Double.isNaN(d) || JSRuntime.isNegativeZero(d);
    }

    @Specialization
    protected static int round(int a) {
        return a;
    }

    @Specialization(guards = "isCornercase(value)")
    protected static double roundCornercase(double value) {
        return value;
    }

    private final ConditionProfile shiftProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile negativeLongBitsProfile = BranchProfile.create();

    // Copied from sun.misc.DoubleConsts
    private static final int EXP_BIAS = 1023;
    private static final int SIGNIFICAND_WIDTH = 53;
    private static final long EXP_BIT_MASK = 9218868437227405312L;
    private static final long SIGNIF_BIT_MASK = 4503599627370495L;

    // Copy of Math.round() with added profiles
    private long round(double a) {
        long longBits = Double.doubleToRawLongBits(a);
        long biasedExp = (longBits & EXP_BIT_MASK) >> (SIGNIFICAND_WIDTH - 1);
        long shift = (SIGNIFICAND_WIDTH - 2 + EXP_BIAS) - biasedExp;
        if (shiftProfile.profile((shift & -64) == 0)) { // shift >= 0 && shift < 64
            // a is a finite number such that pow(2,-64) <= ulp(a) < 1
            long r = ((longBits & SIGNIF_BIT_MASK) | (SIGNIF_BIT_MASK + 1));
            if (longBits < 0) {
                negativeLongBitsProfile.enter();
                r = -r;
            }
            // In the comments below each Java expression evaluates to the value
            // the corresponding mathematical expression:
            // (r) evaluates to a / ulp(a)
            // (r >> shift) evaluates to floor(a * 2)
            // ((r >> shift) + 1) evaluates to floor((a + 1/2) * 2)
            // (((r >> shift) + 1) >> 1) evaluates to floor(a + 1/2)
            return ((r >> shift) + 1) >> 1;
        } else {
            // a is either
            // - a finite number with abs(a) < exp(2,SIGNIFICAND_WIDTH-64) < 1/2
            // - a finite number with ulp(a) >= 1 and hence a is a mathematical integer
            // - an infinity or NaN
            return (long) a;
        }
    }

    @Specialization(guards = {"!isCornercase(value)", "isDoubleInInt32Range(value)"}, rewriteOn = ArithmeticException.class)
    protected int roundDoubleInt(double value) {
        long longValue = round(value);
        if (longValue == 0 && value < 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new ArithmeticException();
        } else {
            assert JSRuntime.longIsRepresentableAsInt(longValue);
            return (int) longValue;
        }
    }

    @Specialization(guards = {"!isCornercase(value)"}, replaces = "roundDoubleInt")
    protected double roundDouble(double value,
                    @Cached("createBinaryProfile()") ConditionProfile profileA,
                    @Cached("createBinaryProfile()") ConditionProfile profileB) {
        long longValue = round(value);
        if (profileA.profile(longValue == Long.MIN_VALUE || longValue == Long.MAX_VALUE)) {
            // The value is too large to have a fractional part (i.e. is rounded already)
            return value;
        } else if (profileB.profile(longValue == 0 && value < 0)) {
            return -0.0;
        } else {
            return longValue;
        }
    }

}

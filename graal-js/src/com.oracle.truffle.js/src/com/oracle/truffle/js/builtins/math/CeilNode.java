/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;

/**
 * Math.ceil(x). Returns the smallest (closest to -Infinity) Number value that is not less than x
 * and is an integer. If x is already an integer, the result is x.
 *
 * The value of {@code Math.ceil(x)} is the same as the value of {@code -Math.floor(-x)}.
 */
public abstract class CeilNode extends MathOperation {

    public CeilNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static int ceilInt(int a) {
        return a;
    }

    @Specialization
    protected static SafeInteger ceilSafeInt(SafeInteger a) {
        return a;
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    protected int ceilDoubleMightReturnInt(double d) throws UnexpectedResultException {
        if (Double.isNaN(d) || JSRuntime.isNegativeZero(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnexpectedResultException(mathCeil(d));
        }
        int i = (int) d;
        int result = d > i ? i + 1 : i;
        if (result == 0 && d < 0) {
            // special-case: return -0.0
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnexpectedResultException(-0.0);
        }
        return result;
    }

    @Specialization
    protected static double ceilDouble(double d,
                    @Cached("createBinaryProfile()") @Shared("isNaN") ConditionProfile isNaN,
                    @Cached("createBinaryProfile()") @Shared("isNegativeZero") ConditionProfile isNegativeZero,
                    @Cached("createBinaryProfile()") @Shared("requiresNegativeZero") ConditionProfile requiresNegativeZero,
                    @Cached("createBinaryProfile()") @Shared("fitsSafeLong") ConditionProfile fitsSafeLong) {
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

    @Specialization(guards = {"!isImplicitDouble(a)"}, rewriteOn = UnexpectedResultException.class)
    protected int ceilMightReturnInt(Object a) throws UnexpectedResultException {
        double d = toDouble(a);
        return ceilDoubleMightReturnInt(d);
    }

    @Specialization(guards = {"!isImplicitDouble(a)"})
    protected double ceilToDouble(Object a,
                    @Cached("createBinaryProfile()") @Shared("isNaN") ConditionProfile isNaN,
                    @Cached("createBinaryProfile()") @Shared("isNegativeZero") ConditionProfile isNegativeZero,
                    @Cached("createBinaryProfile()") @Shared("requiresNegativeZero") ConditionProfile requiresNegativeZero,
                    @Cached("createBinaryProfile()") @Shared("fitsSafeLong") ConditionProfile fitsSafeLong) {
        double d = toDouble(a);
        return ceilDouble(d, isNaN, isNegativeZero, requiresNegativeZero, fitsSafeLong);
    }

    @TruffleBoundary
    private static double mathCeil(double d) {
        return Math.ceil(d);
    }

    protected static boolean isImplicitDouble(Object a) {
        return JSTypesGen.isImplicitDouble(a);
    }
}

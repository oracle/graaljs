/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SuppressFBWarnings;

public abstract class PowNode extends MathOperation {

    public PowNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    public abstract double execute(Object a, Object b);

    @NeverDefault
    protected PowNode create(JSContext context) {
        return PowNodeGen.create(context, null, null);
    }

    @CompilationFinal private boolean hasSeenOne = false;
    @CompilationFinal private boolean hasSeenTwo = false;
    @CompilationFinal private boolean hasSeenThree = false;
    @CompilationFinal private boolean hasSeenZeroPointFive = false;
    @CompilationFinal private boolean hasSeenOnePointFive = false;
    @CompilationFinal private boolean hasSeenTwoPointFive = false;

    @Specialization(rewriteOn = SlowPathException.class)
    protected double pow(double a, double b) throws SlowPathException {
        if (hasSeenOne && b == 1) {
            return a;
        } else if (hasSeenTwo && b == 2) {
            return a * a;
        } else if (hasSeenThree && b == 3) {
            return a * a * a;
        } else if ((hasSeenZeroPointFive || hasSeenOnePointFive || hasSeenTwoPointFive) &&
                        (a < 0.0 || JSRuntime.isNegativeZero(a))) {
            // sqrt behaves differently, counter example is Math.pow(-Infinity,0.5)
            return powIntl(a, b);
        } else if (hasSeenZeroPointFive && b == 0.5) {
            return Math.sqrt(a);
        } else if (hasSeenOnePointFive && b == 1.5) {
            return a * Math.sqrt(a);
        } else if (hasSeenTwoPointFive && b == 2.5) {
            return a * a * Math.sqrt(a);
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (b == 1) {
                hasSeenOne = true;
            } else if (b == 2) {
                hasSeenTwo = true;
            } else if (b == 3) {
                hasSeenThree = true;
            } else if (b == 0.5) {
                hasSeenZeroPointFive = true;
            } else if (b == 1.5) {
                hasSeenOnePointFive = true;
            } else if (b == 2.5) {
                hasSeenTwoPointFive = true;
            } else {
                throw JSNodeUtil.slowPathException();
            }
            return pow(a, b);
        }
    }

    private static double positivePow(double operand, int castExponent) {
        int exponent = castExponent;
        double result = 1;
        double base = operand;
        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result *= base;
            }
            exponent >>= 1;
            base *= base;
        }
        return result;
    }

    @Specialization(rewriteOn = SlowPathException.class)
    protected double pow2(double a, double b) throws SlowPathException {
        if (JSRuntime.doubleIsRepresentableAsInt(b, true) && b > 0) {
            return positivePow(a, (int) b);
        } else {
            throw JSNodeUtil.slowPathException();
        }
    }

    @SuppressFBWarnings(value = "FE_FLOATING_POINT_EQUALITY", justification = "not necessary in this case")
    @Specialization
    protected double pow3(double a, double b,
                    @Cached InlinedConditionProfile branch1,
                    @Cached InlinedConditionProfile branch2) {
        int ib = (int) b;
        if (branch1.profile(this, JSRuntime.doubleIsRepresentableAsInt(b, true) && b > 0)) {
            return positivePow(a, ib);
        } else if (branch2.profile(this, ib + 0.5 == b && b > 0 && a > 0 && !JSRuntime.isNegativeZero(a))) {
            return positivePow(a, ib) * Math.sqrt(a);
        } else {
            return powIntl(a, b);
        }
    }

    @Specialization
    protected Object pow(Object a, Object b,
                    @Cached("create(getContext())") PowNode powNode) {
        return JSRuntime.doubleToNarrowestNumber(powNode.execute(toDouble(a), toDouble(b)));
    }

    @TruffleBoundary
    private static double powIntl(double a, double b) {
        return Math.pow(a, b);
    }
}

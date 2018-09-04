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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSNodeUtil;
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
            throw JSNodeUtil.slowPathException();
        }
        int i = (int) d;
        int result = d > i ? i + 1 : i;
        if (result == 0 && d < 0) {
            throw JSNodeUtil.slowPathException(); // special-case: return -0.0
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

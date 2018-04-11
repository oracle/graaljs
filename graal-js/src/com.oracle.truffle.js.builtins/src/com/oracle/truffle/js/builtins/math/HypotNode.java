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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class HypotNode extends MathOperation {
    public HypotNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @ExplodeLoop
    @Specialization
    protected double hypot(Object... args) {
        int length = args.length;
        double[] values = new double[length];
        boolean isInfinite = false;
        double max = 0;
        for (int i = 0; i < length; i++) {
            double value = toDouble(args[i]);
            isInfinite = isInfinite | Double.isInfinite(value);
            if (value > max) {
                max = value;
            }
            values[i] = value;
        }
        if (isInfinite) {
            return Double.POSITIVE_INFINITY;
        }

        // Avoid division by zero
        if (max == 0) {
            max = 1;
        }

        double sum = 0;
        double compensation = 0;
        for (double value : values) {
            // Normalize to avoid overflow/underflow during squaring
            double normalizedValue = value / max;
            double square = normalizedValue * normalizedValue;

            // Kahan summation to reduce rounding errors
            double compensatedValue = square - compensation;
            double nextSum = sum + compensatedValue;
            compensation = (nextSum - sum) - compensatedValue;
            sum = nextSum;
        }

        return Math.sqrt(sum) * max;
    }
}

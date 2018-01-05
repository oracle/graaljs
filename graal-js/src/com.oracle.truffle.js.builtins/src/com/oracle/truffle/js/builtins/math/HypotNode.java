/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

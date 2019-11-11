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

/* ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunSoft, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class Log2Node extends MathOperation {
    public Log2Node(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    /**
     * Return a double with its low-order bits of the second argument and the high-order bits of the
     * first argument.
     */
    private static double lowBits(double x, int low) {
        long transX = Double.doubleToRawLongBits(x);
        return Double.longBitsToDouble((transX & 0xFFFF_FFFF_0000_0000L) | low);
    }

    /**
     * Return the high-order 32 bits of the double argument as an int.
     */
    private static int highBits(double x) {
        long transducer = Double.doubleToRawLongBits(x);
        return (int) (transducer >> 32);
    }

    /**
     * Return a double with its high-order bits of the second argument and the low-order bits of the
     * first argument.
     */
    private static double highBits(double x, int high) {
        long transX = Double.doubleToRawLongBits(x);
        return Double.longBitsToDouble((transX & 0x0000_0000_FFFF_FFFFL) | (((long) high)) << 32);
    }

    /**
     * Returns the base 2 logarithm. The algorithm comes from FDLIBM library. This library does not
     * have an explicit log2 function but its pow function implements log2 function as part of the
     * pow implementation. The implementation of this function is an extract of log2 function from
     * the Java port of pow function found in {@code java.lang.FdLibm} class.
     *
     * @param x value whose logarithm should be returned.
     * @return base 2 logarithm.
     */
    @TruffleBoundary
    private static strictfp double log2Impl(final double x) {
        double xAbs = Math.abs(x);
        final int hx = highBits(x);
        int ix = hx & 0x7fffffff;

        final double cp = 0x1.ec70_9dc3_a03fdp-1;    // 9.61796693925975554329e-01 = 2/(3ln2)
        final double cph = 0x1.ec709ep-1;            // 9.61796700954437255859e-01 = (float)cp
        final double cpl = -0x1.e2fe_0145_b01f5p-28; // -7.02846165095275826516e-09 = tail of cph

        int n = 0;
        // Take care of subnormal numbers
        if (ix < 0x00100000) {
            xAbs *= 0x1.0p53; // 2^53 = 9007199254740992.0
            n -= 53;
            ix = highBits(xAbs);
        }
        n += ((ix) >> 20) - 0x3ff;
        int j = ix & 0x000fffff;
        // Determine interval
        ix = j | 0x3ff00000; // Normalize ix
        int k;
        if (j <= 0x3988E) {
            k = 0; // |x| <sqrt(3/2)
        } else if (j < 0xBB67A) {
            k = 1; // |x| <sqrt(3)
        } else {
            k = 0;
            n += 1;
            ix -= 0x00100000;
        }
        xAbs = highBits(xAbs, ix);

        // Compute ss = s_h + s_l = (x-1)/(x+1) or (x-1.5)/(x+1.5)

        final double[] bp = {1.0, 1.5};
        final double[] dph = {0.0, 0x1.2b80_34p-1};          // 5.84962487220764160156e-01
        final double[] dpl = {0.0, 0x1.cfde_b43c_fd006p-27}; // 1.35003920212974897128e-08

        // Poly coefs for (3/2)*(log(x)-2s-2/3*s**3
        final double l1 = 0x1.3333_3333_33303p-1; // 5.99999999999994648725e-01
        final double l2 = 0x1.b6db_6db6_fabffp-2; // 4.28571428578550184252e-01
        final double l3 = 0x1.5555_5518_f264dp-2; // 3.33333329818377432918e-01
        final double l4 = 0x1.1746_0a91_d4101p-2; // 2.72728123808534006489e-01
        final double l5 = 0x1.d864_a93c_9db65p-3; // 2.30660745775561754067e-01
        final double l6 = 0x1.a7e2_84a4_54eefp-3; // 2.06975017800338417784e-01
        double u = xAbs - bp[k]; // BP[0]=1.0, BP[1]=1.5
        double v = 1.0 / (xAbs + bp[k]);
        double ss = u * v;
        double sh = ss;
        sh = lowBits(sh, 0);
        // t_h=xAbs + BP[k] High
        double th = 0.0;
        th = highBits(th, ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18));
        double tl = xAbs - (th - bp[k]);
        double sl = v * ((u - sh * th) - sh * tl);
        // Compute log(xAbs)
        double s2 = ss * ss;
        double r = s2 * s2 * (l1 + s2 * (l2 + s2 * (l3 + s2 * (l4 + s2 * (l5 + s2 * l6)))));
        r += sl * (sh + ss);
        s2 = sh * sh;
        th = 3.0 + s2 + r;
        th = lowBits(th, 0);
        tl = r - ((th - 3.0) - s2);
        // u+v = ss*(1+...)
        u = sh * th;
        v = sl * th + tl * ss;
        // 2/(3log2)*(ss + ...)
        double ph = u + v;
        ph = lowBits(ph, 0);
        double pl = v - (ph - u);
        double zh = cph * ph; // cph + cpl = 2/(3*log2)
        double zl = cpl * ph + pl * cp + dpl[k];
        // log2(x_abs) = (ss + ..)*2/(3*log2) = n + DP_H + z_h + z_l
        double t = n;
        double t1 = (((zh + zl) + dph[k]) + t);
        t1 = lowBits(t1, 0);
        double t2 = zl - (((t1 - t) - dph[k]) - zh);

        return t1 + t2;
    }

    @Specialization
    protected double log2(final double x) {
        if (x < 0 || Double.isNaN(x)) {
            return Double.NaN;
        }

        if (x == 0) {
            return Double.NEGATIVE_INFINITY;
        }

        if (x == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }

        return log2Impl(x);
    }

    @Specialization
    protected double log2(Object a) {
        return log2(toDouble(a));
    }
}

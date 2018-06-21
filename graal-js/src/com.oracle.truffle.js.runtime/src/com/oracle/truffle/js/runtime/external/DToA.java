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
/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/****************************************************************
 *
 * The author of this software is David M. Gay.
 *
 * Copyright (c) 1991, 2000, 2001 by Lucent Technologies.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 *
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHOR NOR LUCENT MAKES ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 *
 ***************************************************************/

package com.oracle.truffle.js.runtime.external;

import java.math.*;

import com.oracle.truffle.js.runtime.SuppressFBWarnings;

public class DToA {

    private static char basedigit(int digit) {
        return (char) ((digit >= 10) ? 'a' - 10 + digit : '0' + digit);
    }

    /* Either fixed or exponential format; round-trip */
    public static final int DTOSTR_STANDARD = 0;
    /* Always exponential format; round-trip */
    public static final int DTOSTR_STANDARD_EXPONENTIAL = 1;
    /* Round to <precision> digits after the decimal point; exponential if number is large */
    public static final int DTOSTR_FIXED = 2;
    /* Always exponential format; <precision> significant digits */
    public static final int DTOSTR_EXPONENTIAL = 3;
    /* Either fixed or exponential format; <precision> significant digits */
    public static final int DTOSTR_PRECISION = 4;

    private static final int Frac_mask = 0xfffff;
    private static final int Exp_shift = 20;
    private static final int Exp_msk1 = 0x100000;

    private static final long Frac_maskL = 0xfffffffffffffL;
    private static final int Exp_shiftL = 52;
    private static final long Exp_msk1L = 0x10000000000000L;

    private static final int Bias = 1023;
    private static final int P = 53;

    private static final int Exp_shift1 = 20;
    private static final int Exp_mask = 0x7ff00000;
    private static final int Exp_mask_shifted = 0x7ff;
    private static final int Bndry_mask = 0xfffff;
    private static final int Log2P = 1;

    private static final int Sign_bit = 0x80000000;
    private static final int Exp_11 = 0x3ff00000;
    private static final int Ten_pmax = 22;
    private static final int Quick_max = 14;
    private static final int Bletch = 0x10;
    private static final int Frac_mask1 = 0xfffff;
    private static final int Int_max = 14;
    private static final int n_bigtens = 5;

    private static final double[] tens = {1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22};

    private static final double[] bigtens = {1e16, 1e32, 1e64, 1e128, 1e256};

    private static int lo0bits(int y) {
        int k;
        int x = y;

        if ((x & 7) != 0) {
            if ((x & 1) != 0) {
                return 0;
            }
            if ((x & 2) != 0) {
                return 1;
            }
            return 2;
        }
        k = 0;
        if ((x & 0xffff) == 0) {
            k = 16;
            x >>>= 16;
        }
        if ((x & 0xff) == 0) {
            k += 8;
            x >>>= 8;
        }
        if ((x & 0xf) == 0) {
            k += 4;
            x >>>= 4;
        }
        if ((x & 0x3) == 0) {
            k += 2;
            x >>>= 2;
        }
        if ((x & 1) == 0) {
            k++;
            x >>>= 1;
            if ((x & 1) == 0) {
                return 32;
            }
        }
        return k;
    }

    /* Return the number (0 through 32) of most significant zero bits in x. */
    private static int hi0bits(int xParam) {
        int x = xParam;
        int k = 0;

        if ((x & 0xffff0000) == 0) {
            k = 16;
            x <<= 16;
        }
        if ((x & 0xff000000) == 0) {
            k += 8;
            x <<= 8;
        }
        if ((x & 0xf0000000) == 0) {
            k += 4;
            x <<= 4;
        }
        if ((x & 0xc0000000) == 0) {
            k += 2;
            x <<= 2;
        }
        if ((x & 0x80000000) == 0) {
            k++;
            if ((x & 0x40000000) == 0) {
                return 32;
            }
        }
        return k;
    }

    private static void stuffBits(byte[] bits, int offset, int val) {
        bits[offset] = (byte) (val >> 24);
        bits[offset + 1] = (byte) (val >> 16);
        bits[offset + 2] = (byte) (val >> 8);
        bits[offset + 3] = (byte) (val);
    }

    /*
     * Convert d into the form b*2^e, where b is an odd integer. b is the returned Bigint and e is
     * the returned binary exponent. Return the number of significant bits in b in bits. d must be
     * finite and nonzero.
     */
    private static BigInteger d2b(double d, int[] e, int[] bits) {
        byte[] dblBits;
        int i;
        int k;
        int y;
        int z;
        int de;
        long dBits = Double.doubleToLongBits(d);
        int d0 = (int) (dBits >>> 32);
        int d1 = (int) (dBits);

        z = d0 & Frac_mask;
        d0 &= 0x7fffffff; /* clear sign bit, which we ignore */

        if ((de = (d0 >>> Exp_shift)) != 0) {
            z |= Exp_msk1;
        }

        if ((y = d1) != 0) {
            dblBits = new byte[8];
            k = lo0bits(y);
            y >>>= k;
            if (k != 0) {
                stuffBits(dblBits, 4, y | z << (32 - k));
                z >>= k;
            } else {
                stuffBits(dblBits, 4, y);
            }
            stuffBits(dblBits, 0, z);
            i = (z != 0) ? 2 : 1;
        } else {
            // JS_ASSERT(z);
            dblBits = new byte[4];
            k = lo0bits(z);
            z >>>= k;
            stuffBits(dblBits, 0, z);
            k += 32;
            i = 1;
        }
        if (de != 0) {
            e[0] = de - Bias - (P - 1) + k;
            bits[0] = P - k;
        } else {
            e[0] = de - Bias - (P - 1) + 1 + k;
            bits[0] = 32 * i - hi0bits(z);
        }
        return new BigInteger(dblBits);
    }

    public static String jsDtobasestr(int base, double dParam) {
        if (!(2 <= base && base <= 36)) {
            throw new IllegalArgumentException("Bad base: " + base);
        }
        double d = dParam;

        /* Check for Infinity and NaN */
        if (Double.isNaN(d)) {
            return "NaN";
        } else if (Double.isInfinite(d)) {
            return (d > 0.0) ? "Infinity" : "-Infinity";
        } else if (d == 0) {
            // ALERT: should it distinguish -0.0 from +0.0 ?
            return "0";
        }

        boolean negative;
        if (d >= 0.0) {
            negative = false;
        } else {
            negative = true;
            d = -d;
        }

        /* Get the integer part of d including '-' sign. */
        String intDigits;

        double dfloor = Math.floor(d);
        long lfloor = (long) dfloor;
        if (lfloor == dfloor) {
            // int part fits long
            if (lfloor == 0 && negative) {
                intDigits = "-0"; // CWirth fix
            } else {
                intDigits = Long.toString((negative) ? -lfloor : lfloor, base);
            }
        } else {
            // BigInteger should be used
            long floorBits = Double.doubleToLongBits(dfloor);
            int exp = (int) (floorBits >> Exp_shiftL) & Exp_mask_shifted;
            long mantissa;
            if (exp == 0) {
                mantissa = (floorBits & Frac_maskL) << 1;
            } else {
                mantissa = (floorBits & Frac_maskL) | Exp_msk1L;
            }
            if (negative) {
                mantissa = -mantissa;
            }
            exp -= 1075;
            BigInteger x = BigInteger.valueOf(mantissa);
            if (exp > 0) {
                x = x.shiftLeft(exp);
            } else if (exp < 0) {
                x = x.shiftRight(-exp);
            }
            intDigits = x.toString(base);
        }

        if (d == dfloor) {
            // No fraction part
            return intDigits;
        } else {
            /* We have a fraction. */

            StringBuilder buffer; /* The output string */
            int digit;
            double df; /* The fractional part of d */
            BigInteger b;

            buffer = new StringBuilder();
            buffer.append(intDigits).append('.');
            df = d - dfloor;

            long dBits = Double.doubleToLongBits(d);
            int word0 = (int) (dBits >> 32);
            int word1 = (int) (dBits);

            int[] e = new int[1];
            int[] bbits = new int[1];

            b = d2b(df, e, bbits);
            /* At this point df = b * 2^e. e must be less than zero because 0 < df < 1. */

            int s2 = -(word0 >>> Exp_shift1 & Exp_mask >> Exp_shift1);
            if (s2 == 0) {
                s2 = -1;
            }
            s2 += Bias + P;
            /* 1/2^s2 = (nextDouble(d) - d)/2 */
            BigInteger mlo = BigInteger.ONE;
            BigInteger mhi = mlo;
            if ((word1 == 0) && ((word0 & Bndry_mask) == 0) && ((word0 & (Exp_mask & Exp_mask << 1)) != 0)) {
                /*
                 * The special case. Here we want to be within a quarter of the last input
                 * significant digit instead of one half of it when the output string's value is
                 * less than d.
                 */
                s2 += Log2P;
                mhi = BigInteger.valueOf(1 << Log2P);
            }

            b = b.shiftLeft(e[0] + s2);
            BigInteger s = BigInteger.ONE;
            s = s.shiftLeft(s2);
            /*
             * @formatter:off
             * At this point we have the following:
             * s = 2^s2;
             * 1 > df = b/2^s2 > 0;
             * (d - prevDouble(d))/2 = mlo/2^s2;
             * (nextDouble(d) - d)/2 = mhi/2^s2.
             * @formatter:on
             */
            BigInteger bigBase = BigInteger.valueOf(base);

            boolean done = false;
            do {
                b = b.multiply(bigBase);
                BigInteger[] divResult = b.divideAndRemainder(s);
                b = divResult[1];
                digit = (char) (divResult[0].intValue());
                if (mlo == mhi) {
                    mlo = mhi = mlo.multiply(bigBase);
                } else {
                    mlo = mlo.multiply(bigBase);
                    mhi = mhi.multiply(bigBase);
                }

                /* Do we yet have the shortest string that will round to d? */
                int j = b.compareTo(mlo);
                /* j is b/2^s2 compared with mlo/2^s2. */
                BigInteger delta = s.subtract(mhi);
                int j1 = (delta.signum() <= 0) ? 1 : b.compareTo(delta);
                /* j1 is b/2^s2 compared with 1 - mhi/2^s2. */
                if (j1 == 0 && ((word1 & 1) == 0)) {
                    if (j > 0) {
                        digit++;
                    }
                    done = true;
                } else if (j < 0 || (j == 0 && ((word1 & 1) == 0))) {
                    if (j1 > 0) {
                        /*
                         * Either dig or dig+1 would work here as the least significant digit. Use
                         * whichever would produce an output value closer to d.
                         */
                        b = b.shiftLeft(1);
                        j1 = b.compareTo(s);
                        if (j1 > 0) {
                            /*
                             * The even test (|| (j1 == 0 && (digit & 1))) is not here because it
                             * messes up odd base output such as 3.5 in base 3.
                             */
                            digit++;
                        }
                    }
                    done = true;
                } else if (j1 > 0) {
                    digit++;
                    done = true;
                }
                buffer.append(basedigit(digit));
            } while (!done);

            return buffer.toString();
        }

    }

    /* dtoa for IEEE arithmetic (dmg): convert double to ASCII string.
     * @formatter:off
     * Inspired by "How to Print Floating-Point Numbers Accurately" by
     * Guy L. Steele, Jr. and Jon L. White [Proc. ACM SIGPLAN '90, pp. 92-101].
     *
     * Modifications:
     *  1. Rather than iterating, we use a simple numeric overestimate
     *     to determine k = floor(log10(d)). We scale relevant quantities
     *     using O(log2(k)) rather than O(k) multiplications.
     *  2. For some modes > 2 (corresponding to ecvt and fcvt), we don't
     *     try to generate digits strictly left to right. Instead, we
     *     compute with fewer bits and propagate the carry if necessary
     *     when rounding the final digit up. This is often faster.
     *  3. Under the assumption that input will be rounded nearest,
     *     mode 0 renders 1e23 as 1e23 rather than 9.999999999999999e22.
     *     That is, we allow equality in stopping tests when the
     *     round-nearest rule will give the same floating-point value
     *     as would satisfaction of the stopping test with strict inequality.
     *  4. We remove common factors of powers of 2 from relevant quantities.
     *  5. When converting floating-point integers less than 1e16,
     *     we use floating-point arithmetic rather than resorting
     *     to multiple-precision integers.
     *  6. When asked to produce fewer than 15 digits, we first try
     *     to get by with floating-point arithmetic; we resort to
     *     multiple-precision integer arithmetic only if we cannot
     *     guarantee that the floating-point calculation has given
     *     the correctly rounded result. For k requested digits and
     *     "uniformly" distributed input, the probability is something
     *     like 10^(k-15) that we must resort to the Long calculation.
     * @formatter:on
     */

    static int word0(double d) {
        long dBits = Double.doubleToLongBits(d);
        return (int) (dBits >> 32);
    }

    static double setWord0(double d, int i) {
        long dBits = Double.doubleToLongBits(d);
        dBits = ((long) i << 32) | (dBits & 0x0FFFFFFFFL);
        return Double.longBitsToDouble(dBits);
    }

    static int word1(double d) {
        long dBits = Double.doubleToLongBits(d);
        return (int) (dBits);
    }

    /* Return b * 5^k. k must be nonnegative. */
    // XXXX the C version built a cache of these
    static BigInteger pow5mult(BigInteger b, int k) {
        return b.multiply(BigInteger.valueOf(5).pow(k));
    }

    static boolean roundOff(StringBuilder buf) {
        int i = buf.length();
        while (i != 0) {
            --i;
            char c = buf.charAt(i);
            if (c != '9') {
                buf.setCharAt(i, (char) (c + 1));
                buf.setLength(i + 1);
                return false;
            }
        }
        buf.setLength(0);
        return true;
    }

    /* Always emits at least one digit. */
    /*
     * If biasUp is set, then rounding in modes 2 and 3 will round away from zero when the number is
     * exactly halfway between two representable values. For example, rounding 2.5 to zero digits
     * after the decimal point will return 3 and not 2. 2.49 will still round to 2, and 2.51 will
     * still round to 3.
     */
    /*
     * bufsize should be at least 20 for modes 0 and 1. For the other modes, bufsize should be two
     * greater than the maximum number of output characters expected.
     */
    @SuppressFBWarnings(value = "FE_FLOATING_POINT_EQUALITY", justification = "this is an external file that we do not want to modify")
    @SuppressWarnings("fallthrough")
    static int jsDtoa(double dParam, int modeParam, boolean biasUp, int ndigitsParam, boolean[] sign, StringBuilder buf) {
        /*
         * Arguments ndigits, decpt, sign are similar to those of ecvt and fcvt; trailing zeros are
         * suppressed from the returned string. If not null, *rve is set to point to the end of the
         * return value. If d is +-Infinity or NaN, then *decpt is set to 9999.
         *
         * @formatter:off
         * mode:
         * 0 ==> shortest string that yields d when read in and rounded to nearest.
         * 1 ==> like 0, but with Steele & White stopping rule;e.g. with IEEE P754 arithmetic,
         * mode 0 gives 1e23 whereas mode 1 gives 9.999999999999999e22.
         * 2 ==> max(1,ndigits) significant digits. This gives a return value similar to that of
         * ecvt, except that trailing zeros are suppressed.
         * 3 ==> through ndigits past the decimal point. This gives a return value similar to that
         * from fcvt, except that trailing zeros are suppressed, and ndigits can be negative.
         * 4-9 should give the same return values as 2-3, i.e.,
         * 4 <= mode <= 9 ==> same return as mode 2 + (mode & 1). These modes are mainly for
         * debugging; often they run slower but sometimes faster than modes 2-3.
         * 4,5,8,9 ==> left-to-right digit generation.
         * 6-9 ==> don't try fast floating-point estimate (if applicable).
         * @formatter:on
         *
         * Values of mode other than 0-9 are treated as mode 0.
         *
         * Sufficient space is allocated to the return value to hold the suppressed trailing zeros.
         */
        double d = dParam;
        int ndigits = ndigitsParam;
        int mode = modeParam;
        int b2;
        int b5;
        int i;
        int ieps;
        int ilim;
        int ilim0;
        int ilim1;
        int j;
        int j1;
        int k;
        int k0;
        int m2;
        int m5;
        int s2;
        int s5;
        char dig;
        long lL;
        long x;
        BigInteger b;
        BigInteger b1;
        BigInteger delta;
        BigInteger mlo;
        BigInteger mhi;
        BigInteger sS;
        int[] be = new int[1];
        int[] bbits = new int[1];
        double d2;
        double ds;
        double eps;
        boolean specCase;
        boolean denorm;
        boolean kCheck;
        boolean tryQuick;
        boolean leftright;

        if ((word0(d) & Sign_bit) != 0) {
            /* set sign for everything, including 0's and NaNs */
            sign[0] = true;
            // word0(d) &= ~Sign_bit; /* clear sign bit */
            d = setWord0(d, word0(d) & ~Sign_bit);
        } else {
            sign[0] = false;
        }

        if ((word0(d) & Exp_mask) == Exp_mask) {
            /* Infinity or NaN */
            buf.append(((word1(d) == 0) && ((word0(d) & Frac_mask) == 0)) ? "Infinity" : "NaN");
            return 9999;
        }
        if (d == 0) {
            buf.setLength(0);
            buf.append('0'); /* copy "0" to buffer */
            return 1;
        }

        b = d2b(d, be, bbits);
        if ((i = (word0(d) >>> Exp_shift1 & (Exp_mask >> Exp_shift1))) != 0) {
            d2 = setWord0(d, (word0(d) & Frac_mask1) | Exp_11);
            /*
             * log(x) ~=~ log(1.5) + (x-1.5)/1.5 log10(x) = log(x) / log(10) ~=~ log(1.5)/log(10) +
             * (x-1.5)/(1.5*log(10)) log10(d) = (i-Bias)*log(2)/log(10) + log10(d2)
             *
             * This suggests computing an approximation k to log10(d) by
             *
             * k = (i - Bias)*0.301029995663981 + ((d2-1.5)*0.289529654602168 + 0.176091259055681);
             *
             * We want k to be too large rather than too small. The error in the first-order Taylor
             * series approximation is in our favor, so we just round up the constant enough to
             * compensate for any error in the multiplication of (i - Bias) by 0.301029995663981;
             * since |i - Bias| <= 1077, and 1077 * 0.30103 * 2^-52 ~=~ 7.2e-14, adding 1e-13 to the
             * constant term more than suffices. Hence we adjust the constant term to
             * 0.1760912590558. (We could get a more accurate k by invoking log10, but this is
             * probably not worthwhile.)
             */
            i -= Bias;
            denorm = false;
        } else {
            /* d is denormalized */
            i = bbits[0] + be[0] + (Bias + (P - 1) - 1);
            x = (i > 32) ? ((long) word0(d)) << (64 - i) | word1(d) >>> (i - 32) : ((long) word1(d)) << (32 - i);
            d2 = setWord0(x, word0(x) - 31 * Exp_msk1);
            i -= (Bias + (P - 1) - 1) + 1;
            denorm = true;
        }
        /* At this point d = f*2^i, where 1 <= f < 2. d2 is an approximation of f. */
        ds = (d2 - 1.5) * 0.289529654602168 + 0.1760912590558 + i * 0.301029995663981;
        k = (int) ds;
        if (ds < 0.0 && ds != k) {
            k--; /* want k = floor(ds) */
        }
        kCheck = true;
        if (k >= 0 && k <= Ten_pmax) {
            if (d < tens[k]) {
                k--;
            }
            kCheck = false;
        }
        /*
         * At this point floor(log10(d)) <= k <= floor(log10(d))+1. If k_check is zero, we're
         * guaranteed that k = floor(log10(d)).
         */
        j = bbits[0] - i - 1;
        /* At this point d = b/2^j, where b is an odd integer. */
        if (j >= 0) {
            b2 = 0;
            s2 = j;
        } else {
            b2 = -j;
            s2 = 0;
        }
        if (k >= 0) {
            b5 = 0;
            s5 = k;
            s2 += k;
        } else {
            b2 -= k;
            b5 = -k;
            s5 = 0;
        }
        /*
         * At this point d/10^k = (b * 2^b2 * 5^b5) / (2^s2 * 5^s5), where b is an odd integer, b2
         * >= 0, b5 >= 0, s2 >= 0, and s5 >= 0.
         */
        if (mode < 0 || mode > 9) {
            mode = 0;
        }
        tryQuick = true;
        if (mode > 5) {
            mode -= 4;
            tryQuick = false;
        }
        leftright = true;
        ilim = ilim1 = 0;
        switch (mode) {
            case 0:
            case 1:
                ilim = ilim1 = -1;
                i = 18;
                ndigits = 0;
                break;
            case 2:
                leftright = false;
                /* fall through */
            case 4:
                if (ndigits <= 0) {
                    ndigits = 1;
                }
                ilim = ilim1 = i = ndigits;
                break;
            case 3:
                leftright = false;
                /* fall through */
            case 5:
                i = ndigits + k + 1;
                ilim = i;
                ilim1 = i - 1;
                if (i <= 0) {
                    i = 1;
                }
        }
        /* ilim is the maximum number of significant digits we want, based on k and ndigits. */
        /*
         * ilim1 is the maximum number of significant digits we want, based on k and ndigits, when
         * it turns out that k was computed too high by one.
         */

        boolean fastFailed = false;
        if (ilim >= 0 && ilim <= Quick_max && tryQuick) {

            /* Try to get by with floating-point arithmetic. */

            i = 0;
            d2 = d;
            k0 = k;
            ilim0 = ilim;
            ieps = 2; /* conservative */
            /* Divide d by 10^k, keeping track of the roundoff error and avoiding overflows. */
            if (k > 0) {
                ds = tens[k & 0xf];
                j = k >> 4;
                if ((j & Bletch) != 0) {
                    /* prevent overflows */
                    j &= Bletch - 1;
                    d /= bigtens[n_bigtens - 1];
                    ieps++;
                }
                for (; (j != 0); j >>= 1, i++) {
                    if ((j & 1) != 0) {
                        ieps++;
                        ds *= bigtens[i];
                    }
                }
                d /= ds;
            } else if ((j1 = -k) != 0) {
                d *= tens[j1 & 0xf];
                for (j = j1 >> 4; (j != 0); j >>= 1, i++) {
                    if ((j & 1) != 0) {
                        ieps++;
                        d *= bigtens[i];
                    }
                }
            }
            /* Check that k was computed correctly. */
            if (kCheck && d < 1.0 && ilim > 0) {
                if (ilim1 <= 0) {
                    fastFailed = true;
                } else {
                    ilim = ilim1;
                    k--;
                    d *= 10.;
                    ieps++;
                }
            }
            /* eps bounds the cumulative error. */
            eps = ieps * d + 7.0;
            eps = setWord0(eps, word0(eps) - (P - 1) * Exp_msk1);
            if (ilim == 0) {
                sS = mhi = null;
                d -= 5.0;
                if (d > eps) {
                    buf.append('1');
                    k++;
                    return k + 1;
                }
                if (d < -eps) {
                    buf.setLength(0);
                    buf.append('0'); /* copy "0" to buffer */
                    return 1;
                }
                fastFailed = true;
            }
            if (!fastFailed) {
                fastFailed = true;
                if (leftright) {
                    /*
                     * Use Steele & White method of only generating digits needed.
                     */
                    eps = 0.5 / tens[ilim - 1] - eps;
                    for (i = 0;;) {
                        lL = (long) d;
                        d -= lL;
                        buf.append((char) ('0' + lL));
                        if (d < eps) {
                            return k + 1;
                        }
                        if (1.0 - d < eps) {
                            char lastCh;
                            while (true) {
                                lastCh = buf.charAt(buf.length() - 1);
                                buf.setLength(buf.length() - 1);
                                if (lastCh != '9') {
                                    break;
                                }
                                if (buf.length() == 0) {
                                    k++;
                                    lastCh = '0';
                                    break;
                                }
                            }
                            buf.append((char) (lastCh + 1));
                            return k + 1;
                        }
                        if (++i >= ilim) {
                            break;
                        }
                        eps *= 10.0;
                        d *= 10.0;
                    }
                } else {
                    /* Generate ilim digits, then fix them up. */
                    eps *= tens[ilim - 1];
                    for (i = 1;; i++, d *= 10.0) {
                        lL = (long) d;
                        d -= lL;
                        buf.append((char) ('0' + lL));
                        if (i == ilim) {
                            if (d > 0.5 + eps) {
                                char lastCh;
                                while (true) {
                                    lastCh = buf.charAt(buf.length() - 1);
                                    buf.setLength(buf.length() - 1);
                                    if (lastCh != '9') {
                                        break;
                                    }
                                    if (buf.length() == 0) {
                                        k++;
                                        lastCh = '0';
                                        break;
                                    }
                                }
                                buf.append((char) (lastCh + 1));
                                return k + 1;
                            } else if (d < 0.5 - eps) {
                                stripTrailingZeroes(buf);
                                return k + 1;
                            }
                            break;
                        }
                    }
                }
            }
            if (fastFailed) {
                buf.setLength(0);
                d = d2;
                k = k0;
                ilim = ilim0;
            }
        }

        /* Do we have a "small" integer? */

        if (be[0] >= 0 && k <= Int_max) {
            /* Yes. */
            ds = tens[k];
            if (ndigits < 0 && ilim <= 0) {
                sS = mhi = null;
                if (ilim < 0 || d < 5 * ds || (!biasUp && d == 5 * ds)) {
                    buf.setLength(0);
                    buf.append('0'); /* copy "0" to buffer */
                    return 1;
                }
                buf.append('1');
                k++;
                return k + 1;
            }
            for (i = 1;; i++) {
                lL = (long) (d / ds);
                d -= lL * ds;
                buf.append((char) ('0' + lL));
                if (i == ilim) {
                    d += d;
                    if ((d > ds) || (d == ds && (((lL & 1) != 0) || biasUp))) {
                        char lastCh;
                        while (true) {
                            lastCh = buf.charAt(buf.length() - 1);
                            buf.setLength(buf.length() - 1);
                            if (lastCh != '9') {
                                break;
                            }
                            if (buf.length() == 0) {
                                k++;
                                lastCh = '0';
                                break;
                            }
                        }
                        buf.append((char) (lastCh + 1));
                    }
                    break;
                }
                d *= 10.0;
                if (d == 0) {
                    break;
                }
            }
            return k + 1;
        }

        m2 = b2;
        m5 = b5;
        mhi = mlo = null;
        if (leftright) {
            if (mode < 2) {
                i = (denorm) ? be[0] + (Bias + (P - 1) - 1 + 1) : 1 + P - bbits[0];
                /*
                 * i is 1 plus the number of trailing zero bits in d's significand. Thus,
                 *
                 * (2^m2 * 5^m5) / (2^(s2+i) * 5^s5) = (1/2 lsb of d)/10^k.
                 */
            } else {
                j = ilim - 1;
                if (m5 >= j) {
                    m5 -= j;
                } else {
                    s5 += j -= m5;
                    b5 += j;
                    m5 = 0;
                }
                if ((i = ilim) < 0) {
                    m2 -= i;
                    i = 0;
                }
                /* (2^m2 * 5^m5) / (2^(s2+i) * 5^s5) = (1/2 * 10^(1-ilim))/10^k. */
            }
            b2 += i;
            s2 += i;
            mhi = BigInteger.ONE;
            /*
             * (mhi * 2^m2 * 5^m5) / (2^s2 * 5^s5) = one-half of last printed (when mode >= 2) or
             * input (when mode < 2) significant digit, divided by 10^k.
             */
        }
        /*
         * We still have d/10^k = (b * 2^b2 * 5^b5) / (2^s2 * 5^s5). Reduce common factors in b2,
         * m2, and s2 without changing the equalities.
         */
        if (m2 > 0 && s2 > 0) {
            i = (m2 < s2) ? m2 : s2;
            b2 -= i;
            m2 -= i;
            s2 -= i;
        }

        /* Fold b5 into b and m5 into mhi. */
        if (b5 > 0) {
            if (leftright) {
                if (m5 > 0) {
                    mhi = pow5mult(mhi, m5);
                    b1 = mhi.multiply(b);
                    b = b1;
                }
                if ((j = b5 - m5) != 0) {
                    b = pow5mult(b, j);
                }
            } else {
                b = pow5mult(b, b5);
            }
        }
        /*
         * Now we have d/10^k = (b * 2^b2) / (2^s2 * 5^s5) and (mhi * 2^m2) / (2^s2 * 5^s5) =
         * one-half of last printed or input significant digit, divided by 10^k.
         */

        sS = BigInteger.ONE;
        if (s5 > 0) {
            sS = pow5mult(sS, s5);
        }
        /*
         * Now we have d/10^k = (b * 2^b2) / (S * 2^s2) and (mhi * 2^m2) / (S * 2^s2) = one-half of
         * last printed or input significant digit, divided by 10^k.
         */

        /* Check for special case that d is a normalized power of 2. */
        specCase = false;
        if (mode < 2) {
            if ((word1(d) == 0) && ((word0(d) & Bndry_mask) == 0) && ((word0(d) & (Exp_mask & Exp_mask << 1)) != 0)) {
                /*
                 * The special case. Here we want to be within a quarter of the last input
                 * significant digit instead of one half of it when the decimal output string's
                 * value is less than d.
                 */
                b2 += Log2P;
                s2 += Log2P;
                specCase = true;
            }
        }

        /*
         * Arrange for convenient computation of quotients: shift left if necessary so divisor has 4
         * leading 0 bits.
         *
         * Perhaps we should just compute leading 28 bits of S once and for all and pass them and a
         * shift to quorem, so it can do shifts and ors to compute the numerator for q.
         */
        byte[] sbytes = sS.toByteArray();
        int sHiWord = 0;
        for (int idx = 0; idx < 4; idx++) {
            sHiWord = (sHiWord << 8);
            if (idx < sbytes.length) {
                sHiWord |= (sbytes[idx] & 0xFF);
            }
        }
        if ((i = (((s5 != 0) ? 32 - hi0bits(sHiWord) : 1) + s2) & 0x1f) != 0) {
            i = 32 - i;
        }
        /* i is the number of leading zero bits in the most significant word of S*2^s2. */
        if (i > 4) {
            i -= 4;
            b2 += i;
            m2 += i;
            s2 += i;
        } else if (i < 4) {
            i += 28;
            b2 += i;
            m2 += i;
            s2 += i;
        }
        /* Now S*2^s2 has exactly four leading zero bits in its most significant word. */
        if (b2 > 0) {
            b = b.shiftLeft(b2);
        }
        if (s2 > 0) {
            sS = sS.shiftLeft(s2);
        }
        /*
         * Now we have d/10^k = b/S and (mhi * 2^m2) / S = maximum acceptable error, divided by
         * 10^k.
         */
        if (kCheck) {
            if (b.compareTo(sS) < 0) {
                k--;
                b = b.multiply(BigInteger.TEN); /* we botched the k estimate */
                if (leftright) {
                    mhi = mhi.multiply(BigInteger.TEN);
                }
                ilim = ilim1;
            }
        }
        /* At this point 1 <= d/10^k = b/S < 10. */

        if (ilim <= 0 && mode > 2) {
            /*
             * We're doing fixed-mode output and d is less than the minimum nonzero output in this
             * mode. Output either zero or the minimum nonzero output depending on which is closer
             * to d.
             */
            if ((ilim < 0) || ((i = b.compareTo(sS = sS.multiply(BigInteger.valueOf(5)))) < 0) || ((i == 0 && !biasUp))) {
                /*
                 * Always emit at least one digit. If the number appears to be zero using the
                 * current mode, then emit one '0' digit and set decpt to 1.
                 */
                buf.setLength(0);
                buf.append('0'); /* copy "0" to buffer */
                return 1;
            }
            buf.append('1');
            k++;
            return k + 1;
        }
        if (leftright) {
            if (m2 > 0) {
                mhi = mhi.shiftLeft(m2);
            }

            /*
             * Compute mlo -- check for special case that d is a normalized power of 2.
             */

            mlo = mhi;
            if (specCase) {
                mhi = mlo;
                mhi = mhi.shiftLeft(Log2P);
            }
            /* mlo/S = maximum acceptable error, divided by 10^k, if the output is less than d. */
            /*
             * mhi/S = maximum acceptable error, divided by 10^k, if the output is greater than d.
             */

            for (i = 1;; i++) {
                BigInteger[] divResult = b.divideAndRemainder(sS);
                b = divResult[1];
                dig = (char) (divResult[0].intValue() + '0');
                /*
                 * Do we yet have the shortest decimal string that will round to d?
                 */
                j = b.compareTo(mlo);
                /* j is b/S compared with mlo/S. */
                delta = sS.subtract(mhi);
                j1 = (delta.signum() <= 0) ? 1 : b.compareTo(delta);
                /* j1 is b/S compared with 1 - mhi/S. */
                if ((j1 == 0) && (mode == 0) && ((word1(d) & 1) == 0)) {
                    if (dig == '9') {
                        buf.append('9');
                        if (roundOff(buf)) {
                            k++;
                            buf.append('1');
                        }
                        return k + 1;
                    }
                    if (j > 0) {
                        dig++;
                    }
                    buf.append(dig);
                    return k + 1;
                }
                if ((j < 0) || ((j == 0) && (mode == 0) && ((word1(d) & 1) == 0))) {
                    if (j1 > 0) {
                        /*
                         * Either dig or dig+1 would work here as the least significant decimal
                         * digit. Use whichever would produce a decimal value closer to d.
                         */
                        b = b.shiftLeft(1);
                        j1 = b.compareTo(sS);
                        if (((j1 > 0) || (j1 == 0 && (((dig & 1) == 1) || biasUp))) && (dig++ == '9')) {
                            buf.append('9');
                            if (roundOff(buf)) {
                                k++;
                                buf.append('1');
                            }
                            return k + 1;
                        }
                    }
                    buf.append(dig);
                    return k + 1;
                }
                if (j1 > 0) {
                    if (dig == '9') { /* possible if i == 1 */
                        buf.append('9');
                        if (roundOff(buf)) {
                            k++;
                            buf.append('1');
                        }
                        return k + 1;
                    }
                    buf.append((char) (dig + 1));
                    return k + 1;
                }
                buf.append(dig);
                if (i == ilim) {
                    break;
                }
                b = b.multiply(BigInteger.TEN);
                if (mlo == mhi) {
                    mlo = mhi = mhi.multiply(BigInteger.TEN);
                } else {
                    mlo = mlo.multiply(BigInteger.TEN);
                    mhi = mhi.multiply(BigInteger.TEN);
                }
            }
        } else {
            for (i = 1;; i++) {
                BigInteger[] divResult = b.divideAndRemainder(sS);
                b = divResult[1];
                dig = (char) (divResult[0].intValue() + '0');
                buf.append(dig);
                if (i >= ilim) {
                    break;
                }
                b = b.multiply(BigInteger.TEN);
            }
        }

        /* Round off last digit */

        b = b.shiftLeft(1);
        j = b.compareTo(sS);
        if ((j > 0) || (j == 0 && (((dig & 1) == 1) || biasUp))) {
            if (roundOff(buf)) {
                k++;
                buf.append('1');
                return k + 1;
            }
        } else {
            stripTrailingZeroes(buf);
        }
        return k + 1;
    }

    private static void stripTrailingZeroes(StringBuilder buf) {
        int bl = buf.length();
        while (bl-- > 0 && buf.charAt(bl) == '0') {
            // empty
        }
        buf.setLength(bl + 1);
    }

    /* Mapping of JSDToStrMode -> JS_dtoa mode */
    private static final int[] dtoaModes = {
                    0, /* DTOSTR_STANDARD */
                    0, /* DTOSTR_STANDARD_EXPONENTIAL, */
                    3, /* DTOSTR_FIXED, */
                    2, /* DTOSTR_EXPONENTIAL, */
                    2, /* DTOSTR_PRECISION */
    };

    @SuppressWarnings("fallthrough")
    public static void jsDtostr(StringBuilder buffer, int modeParam, int precision, double d) {
        int decPt; /* Position of decimal point relative to first digit returned by JS_dtoa */
        boolean[] sign = new boolean[1]; /* true if the sign bit was set in d */
        int nDigits; /* Number of significand digits returned by JS_dtoa */
        int mode = modeParam;

        // JS_ASSERT(bufferSize >= (size_t)(mode <= DTOSTR_STANDARD_EXPONENTIAL
        // ? DTOSTR_STANDARD_BUFFER_SIZE : DTOSTR_VARIABLE_BUFFER_SIZE(precision)));

        if (mode == DTOSTR_FIXED && (d >= 1e21 || d <= -1e21)) {
            /*
             * Change mode here rather than below because the buffer may not be large enough to hold
             * a large integer.
             */
            mode = DTOSTR_STANDARD;
        }
        decPt = jsDtoa(d, dtoaModes[mode], mode >= DTOSTR_FIXED, precision, sign, buffer);
        nDigits = buffer.length();

        /* If Infinity, -Infinity, or NaN, return the string regardless of the mode. */
        if (decPt != 9999) {
            boolean exponentialNotation = false;
            /* Minimum number of significand digits required by mode and precision */
            int minNDigits = 0;
            int p;

            switch (mode) {
                case DTOSTR_STANDARD:
                    if (decPt < -5 || decPt > 21) {
                        exponentialNotation = true;
                    } else {
                        minNDigits = decPt;
                    }
                    break;

                case DTOSTR_FIXED:
                    if (precision >= 0) {
                        minNDigits = decPt + precision;
                    } else {
                        minNDigits = decPt;
                    }
                    break;

                case DTOSTR_EXPONENTIAL:
                    // JS_ASSERT(precision > 0);
                    minNDigits = precision;
                    /* fall through */
                case DTOSTR_STANDARD_EXPONENTIAL:
                    exponentialNotation = true;
                    break;

                case DTOSTR_PRECISION:
                    // JS_ASSERT(precision > 0);
                    minNDigits = precision;
                    if (decPt < -5 || decPt > precision) {
                        exponentialNotation = true;
                    }
                    break;
            }

            /* If the number has fewer than minNDigits, pad it with zeros at the end */
            if (nDigits < minNDigits) {
                p = minNDigits;
                nDigits = minNDigits;
                do {
                    buffer.append('0');
                } while (buffer.length() != p);
            }

            if (exponentialNotation) {
                /* Insert a decimal point if more than one significand digit */
                if (nDigits != 1) {
                    buffer.insert(1, '.');
                }
                buffer.append('e');
                if ((decPt - 1) >= 0) {
                    buffer.append('+');
                }
                buffer.append(decPt - 1);
            } else if (decPt != nDigits) {
                /* Some kind of a fraction in fixed notation */
                // JS_ASSERT(decPt <= nDigits);
                if (decPt > 0) {
                    /* dd...dd . dd...dd */
                    buffer.insert(decPt, '.');
                } else {
                    /* 0 . 00...00dd...dd */
                    for (int i = 0; i < 1 - decPt; i++) {
                        buffer.insert(0, '0');
                    }
                    buffer.insert(1, '.');
                }
            }
        }

        /* If negative and neither -0.0 nor NaN, output a leading '-'. */
        if (sign[0] && !(word0(d) == Sign_bit && word1(d) == 0) && !((word0(d) & Exp_mask) == Exp_mask && ((word1(d) != 0) || ((word0(d) & Frac_mask) != 0)))) {
            buffer.insert(0, '-');
        }
    }

}

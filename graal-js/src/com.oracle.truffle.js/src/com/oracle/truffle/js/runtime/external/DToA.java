/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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

import java.math.BigInteger;

/**
 * This file is from Mozilla Rhino, published under MPL 2.0.
 */
public class DToA {

    private DToA() {
        // should not be constructed
    }

    private static char basedigit(int digit) {
        return (char) ((digit >= 10) ? 'a' - 10 + digit : '0' + digit);
    }

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

}

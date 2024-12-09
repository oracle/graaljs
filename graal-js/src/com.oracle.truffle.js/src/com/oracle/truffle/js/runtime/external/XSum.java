/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

/* Copyright 2015, 2018, 2021, 2024 Radford M. Neal

   Permission is hereby granted, free of charge, to any person obtaining
   a copy of this software and associated documentation files (the
   "Software"), to deal in the Software without restriction, including
   without limitation the rights to use, copy, modify, merge, publish,
   distribute, sublicense, and/or sell copies of the Software, and to
   permit persons to whom the Software is furnished to do so, subject to
   the following conditions:

   The above copyright notice and this permission notice shall be
   included in all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
   LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
   OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
   WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.oracle.truffle.js.runtime.external;

/**
 * Implementation of exact summation of double numbers based on xsum
 * (https://gitlab.com/radfordneal/xsum).
 */
public class XSum {

    // CONSTANTS DEFINING THE FLOATING POINT FORMAT

    /**
     * Bits in fp mantissa, excludes implicit 1.
     */
    private static final int XSUM_MANTISSA_BITS = 52;

    /**
     * Bits in fp exponent.
     */
    private static final int XSUM_EXP_BITS = 11;

    /**
     * Mask for mantissa bits.
     */
    private static final long XSUM_MANTISSA_MASK = (1L << XSUM_MANTISSA_BITS) - 1;

    /**
     * Mask for exponent.
     */
    private static final long XSUM_EXP_MASK = (1 << XSUM_EXP_BITS) - 1;

    /**
     * Bias added to signed exponent.
     */
    private static final int XSUM_EXP_BIAS = (1 << (XSUM_EXP_BITS - 1)) - 1;

    /**
     * Position of sign bit.
     */
    private static final int XSUM_SIGN_BIT = XSUM_MANTISSA_BITS + XSUM_EXP_BITS;

    /**
     * Mask for sign bit.
     */
    private static final long XSUM_SIGN_MASK = 1L << XSUM_SIGN_BIT;

    // CONSTANTS DEFINING THE SMALL ACCUMULATOR FORMAT

    /**
     * Bits in chunk of the small accumulator.
     */
    private static final int XSUM_SCHUNK_BITS = 64;

    /**
     * Number of low bits of exponent, in one chunk.
     */
    private static final int XSUM_LOW_EXP_BITS = 5;

    /**
     * Mask for low-order exponent bits.
     */
    private static final int XSUM_LOW_EXP_MASK = (1 << XSUM_LOW_EXP_BITS) - 1;

    /**
     * Number of high exponent bits for index.
     */
    private static final int XSUM_HIGH_EXP_BITS = XSUM_EXP_BITS - XSUM_LOW_EXP_BITS;

    /**
     * Number of chunks in the small accumulator.
     */
    private static final int XSUM_SCHUNKS = (1 << XSUM_HIGH_EXP_BITS) + 3;

    /**
     * Bits in low part of mantissa.
     */
    private static final int XSUM_LOW_MANTISSA_BITS = 1 << XSUM_LOW_EXP_BITS;

    /**
     * Mask for low bits.
     */
    private static final long XSUM_LOW_MANTISSA_MASK = (1L << XSUM_LOW_MANTISSA_BITS) - 1;

    /**
     * Bits sums can carry into.
     */
    private static final int XSUM_SMALL_CARRY_BITS = (XSUM_SCHUNK_BITS - 1) - XSUM_MANTISSA_BITS;

    /**
     * Number of terms that can added before the propagation is needed.
     */
    private static final int XSUM_SMALL_CARRY_TERMS = (1 << XSUM_SMALL_CARRY_BITS) - 1;

    public static class SmallAccumulator {
        // Chunks making up small accumulator
        private long[] chunk = new long[XSUM_SCHUNKS];
        // If non-zero, +Inf, -Inf, or NaN
        private long inf;
        // If non-zero, a NaN value with payload
        private long nan;
        // Number of remaining adds before carry propagation must be done again
        private int addsUntilPropagate = XSUM_SMALL_CARRY_TERMS;

        /**
         * Add an array of floating-point numbers to a small accumulator. Mixes calls of
         * carryPropagate with calls of add1NoCarry.
         */
        public void addArray(double[] vec) {
            int n = vec.length; // number of elements not added yet
            int pos = 0;
            while (n > 0) {
                if (addsUntilPropagate == 0) {
                    carryPropagate();
                }
                int m = (n <= addsUntilPropagate) ? n : addsUntilPropagate;
                int nextPos = pos + m;
                for (int i = pos; i < nextPos; i++) {
                    add1NoCarry(vec[i]);
                }
                addsUntilPropagate -= m;
                pos = nextPos;
                n -= m;
            }
        }

        /**
         * Add one double to a small accumulator. This is equivalent to, but somewhat faster than,
         * calling addArray() with a vector of one value.
         */
        public void add(double value) {
            if (addsUntilPropagate == 0) {
                carryPropagate();
            }

            add1NoCarry(value);

            addsUntilPropagate--;
        }

        /**
         * Propagate carries to next chunk in a small accumulator. Needs to be called often enough
         * that accumulated carries don't overflow out the top, as indicated by
         * {@code addsUntilPropagate}. Returns the index of the uppermost non-zero chunk (0 if
         * number is zero).
         *
         * After carry propagation, the uppermost non-zero chunk will indicate the sign of the
         * number, and will not be -1 (all 1s). It will be in the range -2^XSUM_LOW_MANTISSA_BITS to
         * 2^XSUM_LOW_MANTISSA_BITS - 1. Lower chunks will be non-negative, and in the range from 0
         * up to 2^XSUM_LOW_MANTISSA_BITS - 1.
         */
        private int carryPropagate() {
            int u;
            int uix;

            done: {
                // Set u to the index of the uppermost non-zero (for now) chunk, or
                // return with value 0 if there is none.

                for (u = XSUM_SCHUNKS - 1; chunk[u] == 0; u--) {
                    if (u == 0) {
                        uix = 0;
                        break done;
                    }
                }

                // Carry propagate, starting at the low-order chunks. Note that the
                // loop limit of u may be increased inside the loop.

                uix = -1; // indicates that a non-zero chunk has not been found yet

                int i = 0; // set to the index of the next non-zero chunck, from bottom

                // Quickly skip over unused low-order chunks. Done here at the start
                // on the theory that there are often many unused low-order chunks,
                // justifying some overhead to begin, but later stretches of unused
                // chunks may not be as large.

                int e = u - 3; // go only to 3 before so won't access beyond chunk array
                do {
                    if ((chunk[i] | chunk[i + 1] | chunk[i + 2] | chunk[i + 3]) != 0) {
                        break;
                    }
                    i += 4;
                } while (i <= e);

                do {
                    long c; // Set to the chunk at index i (next non-zero one)

                    // Find the next non-zero chunk, setting i to its index, or break out
                    // of loop if there is none. Note that the chunk at index u is not
                    // necessarily non-zero - it was initially, but u or the chunk at u
                    // may have changed.

                    do {
                        c = chunk[i];
                        if (c != 0) {
                            break;
                        }
                        i += 1;
                    } while (i <= u);

                    if (c == 0) {
                        break;
                    }

                    // Propagate possible carry from this chunk to next chunk up.

                    long chigh = c >> XSUM_LOW_MANTISSA_BITS;
                    if (chigh == 0) {
                        uix = i;
                        i += 1;
                        continue;  // no need to change this chunk
                    }

                    if (u == i) {
                        if (chigh == -1) {
                            uix = i;
                            break; // don't propagate -1 into the region of all zeros above
                        }
                        u = i + 1; // we will change chunk[u+1], so we'll need to look at it
                    }

                    long clow = c & XSUM_LOW_MANTISSA_MASK;
                    if (clow != 0) {
                        uix = i;
                    }

                    // We now change chunk[i] and add to chunk[i+1]. Note that i+1 should be
                    // in range (no bigger than XSUM_CHUNKS-1) if summing memory, since
                    // the number of chunks is big enough to hold any sum, and we do not
                    // store redundant chunks with values 0 or -1 above previously non-zero
                    // chunks. But other add operations might cause overflow, in which
                    // case we produce a NaN with all 1s as payload. (We can't reliably produce
                    // an Inf of the right sign.)

                    chunk[i] = clow;
                    if (i + 1 >= XSUM_SCHUNKS) {
                        addInfNan((XSUM_EXP_MASK << XSUM_MANTISSA_BITS) | XSUM_MANTISSA_MASK);
                        u = i;
                    } else {
                        chunk[i + 1] += chigh;
                    }

                    i += 1;

                } while (i <= u);

                // Check again for the number being zero, since carry propagation might
                // have created zero from something that initially looked non-zero. */

                if (uix < 0) {
                    uix = 0;
                    break done;
                }

                // While the uppermost chunk is negative, with value -1, combine it with
                // the chunk below (if there is one) to produce the same number but with
                // one fewer non-zero chunks.

                while (chunk[uix] == -1 && uix > 0) {
                    // Left shift of a negative number is undefined according to the standard,
                    // so do a multiply - it's all presumably constant-folded by the compiler.
                    chunk[uix - 1] += (-1L) * (1L << XSUM_LOW_MANTISSA_BITS);
                    chunk[uix] = 0;
                    uix -= 1;
                }
            }

            // We can now add one less than the total allowed terms before the next carry propagate.

            addsUntilPropagate = XSUM_SMALL_CARRY_TERMS - 1;

            // Return index of uppermost non-zero chunk.

            return uix;
        }

        /**
         * Add one number to a small accumulator assuming no carry propagation is required.
         */
        private void add1NoCarry(double value) {
            // Extract exponent and mantissa. Split exponent into high and low parts.
            long ivalue = Double.doubleToRawLongBits(value);
            int exp = (int) ((ivalue >> XSUM_MANTISSA_BITS) & XSUM_EXP_MASK);
            long mantissa = ivalue & XSUM_MANTISSA_MASK;
            int highExp = exp >> XSUM_LOW_EXP_BITS;
            int lowExp = exp & XSUM_LOW_EXP_MASK;

            // Categorize number as normal, denormalized, or Inf/NaN according to
            // the value of the exponent field.

            if (exp == 0) { // zero or denormalized
                // If it's a zero (positive or negative), we do nothing.
                if (mantissa == 0) {
                    return;
                }
                // Denormalized mantissa has no implicit 1, but exponent is 1 not 0.
                exp = lowExp = 1;
            } else if (exp == XSUM_EXP_MASK) { // Inf or NaN
                // Just update flags in accumulator structure.
                addInfNan(ivalue);
                return;
            } else { // normalized
                // OR in implicit 1 bit at top of mantissa
                mantissa |= 1L << XSUM_MANTISSA_BITS;
            }

            // Use high part of exponent as index of chunk, and low part of
            // exponent to give position within chunk. Fetch the two chunks
            // that will be modified.

            // Separate mantissa into two parts, after shifting, and add to (or
            // subtract from) this chunk and the next higher chunk (which always
            // exists since there are three extra ones at the top).

            // Note that lowMantissa will have at most XSUM_LOW_MANTISSA_BITS bits,
            // while highMantissa will have at most XSUM_MANTISSA_BITS bits, since
            // even though highMantissa includes the extra implicit 1 bit, it will
            // also be shifted right by at least one bit.

            long lowMantissa = (mantissa << lowExp) & XSUM_LOW_MANTISSA_MASK;
            long highMantissa = mantissa >> (XSUM_LOW_MANTISSA_BITS - lowExp);

            // Add or subtract to or from the two affected chunks.

            if (ivalue < 0) {
                chunk[highExp] -= lowMantissa;
                chunk[highExp + 1] -= highMantissa;
            } else {
                chunk[highExp] += lowMantissa;
                chunk[highExp + 1] += highMantissa;
            }
        }

        /**
         * Add an inf or NaN to a small accumulator. This only changes the flags, not the chunks in
         * the accumulator, which retains the sum of the finite terms (which is perhaps sometimes
         * useful to access, though no function to do so is defined at present). A NaN with larger
         * payload (seen as a 52-bit unsigned integer) takes precedence, with the sign of the NaN
         * always being positive. This ensures that the order of summing NaN values doesn't matter.
         */
        private void addInfNan(long ivalue) {
            long mantissa = ivalue & XSUM_MANTISSA_MASK;

            if (mantissa == 0) { // Inf
                if (inf == 0) { // no previous Inf
                    inf = ivalue;
                } else if (inf != ivalue) { // previous Inf was opposite sign
                    inf = Double.doubleToRawLongBits(Double.NaN); // result will be a NaN
                }
            } else { // NaN
                // Choose the NaN with the bigger payload and clear its sign.
                // Using <= ensures that we will choose the first NaN over the previous zero.
                if ((nan & XSUM_MANTISSA_MASK) <= mantissa) {
                    nan = ivalue & ~XSUM_SIGN_MASK;
                }
            }
        }

        /**
         * Return the result of rounding a small accumulator. The rounding mode is to nearest, with
         * ties to even. The small accumulator may be modified by this operation (by carry
         * propagation being done), but the value it represents should not change.
         */
        public double round() {
            long intv;

            // See if we have a NaN from one of the numbers being a NaN, in
            // which case we return the NaN with largest payload, or an infinite
            // result (+Inf, -Inf, or a NaN if both +Inf and -Inf occurred).
            // Note that we do NOT return NaN if we have both an infinite number
            // and a sum of other numbers that overflows with opposite sign,
            // since there is no real ambiguity regarding the sign in such a case.

            if (nan != 0) {
                return Double.longBitsToDouble(nan);
            }

            if (inf != 0) {
                return Double.longBitsToDouble(inf);
            }

            // If none of the numbers summed were infinite or NaN, we proceed to
            // propagate carries, as a preliminary to finding the magnitude of
            // the sum. This also ensures that the sign of the result can be
            // determined from the uppermost non-zero chunk.

            // We also find the index, i, of this uppermost non-zero chunk, as
            // the value returned by carryPropagate, and set ivalue to
            // chunk[i]. Note that ivalue will not be 0 or -1, unless
            // i is 0 (the lowest chunk), in which case it will be handled by
            // the code for denormalized numbers.

            int i = carryPropagate();

            long ivalue = chunk[i];

            // Handle a possible denormalized number, including zero.

            if (i <= 1) {
                // Check for zero value, in which case we can return immediately.
                if (ivalue == 0) {
                    return 0.0;
                }

                // Check if it is actually a denormalized number. It always is if only
                // the lowest chunk is non-zero. If the highest non-zero chunk is the
                // next-to-lowest, we check the magnitude of the absolute value.
                // Note that the real exponent is 1 (not 0), so we need to shift right
                // by 1 here.

                if (i == 0) {
                    intv = ivalue >= 0 ? ivalue : -ivalue;
                    intv >>= 1;
                    if (ivalue < 0) {
                        intv |= XSUM_SIGN_MASK;
                    }
                    return Double.longBitsToDouble(intv);
                } else {
                    // Note: Left shift of -ve number is undefined, so do a multiply instead,
                    // which is probably optimized to a shift.
                    intv = ivalue * (1L << (XSUM_LOW_MANTISSA_BITS - 1)) + (chunk[0] >> 1);
                    if (intv < 0) {
                        if (intv > -(1L << XSUM_MANTISSA_BITS)) {
                            intv = (-intv) | XSUM_SIGN_MASK;
                            return Double.longBitsToDouble(intv);
                        }
                    } else {
                        if (intv < 1L << XSUM_MANTISSA_BITS) {
                            return Double.longBitsToDouble(intv);
                        }
                    }
                    // otherwise, it's not actually denormalized, so fall through to below
                }
            }

            // Find the location of the uppermost 1 bit in the absolute value of the
            // upper chunk by converting it (as a signed integer) to a floating point
            // value, and looking at the exponent. Then set 'more' to the number of
            // bits from the lower chunk (and maybe the next lower) that are needed
            // to fill out the mantissa of the result (including the top implicit 1 bit), plus two
            // extra bits to help decide on rounding. For negative numbers, it may turn out later
            // that we need another bit because negating a negative value may carry out of the top
            // here, but not once more bits are shifted into the bottom later on.

            intv = Double.doubleToRawLongBits(ivalue);
            int e = (int) ((intv >> XSUM_MANTISSA_BITS) & XSUM_EXP_MASK); // e-bias is in 0..32
            int more = 2 + XSUM_MANTISSA_BITS + XSUM_EXP_BIAS - e;

            // Change 'ivalue' to put in 'more' bits from lower chunks into the bottom.
            // Also set 'j' to the index of the lowest chunk from which these bits came,
            // and 'lower' to the remaining bits of that chunk not now in 'ivalue'.
            // Note that 'lower' initially has at least one bit in it, which we can
            // later move into 'ivalue' if it turns out that one more bit is needed.

            ivalue *= 1L << more;  // multiply, since << of negative undefined
            int j = i - 1;
            long lower = chunk[j];  // must exist, since denormalized if i==0
            if (more >= XSUM_LOW_MANTISSA_BITS) {
                more -= XSUM_LOW_MANTISSA_BITS;
                ivalue += lower << more;
                j -= 1;
                lower = j < 0 ? 0 : chunk[j];
            }
            ivalue += lower >> (XSUM_LOW_MANTISSA_BITS - more);
            lower &= (1L << (XSUM_LOW_MANTISSA_BITS - more)) - 1;

            // Decide on rounding, with separate code for positive and negative values.

            // At this point, 'ivalue' has the signed mantissa bits, plus two extra
            // bits, with 'e' recording the exponent position for these within their
            // top chunk. For positive 'ivalue', the bits in 'lower' and chunks
            // below 'j' add to the absolute value; for negative 'ivalue' they
            // subtract.

            // After setting 'ivalue' to the tentative unsigned mantissa
            // (shifted left 2), and 'intv' to have the correct sign, this
            // code goes to done_rounding if it finds that just discarding lower
            // order bits is correct, and to round_away_from_zero if instead the
            // magnitude should be increased by one in the lowest mantissa bit. */

            done_rounding: {
                round_away_from_zero: {

                    if (ivalue >= 0) { // number is positive, lower bits are added to magnitude
                        intv = 0; // positive sign

                        if ((ivalue & 2) == 0) { // extra bits are 0x
                            break done_rounding;
                        }

                        if ((ivalue & 1) != 0) { // extra bits are 11
                            break round_away_from_zero;
                        }

                        if ((ivalue & 4) != 0) { // low bit is 1 (odd), extra bits are 10
                            break round_away_from_zero;
                        }

                        if (lower == 0) { // see if any lower bits are non-zero
                            while (j > 0) {
                                j -= 1;
                                if (chunk[j] != 0) {
                                    lower = 1;
                                    break;
                                }
                            }
                        }

                        if (lower != 0) { // low bit 0 (even), extra bits 10, non-zero lower bits
                            break round_away_from_zero;
                        } else { // low bit 0 (even), extra bits 10, all lower bits 0
                            break done_rounding;
                        }
                    } else { // number is negative, lower bits are subtracted from magnitude

                        // Check for a negative 'ivalue' that when negated doesn't contain a full
                        // mantissa's worth of bits, plus one to help rounding. If so, move one
                        // more bit into 'ivalue' from 'lower' (and remove it from 'lower').
                        // This happens when the negation of the upper part of 'ivalue' has the
                        // form 10000... but the negation of the full 'ivalue' is not 10000...

                        if (((-ivalue) & (1L << (XSUM_MANTISSA_BITS + 2))) == 0) {
                            long pos = 1L << (XSUM_LOW_MANTISSA_BITS - 1 - more);
                            ivalue *= 2; // note that left shift undefined if ivalue is negative
                            if ((lower & pos) != 0) {
                                ivalue += 1;
                                lower &= ~pos;
                            }
                            e -= 1;
                        }

                        intv = XSUM_SIGN_MASK; // negative sign
                        ivalue = -ivalue; // ivalue now contains the absolute value

                        if ((ivalue & 3) == 3) { // extra bits are 11
                            break round_away_from_zero;
                        }

                        if ((ivalue & 3) <= 1) { // extra bits are 00 or 01
                            break done_rounding;
                        }

                        if ((ivalue & 4) == 0) { // low bit is 0 (even), extra bits are 10
                            break done_rounding;
                        }

                        if (lower == 0) { // see if any lower bits are non-zero
                            while (j > 0) {
                                j -= 1;
                                if (chunk[j] != 0) {
                                    lower = 1;
                                    break;
                                }
                            }
                        }

                        if (lower != 0) { // low bit 1 (odd), extra bits 10, non-zero lower bits
                            break done_rounding;
                        } else { // low bit 1 (odd), extra bits are 10, lower bits are all 0
                            break round_away_from_zero;
                        }

                    }
                } // round_away_from_zero:

                // Round away from zero, then check for carry having propagated out the
                // top, and shift if so.

                ivalue += 4; // add 1 to low-order mantissa bit
                if ((ivalue & (1L << (XSUM_MANTISSA_BITS + 3))) != 0) {
                    ivalue >>= 1;
                    e += 1;
                }
            } // done_rounding:

            // Get rid of the bottom 2 bits that were used to decide on rounding.

            ivalue >>= 2;

            // Adjust to the true exponent, accounting for where this chunk is.

            e += (i << XSUM_LOW_EXP_BITS) - XSUM_EXP_BIAS - XSUM_MANTISSA_BITS;

            // If exponent has overflowed, change to plus or minus Inf and return.

            if (e >= XSUM_EXP_MASK) {
                intv |= XSUM_EXP_MASK << XSUM_MANTISSA_BITS;
                return Double.longBitsToDouble(intv);
            }

            // Put exponent and mantissa into intv, which already has the sign,
            // then return fltv.

            intv += ((long) e) << XSUM_MANTISSA_BITS;
            intv += ivalue & XSUM_MANTISSA_MASK; /* mask out the implicit 1 bit */

            return Double.longBitsToDouble(intv);
        }

    }

}

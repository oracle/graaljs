/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Large binary and octal Number literals must round the exact integer once, rather than accumulating
 * rounding errors digit by digit. Cover rounding ties, numeric separators, legacy octal, and overflow.
 */

load('assert.js');

const expected = 2 ** 63 + 2048;
assertSame(expected, 0b1000000000000000000000000000000000000000000000000000010000000001);
assertSame(expected, 0o1000000000000000002001);
assertSame(expected, Function('return 01000000000000000002001;')());

for (const bits of [52, 53, 62, 63, 64, 65, 127, 511, 1023, 1024]) {
    const base = 1n << BigInt(bits);
    const spacing = 1n << BigInt(Math.max(0, bits - 52));
    const half = spacing / 2n;
    for (const offset of [-1n, 0n, 1n, half - 1n, half, half + 1n, spacing - 1n]) {
        const value = base + offset;
        for (const [radix, prefix] of [[2, '0b'], [8, '0o'], [16, '0x']]) {
            const digits = value.toString(radix);
            const separated = digits.replace(/(.{4})(?=.)/g, '$1_');
            for (const literal of [prefix + digits, prefix + separated]) {
                assertSame(Number(value), Function('return ' + literal + ';')());
                assertSame(-Number(value), Function('return -' + literal + ';')());
            }
            if (radix === 8) {
                assertSame(Number(value), Function('return 0' + digits + ';')());
            }
        }
    }
}

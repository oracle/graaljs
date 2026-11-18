/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

function assertConversion(expected, input) {
    assertSame(expected, Number(input));
    assertSame(expected, +input);
}

// The BigInteger fallback must reject signs and Unicode digits just like the short integer paths.
for (const [radixPrefix, shortDigitLimit] of [["0x", 13], ["0o", 17], ["0b", 53]]) {
    for (const prefix of [radixPrefix, radixPrefix.toUpperCase()]) {
        for (const digitCount of [shortDigitLimit - 1, shortDigitLimit, shortDigitLimit + 1, shortDigitLimit + 20]) {
            const digits = "0".repeat(digitCount - 1) + "1";
            assertConversion(1, prefix + digits);
            assertConversion(NaN, prefix + "+" + digits);
            assertConversion(NaN, prefix + "-" + digits);
        }

        const zeros = "0".repeat(shortDigitLimit + 1);
        assertConversion(0, prefix + zeros);
        assertConversion(1, "\u00a0" + prefix + zeros + "1\ufeff");
        for (const digit of ["\u0661", "\u06f1", "\u0967", "\uff11", "\u{1d7d9}"]) {
            assertConversion(NaN, prefix + digit + zeros);
            assertConversion(NaN, prefix + "1" + zeros + digit + "1");
            assertConversion(NaN, prefix + zeros + digit);
        }

        // BigInteger must still reject invalid ASCII characters after a valid first digit.
        assertConversion(NaN, prefix + "1" + zeros + "g");
        assertConversion(NaN, prefix + "1" + zeros + "+1");
        assertConversion(NaN, prefix + "1" + zeros + ".0");
    }
}

// Full-width hexadecimal letters are also accepted as digits by BigInteger.
for (const letter of ["\uff21", "\uff26", "\uff41", "\uff46"]) {
    assertConversion(NaN, "0x" + "0".repeat(13) + letter);
    assertConversion(NaN, "0X" + letter + "0".repeat(13));
}

assertConversion(2 ** 80, "0x1" + "0".repeat(20));
assertConversion(2 ** 60, "0o1" + "0".repeat(20));
assertConversion(2 ** 60, "0b1" + "0".repeat(60));
assertConversion(Infinity, "0x1" + "0".repeat(256));
assertConversion(Infinity, "0o1" + "0".repeat(342));
assertConversion(Infinity, "0b1" + "0".repeat(1024));

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

assertConversion(12, "12");
assertConversion(-12, "-12");

// StringNumericLiteral supports hexadecimal integers, but no hexadecimal floating-point form.
for (const hexadecimal of [
    "0x1p0", "0X1P+1", "0x1.8p1", "0x1.p-1", "0x.8p0",
    "0x.p1", "0x0p0", "0x1p10000", "0x1p-10000"
]) {
    for (const sign of ["+", "-", ""]) {
        const input = sign + hexadecimal;
        assertConversion(NaN, input);
        assertConversion(NaN, " \t" + input + "\n");
    }
}

for (const [input, expected] of [
    ["0", 0],
    ["-0", -0],
    ["0.", 0],
    ["-0.", -0],
    ["0.5", 0.5],
    ["-0.5", -0.5],
    ["0e1", 0],
    ["-0e1", -0],
    ["1e3", 1000],
    ["0x10", 16],
    ["0Xff", 255]
]) {
    assertConversion(expected, input);
}

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

// Exercise the integer paths before and after falling back to double parsing.
for (let iteration = 0; iteration < 2; iteration++) {
    assertConversion(12, "12");
    assertConversion(-12, "-12");
    for (const input of ["12\0", "123\x1f", "1.5\x01", "1e3\x1e", "-1.00000e+0\x01\n"]) {
        assertConversion(NaN, input);
    }
    assertConversion(1.5, "1.5");
}

const numbers = [
    ["12", 12],
    ["123", 123],
    ["1.5", 1.5],
    ["1e3", 1000],
    ["-1.00000e+0", -1],
    ["0000000000000000000000000000000012", 12]
];
const validC0Whitespace = new Set([0x0009, 0x000a, 0x000b, 0x000c, 0x000d, 0x0020]);
for (let codePoint = 0; codePoint <= 0x0020; codePoint++) {
    const control = String.fromCodePoint(codePoint);
    const whitespace = validC0Whitespace.has(codePoint);
    assertConversion(whitespace ? 0 : NaN, control);
    for (const [input, value] of numbers) {
        const expected = whitespace ? value : NaN;
        assertConversion(expected, control + input);
        assertConversion(expected, input + control);
        assertConversion(expected, " \t" + input + control + "\n");
    }
    assertConversion(NaN, "1" + control + "2");
}

for (const [input, expected] of [
    ["", 0],
    ["-0", -0],
    ["1.", 1],
    [".5", 0.5],
    ["+.5", 0.5],
    ["1.e2", 100],
    ["Infinity", Infinity],
    ["-Infinity", -Infinity],
    ["1f", NaN],
    ["1d", NaN]
]) {
    assertConversion(expected, input);
}

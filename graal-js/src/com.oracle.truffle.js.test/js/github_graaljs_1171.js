/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

function pow(a, b) {
    return Math.pow(a, b);
}

function exponentiate(a, b) {
    return a ** b;
}

const nearOne = 1 + 2 ** -30;
const largeExponent = 2 ** 31 - 1;

// Interleaving different exponents must not change the result at the same call site.
assertSame(2, pow(4, 0.5));
const before = pow(-nearOne, largeExponent);
assertSame(0.5, pow(2, -1));
assertSame(before, pow(-nearOne, largeExponent));

assertSame(exponentiate(2.5, 1.5), pow(2.5, 1.5));
assertSame(exponentiate(nearOne, largeExponent), pow(nearOne, largeExponent));
assertTrue(pow(nearOne, largeExponent) > pow(nearOne, 2 ** 31 - 1.1));

for (const [a, b, expected] of [
    [-3, 2, 9],
    [-0, 2, 0],
    [-0, 0.5, 0],
    [-Infinity, 0.5, Infinity],
    [-1, 0.5, NaN],
    ["4", "0.5", 2],
    [NaN, 0, 1],
    [NaN, -0, 1],
    [-0, 1, -0],
    [NaN, 1, NaN],
    [591.7, -1, 1 / 591.7],
    [0, -1, Infinity],
    [-0, -1, -Infinity],
    [-Infinity, -1, -0],
]) {
    assertSame(expected, pow(a, b));
    assertSame(expected, exponentiate(a, b));
}

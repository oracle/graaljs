/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const transitions = [
    {a: 0, b: -7, expected: -0},
    {a: -7, b: 0, expected: -0},
    {a: 65536, b: 65536, expected: 4294967296},
    {a: -65536, b: -65536, expected: 4294967296},
    {a: 65536, b: -65536, expected: -4294967296},
    {a: -2147483648, b: -1, expected: 2147483648},
];

for (let i = 0; i < transitions.length; i++) {
    // Use a distinct function body so each transition starts with fresh specializations.
    const multiply = new Function(`return function multiply${i}(a, b) { return a * b; };`)();
    for (let j = 0; j < 1000; j++) {
        assertSame(21, multiply(3, 7));
        assertSame(21, multiply(-3, -7));
        assertSame(0, multiply(0, 0));
        if (i % 2) {
            assertSame(-21, multiply(3, -7));
        }
    }

    const {a, b, expected} = transitions[i];
    assertSame(expected, multiply(a, b));
    assertSame(21, multiply(3, 7));
    assertSame(21, multiply(-3, -7));
    assertSame(-21, multiply(3, -7));
    assertSame(-21, multiply(-3, 7));
    assertSame(0, multiply(0, 0));
    assertSame(0, multiply(0, 7));
    assertSame(0, multiply(7, 0));
    assertSame(-0, multiply(0, -7));
    assertSame(-0, multiply(-7, 0));
    assertSame(-0, multiply(-0, 7));
    assertSame(-0, multiply(7, -0));
    assertSame(0, multiply(-0, -7));
    assertSame(0, multiply(-7, -0));
    assertSame(-2147483648, multiply(-2147483648, 1));
    assertSame(2147395600, multiply(46340, 46340));
    assertSame(-0, multiply(-Number.MIN_VALUE, 0.5));
    assertSame(Infinity, multiply(Number.MAX_VALUE, 2));
    assertSame(NaN, multiply(Infinity, 0));
    assertSame(NaN, multiply(NaN, 7));

    const log = [];
    const left = {valueOf() { log.push("left"); return 0; }};
    const right = {valueOf() { log.push("right"); return -7; }};
    assertSame(-0, multiply(left, right));
    assertSameContent(["left", "right"], log);
    assertSame(-0, multiply("0", -7));
    assertSame(0n, multiply(0n, -7n));
    assertSame(-21n, multiply(3n, -7n));
    assertThrows(() => multiply(1n, 1), TypeError);
    assertThrows(() => multiply(1, 1n), TypeError);
    assertThrows(() => multiply(Symbol(), 1), TypeError);
    assertSame(21, multiply(3, 7));
}

assertSame(-0, 0 * -7);
assertSame(-0, -7 * 0);
assertSame(0, 0 * 0);
assertSame(4294967296, 65536 * 65536);
assertSame(-4294967296, 65536 * -65536);

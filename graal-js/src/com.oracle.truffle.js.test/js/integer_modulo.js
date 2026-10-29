/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const transitions = [
    {a: -4, b: 2, expected: -0},
    {a: -6, b: 3, expected: -0},
    {a: -4, b: -2, expected: -0},
    {a: -2147483648, b: -1, expected: -0},
    {a: -2147483648, b: 1073741824, expected: -0},
    {a: -2147483648, b: -2147483648, expected: -0},
    {a: 0, b: 0, expected: NaN},
    {a: 5, b: 0, expected: NaN},
    {a: -5, b: 0, expected: NaN},
];

for (let i = 0; i < transitions.length; i++) {
    for (const warmed of [false, true]) {
        // Use a distinct function body so each transition starts with fresh specializations.
        const remainder = new Function(`return function remainder${i}_${warmed}(a, b) { return a % b; };`)();
        if (warmed) {
            for (let j = 0; j < 1000; j++) {
                assertSame(1, remainder(5, 4));
                assertSame(2, remainder(5, 3));
                assertSame(-1, remainder(-5, 4));
                assertSame(-2, remainder(-5, 3));
            }
        }

        const {a, b, expected} = transitions[i];
        assertSame(expected, remainder(a, b));
        assertSame(1, remainder(5, 4));
        assertSame(2, remainder(5, 3));
        assertSame(-1, remainder(-5, 4));
        assertSame(-2, remainder(-5, 3));
        assertSame(0, remainder(4, 2));
        assertSame(0, remainder(6, 3));
        assertSame(0, remainder(0, -3));
        assertSame(2, remainder(5, -3));
        assertSame(-0, remainder(-4, 2));
        assertSame(-0, remainder(-6, 3));
        assertSame(-0, remainder(-6, -3));
        assertSame(-0, remainder(-0, 4));
        assertSame(-0, remainder(-0, -3));
        assertSame(-0, remainder(-2147483648, -1));
        assertSame(-2, remainder(-2147483648, 3));
        assertSame(-1, remainder(-2147483647, 2));
        assertSame(-1, remainder(-1, -2147483648));
        assertSame(2147483647, remainder(2147483647, -2147483648));
        assertSame(-Number.MIN_VALUE, remainder(-Number.MIN_VALUE, 4));
        assertSame(-5, remainder(-5, Infinity));
        assertSame(NaN, remainder(Infinity, 4));
        assertSame(NaN, remainder(NaN, 3));
        assertSame(NaN, remainder(0, 0));
        assertSame(NaN, remainder(5, -0));
    }
}

function remainderGeneric(a, b) {
    return a % b;
}
assertSame(1, remainderGeneric(5, 4));
assertSame(2, remainderGeneric(5, 3));
assertSame(-2, remainderGeneric(-5, 3));
const log = [];
const left = {valueOf() { log.push("left"); return -6; }};
const right = {valueOf() { log.push("right"); return 3; }};
assertSame(-0, remainderGeneric(left, right));
assertSameContent(["left", "right"], log);
assertSame(1, remainderGeneric(5, 4));
assertSame(-2, remainderGeneric(-5, 3));
assertSame(0, remainderGeneric("0", -3));
assertSame(-0, remainderGeneric("-6", 3));
assertSame(0n, remainderGeneric(-6n, 3n));
assertSame(-2n, remainderGeneric(-5n, 3n));
assertThrows(() => remainderGeneric(1n, 0n), RangeError);
assertThrows(() => remainderGeneric(1n, 0), TypeError);
assertThrows(() => remainderGeneric(1, 0n), TypeError);
assertThrows(() => remainderGeneric(Symbol(), 1), TypeError);

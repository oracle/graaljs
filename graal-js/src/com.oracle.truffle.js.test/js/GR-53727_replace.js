/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const veryLargeIndex = 9.197828594769734e+307;

function Symbol_replace(rx, rv) {
    return rx[Symbol.replace](this, rv);
}
const replaceMethods = [
    String.prototype.replaceAll,
    String.prototype.replace,
    Symbol_replace,
];

function assertReplace(rx, string, replacement, lastIndex, expectedLastIndex, expectedResult) {
    for (const replace of replaceMethods) {
        if (!rx.global && replace.name === "replaceAll") {
            // replaceAll requires a RegExp with global flag
            assertThrows(() => replace.call(string, rx, replacement), TypeError);
            continue;
        }
        rx.lastIndex = lastIndex;
        const result = replace.call(string, rx, replacement);
        try {
            assertSame(expectedResult, result);
            assertSame(expectedLastIndex, rx.lastIndex);
        } catch (e) {
            throw new Error((e.message ?? "") + `
        ${replace.name}(regexp=${rx}, string="${string}", replacement=${replacement}, lastIndex=${lastIndex}):
            result:    expected "${expectedResult}", actual "${result}"
            lastIndex: expected ${expectedLastIndex}, actual ${rx.lastIndex}`);
        }
    }
}

for (const v1 of [/1/guy, /1/gy, /1/gu, /1/g]) {
    for (const lastIndex of [veryLargeIndex, 1, 0]) {
        assertReplace(v1, "1", undefined, lastIndex, 0, "undefined");
        assertReplace(v1, "11", "eleven", lastIndex, 0, "eleveneleven");
    }
}
for (const v1 of [/1/guy, /1/gy]) {
    for (const lastIndex of [veryLargeIndex, 1, 0]) {
        assertReplace(v1, "21", "eleven", lastIndex, 0, "21");
    }
}
for (const v1 of [/1/gu, /1/g]) {
    for (const lastIndex of [veryLargeIndex, 1, 0]) {
        assertReplace(v1, "21", "eleven", lastIndex, 0, "2eleven");
    }
}

for (const v2 of [/(?:)/guy, /(?:)/gy, /(?:)/gu, /(?:)/g]) {
    for (const lastIndex of [veryLargeIndex, 1, 0]) {
        assertReplace(v2, "1", undefined, lastIndex, 0, "undefined1undefined");
        assertReplace(v2, "1", "eleven", lastIndex, 0, "eleven1eleven");
    }
}

for (const v5 of [/1/y, /1/uy]) {
    assertReplace(v5, "1", undefined, veryLargeIndex, 0, "1");
    assertReplace(v5, "1", undefined, 0, 1, "undefined");

    assertReplace(v5, "21", "eleven", veryLargeIndex, 0, "21");
    assertReplace(v5, "21", "eleven", 1, 2, "2eleven");
    assertReplace(v5, "21", "eleven", 2, 0, "21");
    assertReplace(v5, "11", "eleven", 0, 1, "eleven1");
    assertReplace(v5, "11", "eleven", 1, 2, "1eleven");
}

for (const v6 of [/(?:)/y, /(?:)/uy]) {
    assertReplace(v6, "1", undefined, veryLargeIndex, 0, "1");
    assertReplace(v6, "1", undefined, 0, 0, "undefined1");
}

const v7 = /1/;
assertReplace(v7, "1", undefined, veryLargeIndex, veryLargeIndex, "undefined");
assertReplace(v7, "11", undefined, 1, 1, "undefined1");

const v8 = /(?:)/;
assertReplace(v8, "1", undefined, veryLargeIndex, veryLargeIndex, "undefined1");
assertReplace(v8, "11", undefined, 1, 1, "undefined11");

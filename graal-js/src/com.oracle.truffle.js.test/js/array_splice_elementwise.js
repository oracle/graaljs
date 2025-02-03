/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function assertArrayEquals(actual, expected) {
    if (actual.length !== expected.length) {
        throw new Error(`expected length: [${expected.length}] actual length: [${actual.length}]\nexpected array: [${expected}]\nactual array: [${actual}]`);
    }

    for (let i = 0; i < expected.length; i++) {
        let expectedHas = expected.hasOwnProperty(i);
        let actualHas = actual.hasOwnProperty(i);
        if (actualHas !== expectedHas || actual[i] !== expected[i]) {
            throw new Error(`index: ${i} expected value: ${expectedHas ? expected[i] : "<empty>"} actual value: ${actualHas ? actual[i] : "<empty>"}\nexpected array: [${expected}]\nactual array: [${actual}]`);
        }
    }
}

// splice block-wise
var arr = [0, 1, 2, 3, 4, 5, , 7];
var rem = arr.splice(2, 3);
assertArrayEquals(arr, [0, 1, 5, ,7]);
assertArrayEquals(rem, [2, 3, 4]);

// Invalidate no-prototype-elements assumption.
Object.defineProperty(Object.prototype, "2", {value: "^2", configurable: true, writable: true});

// splice element-wise
// itemCount < actualDeleteCount
var arr = [0, 1, 2, 3, 4, 5, , 7];
var rem = arr.splice(2, 3);
assertArrayEquals(arr, [0, 1, 5, , 7]);
assertArrayEquals(rem, [2, 3, 4]);

var arr = [0, 1, 2, 3, 4, 5, , 7];
var rem = arr.splice(2, 3, 8, 9);
assertArrayEquals(arr, [0, 1, 8, 9, 5, , 7]);
assertArrayEquals(rem, [2, 3, 4]);

var arr = [0, 1, , , 4, , , 7, ,];
var rem = arr.splice(0, 3);
assertArrayEquals(arr, [ , 4, , , 7, ,]);
assertArrayEquals(rem, [0, 1, "^2"]);

var arr = [0, 1, , , 4, 5, , 7, , 9];
var rem = arr.splice(8, 2);
assertArrayEquals(arr, [0, 1, , , 4, 5, , 7]);
assertArrayEquals(rem, [ , 9]);

// itemCount > actualDeleteCount
var arr = [0, 1, 2, 3, , 5, , , , 9];
var rem = arr.splice(1, 2, 6, 7, 8);
assertArrayEquals(arr, [0, 6, 7, 8, 3, , 5, , , , 9]);
assertArrayEquals(rem, [1, 2]);

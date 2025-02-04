/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

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


// Proxy object
// itemCount < actualDeleteCount
arr = new Proxy([0, 1, , , 4, , , 7, ,], proxyHandler(
    "get(length)",
    "get(constructor)",
    "has(0)", "get(0)",
    "has(1)", "get(1)",
    "has(2)", "get(2)",
    "has(3)", "deleteProperty(0)",
    "has(4)", "get(4)", "set(1, 4)",
    "has(5)", "deleteProperty(2)",
    "has(6)", "deleteProperty(3)",
    "has(7)", "get(7)", "set(4, 7)",
    "has(8)", "deleteProperty(5)",
    "deleteProperty(8)",
    "deleteProperty(7)",
    "deleteProperty(6)",
    "set(length, 6)",
));
rem = Array.prototype.splice.call(arr, 0, 3);
assertArrayEquals(arr, [ , 4, , , 7, ,]);
assertArrayEquals(rem, [0, 1, "^2"]);

arr = new Proxy([0, 1, , , 4], proxyHandler(
    "get(length)",
    "get(constructor)",
    "has(3)",
    "has(4)", "get(4)", "set(3, 4)",
    "deleteProperty(4)",
    "set(length, 4)",
));
rem = Array.prototype.splice.call(arr, 3, 1);
assertArrayEquals(arr, [0, 1, , 4]);
assertArrayEquals(rem, [,]);

// itemCount > actualDeleteCount
arr = new Proxy([0, 1, 2, 3, , 5, , , , 9], proxyHandler(
    "get(length)",
    "get(constructor)",
    "has(1)", "get(1)",
    "has(2)", "get(2)",
    "has(9)", "get(9)", "set(10, 9)",
    "has(8)", "deleteProperty(9)",
    "has(7)", "deleteProperty(8)",
    "has(6)", "deleteProperty(7)",
    "has(5)", "get(5)", "set(6, 5)",
    "has(4)", "deleteProperty(5)",
    "has(3)", "get(3)",  "set(4, 3)",
    "set(1, 6)",
    "set(2, 7)",
    "set(3, 8)",
    "set(length, 11)",
));
rem = Array.prototype.splice.call(arr, 1, 2, 6, 7, 8);
assertArrayEquals(arr, [0, 6, 7, 8, 3, , 5, , , , 9]);
assertArrayEquals(rem, [1, 2]);


// Array with Proxy prototype
// itemCount < actualDeleteCount
arr = Object.setPrototypeOf([0, 1, , , 4, , , 7, ,], new Proxy({}, proxyHandler(
    "get(constructor)",
    "has(2)",
    "get(2)",
    "has(3)",
    "has(5)",
    "has(6)",
    "has(8)",
)));
rem = Array.prototype.splice.call(arr, 0, 3);
assertArrayEquals(arr, [ , 4, , , 7, ,]);
assertArrayEquals(rem, [0, 1, "^2"]);

// itemCount > actualDeleteCount
arr = Object.setPrototypeOf([0, 1, 2, 3, , 5, , , , 9], new Proxy({}, proxyHandler(
    "get(constructor)",
    "set(10, 9)",
    "has(8)",
    "has(7)",
    "has(6)",
    "set(6, 5)",
    "has(4)",
    "set(4, 3)",
)));
rem = Array.prototype.splice.call(arr, 1, 2, 6, 7, 8);
assertArrayEquals(arr, [0, 6, 7, 8, 3, , 5, , , , 9]);
assertArrayEquals(rem, [1, 2]);


function proxyHandler(...expectedTraps) {
    let it = expectedTraps.values();
    function log(trap) {
        const expected = it.next().value;
        if (expected !== undefined) {
            assertSame(expected, trap);
        } else {
            assertTrue(trap.startsWith("get"));
        }
    }
    return {
        has(target, p) {
            log(`has(${String(p)})`);
            return Reflect.has(target, p);
        },
        get(target, p, receiver) {
            log(`get(${String(p)})`);
            return Reflect.get(target, p, receiver);
        },
        set(target, p, value, receiver) {
            log(`set(${String(p)}, ${value})`);
            return Reflect.set(target, p, value, receiver);
        },
        deleteProperty(target, p) {
            log(`deleteProperty(${String(p)})`);
            return Reflect.deleteProperty(target, p);
        },
    };
}

/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/*
 * Test that getting Error.stackTraceLimit is side-effect-free.
 */

class MyError extends Error {
}

function recurse(n) {
    if (n == 0) {
        throw new MyError("boom");
    } else {
        return recurse(n - 1);
    }
}

function countOccurrences(str, search) {
    var count = 0;
    var pos = 0;
    while ((pos = str.indexOf(search, pos)) >= 0) {
        count++;
        pos++;
    }
    return count;
}

Error.stackTraceLimit = 10;
assertThrows(() => recurse(20), MyError, e => assertSame(10, countOccurrences(e.stack, 'recurse')));

var sideEffect = false;
Object.defineProperty(Error, "stackTraceLimit", {get: function() { sideEffect = true; return 5; }, configurable: true})
assertThrows(() => recurse(20), MyError);
assertSame(false, sideEffect);

Object.defineProperty(Error, "stackTraceLimit", {get: function() { throw "oh no"; }, configurable: true})
assertThrows(() => recurse(20), MyError);

Object.defineProperty(Error, "stackTraceLimit", {get: function() { throw new Error("oh no"); }, configurable: true})
assertThrows(() => recurse(20), MyError);

Object.defineProperty(Error, "stackTraceLimit", {value: 5, configurable: true, writable: true})
assertThrows(() => recurse(20), MyError, e => assertSame(5, countOccurrences(e.stack, 'recurse')));

// stackTraceLimit missing or not of type Number.
var emptyStack = e => {assertSame(0, countOccurrences(String(e.stack), 'recurse')); assertSame(true, 'stack' in e);};

Error.stackTraceLimit = 0;
assertThrows(() => recurse(20), MyError, emptyStack);

Error.stackTraceLimit = -2147483649;
assertThrows(() => recurse(20), MyError, emptyStack);

// stackTraceLimit missing or not of type Number.
var invalidLimit = e => {assertSame(0, countOccurrences(String(e.stack), 'recurse')); assertSame(true, 'stack' in e);};

delete Error.stackTraceLimit;
assertSame(false, 'stackTraceLimit' in Error);
assertThrows(() => recurse(20), MyError, invalidLimit);

Error.prototype.stackTraceLimit = 10;
assertThrows(() => recurse(20), MyError, invalidLimit);

Error.stackTraceLimit = {valueOf() { return 10; }};
assertThrows(() => recurse(20), MyError, invalidLimit);

Error.stackTraceLimit = "10";
assertThrows(() => recurse(20), MyError, invalidLimit);

Error.stackTraceLimit = 10n;
assertThrows(() => recurse(20), MyError, invalidLimit);

function assertThrows(fn, errorType, verifier) {
    try {
        fn();
    } catch (e) {
        if (errorType) {
            if (!(e instanceof errorType)) {
                throw new Error('expected ' + errorType.name + ', actual: ' + (e.name || e));
            }
        }
        if (verifier) {
            verifier(e);
        }
        return;
    }
    throw Error('should have thrown: ' + fn);
}

function assertSame(expected, actual) {
    if (expected !== actual) {
        throw new Error('expected: [' + expected + '], actual: [' + actual +']');
    }
}

/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify correct for-loop variable initialization and evaluation order.
 */

load("assert.js");

assertThrows(() => {
    for (let [a, b, c] = arr, arr = [1, 2, 3]; ; ) break;
}, ReferenceError); // Cannot access 'arr' before initialization

assertThrows(() => {
    for (const [a, b, c] = arr, arr = [1, 2, 3]; ; ) break;
}, ReferenceError); // Cannot access 'arr' before initialization

assertThrows(() => {
    for (var [a, b, c] = arr, arr = [1, 2, 3]; ; ) break;
}, TypeError); // arr is not iterable


assertThrows(() => {
    for (let {p} = obj, obj = {p: 42}; ; ) break;
}, ReferenceError); // Cannot access 'obj' before initialization

assertThrows(() => {
    for (const {p} = obj, obj = {p: 42}; ; ) break;
}, ReferenceError); // Cannot access 'obj' before initialization

assertThrows(() => {
    for (var {p} = obj, obj = {p: 42}; ; ) break;
}, TypeError); // Cannot destructure property 'p' of 'obj' as it is undefined.


assertThrows(() => {
    for (let [a, b = c, c] = [42]; ; ) break;
}, ReferenceError); // Cannot access 'c' before initialization

assertThrows(() => {
    for (const [a, b = c, c] = [42]; ; ) break;
}, ReferenceError); // Cannot access 'c' before initialization


let sideEffects = [];
function s(e) { sideEffects.push(String(e)); return e; }

(function initEvalOrder() {
    let arrayIter = Array.prototype[Symbol.iterator];
    Array.prototype[Symbol.iterator] = function() { s('it'); return arrayIter.call(this); }

    for (let [a, b] = [s('a'), s('b')], arr = [s('0'), s('1'), s('2')], [c] = [s('c')], d = s('d'); ; ) {
        break;
    }
    assertSame('a:b:it:0:1:2:c:it:d', sideEffects.join(':')), sideEffects.length = 0;

    for (const [a, b] = [s('a'), s('b')], arr = [s('0'), s('1'), s('2')], [c] = [s('c')], d = s('d'); ; ) {
        break;
    }
    assertSame('a:b:it:0:1:2:c:it:d', sideEffects.join(':')), sideEffects.length = 0;

    for (var [a, b] = [s('a'), s('b')], arr = [s('0'), s('1'), s('2')], [c] = [s('c')], d = s('d'); ; ) {
        break;
    }
    assertSame('a:b:it:0:1:2:c:it:d', sideEffects.join(':')), sideEffects.length = 0;

    for (var i = s('i') in [s('a'), i, s('b')]) { // legacy syntax
        s(i);
    }
    assertSame('i:a:b:0:1:2', sideEffects.join(':')), sideEffects.length = 0;

    Array.prototype[Symbol.iterator] = arrayIter;
})();

(function uninitializedVar() {
    assertThrows(() => {
        for (let [a, b, c] = [s('a'), d, s('c')], d = s('d'); ; ) break;
    }, ReferenceError);
    assertSame('a', sideEffects.join(':')), sideEffects.length = 0;

    assertThrows(() => {
        for (const [a, b, c] = [s('a'), d, s('c')], d = s('d'); ; ) break;
    }, ReferenceError);
    assertSame('a', sideEffects.join(':')), sideEffects.length = 0;

    assertThrows(() => {
        for (let [a, b] = [s('a')], c = [b || s('b'), d, s('c')], d = [s('d')]; ; ) break;
    }, ReferenceError);
    assertSame('a:b', sideEffects.join(':')), sideEffects.length = 0;

    assertThrows(() => {
        for (let [a, b] = [s('a')], c = [b || s('b'), d, s('c')], d = [s('d')]; ; ) break;
    }, ReferenceError);
    assertSame('a:b', sideEffects.join(':')), sideEffects.length = 0;

    for (var [a, b] = [s('a'), s('b'), s(arr), s(c), s(d)], arr = [s('0'), s('1'), s('2')], [c = s('x'), d = s('d')] = [s('c')], e = s('e'), f = s(f); ; ) break;
    assertSame('a:b:undefined:undefined:undefined:0:1:2:c:d:e:undefined', sideEffects.join(':')), sideEffects.length = 0;
})();

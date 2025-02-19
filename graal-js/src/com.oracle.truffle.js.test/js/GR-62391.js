/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for the ToPrimitive operation.
 */

"use strict";
load("./assert.js");

const hostNull = globalThis.Java.to(Proxy)[1];

function f1() {
    function f2() {
        return hostNull;
    }
    return f2;
}
function f3() {
    return hostNull;
}

function lt(a1, a2) {
    return a1 < a2;
}
function gt(a1, a2) {
    return a1 > a2;
}
function le(a1, a2) {
    return a1 <= a2;
}
function ge(a1, a2) {
    return a1 >= a2;
}
function eq(a1, a2) {
    return a1 == a2;
}
function ne(a1, a2) {
    return a1 != a2;
}

function testBinaryOperators(v0) {
    assertFalse(lt(v0));
    assertFalse(gt(v0));
    assertFalse(le(v0));
    assertFalse(ge(v0));

    assertFalse(lt(undefined, v0));
    assertFalse(gt(undefined, v0));
    assertFalse(le(undefined, v0));
    assertFalse(ge(undefined, v0));

    assertFalse(lt(v0, null));
    assertFalse(gt(v0, null));
    assertTrue(le(v0, null));
    assertTrue(ge(v0, null));

    assertFalse(lt(null, v0));
    assertFalse(gt(null, v0));
    assertTrue(le(null, v0));
    assertTrue(ge(null, v0));

    assertFalse(eq(v0));
    assertTrue(ne(v0));
    assertFalse(eq(undefined, v0));
    assertTrue(ne(undefined, v0));
    assertFalse(eq(v0, null));
    assertTrue(ne(v0, null));
    assertFalse(eq(null, v0));
    assertTrue(ne(null, v0));

    assertSame(0, v0 + v0);
    assertSame(1, v0 + 1);
    assertSame(1, 1 + v0);
    assertSame("null", v0 + "");
    assertSame("null", "" + v0);

    assertSame(0, v0 - v0);
    assertSame(0, v0 * v0);
    assertSame(1, v0 ** v0);
    assertSame(NaN, v0 / v0);
    assertSame(NaN, v0 % v0);

    assertSame(0, v0 << v0);
    assertSame(0, v0 >> v0);
    assertSame(0, v0 >>> v0);
    assertSame(0, v0 | v0);
    assertSame(0, v0 & v0);
    assertSame(0, v0 ^ v0);
}
function testUnaryOperators(v0) {
    assertSame(+0, +v0);
    assertSame(-0, -v0);
    assertSame(~0, ~v0);

    assertSame(0, v0 | 0); // ToInt32
    assertSame(0, v0 >>> 0); // ToUint32
}

const re0 = /\cj\cJ\ci\cI\ck\cK/gsum;
const re1 = /\cj\cJ\ci\cI\ck\cK/gsum;
Object.defineProperty(re0, Symbol.toPrimitive, { configurable: true, enumerable: true, get: f1 });
testBinaryOperators(re0);
testUnaryOperators(re0);

Object.defineProperty(re1, "toString", { configurable: true, enumerable: true, get: f1 });
Object.defineProperty(re1, "valueOf", { configurable: true, enumerable: true, get: f1 });
testBinaryOperators(re1);
testUnaryOperators(re1);

// Invalid Symbol.toPrimitive value: null is not callable.
const re3 = /./;
Object.defineProperty(re3, Symbol.toPrimitive, { configurable: true, enumerable: true, get: f3 });
assertThrows(() => lt(re3), TypeError);
assertThrows(() => gt(re3), TypeError);
assertThrows(() => le(re3), TypeError);
assertThrows(() => ge(re3), TypeError);

// Tests OrdinaryToPrimitive.
assertFalse(lt(Date.prototype[Symbol.toPrimitive].call({ toString() { return hostNull; }}, "string")))
assertFalse(lt(Date.prototype[Symbol.toPrimitive].call({ valueOf () { return hostNull; }}, "string")))
assertFalse(lt(Date.prototype[Symbol.toPrimitive].call({ toString() { return hostNull; }}, "number")))
assertFalse(lt(Date.prototype[Symbol.toPrimitive].call({ valueOf () { return hostNull; }}, "number")))
// Cannot convert object to primitive (because neither toString, nor valueOf are callable).
assertThrows(() => Date.prototype[Symbol.toPrimitive].call({ toString: hostNull, valueOf: hostNull }, "string"), TypeError);
assertThrows(() => Date.prototype[Symbol.toPrimitive].call({ toString: hostNull, valueOf: hostNull }, "number"), TypeError);
// Fails IsObject check of this value in Date.prototype[Symbol.toPrimitive]().
assertThrows(() => Date.prototype[Symbol.toPrimitive].call(hostNull, "string"), TypeError);

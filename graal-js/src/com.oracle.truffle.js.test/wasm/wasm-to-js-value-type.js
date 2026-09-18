/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option webassembly
 */

load('../js/assert.js');

// (module
//   (func (export "identity") (param externref) (result externref)
//     local.get 0))
const identityBytes = new Uint8Array([
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
    0x01, 0x06, 0x01, 0x60, 0x01, 0x6f, 0x01, 0x6f,
    0x03, 0x02, 0x01, 0x00,
    0x07, 0x0c, 0x01, 0x08, 0x69, 0x64, 0x65, 0x6e, 0x74, 0x69, 0x74, 0x79,
    0x00, 0x00,
    0x0a, 0x06, 0x01, 0x04, 0x00, 0x20, 0x00, 0x0b,
]);
const identity = new WebAssembly.Instance(new WebAssembly.Module(identityBytes)).exports.identity;

// (module
//   (func (export "value") (result i64)
//     i64.const 42))
const i64Bytes = new Uint8Array([
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
    0x01, 0x05, 0x01, 0x60, 0x00, 0x01, 0x7e,
    0x03, 0x02, 0x01, 0x00,
    0x07, 0x09, 0x01, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x00, 0x00,
    0x0a, 0x06, 0x01, 0x04, 0x00, 0x42, 0x2a, 0x0b,
]);
const i64Value = new WebAssembly.Instance(new WebAssembly.Module(i64Bytes)).exports.value;

// (module
//   (import "" "thrower" (func $thrower))
//   (func (export "through")
//     call $thrower))
let thrownValue;
const throwBytes = new Uint8Array([
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
    0x01, 0x04, 0x01, 0x60, 0x00, 0x00,
    0x02, 0x0c, 0x01, 0x00, 0x07, 0x74, 0x68, 0x72, 0x6f, 0x77, 0x65, 0x72,
    0x00, 0x00,
    0x03, 0x02, 0x01, 0x00,
    0x07, 0x0b, 0x01, 0x07, 0x74, 0x68, 0x72, 0x6f, 0x75, 0x67, 0x68, 0x00,
    0x01,
    0x0a, 0x06, 0x01, 0x04, 0x00, 0x10, 0x00, 0x0b,
]);
const through = new WebAssembly.Instance(new WebAssembly.Module(throwBytes), {
    "": {thrower: () => { throw thrownValue; }},
}).exports.through;

function exceptionRoundTrip(value) {
    thrownValue = value;
    try {
        through();
    } catch (caught) {
        return caught;
    }
    fail("thrower returned normally");
}

function assertSameNumber(expected, actual) {
    assertSame("number", typeof actual);
    assertSame(expected, actual);
}

for (const value of [
    java.lang.Long.valueOf("0"),
    java.lang.Long.valueOf("42"),
    java.lang.Long.valueOf("-1073741824"),
    java.lang.Long.valueOf("1073741823"),
    java.lang.Long.valueOf("-1073741825"),
    java.lang.Long.valueOf("1073741824"),
    java.lang.Long.valueOf("-2147483649"),
    java.lang.Long.valueOf("2147483648"),
    java.lang.Long.valueOf("-9007199254740991"),
    java.lang.Long.valueOf("9007199254740991"),
    java.lang.Long.MIN_VALUE,
    java.lang.Long.MAX_VALUE,
]) {
    assertSameNumber(value, identity(value));
    assertSameNumber(value, exceptionRoundTrip(value));
}

for (const value of [0, -0, 42, 2 ** 40, 1.5, NaN, Infinity, -Infinity]) {
    assertSameNumber(value, identity(value));
    assertSameNumber(value, exceptionRoundTrip(value));
}

assertSame(42n, identity(42n));
assertSame(42n, exceptionRoundTrip(42n));
assertSame(42n, i64Value());
assertSame(i64Value, identity(i64Value));
assertSame(i64Value, exceptionRoundTrip(i64Value));

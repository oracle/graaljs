/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Test java.math.BigInteger interop.
 */

load('../assert.js');

const BigInteger = java.math.BigInteger;

let zero = BigInteger.ZERO;
let small= BigInteger.valueOf(42);
let big  = new BigInteger("100000000000000000000"); // 10n ** 20n
let huge = BigInteger.valueOf(2).pow(1000);
let bigIntegers = [zero, small, big, huge];

function nonStrictThis() {
    return this;
}

for (let b of bigIntegers) {
    // typeof value is 'object' for java.math.BigInteger
    assertSame('object', typeof b);
    assertTrue(typeof b == 'object');
    assertSame('object', typeof Object(b));
    assertTrue(typeof Object(b) == 'object');
    assertSame('object', typeof nonStrictThis.call(b));
    assertTrue(typeof nonStrictThis.call(b) == 'object');

    assertTrue(b == b.multiply(BigInteger.ONE));
    // ToObject should keep foreign members
    assertTrue(b == Object(b).multiply(BigInteger.ONE));
    assertTrue(b == nonStrictThis.call(b).multiply(BigInteger.ONE));

    assertTrue(b == Object(b));
    assertTrue(Object(b) == b);
    assertTrue(b == BigInt(b));
    assertTrue(BigInt(b) == b);

    // ToObject does not wrap BigInteger.
    assertTrue(b === Object(b));

    // Number (double) coercion
    assertSame(Number(b), Number.prototype.valueOf.call(b));
    assertSame(String(+b), Number.prototype.toString.call(b));

    // Avoid double coercion in ToString.
    assertSame(b.toString(), String(b));
}

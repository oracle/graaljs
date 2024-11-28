/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

for (const snippet of [
    "9e",
    "9.e",
    "9e+",
    "9.e+",
    "9e-",
    "9.e-",
    "9e_",
    "9.e_",
    "9e+_",
    "9.e+_",
    "9e-_",
    "9.e-_",
    "9e_0",
    "9.e_0",
    "9e+_0",
    "9.e+_0",
    "9e-_0",
    "9.e-_0",
].flatMap(x => [x, x.toUpperCase()])) {
    assertThrows(() => new Function(snippet), SyntaxError, "Missing exponent");
    assertSame(9, parseFloat(snippet));
}

for (const snippet of [
    "0x",
    "0o",
    "0b",
    "0x_",
    "0o_",
    "0b_",
    "0xg",
    "0o8",
    "0b2",
    "0x_0",
    "0o_0",
    "0b_0",
    "0x-1",
    "0o-1",
    "0b-1",
    "0x+1",
    "0o+1",
    "0b+1",
].flatMap(x => [x, x.toUpperCase()])) {
    assertThrows(() => new Function(snippet), SyntaxError, "Missing digit");
    assertThrows(() => new Function(snippet + "n"), SyntaxError, "Missing digit");
    assertThrows(() => BigInt(snippet), SyntaxError);
}

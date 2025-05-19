/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js")

// Original test-case from the fuzzer
function f0() {
    return f0;
}
const v1 = [64,-16952,591226145];
v1[5] = f0;
v1[3] = v1;
const v4 = this.Java.to(v1);
const v5 = v4.flat();
function f6(a7, a8) {
    return v4;
}
function f9(a10) {
    return v4;
}
f6[Symbol.species] = f9;
v5.constructor = f6;
assertThrows(() => v5.flat(), TypeError);

// Simplified test-case
Object.defineProperty(Array, Symbol.species, { value: Java.type('java.util.ArrayList') });
assertSameContent([42, 211, -1], [[42, 211], -1].flat());

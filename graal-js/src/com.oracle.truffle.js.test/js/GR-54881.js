/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Checks that %TypedArray%.prototype.map uses Set (not CreateDataPropertyOrThrow)

load("assert.js");

var buffer = new ArrayBuffer(8);
var array = new Uint8Array(buffer);
var ctor = function() { return array; };
ctor[Symbol.species] = ctor;
array.constructor = ctor;

var calls = 0;
array.map(function(_,i) {
    calls++;
    if (i === 1) buffer.transfer();
});

assertSame(8, calls);

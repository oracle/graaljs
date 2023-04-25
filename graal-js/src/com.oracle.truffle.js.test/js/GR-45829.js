/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests foreign mapFn in (Typed)Array.from(items, mapFn);
 */

load("assert.js");

var array = [2,4,6];
var arrayLike = { 0:2, 1:4, 2:6, length:3 };
// mapFn takes value and index => we have to use Java method with two parameters
var mapFn = java.lang.Math.addExact;
var expected = [2,5,8];

assertSameContent(expected, Array.from(array, mapFn));
assertSameContent(expected, Array.from(arrayLike, mapFn));
assertSameContent(expected, Uint8Array.from(array, mapFn));
assertSameContent(expected, Uint8Array.from(arrayLike, mapFn));

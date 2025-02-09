/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

[[], new Array(100)].forEach(function (array) {
    assertFalse(Object.isSealed(array));
    assertFalse(Object.isFrozen(array));

    Object.preventExtensions(array);

    assertTrue(Object.isSealed(array));
    assertFalse(Object.isFrozen(array)); // length is writable still

    Object.defineProperty(array, 'length', { writable: false });

    assertTrue(Object.isSealed(array));
    assertTrue(Object.isFrozen(array));
});

var array = [,42];
Object.preventExtensions(array); 
assertFalse(Object.isSealed(array)); // has array[1]
assertFalse(Object.isFrozen(array));
delete array[1];
assertTrue(Object.isSealed(array));
assertFalse(Object.isFrozen(array)); // length is writable still
Object.defineProperty(array, 'length', { length: 2, writable: false });
assertTrue(Object.isSealed(array));
assertTrue(Object.isFrozen(array));

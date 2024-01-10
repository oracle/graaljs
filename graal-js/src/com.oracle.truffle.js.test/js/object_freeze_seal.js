/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

// Test Object.seal/freeze of object in dictionary mode.

var o1 = {};
for (var i = 0; i < 2000; i++) o1["a" + i] = i;

Object.seal(o1);
assertFalse(Object.isExtensible(o1));
assertTrue(Object.isSealed(o1));
assertFalse(Object.isFrozen(o1));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(o1, "a1337")), `{"value":1337,"writable":true,"enumerable":true,"configurable":false}`);

Object.freeze(o1);
assertFalse(Object.isExtensible(o1));
assertTrue(Object.isSealed(o1));
assertTrue(Object.isFrozen(o1));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(o1, "a1337")), `{"value":1337,"writable":false,"enumerable":true,"configurable":false}`);

var o2 = {};
for (var i = 0; i < 2000; i++) o2[i] = i;

Object.seal(o2);
assertFalse(Object.isExtensible(o2));
assertTrue(Object.isSealed(o2));
assertFalse(Object.isFrozen(o2));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(o2, "1337")), `{"value":1337,"writable":true,"enumerable":true,"configurable":false}`);

Object.freeze(o2);
assertFalse(Object.isExtensible(o2));
assertTrue(Object.isSealed(o2));
assertTrue(Object.isFrozen(o2));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(o2, "1337")), `{"value":1337,"writable":false,"enumerable":true,"configurable":false}`);

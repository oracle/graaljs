/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

// Test Object.seal/freeze of typed array.

var ta = new Int8Array(0);

assertTrue(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));

Object.seal(ta);
assertFalse(Object.isExtensible(ta));
assertTrue(Object.isSealed(ta));
assertTrue(Object.isFrozen(ta));

Object.freeze(ta);
assertFalse(Object.isExtensible(ta));
assertTrue(Object.isSealed(ta));
assertTrue(Object.isFrozen(ta));

var ta = new Int8Array(0);
ta.x = 42;

assertTrue(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));

Object.seal(ta);
assertFalse(Object.isExtensible(ta));
assertTrue(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "x")), `{"value":42,"writable":true,"enumerable":true,"configurable":false}`);

Object.freeze(ta);
assertFalse(Object.isExtensible(ta));
assertTrue(Object.isSealed(ta));
assertTrue(Object.isFrozen(ta));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "x")), `{"value":42,"writable":false,"enumerable":true,"configurable":false}`);

var ta = new Int8Array(1);

assertTrue(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));

try { Object.seal(ta); } catch {}
assertFalse(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "0")), `{"value":0,"writable":true,"enumerable":true,"configurable":true}`);

try { Object.freeze(ta); } catch {}
assertFalse(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "0")), `{"value":0,"writable":true,"enumerable":true,"configurable":true}`);

var ta = new Int8Array(1);
ta.x = 42;

assertTrue(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));

try { Object.seal(ta); } catch {}
assertFalse(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "0")), `{"value":0,"writable":true,"enumerable":true,"configurable":true}`);
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "x")), `{"value":42,"writable":true,"enumerable":true,"configurable":true}`);

try { Object.freeze(ta); } catch {}
assertFalse(Object.isExtensible(ta));
assertFalse(Object.isSealed(ta));
assertFalse(Object.isFrozen(ta));
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "0")), `{"value":0,"writable":true,"enumerable":true,"configurable":true}`);
assertEqual(JSON.stringify(Object.getOwnPropertyDescriptor(ta, "x")), `{"value":42,"writable":true,"enumerable":true,"configurable":true}`);

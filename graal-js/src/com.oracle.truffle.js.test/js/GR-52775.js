/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of Array.prototype.sort applied to slow arrays.
 */

load("assert.js");

var array;

// int version
array = [0];
Object.defineProperty(array, '1', { value: 'foo' });
assertThrows(() => array.sort(), TypeError); // non-writable property

array = [2];
Object.defineProperty(array, '1', { writable: true, value: '1foo' });
assertSameContent(['1foo', 2], array.sort());

// double version
array = [Math.PI];
Object.defineProperty(array, '1', { value: 'foo' });
assertThrows(() => array.sort(), TypeError); // non-writable property

array = [Math.PI];
Object.defineProperty(array, '1', { writable: true, value: '1foo' });
assertSameContent(['1foo', Math.PI], array.sort());

// invalidate arrayPrototypeNoElementsAssumption
Object.defineProperty(Array.prototype, '1', { writable: true, value: 'foo' });

array = [2,3,4];
delete array[1];
assertSameContent([2, 4, 'foo'], array.sort());

array = [2.3,3.4,4.5];
delete array[1];
assertSameContent([2.3, 4.5, 'foo'], array.sort());

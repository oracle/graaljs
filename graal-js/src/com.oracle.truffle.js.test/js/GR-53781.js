/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

var array = [1];
array[2] = 42;
array.splice(0,1);

// safeToString(array) should not result in an internal error
assertThrows(() => [].findLastIndex(array), TypeError);

// forEach should not be invoked for array index 1
array.forEach(x => assertSame(42, x));

// original test-case from the fuzzing:

const v3 = () => {};
const v4 = [1];
v4[2] = 0;
v4.splice(v3, 1);
const v7 = [];
assertThrows(() => v7.findLastIndex(v4), TypeError);

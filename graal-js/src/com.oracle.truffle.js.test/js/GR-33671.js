/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

// Used to throw ArrayIndexOutOfBoundsException
var array = new Array(3);
for (var i = 0; i < 3; i++) {
  array.shift();
  array.push(i);
}

assertSame(3, array.length);
assertSame(0, array[0]);
assertSame(1, array[1]);
assertSame(2, array[2]);

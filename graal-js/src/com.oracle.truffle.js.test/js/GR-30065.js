/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

var length = 2147483647;
var array = new Array(length);

array[length - 1] = 42;
assertSame(42, array[length - 1]);
assertSame(length, array.length);

// Increase the array length over Integer.MAX_VALUE
array[length] = 211;
assertSame(211, array[length]);
assertSame(length + 1, array.length);

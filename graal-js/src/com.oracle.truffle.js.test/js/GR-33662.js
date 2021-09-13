/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

// DefaultObjectArrayWithEmptyLiteralNode
var array = [,Object];
assertSame(undefined, array.shift());
assertSame(1, array.length);
assertSame(Object, array[0]);

array = [Object,,Object,,];
assertSame(Object, array.shift());
assertSame(3, array.length);
assertSame(undefined, array[0]);
assertSame(Object, array[1]);
assertSame(undefined, array[2]);

// DefaultArrayLiteralWithSpreadNode
array = [,Object,...[]];
assertSame(undefined, array.shift());
assertSame(1, array.length);
assertSame(Object, array[0]);

array = [Object,,Object,,...[]];
assertSame(Object, array.shift());
assertSame(3, array.length);
assertSame(undefined, array[0]);
assertSame(Object, array[1]);
assertSame(undefined, array[2]);

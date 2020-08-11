/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests the correct processing of array literal that combines SpreadElement and Elision
// as reported at https://github.com/graalvm/graaljs/issues/327

load('assert.js');

var array = [211, , ...[42]];
assertSame(3, array.length);
assertSame(211, array[0]);
assertSame(undefined, array[1]);
assertSame(42, array[2]);

var array = [...[211], , 42];
assertSame(3, array.length);
assertSame(211, array[0]);
assertSame(undefined, array[1]);
assertSame(42, array[2]);

true;

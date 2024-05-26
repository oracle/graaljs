/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var array;

array = Array(1000000);
array[6] = 0;
array.splice(8, 0, 0);

array = Array(8);
array[6] = 0;
array.splice(8, 0, 0);
assertSameContent([,,,,,,0,,0], array);

array = Array(8);
array[6] = 0;
array.splice(8, 0, 0, 1);
assertSameContent([,,,,,,0,,0,1], array);

array = Array(1000000);
array[6] = 0;
array.splice(15, 0, 0, 1);

array = Array();
array[7] = 0;
array.splice(8, 0, 0);
assertSameContent([,,,,,,,0,0], array);

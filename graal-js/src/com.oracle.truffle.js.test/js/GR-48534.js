/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var array = [1,2,4];
delete array[1];
array.splice(2,0,3);
array[2] = '3';
assertSameContent([1,,'3',4], array);

var array = new Array(10);
array[0] = 0;
array[2] = 2;
array.splice(1,0,1);
array[1] = '1';
assertSameContent([0,'1',,2,,,,,,,,], array);

/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var array = new Array(8);
array[0] = '1';
array[2] = 3;
array.splice(3,0,4,5);
assertSameContent(['1',,3,4,5,,,,,,], array);

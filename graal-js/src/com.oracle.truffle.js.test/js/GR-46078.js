/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of a write of a "hole value" into a hole.
 */

load("assert.js");

var array = new Array();
array.push(1,2,3);
delete array[1];
array[1] = java.lang.Integer.MIN_VALUE;

assertSameContent([1, java.lang.Integer.MIN_VALUE, 3], array);

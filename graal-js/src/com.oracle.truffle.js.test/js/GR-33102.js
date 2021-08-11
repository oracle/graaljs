/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for an incorrect conversion to zero-based array.
 */

load('assert.js');

var array;

array = [];
array.unshift(0, 4);
array.shift();
array.unshift(2, 3);
array.unshift(1);
assertSame('1,2,3,4', array.toString());

array = [];
array.unshift(0.5, 4.5);
array.shift();
array.unshift(2.5, 3.5);
array.unshift(1.5);
assertSame('1.5,2.5,3.5,4.5', array.toString());

array = [];
array.unshift('0', '4');
array.shift();
array.unshift('2', '3');
array.unshift('1');
assertSame('1,2,3,4', array.toString());

var array = [];
array.unshift(Object(0), Object(4));
array.shift();
array.unshift(Object(2), Object(3));
array.unshift(Object(1));
array.toString();
assertSame('1,2,3,4', array.toString());

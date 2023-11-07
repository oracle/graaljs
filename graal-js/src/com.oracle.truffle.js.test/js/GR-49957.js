/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var minValue = java.lang.Long.MIN_VALUE;
var maxValue = java.lang.Long.MAX_VALUE;
var someValue = java.lang.Long.parseLong("12345678901");

assertTrue(Number.isFinite(minValue));
assertTrue(Number.isFinite(maxValue));
assertTrue(Number.isFinite(someValue));

assertTrue(Number.isInteger(minValue));
assertTrue(Number.isInteger(maxValue));
assertTrue(Number.isInteger(someValue));

assertFalse(Number.isSafeInteger(minValue));
assertFalse(Number.isSafeInteger(maxValue));
assertTrue(Number.isSafeInteger(someValue));

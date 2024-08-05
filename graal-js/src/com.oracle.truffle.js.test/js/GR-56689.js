/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the grow of an array length over Integer.MAX_VALUE (during Array.prototype.splice).
 */

load("assert.js");

var array = Array(2147483647);
assertSameContent([], array.splice(0, 0, 'newElement'));
assertSame('newElement', array[0]);
assertSame(2147483648, array.length);
assertFalse(1 in array);
assertFalse(2147483647 in array);

// Original test-case from the fuzzer

Array(2147483647)["splice"](255, 0, "splice");
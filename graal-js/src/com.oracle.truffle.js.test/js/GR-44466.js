/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Checks that the line number in the stack trace of RangeError: Invalid array length
 * is correct, see https://github.com/oracle/graaljs/issues/705
 */

load("assert.js");

// Large length
try {
    var array = [];
    array.length = 88 ** 99;
    fail('should have thrown');
} catch (e) {
    assertTrue(e.stack.includes('GR-44466.js:18'));
}

// Negative length
try {
    var array = [];
    array.length = -42;
    fail('should have thrown');
} catch (e) {
    assertTrue(e.stack.includes('GR-44466.js:27'));
}

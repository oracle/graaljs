/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Sanity check of stack-trace-api option.
 * 
 * @option stack-trace-api=false
 */

load("assert.js");

function createError(n) {
    return n ? createError(n-1) : new Error();
}

// Error.captureStackTrace should not be defined
assertSame(undefined, Error.captureStackTrace);

// Error.stackTraceLimit should have no impact on the stack property
var expected = createError(10).stack;

for (var i = 0; i < 5; i++) {
    Error.stackTraceLimit = i;
    assertSame(expected, createError(10).stack);
}

// Error.prepareStackTrace should not be invoked
var invoked = false;
Error.prepareStackTrace = function() {
    invoked = true;
};

new Error().stack;
assertFalse(invoked);

// Error.prepareStackTrace should not be read
var read = false;
Object.defineProperty(Error, 'prepareStackTrace', {
    get() {
        read = true;
    }
});

new Error().stack;
assertFalse(read);

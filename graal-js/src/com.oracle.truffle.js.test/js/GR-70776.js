/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression tests of the number of arguments passed to the constructor
 * of the result of typedArray.subarray().
 */

load("./assert.js");

function test(array, subarrayArgs, expected) {
    var actual;
    array.constructor = {};
    array.constructor[Symbol.species] = function() {
        actual = arguments.length;
        return array;
    };
    array.subarray(...subarrayArgs);
    assertSame(expected, actual);
}

test(new Uint8Array(new ArrayBuffer(8)), [1], 3);
test(new Uint8Array(new ArrayBuffer(8)), [1,4], 3);
test(new Uint8Array(new ArrayBuffer(8, { maxByteLength: 16 })), [1], 2);
test(new Uint8Array(new ArrayBuffer(8, { maxByteLength: 16 })), [1,4], 3);

/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

var array;
var key = {
    [Symbol.toPrimitive]() {
        array[0] = 'foo';
        return 0;
    }
};

array = [0];
assertSame('foo', array[key]);

array = [0];
array[key] = 42;
assertSame(42, array[0]);

// original test-case from the fuzzing:

const v0 = [0];
function f1() {
    v0.length = 1;
    return v0.length;
}
v0[Symbol.toPrimitive] = f1;
function f2(a8) {
    a8[v0];
}
f2(v0);

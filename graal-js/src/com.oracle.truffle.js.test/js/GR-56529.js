/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

let array = Object.freeze(new Array(2147483648));
assertThrows(() => {
    array.pop();
}, TypeError);

// Original test-case from the fuzzer

const v1 = [-60891989,1,260564070,4294967296,10030];
v1[2147483647] %= 1607265652;
assertThrows(() => {
    Object.freeze(v1).pop();
}, TypeError);

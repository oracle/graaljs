/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests Math.floor and Math.ceil corner cases.
 */

load('assert.js');

let double;
let called;
let fakeDouble = {
  valueOf() {
    called++;
    return double;
  }
};

double = 2**31 + 0.5;
called = 0;
assertSame(2**31, Math.floor(fakeDouble));
assertSame(1, called);

double = -(2**31) - 0.5;
called = 0;
assertSame(-(2**31), Math.ceil(fakeDouble));
assertSame(1, called);

true;

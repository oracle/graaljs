/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for GR-52569, setting an element at a negative index.
 */

load("assert.js");

var v0 = [0];
Object.defineProperty(v0, 1, {});
v0[-1] = 1;
assertSame(1, v0[-1]);

var v0 = [0];
v0[-1] = 1;
Object.defineProperty(v0, 1, {});
v0[-1] = 2;
v0[-2] = 3;
assertSame(2, v0[-1]);
assertSame(3, v0[-2]);

var v0 = [0];
Object.defineProperty(v0, 1, {});
assertSame(undefined, v0[-1]);


var v0 = [0];
v0[-1] = 1;
assertSame(1, v0[-1]);

var v0 = [0];
v0[-1] = 2;
v0[-2] = 3;
assertSame(2, v0[-1]);
assertSame(3, v0[-2]);

var v0 = [0];
assertSame(undefined, v0[-1]);

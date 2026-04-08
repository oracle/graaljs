/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var array = new Int32Array(new SharedArrayBuffer(8));
Atomics.waitAsync(array, 0, 0);
Atomics.waitAsync(array, 0, 0);
assertSame(2, Atomics.notify(array, 0, 4294967297));

Atomics.waitAsync(array, 1, 0);
Atomics.waitAsync(array, 1, 0);
assertSame(2, Atomics.notify(array, 1, Infinity));

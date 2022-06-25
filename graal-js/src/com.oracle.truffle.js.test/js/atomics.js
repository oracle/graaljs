/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @options shared-array-buffer=true
 * @options atomics=true
 */

load("assert.js");

let array = new Int16Array(new SharedArrayBuffer(32));
array[0] = 0x0102;
assertSame(0x0102, Atomics.compareExchange(array, 0, 42, 43));
assertSame(0x0102, array[0]);
assertSame(0, Atomics.compareExchange(array, 15, 0, 42));
assertSame(42, array[15]);

array = new Uint8Array(new SharedArrayBuffer(32));
array[0] = 255;
assertSame(255, Atomics.compareExchange(array, 0, 42, 43));
assertSame(255, array[0]);
assertSame(0, Atomics.compareExchange(array, 29, 0, 42));
assertSame(42, array[29]);
assertSame(0, Atomics.compareExchange(array, 30, 0, 43));
assertSame(43, array[30]);
assertSame(0, Atomics.compareExchange(array, 31, 0, 44));
assertSame(44, array[31]);

assertSame(0, Atomics.notify(new Int32Array(new SharedArrayBuffer(4)), 0, 0));

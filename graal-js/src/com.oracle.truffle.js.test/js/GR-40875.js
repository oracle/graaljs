/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests late time detach in TypedArray.prototype.set.
 * Motivated by https://github.com/v8/v8/blob/main/test/mjsunit/es6/typedarray.js
 * 
 * @option debug-builtin
 */

// checking just that no (internal) error is thrown
var array = new Uint8Array(8);
var evil = [{ valueOf() { Debug.typedArrayDetachBuffer(array.buffer); return 42; } }];
array.set(evil);

var array = new Float32Array(8);
array.set(evil);

var array = new BigInt64Array(8);
var evilBigInt = [{ valueOf() { Debug.typedArrayDetachBuffer(array.buffer); return 42n; } }];
array.set(evilBigInt);

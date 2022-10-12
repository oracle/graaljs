/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify where Symbol.species property is defined on typed arrays.
 */

load("assert.js");

[
  BigInt64Array,
  BigUint64Array,
  Float64Array,
  Float32Array,
  Int32Array,
  Int16Array,
  Int8Array,
  Uint32Array,
  Uint16Array,
  Uint8Array,
  Uint8ClampedArray
].forEach(function(constructor) {
    assertFalse(Object.hasOwn(constructor, Symbol.species));
    assertTrue(Object.hasOwn(Object.getPrototypeOf(constructor), Symbol.species));
});

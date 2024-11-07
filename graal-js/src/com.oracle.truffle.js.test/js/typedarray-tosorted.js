/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

/**
 * Tests for toSorted() and sort() of typed arrays.
 * 
 * @option ecmascript-version=staging
 */

// GR-53898
assertSameContent([1, 4294967295], new Uint32Array([1, -1]).toSorted());

const ui32 = new Uint32Array([
  1,
  -1,
  -2147483648,
  2147483649,
  2147483647,
]);
const i32 = new Int32Array(ui32);
const expected_ui32 = [
  1,
  2147483647,
  2147483648,
  2147483649,
  4294967295,
];
const expected_i32 = [
  -2147483648,
  -2147483647,
  -1,
  1,
  2147483647,
];

const ui64 = new BigUint64Array([
  1n,
  -1n,
  -9223372036854775808n,
  9223372036854775809n,
  9223372036854775807n,
]);
const i64 = new BigInt64Array(ui64);
const expected_ui64 = [
  1n,
  9223372036854775807n,
  9223372036854775808n,
  9223372036854775809n,
  18446744073709551615n,
];
const expected_i64 = [
  -9223372036854775808n,
  -9223372036854775807n,
  -1n,
  1n,
  9223372036854775807n,
];

const reverseOrder = (a, b) => b > a ? 1 : b < a ? -1 : 0;
for (const [expected, typedArray] of [
      [expected_ui32, ui32],
      [expected_i32,   i32],
      [expected_ui64, ui64],
      [expected_i64,   i64],
    ]) {
  assertSameContent(expected, typedArray.toSorted());
  assertSameContent(expected.toReversed(), typedArray.toSorted(reverseOrder));
  assertSameContent(expected, typedArray.sort());
  assertSameContent(expected.toReversed(), typedArray.sort(reverseOrder));
}

const floats = [0.17, 0.46, 0.23, 0.28, 0.97, 0.49, 0.76, 0.35, -0.42, 0.42];
const f64a = new Float64Array(floats);
assertSameContent(floats.toSorted(), f64a.toSorted());
assertSameContent(floats.toSorted(reverseOrder), f64a.toSorted(reverseOrder));
assertSameContent(floats.toSorted(), f64a.sort());
assertSameContent(floats.toSorted(reverseOrder), f64a.sort(reverseOrder));
const f32a = new Float32Array(floats);
assertSameContent(floats.map(Math.fround).toSorted(), f32a.toSorted());
assertSameContent(floats.map(Math.fround).toSorted(), f32a.sort());
//const f16a = new Float16Array(floats);
//assertSameContent(floats.map(Math.f16round).toSorted(), f16a.toSorted());
//assertSameContent(floats.map(Math.f16round).toSorted(), f16a.sort());

// toSorted() on Interop-backed typed array.
const interopTypedArray = new Uint32Array(java.nio.ByteBuffer.allocateDirect(40));
for (let i = 0; i < 10; i++) {
  interopTypedArray[i] = (7 ^ (i * 3 ^ 13)) % 37;
}
assertSameContent([10,9,12,3,6,5,24,31,18,17], interopTypedArray);
assertSameContent([3,5,6,9,10,12,17,18,24,31], interopTypedArray.toSorted());
assertSameContent([31,24,18,17,12,10,9,6,5,3], interopTypedArray.toSorted(reverseOrder));
assertSameContent([3,5,6,9,10,12,17,18,24,31], interopTypedArray.sort());
assertSameContent([31,24,18,17,12,10,9,6,5,3], interopTypedArray.sort(reverseOrder));

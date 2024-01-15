/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Basic Float16-related tests.
 * 
 * @option ecmascript-version=staging
 */

load("assert.js");

const pairs = [
    [NaN, NaN],
    [0, 0],
    [-0, -0],
    [Infinity, Infinity],
    [-Infinity, -Infinity],
    [42.84, 42.84375],
    [0.123, 0.12298583984375],
    [-0.123, -0.12298583984375],
    [1.337, 1.3369140625],
    [65504, 65504],
    [65505, 65504]
];

pairs.forEach(([value, convertedValue]) => assertSame(convertedValue, Math.f16round(value)));

const dataView = new DataView(new ArrayBuffer(4));
pairs.forEach(([value, convertedValue]) => {
    dataView.setFloat16(1, value);
    assertSame(convertedValue, dataView.getFloat16(1));
});

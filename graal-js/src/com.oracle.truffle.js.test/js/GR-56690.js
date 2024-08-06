/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the comparisons involving huge BigInts.
 */

load("assert.js");

const hugePositive = 2n**2000000000n;
const hugeNegative = -hugePositive;
const number = 42.211;

assertFalse(hugePositive < number);
assertTrue(number < hugePositive);
assertTrue(hugeNegative < number);
assertFalse(number < hugeNegative);

assertTrue(hugePositive > number);
assertFalse(number > hugePositive);
assertFalse(hugeNegative > number);
assertTrue(number > hugeNegative);

assertFalse(hugePositive <= number);
assertTrue(number <= hugePositive);
assertTrue(hugeNegative <= number);
assertFalse(number <= hugeNegative);

assertTrue(hugePositive >= number);
assertFalse(number >= hugePositive);
assertFalse(hugeNegative >= number);
assertTrue(number >= hugeNegative);

// Original test-case from the fuzzer

let v1 = 5n;
const o6 = {
    p(a3) {
        return v1 < 1.9255255399849514;
    },
};
v1 >>= -1825025338n;
o6["p"]();

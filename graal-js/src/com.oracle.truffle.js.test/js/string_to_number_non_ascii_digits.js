/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

// With literal operands, | 0 and >>> 0 convert the strings during AST construction,
// calling JSRuntime.stringToNumber. Using variables or Number() calls
// JSStringToNumberNode instead, which already rejects these Unicode digits.
assertSame(0, "1\u0661" | 0);
assertSame(0, "1\uff11" | 0);
assertSame(0, "1\u{1d7d9}" | 0);
assertSame(0, "1\u06612" | 0);
assertSame(0, "+\uff11" | 0);
assertSame(0, "-\uff11" | 0);

assertSame(0, "1\u0661" >>> 0);
assertSame(0, "1\uff11" >>> 0);
assertSame(0, "1\u{1d7d9}" >>> 0);
assertSame(0, "1\u06612" >>> 0);
assertSame(0, "+\uff11" >>> 0);
assertSame(0, "-\uff11" >>> 0);

assertSame(0, "0x1\u0661" | 0);
assertSame(0, "0X1\uff11" >>> 0);
assertSame(0, "0x1\u{1d7d9}" | 0);
assertSame(0, "0x1\u06612" >>> 0);
assertSame(0, "0x\uff21" | 0);

// Strings.parseDouble already rejects Unicode digits in fractions and exponents.
assertSame(0, "1.\u06611" | 0);
assertSame(0, "1\u0661.0" >>> 0);
assertSame(0, "1e\u06611" | 0);
assertSame(0, "1\u{1d7d9}.0" >>> 0);

assertSame(12, "12" | 0);
assertSame(12, "+12" | 0);
assertSame(-12, "-12" | 0);
assertSame(4294967284, "-12" >>> 0);
assertSame(2147483647, "2147483647" | 0);
assertSame(4294967295, "4294967295" >>> 0);
assertSame(255, "0xff" >>> 0);
assertSame(255, "0XFF" | 0);

// Trimming Unicode whitespace must still leave valid ASCII digits accepted.
assertSame(12, "\u00a012\ufeff" >>> 0);
assertSame(-12, "\u00a0-12\ufeff" | 0);

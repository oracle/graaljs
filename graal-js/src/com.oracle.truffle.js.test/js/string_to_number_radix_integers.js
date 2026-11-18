/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

// With literal operands, | 0 and >>> 0 convert the strings during AST construction,
// calling JSRuntime.stringToNumber. Using variables or Number() calls
// JSStringToNumberNode instead, which already accepts these radix forms.
assertSame(3, "0b11" | 0);
assertSame(3, "0B11" >>> 0);
assertSame(15, "0o17" | 0);
assertSame(15, "0O17" >>> 0);
assertSame(255, "0xff" | 0);
assertSame(255, "0XFF" >>> 0);

assertSame(-1, "0b11111111111111111111111111111111" | 0);
assertSame(4294967295, "0B11111111111111111111111111111111" >>> 0);
assertSame(-1, "0o37777777777" | 0);
assertSame(4294967295, "0O37777777777" >>> 0);
assertSame(-1, "0xffffffff" | 0);
assertSame(4294967295, "0XFFFFFFFF" >>> 0);

// Values above the safe integer range must be rounded to double before ToInt32/ToUint32.
assertSame(4294967295, "0b11111111111111111111111111111111111111111111111111111" >>> 0);
assertSame(4, "0b100000000000000000000000000000000000000000000000000011" | 0);
assertSame(4294967295, "0o377777777777777777" >>> 0);
assertSame(4, "0o400000000000000003" | 0);
assertSame(4294967295, "0x1fffffffffffff" >>> 0);
assertSame(4, "0x20000000000003" | 0);

// Hexadecimal integers may overflow long or contain more than 18 characters.
assertSame(2048, "0x8000000000000401" >>> 0);
assertSame(4096, "0X10000000000001000" | 0);
assertSame(1, "0x00000000000000001" >>> 0);

assertSame(3, "\u00a00b11\ufeff" | 0);
assertSame(15, "\n0O17\t" >>> 0);
assertSame(4096, " 0x10000000000001000 " | 0);

assertSame(0, "0b" | 0);
assertSame(0, "0o" >>> 0);
assertSame(0, "0x" | 0);
assertSame(0, "0b102" >>> 0);
assertSame(0, "0o78" | 0);
assertSame(0, "0xfg" >>> 0);
assertSame(0, "0b+11" | 0);
assertSame(0, "0o-17" >>> 0);
assertSame(0, "0x+1" | 0);
assertSame(0, "0x-1" >>> 0);
assertSame(0, "+0b11" | 0);
assertSame(0, "-0o17" >>> 0);
assertSame(0, "+0xff" | 0);
assertSame(0, "0b1.1" >>> 0);
assertSame(0, "0o1e2" | 0);
assertSame(0, "0x1p2" >>> 0);

// Reusing the shared large-integer parser must preserve rejection of non-ASCII digits.
assertSame(0, "0x0000000000000\u0661" | 0);
assertSame(0, "0X0000000000000\uff21" >>> 0);
assertSame(0, "0o00000000000000000\uff11" | 0);

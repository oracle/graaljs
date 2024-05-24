/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

// GR-54085
assertSame(1.7976931348623157e+308, Math.hypot(-1.7976931348623157e+308));

// other values
assertSame(2.6742622187558113e-307, Math.hypot(2.6718173667255144e-307, 1.1432573432387167e-308));

assertSame(Infinity, Math.hypot(-1.7976931348623157e+308, -1.7976931348623157e+308));
assertSame(Infinity, Math.hypot(-1.7976931348623157e+308, -1.7976931348623157e+308, -1.7976931348623157e+308));

assertSame(2.542322012307292e-308, Math.hypot(-1.7976931348623157e-308, -1.7976931348623157e-308));
assertSame(3.1136958459993e-308, Math.hypot(-1.7976931348623157e-308, -1.7976931348623157e-308, -1.7976931348623157e-308));

assertSame(2.5423220123072925e+307, Math.hypot(-1.7976931348623157e+307, -1.7976931348623157e+307));
assertSame(3.1136958459993004e+307, Math.hypot(-1.7976931348623157e+307, -1.7976931348623157e+307, -1.7976931348623157e+307));

assertSame(2.8284271247461903, Math.hypot(-2, -2));
assertSame(2.8284271247461903, Math.hypot(Math.hypot(-1, -1), Math.hypot(1, -1), Math.hypot(-1, 1), Math.hypot(1, 1)));
assertSame(3.4641016151377544, Math.hypot(-2, -2, -2));

assertSame(10050.37810234023, Math.hypot(-1e+1, -1e+2, -1e+3, -1e+4));
assertSame(0.1005037810234023, Math.hypot(-1e-1, -1e-2, -1e-3, -1e-4));
assertSame(100005000.37503123, Math.hypot(-1e+2, -1e+4, -1e+6, -1e+8));
assertSame(0.010000500037503125, Math.hypot(-1e-2, -1e-4, -1e-6, -1e-8));
assertSame(1.0049875621120888e+28, Math.hypot(-1e+19, -1e+20, -1e+27, -1e+28));
assertSame(1.004987562112089e-19, Math.hypot(-1e-19, -1e-20, -1e-27, -1e-28));

assertSame(1.7320508075688771e+100, Math.hypot(-1e+100, -1e+100, -1e+100));
assertSame(1.7320508075688773e+200, Math.hypot(-1e+200, -1e+200, -1e+200));
assertSame(1.7320508075688774e+300, Math.hypot(-1e+300, -1e+300, -1e+300));

assertSame(1.4142135623730951e+308, Math.hypot(-1e+308, -1e+308));
assertSame(1.7320508075688772e+308, Math.hypot(-1e+308, -1e+308, -1e+308));

assertSame(Infinity, Math.hypot(-Infinity, -Infinity, -Infinity));
assertSame(Infinity, Math.hypot(-Infinity, NaN, -Infinity));
assertSame(Infinity, Math.hypot(-Infinity, NaN));
assertSame(Infinity, Math.hypot(NaN, Infinity));

assertSame(0, Math.hypot());
assertSame(0, Math.hypot(-0));
assertSame(0, Math.hypot(-0, -0));
assertSame(0, Math.hypot(-0, -0, -0));

assertSame(314.1592653589793, Math.hypot(...new Array(10000).fill(Math.PI)));

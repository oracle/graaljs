/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

for (const n of [2147483647, Number.MAX_SAFE_INTEGER]) {
    assertSame(1n, BigInt.asUintN(n, 1n));
    assertThrows(() => BigInt.asUintN(n, -1n), RangeError);

    assertSame(1n, BigInt.asIntN(n, 1n));
    assertSame(-1n, BigInt.asIntN(n, -1n));
}

// testing with very large, but still supported bits;
// not testing with 2147483646 to save memory and time.
for (const n of [1073741823/*, 2147483646*/]) {
    const N = BigInt(n);
    const N_1 = BigInt(n - 1);
    const twoN = (1n << N);
    const half = (1n << N_1);

    assertSame(1n, BigInt.asUintN(n, 1n));
    assertTrue(twoN - 1n === BigInt.asUintN(n, -1n));
    assertTrue(half === BigInt.asUintN(n, half));
    assertTrue(half === BigInt.asUintN(n, -half));
    assertTrue((half - 1n) === BigInt.asUintN(n, (half - 1n)));
    assertTrue((twoN - (half - 1n)) === BigInt.asUintN(n, -(half - 1n)));
    assertTrue((twoN - (half - 1n)) === BigInt.asUintN(n, (twoN - (half - 1n))));
    assertTrue((half - 1n) === BigInt.asUintN(n, -(twoN - (half - 1n))));
    assertTrue(0n === BigInt.asUintN(n, twoN));

    assertSame(1n, BigInt.asIntN(n, 1n));
    assertTrue(-1n === BigInt.asIntN(n, -1n));
    assertTrue(-half === BigInt.asIntN(n, half));
    assertTrue(-half === BigInt.asIntN(n, -half));
    assertTrue((half - 1n) === BigInt.asIntN(n, (half - 1n)));
    assertTrue(-(half - 1n) === BigInt.asIntN(n, -(half - 1n)));
    assertTrue(-(half - 1n) === BigInt.asIntN(n, (twoN - (half - 1n))));
    assertTrue((half - 1n) === BigInt.asIntN(n, -(twoN - (half - 1n))));
    assertTrue(0n === BigInt.asIntN(n, twoN));
}

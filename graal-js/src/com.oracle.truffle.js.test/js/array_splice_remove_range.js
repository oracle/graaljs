/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests splice removal before the first element of contiguous and holes arrays.
 * Checks that subsequent writes do not expose elements retained in vacated backing-store slots.
 */

load("./assert.js");

for (const gap of [0, 1]) {
    const array = [];
    array[20] = {};
    array[21 + gap] = 1.5;
    array.splice(7, 7, null);

    assertSameContent(["7", "14", String(15 + gap), "length"], Reflect.ownKeys(array));

    assertTrue(Reflect.set(array, 31, -1));
    assertSameContent(["7", "14", String(15 + gap), "31", "length"], Reflect.ownKeys(array));
}

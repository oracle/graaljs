/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Deleting the last element must update the used range of contiguous and holes arrays.
 * After sealing, the length can still shrink across the deleted tail and preceding holes.
 */
load("./assert.js");

for (const value of [1, 1.5, "x", {}]) {
    for (const offset of [0, 20, 1000000]) {
        for (const gap of [1, 3]) {
            const array = [];
            array[offset] = value;
            array[offset + gap] = value;
            assertTrue(delete array[offset + gap]);
            assertSame(offset + gap + 1, array.length);

            Object.seal(array);
            assertTrue(Reflect.set(array, "length", offset + 1));
            assertSame(offset + 1, array.length);
            assertSameContent([String(offset)], Object.keys(array));
            assertSame(value, array[offset]);
        }
    }
}

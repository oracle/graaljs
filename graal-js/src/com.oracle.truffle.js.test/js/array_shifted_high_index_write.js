/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Large array-index writes after shifts must extend length and preserve existing elements,
 * rather than alias backing-store slots through signed-int truncation.
 */

load("assert.js");

for (const values of [[0, 1, 2, 3], [0.5, 1.5, 2.5, 3.5], ["a", "b", "c", "d"], [{}, {}, {}, {}], [undefined, null, true, "x"]]) {
    for (const shiftCount of [2, 3, 4]) {
        for (const index of [0x80000000, 0xfffffffc, 0xfffffffd, 0xfffffffe]) {
            for (const value of [values[0], 99, 1.5, "x", {}, undefined]) {
                const array = [];
                for (let i = 0; i < 8; i++) {
                    array.push(values[i % values.length]);
                }
                for (let i = 0; i < shiftCount; i++) {
                    array.shift();
                }
                array[10] = values[0];
                const entries = Object.keys(array).map(key => [key, array[key]]);
                array[index] = value;

                assertSame(index + 1, array.length);
                assertTrue(Object.hasOwn(array, index));
                assertSame(value, array[index]);
                assertSameContent([...entries.map(entry => entry[0]), String(index)], Object.keys(array));
                for (const [key, oldValue] of entries) {
                    assertSame(oldValue, array[key]);
                }
            }
        }
    }
}

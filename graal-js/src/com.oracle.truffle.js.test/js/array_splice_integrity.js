/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * splice on non-extensible arrays must match elementwise semantics, including legal hole removal
 * and partial mutations before an integrity violation throws.
 */

load("assert.js");

function createArray(layout, values) {
    const array = [];
    if (layout === "dense") {
        array.push(...values);
    } else if (layout === "holes") {
        array[0] = values[0];
        array[2] = values[1];
        array.length = 5;
    } else if (layout === "leadingHoles" || layout === "sparse") {
        const base = layout === "sparse" ? 100000 : 0;
        array[base + 2] = values[0];
        array[base + 3] = values[1];
        array.length = base + 6;
    } else {
        array.length = 3;
    }
    return array;
}

function spliceResult(array, start, deleteCount, items) {
    try {
        return {removed: Array.prototype.splice.call(array, start, deleteCount, ...items)};
    } catch (error) {
        assertTrue(error instanceof TypeError);
        return {error: error.constructor};
    }
}

function assertArrayState(expected, actual) {
    assertSame(expected.length, actual.length);
    assertSameContent(Reflect.ownKeys(expected), Reflect.ownKeys(actual));
    for (const key of Reflect.ownKeys(expected)) {
        const expectedDescriptor = Object.getOwnPropertyDescriptor(expected, key);
        const actualDescriptor = Object.getOwnPropertyDescriptor(actual, key);
        for (const attribute of ["value", "writable", "enumerable", "configurable"]) {
            assertSame(expectedDescriptor[attribute], actualDescriptor[attribute]);
        }
    }
}

for (const values of [[1, 2, 3], [1.5, 2.5, 3.5], ["a", "b", "c"], [{}, {}, {}]]) {
    for (const layout of ["dense", "holes", "leadingHoles", "empty", "sparse"]) {
        for (const integrity of [Object.preventExtensions, Object.seal, Object.freeze]) {
            for (const offset of [0, 1, 3, 9]) {
                for (const deleteCount of [0, 1, 3]) {
                    for (const items of [[], [9], [9, 10]]) {
                        const actual = integrity(createArray(layout, values));
                        // Non-extensible receivers cannot gain indexed properties. A plain
                        // object with matching descriptors is an elementwise reference.
                        const expected = integrity(Object.create(Object.prototype, Object.getOwnPropertyDescriptors(createArray(layout, values))));
                        const start = (layout === "sparse" ? 100000 : 0) + offset;
                        const expectedResult = spliceResult(expected, start, deleteCount, items);
                        const actualResult = spliceResult(actual, start, deleteCount, items);
                        assertSame(expectedResult.error, actualResult.error);
                        assertArrayState(expected, actual);
                        if (!expectedResult.error) {
                            assertArrayState(expectedResult.removed, actualResult.removed);
                        }
                    }
                }
            }
        }
    }
}

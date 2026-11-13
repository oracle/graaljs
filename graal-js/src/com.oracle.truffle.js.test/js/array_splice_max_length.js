/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * splice on maximum-length arrays must preserve property writes and deletions before RangeError,
 * treating destinations outside the array-index range as ordinary properties.
 */

load("assert.js");

const maxLength = 0xffffffff;
for (const layout of ["emptyTail", "last", "lastTwo"]) {
    for (const ordinaryProperties of [false, true]) {
        for (const start of [maxLength - 3, maxLength - 1, maxLength]) {
            for (const items of [[2], [2, 3], [2, 3, 4]]) {
                const array = [1];
                array.length = maxLength;
                if (layout !== "emptyTail") {
                    array[maxLength - 1] = "last";
                }
                if (layout === "lastTwo") {
                    array[maxLength - 2] = "penultimate";
                }
                if (ordinaryProperties) {
                    array[maxLength] = "old";
                    array[maxLength + 1] = "old2";
                }
                const expected = Object.assign({}, array);
                for (let from = maxLength - 1; from >= start; from--) {
                    const to = from + items.length;
                    if (Object.hasOwn(expected, from)) {
                        expected[to] = expected[from];
                    } else {
                        delete expected[to];
                    }
                }
                items.forEach((value, index) => expected[start + index] = value);

                assertThrows(() => array.splice(start, 0, ...items), RangeError);
                assertSame(maxLength, array.length);
                assertSameContent(Object.keys(expected), Object.keys(array));
                assertSameContent([...Object.keys(expected).filter(key => Number(key) < maxLength), "length",
                    ...Object.keys(expected).filter(key => Number(key) >= maxLength)], Reflect.ownKeys(array));
                for (const key of Object.keys(expected)) {
                    assertSame(expected[key], array[key]);
                    const descriptor = Object.getOwnPropertyDescriptor(array, key);
                    assertTrue(descriptor.writable && descriptor.enumerable && descriptor.configurable);
                }
            }
        }
    }
}

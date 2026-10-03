/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

function strictDelete(array, key) {
    "use strict";
    return delete array[key];
}

const deleteOperations = [
    (array, key) => delete array[key],
    strictDelete,
    Reflect.deleteProperty,
];
const arrayFactories = [
    () => [],
    () => [1, 2],
    () => Object.defineProperty([1, 2], "0", {get() { return 1; }}),
];

for (const createArray of arrayFactories) {
    for (const index of [-1, 4294967295, 4294967296, 9999999999, Number.MAX_SAFE_INTEGER]) {
        for (const key of [index, String(index)]) {
            const array = createArray();
            const length = array.length;
            for (const deleteProperty of deleteOperations) {
                Object.defineProperty(array, key, {value: 42, configurable: true});
                assertTrue(deleteProperty(array, key));
                assertFalse(Object.hasOwn(array, key));
                assertTrue(deleteProperty(array, key));
                assertSame(length, array.length);

                Object.defineProperty(array, key, {
                    get() { throw new Error("getter must not be called during deletion"); },
                    configurable: true,
                });
                assertTrue(deleteProperty(array, key));
                assertFalse(Object.hasOwn(array, key));
            }

            Object.defineProperty(array, key, {value: 42, configurable: false});
            assertFalse(delete array[key]);
            assertFalse(Reflect.deleteProperty(array, key));
            assertThrows(() => strictDelete(array, key), TypeError);
            assertSame(42, array[key]);
            assertSame(length, array.length);
        }
    }
}

const array = [];
array[4294967294] = 42;
assertSame(4294967295, array.length);
assertTrue(delete array[4294967294]);
assertFalse(Object.hasOwn(array, 4294967294));
assertSame(4294967295, array.length);

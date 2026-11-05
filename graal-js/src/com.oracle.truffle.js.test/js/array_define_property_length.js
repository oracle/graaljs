/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const arrayFactories = [
    () => [],
    () => [1, 2],
    () => new Array(2),
    () => Object.defineProperty([1, 2], "0", {get() { return 1; }}),
];
const descriptors = [{value: 0}, {get() { return 0; }}, {}];

for (const createArray of arrayFactories) {
    for (const index of [2, 5, 4294967294]) {
        for (const descriptor of descriptors) {
            for (const defineProperty of [Object.defineProperty, Reflect.defineProperty]) {
                const array = createArray();
                const length = array.length;
                Object.preventExtensions(array);

                if (defineProperty === Object.defineProperty) {
                    assertThrows(() => defineProperty(array, index, descriptor), TypeError);
                } else {
                    assertFalse(defineProperty(array, index, descriptor));
                }
                assertSame(length, array.length);
                assertFalse(Object.hasOwn(array, index));

                const extensibleArray = createArray();
                const expectedResult = defineProperty === Object.defineProperty ? extensibleArray : true;
                assertSame(expectedResult, defineProperty(extensibleArray, index, descriptor));
                assertSame(index + 1, extensibleArray.length);
                assertTrue(Object.hasOwn(extensibleArray, index));
            }
        }
    }
}

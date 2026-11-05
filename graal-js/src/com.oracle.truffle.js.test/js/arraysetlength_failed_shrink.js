/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const arrayFactories = [
    () => Object.seal([1, 2]),
    () => Object.seal([1, 2, , ,]),
    () => Object.seal(Object.defineProperty([1, 2], "0", {get() { return 1; }})),
    () => Object.seal(Object.defineProperty([1, 2, , ,], "0", {get() { return 1; }})),
    () => Object.defineProperty([1, 2, 3], "1", {configurable: false}),
    () => Object.defineProperty([1, 2, 3], "1", {get() { return 2; }, configurable: false}),
];

for (const createArray of arrayFactories) {
    for (const descriptor of [{value: 0}, {value: 0, writable: true}, {value: 0, writable: false}]) {
        for (const defineProperty of [Reflect.defineProperty, Object.defineProperty]) {
            const array = createArray();
            if (defineProperty === Reflect.defineProperty) {
                assertFalse(defineProperty(array, "length", descriptor));
            } else {
                assertThrows(() => defineProperty(array, "length", descriptor), TypeError);
            }
            assertSame(2, array.length);
            assertTrue(Object.hasOwn(array, "1"));
            assertSame(2, array[1]);
            assertFalse(Object.hasOwn(array, "2"));
            const lengthDescriptor = Object.getOwnPropertyDescriptor(array, "length");
            assertSame(2, lengthDescriptor.value);
            assertSame(descriptor.writable !== false, lengthDescriptor.writable);
        }
    }
}

// Shrinking past trailing holes succeeds when no element needs to be deleted.
for (const defineProperty of [Reflect.defineProperty, Object.defineProperty]) {
    for (const createArray of [arrayFactories[1], arrayFactories[3]]) {
        const array = createArray();
        const expectedResult = defineProperty === Reflect.defineProperty ? true : array;
        assertSame(expectedResult, defineProperty(array, "length", {value: 2}));
        assertSame(2, array.length);
        assertSame(2, array[1]);
    }
}

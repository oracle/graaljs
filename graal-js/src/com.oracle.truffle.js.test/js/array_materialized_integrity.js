/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Shape integrity flags must account for strategy-backed elements before they become ordinary
 * properties, including when the requested property definition fails.
 */

load("assert.js");

const factories = [
    () => [1],
    () => [1, 2],
    () => [1000, 2000],
    () => [1.5, 2.5],
    () => ["a", "b"],
    () => [{}, {}],
    () => [1, , 2],
    () => { const array = []; array.push(1, 2); return array; },
    () => { const array = [1]; array.length = 0x80000000; return array; },
    () => Object.getOwnPropertyNames({a: 1, b: 2}),
    () => /a/.exec("a")
];

for (const createArray of factories) {
    for (const readonlyLength of [false, true]) {
        for (const freeze of [false, true]) {
            const array = createArray();
            const length = array.length;
            if (readonlyLength) {
                Object.defineProperty(array, "length", {writable: false});
            }
            Object.preventExtensions(array);
            Object.defineProperty(array, "0", {enumerable: false});

            assertFalse(Object.isSealed(array));
            assertFalse(Object.isFrozen(array));
            assertTrue(Object.getOwnPropertyDescriptor(array, "0").configurable);

            (freeze ? Object.freeze : Object.seal)(array);

            assertFalse(Object.isExtensible(array));
            assertTrue(Object.isSealed(array));
            assertSame(freeze, Object.isFrozen(array));
            const descriptor = Object.getOwnPropertyDescriptor(array, "0");
            assertFalse(descriptor.configurable);
            assertFalse(descriptor.enumerable);
            assertSame(!freeze, descriptor.writable);
            assertFalse(Reflect.deleteProperty(array, "0"));
            assertSame(!freeze, Reflect.set(array, "0", "changed"));
            assertSame(freeze ? descriptor.value : "changed", array[0]);
            assertSame(length, array.length);
            assertSame(!freeze && !readonlyLength, Object.getOwnPropertyDescriptor(array, "length").writable);
        }
    }
}

for (const freeze of [false, true]) {
    const array = [1];
    Object.defineProperty(array, "length", {writable: false});
    (freeze ? Object.freeze : Object.seal)(array);

    assertFalse(Reflect.defineProperty(array, "0", {enumerable: false}));
    assertTrue(Object.isSealed(array));
    assertSame(freeze, Object.isFrozen(array));
    assertFalse(Object.getOwnPropertyDescriptor(array, "0").configurable);
    assertSame(!freeze, Object.getOwnPropertyDescriptor(array, "0").writable);
    assertSame(1, array[0]);

    Object.freeze(array);
    assertTrue(Object.isFrozen(array));
    assertFalse(Reflect.set(array, "0", 2));
    assertFalse(Reflect.deleteProperty(array, "0"));
    assertSame(1, array[0]);
}

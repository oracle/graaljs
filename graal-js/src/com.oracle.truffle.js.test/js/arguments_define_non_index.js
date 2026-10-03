/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

function unexpectedGetter() {
    throw new Error("property definition must not invoke a getter");
}

function unexpectedSetter(value) {
    throw new Error("property definition must not invoke a setter");
}

const argumentsFactories = [
    function mapped(a) { return arguments; },
    function unmapped(a) { "use strict"; return arguments; },
];

for (const createArguments of argumentsFactories) {
    for (const index of [4294967295, 4294967296, 9999999999]) {
        for (const key of [index, String(index)]) {
            for (const own of [false, true]) {
                for (const defineProperty of [Object.defineProperty, Reflect.defineProperty]) {
                    const args = createArguments(1);
                    const holder = own ? args : {};
                    Object.defineProperty(holder, key, {
                        get: unexpectedGetter, set: unexpectedSetter, configurable: true
                    });
                    if (!own) {
                        Object.setPrototypeOf(args, holder);
                    }
                    const expectedResult = defineProperty === Object.defineProperty ? args : true;
                    if (own) {
                        assertSame(expectedResult, defineProperty(args, key, {}));
                        assertSame(unexpectedGetter, Object.getOwnPropertyDescriptor(args, key).get);
                    }
                    assertSame(expectedResult, defineProperty(args, key, {
                        value: 42, writable: true, enumerable: true, configurable: true
                    }));
                    assertTrue(Object.hasOwn(args, key));
                    assertSame(42, args[key]);
                    assertSame(1, args[0]);
                    assertSame(1, args.length);
                }
            }

            const args = createArguments(1);
            Object.defineProperty(args, key, {get: unexpectedGetter, configurable: false});
            assertFalse(Reflect.defineProperty(args, key, {value: 42}));
            assertThrows(() => Object.defineProperty(args, key, {value: 42}), TypeError);
            assertSame(unexpectedGetter, Object.getOwnPropertyDescriptor(args, key).get);
        }
    }
}

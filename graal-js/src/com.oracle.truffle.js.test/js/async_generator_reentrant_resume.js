/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option unhandled-rejections=throw
 */

load('assert.js');

(async function () {
    for (const operation of ['next', 'return', 'throw']) {
        const thrown = {};
        async function* generate() {
            try {
                yield 1;
            } catch (error) {
                assertSame(thrown, error);
                await 0;
                return 'caught';
            }
            await 0;
            return 'finished';
        }
        const iterator = generate();
        let reentered = false;
        let nested;
        Object.defineProperty(Object.prototype, 'then', {
            configurable: true,
            get() {
                if (!reentered && Object.prototype.hasOwnProperty.call(this, 'done')) {
                    reentered = true;
                    nested = iterator[operation](operation === 'throw' ? thrown : 42);
                }
                return undefined;
            }
        });
        try {
            const first = await iterator.next();
            assertSame(1, first.value);
            assertFalse(first.done);
            assertTrue(reentered);
            const result = await nested;
            assertTrue(result.done);
            assertSame(operation === 'throw' ? 'caught' : operation === 'return' ? 42 : 'finished', result.value);
            const completed = await iterator.next();
            assertTrue(completed.done);
            assertSame(undefined, completed.value);
        } finally {
            delete Object.prototype.then;
        }
    }
})();

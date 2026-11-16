/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that for-of and for-await-of do not read value when done is truthy.
 * Regression test for https://github.com/oracle/graaljs/issues/1157.
 *
 * @option unhandled-rejections=throw
 */

load('assert.js');

function createIterable(done, length) {
    let index = 0;
    return {
        [Symbol.iterator]() {
            return this;
        },
        [Symbol.asyncIterator]() {
            return this;
        },
        next() {
            const value = index++;
            return {
                done: value === length ? done : false,
                get value() {
                    if (value === length) {
                        throw new Error('value read after completion');
                    }
                    return value;
                },
            };
        },
        return() {
            throw new Error('iterator closed after normal completion');
        },
    };
}

const doneValues = [true, 1, 'done', {}];

for (const done of doneValues) {
    for (const length of [0, 2]) {
        let count = 0;
        for (const value of createIterable(done, length)) {
            assertSame(count++, value);
        }
        assertSame(length, count);

        function* generate() {
            for (const value of createIterable(done, length)) {
                yield value;
            }
        }
        const generator = generate();
        for (let index = 0; index < length; index++) {
            const result = generator.next();
            assertFalse(result.done);
            assertSame(index, result.value);
        }
        assertTrue(generator.next().done);
    }
}

(async function () {
    for (const done of doneValues) {
        for (const length of [0, 2]) {
            let count = 0;
            // Use Symbol.asyncIterator directly: Async-from-Sync iterators read
            // value even when done is true, as required by their own algorithm.
            for await (const value of createIterable(done, length)) {
                await Promise.resolve();
                assertSame(count++, value);
            }
            assertSame(length, count);
        }
    }
})();

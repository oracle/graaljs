/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option explicit-resource-management
 * @option unhandled-rejections=throw
 */

load('../assert.js');

function startTicker(values) {
    return (async function () {
        for (let i = 0; i < 10; i++) {
            values.push(i);
            await 0;
        }
    })();
}

async function testAsyncDisposerTiming() {
    const values = [];
    const ticker = startTicker(values);

    async function dispose() {
        await using x = {
            [Symbol.asyncDispose]() {
                values.push(42);
            }
        };
        await using y = {
            [Symbol.asyncDispose]() {
                values.push(43);
            }
        };
        values.push(44);
    }

    await dispose();
    assertSame('0,44,43,1,42,2,3', values.join(','));
    await ticker;
}

async function testSyntheticAwaitTiming() {
    const values = [];
    const ticker = startTicker(values);

    async function dispose() {
        using x = {
            [Symbol.dispose]() {
                values.push(42);
            }
        };
        await using y = null;
        await using z = undefined;
        values.push(44);
    }

    await dispose();
    assertSame('0,44,1,42,2', values.join(','));
    await ticker;
}

async function testMixedAwaitTiming() {
    const values = [];
    const ticker = startTicker(values);

    async function dispose() {
        using x = {
            [Symbol.dispose]() {
                values.push(42);
            }
        };
        await using y = null;
        await using z = {
            [Symbol.asyncDispose]() {
                values.push(43);
            }
        };
        await using w = undefined;
        values.push(44);
    }

    await dispose();
    assertSame('0,44,43,1,42,2', values.join(','));
    await ticker;
}

async function testForLoopTiming() {
    const values = [];
    const ticker = startTicker(values);

    async function dispose() {
        for (await using x = {
            value: 42,
            [Symbol.asyncDispose]() {
                values.push('asyncDispose');
            }
        }; x.value < 44; x.value++) {
            values.push(x.value);
        }
        values.push('afterForLoop');
    }

    await dispose();
    assertSameContent([0, 42, 43, 'asyncDispose', 1, 'afterForLoop', 2], values);
    await ticker;
}

async function testRejectedAndThrowingDisposers() {
    const asyncError = new Error('async');
    const syncError = new Error('sync');

    async function dispose() {
        using syncResource = {
            [Symbol.dispose]() {
                throw syncError;
            }
        };
        await using asyncResource = {
            [Symbol.asyncDispose]() {
                return Promise.reject(asyncError);
            }
        };
    }

    try {
        await dispose();
        fail('disposal should have failed');
    } catch (error) {
        assertTrue(error instanceof SuppressedError);
        assertSame(syncError, error.error);
        assertSame(asyncError, error.suppressed);
    }
}

(async function main() {
    try {
        await testAsyncDisposerTiming();
        await testSyntheticAwaitTiming();
        await testMixedAwaitTiming();
        await testForLoopTiming();
        await testRejectedAndThrowingDisposers();
    } catch (error) {
        console.error(error.stack ?? error);
        throw error;
    }
})();

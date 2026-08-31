/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests async stack traces of Promise combinators.
 *
 * @option ecmascript-version=staging
 * @option unhandled-rejections=throw
 * @option v8-compat
 */

load('./assert.js');

Error.prepareStackTrace = (error, frames) => frames;

async function rejectWithError(message) {
    await 0;
    throw new Error(message);
}

async function fulfillWithError(message) {
    await 0;
    return new Error(message);
}

function assertCombinatorFrame(error, name, index) {
    const frame = error.stack.find(callSite => callSite.getPromiseIndex() !== null);
    assertSame(`async Promise.${name} (index ${index})`, String(frame));
    assertSame(name, frame.getFunctionName());
    assertSame(index, frame.getPromiseIndex());
    assertTrue(frame.isAsync());
    assertSame(name === 'all', frame.isPromiseAll());
}

(async function main() {
    try {
        try {
            await Promise.all([rejectWithError('all')]);
            fail('Promise.all should reject');
        } catch (error) {
            assertCombinatorFrame(error, 'all', 0);
        }

        let settled = await Promise.allSettled([
            fulfillWithError('allSettled fulfilled'),
            rejectWithError('allSettled rejected'),
        ]);
        assertCombinatorFrame(settled[0].value, 'allSettled', 0);
        assertCombinatorFrame(settled[1].reason, 'allSettled', 1);

        try {
            await Promise.allKeyed({key: rejectWithError('allKeyed')});
            fail('Promise.allKeyed should reject');
        } catch (error) {
            assertCombinatorFrame(error, 'allKeyed', 0);
        }

        settled = await Promise.allSettledKeyed({
            first: fulfillWithError('allSettledKeyed fulfilled'),
            second: rejectWithError('allSettledKeyed rejected'),
        });
        assertCombinatorFrame(settled.first.value, 'allSettledKeyed', 0);
        assertCombinatorFrame(settled.second.reason, 'allSettledKeyed', 1);
    } catch (error) {
        console.error(error.stack ?? error);
        throw error;
    }
})();

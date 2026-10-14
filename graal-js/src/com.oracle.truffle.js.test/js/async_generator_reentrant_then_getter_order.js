/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option unhandled-rejections=throw
 */

load('assert.js');

let iter;
let getterEntered = false;
let resumeCount = 0;
let resumeCountObservedInGetter = -1;

Object.defineProperty(Object.prototype, 'then', {
    configurable: true,
    get() {
        // AsyncGeneratorResolve resolves an iterator result object, so promise
        // resolution observes inherited "then" accessors. A re-entrant next()
        // from this accessor must be allowed to resume the async generator
        // before the accessor returns.
        if (!getterEntered) {
            getterEntered = true;
            iter.next('resume');
            resumeCountObservedInGetter = resumeCount;
        }
        return undefined;
    }
});

function cleanup() {
    delete Object.prototype.then;
}

async function *gen() {
    yield 1;
    resumeCount++;
    await new Promise(() => {});
}

iter = gen();

iter.next().then(first => {
    assertSame(false, first.done);
    assertSame(1, first.value);
    assertSame(true, getterEntered);
    assertSame(1, resumeCountObservedInGetter);
}).then(cleanup, error => {
    cleanup();
    throw error;
});

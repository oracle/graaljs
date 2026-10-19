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
let reentered = false;
let reenteredPromises = [];

Object.defineProperty(Object.prototype, 'then', {
    configurable: true,
    get() {
        // Promise resolution of an async-generator iterator result looks up the
        // result object's "then" property. Re-enter the generator from that
        // side effect, i.e. before AsyncGeneratorResolve returns.
        if (!reentered) {
            reentered = true;
            reenteredPromises.push(iter.next());
            reenteredPromises.push(iter.next({x: 1}));
            reenteredPromises.push(iter.next({y: 2}));
        }
        return undefined;
    }
});

function cleanup() {
    delete Object.prototype.then;
}

async function *gen() {
    yield {...yield yield, ...yield};
}

iter = gen();

iter.next().then(firstResult => {
    assertSame(false, firstResult.done);
    assertSame(undefined, firstResult.value);
    assertSame(true, reentered);
    assertSame(3, reenteredPromises.length);
    return Promise.all(reenteredPromises);
}).then(results => {
    let second = results[0];
    let third = results[1];
    let fourth = results[2];

    assertSame(false, second.done);
    assertSame(undefined, second.value);
    assertSame(false, third.done);
    assertSame(undefined, third.value);
    assertSame(false, fourth.done);
    assertSame(1, fourth.value.x);
    assertSame(2, fourth.value.y);
    assertSame(2, Object.keys(fourth.value).length);
}).then(cleanup, error => {
    cleanup();
    throw error;
});

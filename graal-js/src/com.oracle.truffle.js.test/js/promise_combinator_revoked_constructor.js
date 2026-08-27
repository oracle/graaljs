/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option ecmascript-version=staging
 * @option unhandled-rejections=throw
 */

load('./assert.js');

function callWithRevokedConstructor(method, input) {
    let revoke;
    class Constructor {
        constructor(executor) {
            const promise = new Promise(executor);
            revoke();
            return promise;
        }

        static resolve(value) {
            return Promise.resolve(value);
        }
    }
    const revocable = Proxy.revocable(Constructor, {});
    revoke = revocable.revoke;
    return Promise[method].call(revocable.proxy, input);
}

async function assertRejectsWithTypeError(method, input) {
    try {
        await callWithRevokedConstructor(method, input);
        fail(`Promise.${method} should reject`);
    } catch (error) {
        assertTrue(error instanceof TypeError);
    }
}

function callWithConstructorRevokedByResolveGetter(method, input) {
    let revoke;
    class Constructor {
        constructor(executor) {
            return new Promise(executor);
        }

        static get resolve() {
            revoke();
            return value => Promise.resolve(value);
        }
    }
    const revocable = Proxy.revocable(Constructor, {});
    revoke = revocable.revoke;
    return Promise[method].call(revocable.proxy, input);
}

(async function main() {
    try {
        await assertRejectsWithTypeError('all', []);
        await assertRejectsWithTypeError('allSettled', []);
        await assertRejectsWithTypeError('any', []);
        await assertRejectsWithTypeError('race', []);
        await assertRejectsWithTypeError('allKeyed', {});
        await assertRejectsWithTypeError('allSettledKeyed', {});

        await callWithConstructorRevokedByResolveGetter('all', [1]);
        await callWithConstructorRevokedByResolveGetter('allSettled', [1]);
        await callWithConstructorRevokedByResolveGetter('any', [1]);
        await callWithConstructorRevokedByResolveGetter('race', [1]);
        await callWithConstructorRevokedByResolveGetter('allKeyed', {value: 1});
        await callWithConstructorRevokedByResolveGetter('allSettledKeyed', {value: 1});
    } catch (error) {
        console.error(error.stack ?? error);
        throw error;
    }
})();

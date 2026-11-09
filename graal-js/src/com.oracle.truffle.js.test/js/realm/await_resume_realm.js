/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests async function and top-level await resumption in the originating realm,
 * including object allocation, rejection errors, and caller-realm restoration.
 *
 * @option v8-realm-builtin=true
 * @option debug-builtin
 * @option testV8-mode
 * @option unhandled-rejections=throw
 */

load('../assert.js');

let completed = false;
(async function () {
    const realm = Realm.createAllowCrossRealmAccess();
    const global = Realm.global(realm);
    global.pending = Promise.resolve();
    const run = Realm.eval(realm, `(async function (pending) {
        const before = {};
        await pending;
        const after = {};
        try {
            await Promise.reject(42);
        } catch (error) {
            return [before, after, {}, error];
        }
    })`);
    const results = await run(global.pending);
    for (let i = 0; i < 3; i++) {
        assertSame(global.Object.prototype, Object.getPrototypeOf(results[i]));
    }
    assertSame(42, results[3]);
    await Realm.eval(realm, `Debug.loadModule('entry.js', {
        'entry.js': 'globalThis.before = {}; await pending; globalThis.after = {}; export {};'
    })`);
    for (const value of [global.before, global.after]) {
        assertSame(global.Object.prototype, Object.getPrototypeOf(value));
    }
    for (const action of [
        () => Realm.eval(realm, '(async function () { await pending; null.property; })()'),
        () => Realm.eval(realm, `Debug.loadModule('reject.js', {
            'reject.js': 'await pending; null.property;'
        })`)
    ]) {
        try {
            await action();
            fail('Expected resumed body to throw');
        } catch (error) {
            assertSame(global.TypeError.prototype, Object.getPrototypeOf(error));
        }
    }
    completed = true;
})();

TestV8.runMicrotasks();
assertTrue(completed);
assertSame(0, Realm.current());

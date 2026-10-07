/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests result and promise realms for borrowed async iterator helper methods
 * and Array.fromAsync resumption with iterable and array-like inputs.
 *
 * @option v8-realm-builtin=true
 * @option async-iterator-helpers
 * @option testV8-mode
 * @option unhandled-rejections=throw
 */

load('../assert.js');

let completed = false;
(async function () {
    const helperRealm = Realm.createAllowCrossRealmAccess();
    const methodRealm = Realm.createAllowCrossRealmAccess();
    const helperGlobal = Realm.global(helperRealm);
    const methodGlobal = Realm.global(methodRealm);
    const helper = Realm.eval(helperRealm, 'AsyncIterator.from([1]).map(value => value)');
    const next = Realm.eval(methodRealm, 'Object.getPrototypeOf(AsyncIterator.from([]).map(value => value)).next');

    const pending = next.call(helper);
    assertSame(methodGlobal.Promise.prototype, Object.getPrototypeOf(pending));
    const first = await pending;
    assertSame(helperGlobal.Object.prototype, Object.getPrototypeOf(first));
    assertSame(1, first.value);
    assertFalse(first.done);
    const last = await next.call(helper);
    assertSame(helperGlobal.Object.prototype, Object.getPrototypeOf(last));
    assertTrue(last.done);
    const exhausted = await next.call(helper);
    assertSame(methodGlobal.Object.prototype, Object.getPrototypeOf(exhausted));
    assertTrue(exhausted.done);

    for (const input of [[Promise.resolve(1), 2], {0: Promise.resolve(1), 1: 2, length: 2}]) {
        const result = await helperGlobal.Array.fromAsync.call(null, input, async value => value + 1);
        assertSame(helperGlobal.Array.prototype, Object.getPrototypeOf(result));
        assertSame(2, result.length);
        assertSame(2, result[0]);
        assertSame(3, result[1]);
    }
    completed = true;
})();

TestV8.runMicrotasks();
assertTrue(completed);
assertSame(0, Realm.current());

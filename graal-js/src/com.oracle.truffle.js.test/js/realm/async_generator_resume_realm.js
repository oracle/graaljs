/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests queued async-generator next/throw/return requests across realms, including
 * body allocations and errors, completed iterator results, and caller-realm restoration.
 *
 * @option v8-realm-builtin=true
 * @option testV8-mode
 * @option unhandled-rejections=throw
 */

load('../assert.js');

let completed = false;
(async function () {
    const methodRealm = Realm.createAllowCrossRealmAccess();
    const methods = Realm.eval(methodRealm, 'Object.getPrototypeOf(async function*() {}).prototype');
    for (let i = 0; i < 6; i++) {
        const realm = Realm.createAllowCrossRealmAccess();
        const global = Realm.global(realm);
        const make = Realm.eval(realm, `(async function* () {
            yield {index: ${i}};
            try {
                await 0;
                yield {};
            } catch (error) {
                yield {error};
            } finally {
                yield {};
            }
        })`);
        const iterator = make();
        const thrown = {};
        const returned = {};
        const pending = [
            methods.next.call(iterator),
            methods.next.call(iterator),
            methods.throw.call(iterator, thrown),
            methods.return.call(iterator, returned),
            methods.next.call(iterator)
        ];
        for (let j = 0; j < 4; j++) {
            const result = await pending[j];
            assertFalse(result.done);
            assertSame(global.Object.prototype, Object.getPrototypeOf(result.value));
            if (j === 0) assertSame(i, result.value.index);
            if (j === 2) assertSame(thrown, result.value.error);
        }
        const final = await pending[4];
        assertTrue(final.done);
        assertSame(returned, final.value);
        const result = await methods.next.call(iterator);
        assertTrue(result.done);
        assertSame(Realm.global(methodRealm).Object.prototype, Object.getPrototypeOf(result));

        const throwing = Realm.eval(realm, '(async function* () { yield {}; null.property; })')();
        await methods.next.call(throwing);
        try {
            await methods.next.call(throwing);
            fail('Expected body to throw');
        } catch (error) {
            assertSame(global.TypeError.prototype, Object.getPrototypeOf(error));
        }
    }
    completed = true;
})();

TestV8.runMicrotasks();
assertTrue(completed);
assertSame(0, Realm.current());

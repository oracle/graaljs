/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests dispatch-established realms for class-call errors, dynamic module imports
 * and linking failures, and ShadowRealm export wrapping.
 *
 * @option v8-realm-builtin=true
 * @option shadow-realm=true
 * @option testV8-mode
 * @option unhandled-rejections=throw
 */

load('../assert.js');

const realm = Realm.createAllowCrossRealmAccess();
const global = Realm.global(realm);
const classConstructor = Realm.eval(realm, '(class ForeignClass {})');
for (const target of [classConstructor, classConstructor.bind(null).bind(null)]) {
    assertThrows(() => target(), global.TypeError);
    assertSame(0, Realm.current());
}

const baseURL = import.meta.url.substring(0, import.meta.url.lastIndexOf('/') + 1);
const moduleURL = baseURL + 'fixtures/import_realm_module.mjs';
const linkErrorURL = baseURL + 'fixtures/import_realm_link_error.mjs';
let completed = false;
(async function () {
    const importModule = Realm.eval(realm, '(url => import(url))');
    const pending = importModule(moduleURL);
    assertSame(global.Promise.prototype, Object.getPrototypeOf(pending));
    const namespace = await pending;
    assertSame(global.Object.prototype, Object.getPrototypeOf(namespace.object));
    assertSame(global.Function.prototype, Object.getPrototypeOf(namespace.answer));
    assertSame(42, namespace.answer());
    try {
        await importModule(linkErrorURL);
        fail('Expected linking to fail');
    } catch (error) {
        assertSame(global.SyntaxError.prototype, Object.getPrototypeOf(error));
    }
    assertSame(0, Realm.current());

    const shadowRealm = new global.ShadowRealm();
    const importValue = global.ShadowRealm.prototype.importValue;
    const valuePromise = importValue.call(shadowRealm, moduleURL, 'answer');
    assertSame(global.Promise.prototype, Object.getPrototypeOf(valuePromise));
    const answer = await valuePromise;
    assertSame(global.Function.prototype, Object.getPrototypeOf(answer));
    assertSame(42, answer());
    for (const name of ['missing', 'object']) {
        try {
            await importValue.call(shadowRealm, moduleURL, name);
            fail('Expected export wrapping to fail');
        } catch (error) {
            assertSame(global.TypeError.prototype, Object.getPrototypeOf(error));
        }
        assertSame(0, Realm.current());
    }
    completed = true;
})();

TestV8.runMicrotasks();
assertTrue(completed);
assertSame(0, Realm.current());

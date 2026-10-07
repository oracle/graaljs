/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests intrinsic Locale construction, inlined string builtin fallbacks, and Atomics.waitAsync
 * promise resolution, while ensuring bound-function jobs use generic realm-aware dispatch.
 *
 * @option v8-realm-builtin=true
 * @option testV8-mode
 * @option unhandled-rejections=throw
 */

load('../assert.js');

const realm = Realm.createAllowCrossRealmAccess();
const global = Realm.global(realm);
const Locale = global.Intl.Locale;
const locale = new Intl.Locale('en');
global.Intl.Locale = function () {
    fail('Expected the intrinsic Locale constructor');
};
for (const method of ['maximize', 'minimize']) {
    const result = Locale.prototype[method].call(locale);
    assertSame(Locale.prototype, Object.getPrototypeOf(result));
    assertSame('en', result.language);
}

function callStringMethod(method, receiver, index) {
    return method.call(receiver, index);
}

for (const [name, expected] of [['charAt', 'b'], ['charCodeAt', 98], ['substring', 'bc']]) {
    for (const realmGlobal of [global, globalThis, global]) {
        const method = realmGlobal.String.prototype[name];
        assertSame(expected, callStringMethod(method, 'abc', 1));
        // Symbol receivers force the inlined fast path to fall back to a normal call.
        assertThrows(() => callStringMethod(method, Symbol(), 1), realmGlobal.TypeError);
        assertSame(expected, callStringMethod(method, 'abc', 1));
        assertSame(0, Realm.current());
    }
}

// Jobs can be user-supplied bound functions, so their dispatch must stay generic.
const state = {};
global.state = state;
TestV8.enqueueJob(Realm.eval(realm, '(function () { state.value = {}; }).bind(null).bind(null)'));

// Atomics.waitAsync, in contrast, resolves an intrinsic promise capability.
const array = new Int32Array(new SharedArrayBuffer(4));
const waiting = Atomics.waitAsync(array, 0, 0);
assertTrue(waiting.async);
let waitResult;
waiting.value.then(value => { waitResult = value; });
assertSame(1, Atomics.notify(array, 0));

TestV8.runMicrotasks();
assertSame(global.Object.prototype, Object.getPrototypeOf(state.value));
assertSame('ok', waitResult);
assertSame(0, Realm.current());

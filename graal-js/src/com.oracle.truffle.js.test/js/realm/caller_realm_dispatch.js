/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests uncached cross-realm calls, construction, and bound-function jobs,
 * including which errors belong to the caller realm versus the function realm.
 *
 * @option v8-realm-builtin=true
 * @option function-cache-limit=0
 * @option testV8-mode
 * @option unhandled-rejections=throw
 */

load('../assert.js');

const realm = Realm.createAllowCrossRealmAccess();
const global = Realm.global(realm);
const makeObject = Realm.eval(realm, '(function () { return {}; })');

function bindTwice(fn) {
    return fn.bind(null).bind(null);
}

function call(fn) {
    return fn();
}

function construct(fn) {
    return new fn();
}

for (const fn of [makeObject, bindTwice(makeObject)]) {
    assertSame(global.Object.prototype, Object.getPrototypeOf(call(fn)));
    assertSame(global.Object.prototype, Object.getPrototypeOf(construct(fn)));
    assertSame(global.Object.prototype, Object.getPrototypeOf(Reflect.construct(fn, [], function () {})));
}
for (const fn of [global.Array, bindTwice(global.Array)]) {
    assertSame(global.Array.prototype, Object.getPrototypeOf(call(fn)));
    assertSame(global.Array.prototype, Object.getPrototypeOf(construct(fn)));
}

const classConstructor = Realm.eval(realm, '(class ForeignClass {})');
for (const target of [classConstructor, bindTwice(classConstructor)]) {
    assertThrows(() => call(target), global.TypeError);
}

const primitiveResult = Realm.eval(realm, '(class extends Object { constructor() { return 42; } })');
const missingSuper = Realm.eval(realm, '(class extends Object { constructor() {} })');
for (const [fn, ErrorConstructor] of [
    [primitiveResult, TypeError],
    [missingSuper, ReferenceError],
    [global.Math.abs, TypeError],
    [global.Symbol, global.TypeError]
]) {
    for (const target of [fn, bindTwice(fn)]) {
        assertThrows(() => construct(target), ErrorConstructor);
        assertThrows(() => Reflect.construct(target, [], function () {}), ErrorConstructor);
    }
}

// Raw job dispatch also reaches bound forwarding targets without entering their realm first.
const state = {};
global.state = state;
const job = Realm.eval(realm, '(function () { state.value = {}; })');
TestV8.enqueueJob(bindTwice(job));
TestV8.runMicrotasks();
assertSame(global.Object.prototype, Object.getPrototypeOf(state.value));
assertSame(0, Realm.current());

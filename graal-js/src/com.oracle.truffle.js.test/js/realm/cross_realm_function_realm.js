/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests function-realm selection across calls, constructors, callbacks, modules,
 * generators, async resumption, and resource disposal.
 *
 * @option debug-builtin
 * @option explicit-resource-management
 * @option v8-realm-builtin=true
 * @option test262-mode
 */

load('../assert.js');

let otherRealm = Realm.create();
let otherGlobal = Realm.global(otherRealm);

let test262Realm = $262.createRealm();
assertSame(test262Realm.global, test262Realm.evalScript('(() => globalThis)()'));

var sum = 0;
otherGlobal.Int8Array.prototype.forEach.call(new Int8Array([1, 2, 3]), value => {
    sum += value;
});
assertSame(6, sum);

var result;
function primaryCallback() {
    result = this;
}
otherGlobal.primaryCallback = primaryCallback;
Realm.eval(otherRealm, 'primaryCallback()');
assertSame(this, result);

function PrimaryConstructor() {
    result = globalThis;
}
otherGlobal.PrimaryConstructor = PrimaryConstructor;
Realm.eval(otherRealm, 'new PrimaryConstructor()');
assertSame(this, result);

otherGlobal.mainGlobal = this;
let secondaryModuleResult;
let secondaryModulePromise = Realm.eval(otherRealm, `
    Debug.loadModule('entry.js', {'entry.js': 'export default globalThis;'}).then(namespace => {
        mainGlobal.secondaryModuleResult = namespace.default;
    });
`);
secondaryModulePromise.then(() => assertSame(otherGlobal, secondaryModuleResult));

Realm.eval(otherRealm, `
    function SecondaryConstructor() {
        mainGlobal.result = globalThis;
    }
`);
new otherGlobal.SecondaryConstructor();
assertSame(otherGlobal, result);
Reflect.construct(otherGlobal.SecondaryConstructor, [], PrimaryConstructor);
assertSame(otherGlobal, result);

Realm.eval(otherRealm, `
    globalThis.SecondaryDerivedReturnsPrimitive = class SecondaryDerivedReturnsPrimitive extends Object {
        constructor() {
            return 42;
        }
    }
    globalThis.SecondaryDerivedWithoutSuper = class SecondaryDerivedWithoutSuper extends Object {
        constructor() {
        }
    }
`);
try {
    new otherGlobal.SecondaryDerivedReturnsPrimitive();
    fail('expected derived constructor to throw');
} catch (error) {
    assertSame(TypeError.prototype, Object.getPrototypeOf(error));
}
try {
    new otherGlobal.SecondaryDerivedWithoutSuper();
    fail('expected derived constructor to throw');
} catch (error) {
    assertSame(ReferenceError.prototype, Object.getPrototypeOf(error));
}

Realm.eval(otherRealm, `
    function secondaryThrow() {
        throw new Error('secondary realm error');
    }
`);
try {
    otherGlobal.secondaryThrow();
    fail('expected secondaryThrow to throw');
} catch (error) {
    assertSame(otherGlobal.Error.prototype, Object.getPrototypeOf(error));
    assertTrue(error.stack.includes('secondaryThrow'));
    assertTrue(error.stack.includes('cross_realm_function_realm.js'));
}

Realm.eval(otherRealm, 'function callback() { mainGlobal.result = this; }');
otherGlobal.Int8Array.from.call(Int8Array, [1, 2, 3], otherGlobal.callback);
assertSame(otherGlobal, result);

let siblingRealm = Realm.create();
let siblingGlobal = Realm.global(siblingRealm);
siblingGlobal.mainGlobal = this;
Realm.eval(siblingRealm, 'function siblingCallback() { mainGlobal.result = this; }');
otherGlobal.siblingCallback = siblingGlobal.siblingCallback;
Realm.eval(otherRealm, 'siblingCallback()');
assertSame(siblingGlobal, result);

let secondaryConstructor = Realm.eval(siblingRealm, '(function(){})');
let secondaryHandler = Realm.eval(otherRealm, '({ construct(target, args) { return args; } })');
let crossRealmProxy = new otherGlobal.Proxy(secondaryConstructor, secondaryHandler);
let proxyResult = Reflect.construct(crossRealmProxy, []);
assertSame(Array.prototype, Object.getPrototypeOf(proxyResult));

function* primaryGenerator() {
    yield;
    result = globalThis;
}
otherGlobal.primaryGenerator = primaryGenerator;
Realm.eval(otherRealm, 'mainGlobal.secondaryGeneratorNext = Object.getPrototypeOf(function*(){}()).next;');
let primaryIterator = primaryGenerator();
secondaryGeneratorNext.call(primaryIterator);
secondaryGeneratorNext.call(primaryIterator);
assertSame(this, result);

Realm.eval(otherRealm, 'function* secondaryGenerator() { yield; mainGlobal.result = globalThis; }');
let secondaryIterator = otherGlobal.secondaryGenerator();
secondaryIterator.next();
secondaryIterator.next();
assertSame(otherGlobal, result);

var primaryAsyncResult;
var secondaryAsyncResult;
async function primaryAsync() {
    await 0;
    primaryAsyncResult = globalThis;
}
otherGlobal.primaryAsync = primaryAsync;
Realm.eval(otherRealm, 'primaryAsync()');
Realm.eval(otherRealm, 'async function secondaryAsync() { await 0; mainGlobal.secondaryAsyncResult = globalThis; }');
otherGlobal.secondaryAsync();
Promise.resolve().then(() => {
    assertSame(this, primaryAsyncResult);
    assertSame(otherGlobal, secondaryAsyncResult);
});

var secondaryAsyncDisposeResult;
Realm.eval(otherRealm, `
    async function secondaryAsyncDispose() {
        await using resource = {
            [Symbol.asyncDispose]() {
                return Promise.resolve();
            }
        };
        mainGlobal.secondaryAsyncDisposeResult = globalThis;
    }
`);
(async function() {
    await otherGlobal.secondaryAsyncDispose();
    assertSame(otherGlobal, secondaryAsyncDisposeResult);
})();

var primaryAsyncGeneratorResult;
async function* primaryAsyncGenerator() {
    yield;
    primaryAsyncGeneratorResult = globalThis;
}
Realm.eval(otherRealm, 'mainGlobal.secondaryAsyncGeneratorNext = Object.getPrototypeOf(async function*(){}()).next;');
let primaryAsyncIterator = primaryAsyncGenerator();
(async function() {
    await secondaryAsyncGeneratorNext.call(primaryAsyncIterator);
    await secondaryAsyncGeneratorNext.call(primaryAsyncIterator);
    assertSame(globalThis, primaryAsyncGeneratorResult);
})();

var secondaryAsyncGeneratorResult;
Realm.eval(otherRealm, `
    async function* secondaryAsyncGenerator() {
        yield;
        mainGlobal.secondaryAsyncGeneratorResult = globalThis;
    }
`);
let secondaryAsyncIterator = otherGlobal.secondaryAsyncGenerator();
(async function() {
    await secondaryAsyncIterator.next();
    await secondaryAsyncIterator.next();
    assertSame(otherGlobal, secondaryAsyncGeneratorResult);
})();

function callAtSharedSite(fn) {
    return fn();
}
function constructAtSharedSite(fn) {
    return new fn();
}
for (let i = 0; i < 12; i++) {
    const global = i % 2 === 0 ? globalThis : otherGlobal;
    const callback = i % 2 === 0
        ? new Function('return globalThis;')
        : Realm.eval(otherRealm, '(function() { return globalThis; })');
    const constructor = i % 2 === 0
        ? new Function('this.global = globalThis;')
        : Realm.eval(otherRealm, '(function() { this.global = globalThis; })');
    assertSame(global, callAtSharedSite(callback));
    assertSame(global, callAtSharedSite(callback.bind(null)));
    assertSame(global, constructAtSharedSite(constructor).global);
    assertSame(global, constructAtSharedSite(constructor.bind(null)).global);
}

for (const Constructor of [otherGlobal.SecondaryDerivedReturnsPrimitive, otherGlobal.SecondaryDerivedWithoutSuper]) {
    const expectedPrototype = Constructor === otherGlobal.SecondaryDerivedReturnsPrimitive ? TypeError.prototype : ReferenceError.prototype;
    for (const target of [Constructor, Constructor.bind(null)]) {
        try {
            constructAtSharedSite(target);
            fail('expected derived constructor to throw');
        } catch (error) {
            assertSame(expectedPrototype, Object.getPrototypeOf(error));
        }
    }
}

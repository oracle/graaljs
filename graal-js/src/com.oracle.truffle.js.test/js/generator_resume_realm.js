/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests generator resumption with distinct caller, builtin-method, and generator realms.
 * Reentrant next/return/throw errors and all completed iterator results must use the
 * current builtin method's realm, including results from finishing the generator body.
 * Yielded results and errors created in the body must use the generator's realm.
 * Early return/throw must not execute the body, and throw must preserve the supplied
 * value's identity when it is not handled by the body.
 *
 * @option v8-realm-builtin=true
 */

load('./assert.js');

const methodRealm = Realm.createAllowCrossRealmAccess();
const generatorRealm = Realm.createAllowCrossRealmAccess();
const methodGlobal = Realm.global(methodRealm);
const generatorGlobal = Realm.global(generatorRealm);
const methods = Realm.eval(methodRealm, 'Object.getPrototypeOf(function*() {}).prototype');
const makeGenerator = Realm.eval(generatorRealm, '(function* (callback) { if (callback) callback(); yield 1; return 2; })');

function thrownValue(action) {
    try {
        action();
    } catch (error) {
        return error;
    }
    throw new Error('Expected an exception');
}

function assertCompleted(result, value) {
    assertSame(methodGlobal.Object.prototype, Object.getPrototypeOf(result));
    assertSame(value, result.value);
    assertTrue(result.done);
}

for (const method of ['next', 'return', 'throw']) {
    let iterator;
    iterator = makeGenerator(() => {
        const error = thrownValue(() => methods[method].call(iterator, 42));
        assertSame(methodGlobal.TypeError.prototype, Object.getPrototypeOf(error));
    });
    const result = iterator.next();
    assertSame(1, result.value);
    assertFalse(result.done);
    iterator.return();
}

const completed = makeGenerator();
for (const [value, done] of [[1, false], [2, true]]) {
    const result = methods.next.call(completed);
    assertSame(done ? methodGlobal.Object.prototype : generatorGlobal.Object.prototype, Object.getPrototypeOf(result));
    assertSame(value, result.value);
    assertSame(done, result.done);
}

for (const value of [42, undefined, null, {}]) {
    assertCompleted(methods.next.call(completed, value), undefined);
    assertCompleted(methods.return.call(completed, value), value);
    assertSame(value, thrownValue(() => methods.throw.call(completed, value)));

    for (const method of ['return', 'throw']) {
        let bodyRuns = 0;
        const fresh = makeGenerator(() => { bodyRuns++; });
        if (method === 'return') {
            assertCompleted(methods.return.call(fresh, value), value);
        } else {
            assertSame(value, thrownValue(() => methods.throw.call(fresh, value)));
        }
        assertCompleted(methods.next.call(fresh), undefined);
        assertSame(0, bodyRuns);
    }
}

const emptyGenerator = Realm.eval(generatorRealm, '(function* () {})');
assertCompleted(methods.next.call(emptyGenerator()), undefined);

const catchingGenerator = Realm.eval(generatorRealm, '(function* () { try { yield 1; } catch (error) { return error; } })');
const finallyGenerator = Realm.eval(generatorRealm, '(function* () { try { yield 1; } finally { yield 2; } })');
const callerMethods = Object.getPrototypeOf(function*() {}).prototype;
for (const value of [42, undefined, null, {}]) {
    const returning = makeGenerator();
    returning.next();
    assertCompleted(methods.return.call(returning, value), value);

    const catching = catchingGenerator();
    catching.next();
    assertCompleted(methods.throw.call(catching, value), value);

    const finalizing = finallyGenerator();
    finalizing.next();
    const yielded = methods.return.call(finalizing, value);
    assertSame(generatorGlobal.Object.prototype, Object.getPrototypeOf(yielded));
    assertSame(2, yielded.value);
    assertFalse(yielded.done);
    const result = callerMethods.next.call(finalizing);
    assertSame(Object.prototype, Object.getPrototypeOf(result));
    assertSame(value, result.value);
    assertTrue(result.done);
}

const throwingGenerator = Realm.eval(generatorRealm, '(function* () { yield 1; throw new Error("from body"); })');
const throwing = throwingGenerator();
throwing.next();
const bodyError = thrownValue(() => methods.next.call(throwing));
assertSame(generatorGlobal.Error.prototype, Object.getPrototypeOf(bodyError));
assertCompleted(methods.next.call(throwing), undefined);

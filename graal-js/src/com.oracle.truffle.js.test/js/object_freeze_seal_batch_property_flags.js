/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

'use strict';

/*
 * Tests Object.freeze() and Object.seal(), in particular the optimized batch update of
 * property flags. Verify that descriptors and optimized property accesses remain valid,
 * accessors are not invoked, and special objects and proxies retain their observable semantics.
 */

load('./assert.js');

function readX(object) { return object.x; }
function writeX(object, value) { object.x = value; }

for (const freeze of [false, true]) {
    for (let repeat = 0; repeat < 8; repeat++) {
        const symbol = Symbol('own');
        let gets = 0;
        let sets = 0;
        const object = Object.create(null);
        for (let i = 0; i < 128; i++) {
            Object.defineProperty(object, 'p' + i, {
                value: i, writable: i % 3 !== 0, configurable: i % 5 !== 0, enumerable: i % 2 === 0
            });
        }
        object.x = 0;
        object[symbol] = 42;
        Object.defineProperty(object, 'accessor', {
            get() { gets++; return 17; },
            set(value) { assertSame(19, value); sets++; },
            configurable: true, enumerable: false
        });
        for (let i = 0; i < 100; i++) {
            writeX(object, i);
        }
        assertSame(99, readX(object));
        const before = Object.getOwnPropertyDescriptors(object);
        const keys = Reflect.ownKeys(object);
        const operation = freeze ? Object.freeze : Object.seal;
        assertSame(object, operation(object));
        assertSame(object, operation(object));
        assertSame(0, gets);
        assertSame(0, sets);
        assertTrue(Object.isSealed(object));
        assertSame(freeze, Object.isFrozen(object));
        assertFalse(Object.isExtensible(object));
        assertSame(null, Object.getPrototypeOf(object));
        assertSame(keys.length, Reflect.ownKeys(object).length);
        for (let i = 0; i < keys.length; i++) {
            const key = keys[i];
            assertSame(key, Reflect.ownKeys(object)[i]);
            const old = before[key];
            const actual = Object.getOwnPropertyDescriptor(object, key);
            assertFalse(actual.configurable);
            assertSame(old.enumerable, actual.enumerable);
            if ('value' in old) {
                assertSame(old.value, actual.value);
                assertSame(freeze ? false : old.writable, actual.writable);
            } else {
                assertSame(old.get, actual.get);
                assertSame(old.set, actual.set);
                assertFalse('writable' in actual);
            }
        }
        object.accessor = 19;
        assertSame(1, sets);
        assertSame(17, object.accessor);
        assertSame(1, gets);
        if (freeze) {
            assertThrows(() => writeX(object, 100), TypeError);
            assertSame(99, readX(object));
        } else {
            writeX(object, 'generalized');
            assertSame('generalized', readX(object));
            Object.freeze(object);
            assertThrows(() => writeX(object, 101), TypeError);
        }
        assertThrows(() => { object.newProperty = 1; }, TypeError);
        assertThrows(() => { delete object.x; }, TypeError);
    }
}

for (const key of ['property', Symbol('property')]) {
    for (const enumerable of [false, true]) {
        for (const descriptor of [
            {value: 42, writable: false},
            {value: 42, writable: true},
            {
                get() { throw new Error('unexpected getter call'); },
                set(value) { throw new Error('unexpected setter call'); }
            }
        ]) {
            const object = Object.create(null);
            Object.defineProperty(object, key, {...descriptor, enumerable, configurable: true});
            Object.preventExtensions(object);
            assertFalse(Object.isSealed(object));
            assertFalse(Object.isFrozen(object));

            Object.defineProperty(object, key, {configurable: false});
            assertTrue(Object.isSealed(object));
            assertSame(descriptor.writable !== true, Object.isFrozen(object));
            if (descriptor.writable) {
                Object.defineProperty(object, key, {writable: false});
                assertTrue(Object.isFrozen(object));
            }
        }
    }
}

class PrivateState {
    #state = 1;
    x = 2;
    y = 3;
    setState(value) { this.#state = value; }
    getState() { return this.#state; }
}
const privateState = Object.freeze(new PrivateState());
privateState.setState(42);
assertSame(42, privateState.getState());
assertThrows(() => { privateState.x = 4; }, TypeError);

const sparse = [1, , 3];
sparse.extra = 4;
Object.freeze(sparse);
assertFalse(Object.hasOwn(sparse, 1));
assertTrue(Object.isFrozen(sparse));
assertFalse(Object.getOwnPropertyDescriptor(sparse, 'length').writable);
assertThrows(() => { sparse[0] = 2; }, TypeError);
assertThrows(() => { sparse.length = 1; }, TypeError);
const sealedArray = Object.seal([1, 2]);
sealedArray[0] = 9;
assertSame(9, sealedArray[0]);
assertTrue(Object.getOwnPropertyDescriptor(sealedArray, 'length').writable);
assertThrows(() => Object.freeze(new Uint8Array(1)), TypeError);
assertTrue(Object.isFrozen(Object.freeze(new Uint8Array(0))));

const effects = [];
const target = {x: 1, y: 2};
const proxy = new Proxy(target, {
    preventExtensions(object) { effects.push('preventExtensions'); return Reflect.preventExtensions(object); },
    ownKeys(object) { effects.push('ownKeys'); return Reflect.ownKeys(object); },
    getOwnPropertyDescriptor(object, key) { effects.push('get:' + key); return Reflect.getOwnPropertyDescriptor(object, key); },
    defineProperty(object, key, descriptor) {
        effects.push('define:' + key);
        return Reflect.defineProperty(object, key, descriptor);
    }
});
Object.freeze(proxy);
assertSame('preventExtensions,ownKeys,get:x,define:x,get:y,define:y', effects.join(','));
assertTrue(Object.isFrozen(target));

let definitions = 0;
const partial = {x: 1, y: 2, z: 3};
const failure = new Error('stop');
try {
    Object.freeze(new Proxy(partial, {
        defineProperty(object, key, descriptor) {
            definitions++;
            if (key === 'y') {
                throw failure;
            }
            return Reflect.defineProperty(object, key, descriptor);
        }
    }));
    throw new Error('Expected freeze to throw');
} catch (error) {
    assertSame(failure, error);
}
assertSame(2, definitions);
assertFalse(Object.getOwnPropertyDescriptor(partial, 'x').writable);
assertTrue(Object.getOwnPropertyDescriptor(partial, 'z').writable);

const config = JSON.parse('{"variables":{"a":1,"b":true},"list":["x","y"]}', (_key, value) => Object.freeze(value));
assertTrue(Object.isFrozen(config));
assertTrue(Object.isFrozen(config.variables));
assertTrue(Object.isFrozen(config.list));
for (const value of [null, undefined, 1, 'text', true, Symbol('value')]) {
    assertSame(value, Object.freeze(value));
}

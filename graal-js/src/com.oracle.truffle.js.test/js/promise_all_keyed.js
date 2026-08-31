/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests Promise.allKeyed and Promise.allSettledKeyed.
 *
 * @option ecmascript-version=staging
 * @option unhandled-rejections=throw
 */

load('./assert.js');

async function testAllKeyed() {
    const symbol = Symbol('symbol');
    const input = {
        2: Promise.resolve('two'),
        first: Promise.resolve(1),
        second: 2,
        [symbol]: Promise.resolve(3),
    };
    Object.defineProperty(input, 'hidden', {value: Promise.resolve(4)});
    const inherited = {inherited: Promise.resolve(5)};
    Object.setPrototypeOf(input, inherited);

    const result = await Promise.allKeyed(input);
    assertSame(null, Object.getPrototypeOf(result));
    assertSame('two', result[2]);
    assertSame(1, result.first);
    assertSame(2, result.second);
    assertSame(3, result[symbol]);
    assertSame(false, 'hidden' in result);
    assertSame(false, 'inherited' in result);
    assertSame(4, Reflect.ownKeys(result).length);
    assertSame('2', Reflect.ownKeys(result)[0]);
    assertSame('first', Reflect.ownKeys(result)[1]);
    assertSame('second', Reflect.ownKeys(result)[2]);
    assertSame(symbol, Reflect.ownKeys(result)[3]);

    const empty = await Promise.allKeyed({});
    assertSame(null, Object.getPrototypeOf(empty));
    assertSame(0, Reflect.ownKeys(empty).length);

    const error = new Error('rejected');
    let caught;
    try {
        await Promise.allKeyed({ok: Promise.resolve(1), bad: Promise.reject(error)});
    } catch (e) {
        caught = e;
    }
    assertSame(error, caught);
}

async function testAllSettledKeyed() {
    const reason = {};
    const result = await Promise.allSettledKeyed({
        fulfilled: Promise.resolve(42),
        rejected: Promise.reject(reason),
    });

    assertSame(null, Object.getPrototypeOf(result));
    assertSame(Object.prototype, Object.getPrototypeOf(result.fulfilled));
    assertSame('fulfilled', result.fulfilled.status);
    assertSame(42, result.fulfilled.value);
    assertSame(2, Reflect.ownKeys(result.fulfilled).length);
    assertSame('rejected', result.rejected.status);
    assertSame(reason, result.rejected.reason);
    assertSame(2, Reflect.ownKeys(result.rejected).length);
}

async function testPropertySemantics() {
    const log = [];
    const input = new Proxy({
        get first() {
            log.push('get first');
            return 1;
        },
        get second() {
            log.push('get second');
            return 2;
        },
    }, {
        ownKeys(target) {
            log.push('ownKeys');
            return Reflect.ownKeys(target);
        },
        getOwnPropertyDescriptor(target, key) {
            log.push(`descriptor ${key}`);
            return Reflect.getOwnPropertyDescriptor(target, key);
        },
    });

    const result = await Promise.allKeyed(input);
    assertSame(1, result.first);
    assertSame(2, result.second);
    assertSame('ownKeys,descriptor first,get first,descriptor second,get second', log.join(','));
}

async function testConstructorSemantics() {
    let resolveThis;
    class PromiseSubclass extends Promise {
        static resolve(value) {
            resolveThis = this;
            return super.resolve(value);
        }
    }

    const resultPromise = Promise.allKeyed.call(PromiseSubclass, {value: 42});
    assertSame(true, resultPromise instanceof PromiseSubclass);
    const result = await resultPromise;
    assertSame(PromiseSubclass, resolveThis);
    assertSame(42, result.value);

    let resolveGetterCalled = false;
    class ThrowingResolve extends Promise {}
    Object.defineProperty(ThrowingResolve, 'resolve', {
        get() {
            resolveGetterCalled = true;
            throw 42;
        },
    });
    const rejected = Promise.allKeyed.call(ThrowingResolve, null);
    assertSame(true, rejected instanceof ThrowingResolve);
    let reason;
    try {
        await rejected;
    } catch (e) {
        reason = e;
    }
    assertSame(true, resolveGetterCalled);
    assertSame(42, reason);
}

async function testAlreadyCalled() {
    class RepeatedFulfillmentPromise extends Promise {
        static resolve() {
            return {
                then(resolve) {
                    resolve('first');
                    resolve('second');
                },
            };
        }
    }

    const all = await Promise.allKeyed.call(RepeatedFulfillmentPromise, {value: 0});
    assertSame('first', all.value);

    class AdversarialSettledPromise extends Promise {
        static resolve() {
            return {
                then(resolve, reject) {
                    resolve('first');
                    reject('second');
                    resolve('third');
                },
            };
        }
    }

    const settled = await Promise.allSettledKeyed.call(AdversarialSettledPromise, {value: 0});
    assertSame('fulfilled', settled.value.status);
    assertSame('first', settled.value.value);
}

(async function main() {
    try {
        await testAllKeyed();
        await testAllSettledKeyed();
        await testPropertySemantics();
        await testConstructorSemantics();
        await testAlreadyCalled();
    } catch (error) {
        console.error(error.stack ?? error);
        throw error;
    }
})();

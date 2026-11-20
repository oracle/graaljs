/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * A `using` declaration must not drop the value of the block that ends its scope.
 *
 * @option explicit-resource-management
 * @option unhandled-rejections=throw
 */

load('../assert.js');

const disposed = [];
const resource = (name) => ({ [Symbol.dispose]() { disposed.push(name); } });

function body() { using x = resource('body'); return 'body'; }
function nested() { { using x = resource('nested'); return 'nested'; } }
function early(flag) { using x = resource('early'); if (flag) return 'early'; const y = 1; return y + 1; }
function none() { using x = resource('none'); }
const arrow = () => { using x = resource('arrow'); return 'arrow'; };
function* gen() { using x = resource('gen'); yield 1; return 'gen'; }
function loop() { for (using x of [resource('loop'), resource('loop')]) { } return 'loop'; }

assertSame('body', body());
assertSame('nested', nested());
assertSame('early', early(true));
assertSame(2, early(false));
assertSame(undefined, none());
assertSame('arrow', arrow());
const it = gen();
assertSame(1, it.next().value);
assertSame('gen', it.next().value);
assertSame('loop', loop());
assertThrows(() => { using x = resource('throw'); throw new TypeError('boom'); }, TypeError);
assertSame('body,nested,early,early,none,arrow,gen,loop,loop,throw', disposed.join(','));

// Ordinary try/finally and generator abrupt completions must keep their semantics.
function ordinaryFinally() { try { return 41; } finally { disposed.push('finally'); } }
assertSame(41, ordinaryFinally());
function* yieldingFinally() { try { return 42; } finally { yield 'first'; yield 'second'; } }
const yielding = yieldingFinally();
assertSame('first', yielding.next().value);
assertSame('second', yielding.next().value);
assertSame(42, yielding.next().value);
const closed = gen();
closed.next();
assertSame('closed', closed.return('closed').value);
const thrown = gen();
thrown.next();
assertThrows(() => thrown.throw(new RangeError('generator')), RangeError);

const loopDisposals = [];
for (const i of [0, 1, 2]) {
    using x = { [Symbol.dispose]() { loopDisposals.push(i); } };
    if (i === 0) continue;
    break;
}
assertSame('0,1', loopDisposals.join(','));

async function asyncCases() {
    let resumed = 0;
    async function awaitUsing(value) {
        await using first = { async [Symbol.asyncDispose]() { await null; resumed++; await null; resumed++; } };
        await using second = { async [Symbol.asyncDispose]() { await null; resumed++; } };
        return value;
    }
    const object = {};
    for (const value of ['async', undefined, null, false, 0, object]) {
        assertSame(value, await awaitUsing(value));
    }
    assertSame(18, resumed);

    const bodyError = new TypeError('body');
    const disposeError = new RangeError('dispose');
    async function rejectedDisposal() {
        await using x = { async [Symbol.asyncDispose]() { await null; throw disposeError; } };
        return 99;
    }
    let caught;
    try { await rejectedDisposal(); } catch (error) { caught = error; }
    assertSame(disposeError, caught);

    async function bothThrow() {
        await using x = { async [Symbol.asyncDispose]() { await null; throw disposeError; } };
        throw bodyError;
    }
    caught = undefined;
    try { await bothThrow(); } catch (error) { caught = error; }
    assertTrue(caught instanceof SuppressedError);
    assertSame(disposeError, caught.error);
    assertSame(bodyError, caught.suppressed);

    async function bodyThrows() {
        await using x = { async [Symbol.asyncDispose]() { await null; } };
        throw bodyError;
    }
    caught = undefined;
    try { await bodyThrows(); } catch (error) { caught = error; }
    assertSame(bodyError, caught);
}
asyncCases();
